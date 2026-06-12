package com.ssemi.sampleorder.service;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.model.ProductionQueue;
import com.ssemi.sampleorder.model.Sample;
import com.ssemi.sampleorder.model.StockStatus;
import com.ssemi.sampleorder.repository.OrderRepository;
import com.ssemi.sampleorder.repository.SampleRepository;
import org.junit.jupiter.api.*;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProductionService 테스트")
class ProductionServiceTest {

    private FakeSampleRepository sampleRepo;
    private InMemoryOrderRepo orderRepo;
    private ProductionQueue queue;
    private ProductionService productionService;

    @BeforeEach
    void setUp() {
        sampleRepo = new FakeSampleRepository();
        orderRepo  = new InMemoryOrderRepo();
        queue      = new ProductionQueue();
        productionService = new ProductionService(orderRepo, sampleRepo, queue);
    }

    /** PRODUCING 상태 주문을 생성하고 큐에 삽입하는 헬퍼 */
    private Order enqueueProducingOrder(String orderId, String sampleId, int quantity) {
        Order order = new Order(orderId, sampleId, "테스트고객", quantity);
        order.transitionTo(OrderStatus.PRODUCING);
        orderRepo.save(order);
        queue.enqueue(order);
        return order;
    }

    // ── Regression: 수율 계산 공식 ───────────────────────────────────────────
    // 공식: Math.ceil(shortage / (yield * 0.9))

    @Nested
    @DisplayName("수율 계산 공식 — calculateActualProduction")
    class ActualProductionCalculation {

        @Test
        @DisplayName("기본 케이스: shortage=10, yield=0.9 → ceil(10/0.81) = 13")
        void basicCase() {
            assertEquals(13, productionService.calculateActualProduction(10, 0.9));
        }

        @Test
        @DisplayName("ceil 처리: shortage=1, yield=1.0 → ceil(1/0.9) = 2")
        void ceilProcessing() {
            assertEquals(2, productionService.calculateActualProduction(1, 1.0));
        }

        @Test
        @DisplayName("정확히 나누어 떨어짐: shortage=9, yield=1.0 → ceil(9/0.9) = 10")
        void exactDivision() {
            assertEquals(10, productionService.calculateActualProduction(9, 1.0));
        }

        @Test
        @DisplayName("낮은 수율: shortage=10, yield=0.5 → ceil(10/0.45) = 23")
        void lowYield() {
            assertEquals(23, productionService.calculateActualProduction(10, 0.5));
        }
    }

    // ── Regression: 총 생산시간 계산 ─────────────────────────────────────────

    @Nested
    @DisplayName("총 생산시간 계산 — calculateTotalProductionTime")
    class TotalProductionTimeCalculation {

        @Test
        @DisplayName("avgTime=60, actualProduction=13 → 780분")
        void standardCase() {
            assertEquals(780, productionService.calculateTotalProductionTime(60, 13));
        }

        @Test
        @DisplayName("avgTime=120, actualProduction=1 → 120분")
        void singleUnitProduction() {
            assertEquals(120, productionService.calculateTotalProductionTime(120, 1));
        }
    }

    // ── Regression: 생산라인 처리 ─────────────────────────────────────────────

    @Nested
    @DisplayName("생산라인 processNext 처리")
    class ProcessNext {

        @Test
        @DisplayName("processNext → 주문 status == CONFIRMED")
        void processNextConfirmsOrder() {
            // stock=0, qty=10, yield=1.0 → shortage=10, actual=12, finalStock=2
            sampleRepo.save(new Sample("S001", "GaAs", 60, 1.0, 0));
            Order order = enqueueProducingOrder("O001", "S001", 10);

            productionService.processNext();

            assertEquals(OrderStatus.CONFIRMED,
                    orderRepo.findById("O001").get().getStatus());
        }

        @Test
        @DisplayName("processNext → 재고 정확히 반영 (생산 후 주문량 차감)")
        void processNextUpdatesStockCorrectly() {
            // stock=0, qty=10, yield=1.0
            // shortage=10, actual=ceil(10/0.9)=12
            // newStock = 0 + 12 = 12, finalStock = 12 - 10 = 2
            sampleRepo.save(new Sample("S001", "GaAs", 60, 1.0, 0));
            enqueueProducingOrder("O001", "S001", 10);

            productionService.processNext();

            assertEquals(2, sampleRepo.findById("S001").get().getStock());
        }

        @Test
        @DisplayName("processNext 후 queue size 감소")
        void processNextReducesQueueSize() {
            sampleRepo.save(new Sample("S001", "GaAs", 60, 1.0, 0));
            sampleRepo.save(new Sample("S002", "InP", 60, 1.0, 0));
            enqueueProducingOrder("O001", "S001", 5);
            enqueueProducingOrder("O002", "S002", 5);

            productionService.processNext();

            assertEquals(1, queue.size());
        }

        @Test
        @DisplayName("FIFO 보장 — 먼저 접수된 주문이 먼저 처리됨")
        void processNextFollowsFifoOrder() {
            sampleRepo.save(new Sample("S001", "GaAs", 60, 1.0, 0));
            sampleRepo.save(new Sample("S002", "InP", 60, 1.0, 0));
            Order first  = enqueueProducingOrder("O001", "S001", 5);
            Order second = enqueueProducingOrder("O002", "S002", 5);

            productionService.processNext();

            // 첫 번째 주문만 CONFIRMED, 두 번째는 여전히 PRODUCING
            assertEquals(OrderStatus.CONFIRMED,
                    orderRepo.findById(first.getId()).get().getStatus());
            assertEquals(OrderStatus.PRODUCING,
                    orderRepo.findById(second.getId()).get().getStatus());
        }

        @Test
        @DisplayName("getQueueSnapshot → 현재 대기 목록 반환 (size 변화 없음)")
        void getQueueSnapshotReturnsCopyWithoutMutating() {
            sampleRepo.save(new Sample("S001", "GaAs", 60, 1.0, 0));
            sampleRepo.save(new Sample("S002", "InP", 60, 1.0, 0));
            sampleRepo.save(new Sample("S003", "SiC", 60, 1.0, 0));
            enqueueProducingOrder("O001", "S001", 5);
            enqueueProducingOrder("O002", "S002", 5);
            enqueueProducingOrder("O003", "S003", 5);

            List<Order> snapshot = productionService.getQueueSnapshot();

            assertEquals(3, snapshot.size());
            assertEquals(3, queue.size()); // 원본 큐 변화 없음
        }
    }

    // ── Safety: 유효성 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("유효성 검사")
    class Validation {

        @Test
        @DisplayName("빈 큐에서 processNext → IllegalStateException")
        void processNextOnEmptyQueueThrows() {
            assertThrows(IllegalStateException.class,
                    () -> productionService.processNext());
        }

        @Test
        @DisplayName("shortage == 0 → IllegalArgumentException")
        void zeroShortageThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> productionService.calculateActualProduction(0, 0.9));
        }

        @Test
        @DisplayName("shortage 음수 → IllegalArgumentException")
        void negativeShortageThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> productionService.calculateActualProduction(-1, 0.9));
        }

        @Test
        @DisplayName("yield == 0.0 → IllegalArgumentException")
        void zeroYieldThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> productionService.calculateActualProduction(10, 0.0));
        }
    }

    // ── NEW: startNextProduction ─────────────────────────────────────────────

    @Nested
    @DisplayName("startNextProduction — RESERVED 주문을 PRODUCING으로 전환")
    class StartNextProduction {

        /** 고정 Clock: 2026-06-12 09:00 */
        private Clock fixedClock() {
            Instant instant = LocalDateTime.of(2026, 6, 12, 9, 0)
                    .atZone(ZoneId.systemDefault()).toInstant();
            return Clock.fixed(instant, ZoneId.systemDefault());
        }

        /** RESERVED 주문을 큐에 삽입 */
        private Order enqueueReservedOrder(String orderId, String sampleId, int qty) {
            Order order = new Order(orderId, sampleId, "테스트고객", qty);
            orderRepo.save(order);
            queue.enqueue(order);
            return order;
        }

        @Test
        @DisplayName("큐 첫 번째 RESERVED 주문이 PRODUCING 상태로 전환됨")
        void startNextChangesStatusToProducing() {
            sampleRepo.save(new Sample("S001", "GaAs", 60, 1.0, 0));
            Order o = enqueueReservedOrder("O001", "S001", 10);

            productionService.startNextProduction(fixedClock());

            assertEquals(OrderStatus.PRODUCING, orderRepo.findById("O001").get().getStatus());
        }

        @Test
        @DisplayName("startNextProduction → productionStartedAt이 Clock 시각으로 기록됨")
        void startNextRecordsStartedAt() {
            sampleRepo.save(new Sample("S001", "GaAs", 60, 1.0, 0));
            enqueueReservedOrder("O001", "S001", 10);
            Clock clock = fixedClock();

            productionService.startNextProduction(clock);

            LocalDateTime expected = LocalDateTime.now(clock);
            assertEquals(expected, orderRepo.findById("O001").get().getProductionStartedAt());
        }

        @Test
        @DisplayName("startNextProduction → totalProductionMinutes 계산값 기록됨 (shortage>0)")
        void startNextRecordsTotalProductionMinutes() {
            // stock=0, qty=10, yield=1.0 → shortage=10 → actual=ceil(10/0.9)=12 → total=60*12=720
            sampleRepo.save(new Sample("S001", "GaAs", 60, 1.0, 0));
            enqueueReservedOrder("O001", "S001", 10);

            productionService.startNextProduction(fixedClock());

            assertEquals(720, orderRepo.findById("O001").get().getTotalProductionMinutes());
        }

        @Test
        @DisplayName("startNextProduction → stock 충분 시 (shortage=0) totalMinutes = avgTime * qty")
        void startNextNoShortage() {
            // stock=20 >= qty=10 → shortage=0 → total=60*10=600
            sampleRepo.save(new Sample("S001", "GaAs", 60, 1.0, 20));
            enqueueReservedOrder("O001", "S001", 10);

            productionService.startNextProduction(fixedClock());

            assertEquals(600, orderRepo.findById("O001").get().getTotalProductionMinutes());
        }

        @Test
        @DisplayName("startNextProduction → 큐에서 제거됨 (queue.size 감소)")
        void startNextRemovesFromQueue() {
            sampleRepo.save(new Sample("S001", "GaAs", 60, 1.0, 0));
            enqueueReservedOrder("O001", "S001", 5);

            productionService.startNextProduction(fixedClock());

            assertEquals(0, queue.size());
        }

        @Test
        @DisplayName("큐가 비어있으면 IllegalStateException")
        void startNextOnEmptyQueueThrows() {
            assertThrows(IllegalStateException.class,
                    () -> productionService.startNextProduction(fixedClock()));
        }

        @Test
        @DisplayName("이미 PRODUCING 중인 주문이 있으면 IllegalStateException")
        void startNextWhileProducingThrows() {
            sampleRepo.save(new Sample("S001", "GaAs", 60, 1.0, 0));
            sampleRepo.save(new Sample("S002", "InP",  60, 1.0, 0));
            enqueueReservedOrder("O001", "S001", 5);
            enqueueReservedOrder("O002", "S002", 5);

            // 첫 번째는 성공
            productionService.startNextProduction(fixedClock());

            // 두 번째는 이미 PRODUCING 중이므로 예외
            assertThrows(IllegalStateException.class,
                    () -> productionService.startNextProduction(fixedClock()));
        }
    }

    // ── NEW: completeProductionIfReady ───────────────────────────────────────

    @Nested
    @DisplayName("completeProductionIfReady — 경과 시간 체크 후 CONFIRMED")
    class CompleteProductionIfReady {

        /** PRODUCING 주문을 startedAt + totalMinutes 와 함께 orderRepo에 직접 저장 */
        private Order saveProducingOrder(String orderId, String sampleId, int qty,
                                         LocalDateTime startedAt, int totalMinutes) {
            Order o = new Order(orderId, sampleId, "테스트고객", qty);
            o.transitionTo(OrderStatus.PRODUCING);
            o.startProduction(startedAt, totalMinutes);
            orderRepo.save(o);
            return o;
        }

        private Clock clockAt(LocalDateTime dt) {
            return Clock.fixed(dt.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        }

        @Test
        @DisplayName("경과 시간 < totalProductionMinutes → 상태 변화 없음 (여전히 PRODUCING)")
        void notReadyYetKeepsProducing() {
            sampleRepo.save(new Sample("S001", "GaAs", 60, 1.0, 0));
            LocalDateTime start = LocalDateTime.of(2026, 6, 12, 9, 0);
            saveProducingOrder("O001", "S001", 10, start, 60);

            // 30분 경과 — 아직 미완
            Clock clock = clockAt(start.plusMinutes(30));
            productionService.completeProductionIfReady(clock);

            assertEquals(OrderStatus.PRODUCING, orderRepo.findById("O001").get().getStatus());
        }

        @Test
        @DisplayName("경과 시간 == totalProductionMinutes → CONFIRMED 전환")
        void exactTimeCompletesProduction() {
            sampleRepo.save(new Sample("S001", "GaAs", 60, 1.0, 0));
            LocalDateTime start = LocalDateTime.of(2026, 6, 12, 9, 0);
            saveProducingOrder("O001", "S001", 10, start, 60);

            Clock clock = clockAt(start.plusMinutes(60));
            productionService.completeProductionIfReady(clock);

            assertEquals(OrderStatus.CONFIRMED, orderRepo.findById("O001").get().getStatus());
        }

        @Test
        @DisplayName("경과 시간 > totalProductionMinutes → CONFIRMED 전환")
        void overTimeCompletesProduction() {
            sampleRepo.save(new Sample("S001", "GaAs", 60, 1.0, 0));
            LocalDateTime start = LocalDateTime.of(2026, 6, 12, 9, 0);
            saveProducingOrder("O001", "S001", 10, start, 60);

            Clock clock = clockAt(start.plusMinutes(90));
            productionService.completeProductionIfReady(clock);

            assertEquals(OrderStatus.CONFIRMED, orderRepo.findById("O001").get().getStatus());
        }

        @Test
        @DisplayName("완료 시 재고 정확히 반영 (addStock → reduceStock)")
        void completionUpdatesStockCorrectly() {
            // stock=0, qty=10, yield=1.0 → shortage=10 → actual=12 → finalStock=2
            sampleRepo.save(new Sample("S001", "GaAs", 60, 1.0, 0));
            LocalDateTime start = LocalDateTime.of(2026, 6, 12, 9, 0);
            // totalMinutes=720 (12개 * 60분)
            saveProducingOrder("O001", "S001", 10, start, 720);

            Clock clock = clockAt(start.plusMinutes(720));
            productionService.completeProductionIfReady(clock);

            assertEquals(2, sampleRepo.findById("S001").get().getStock());
        }

        @Test
        @DisplayName("PRODUCING 주문 없으면 아무 것도 안 함 (예외 없음)")
        void noProducingOrderDoesNothing() {
            assertDoesNotThrow(() ->
                    productionService.completeProductionIfReady(
                            Clock.systemDefaultZone()));
        }
    }

    // ── NEW: getCurrentlyProducingInfo ───────────────────────────────────────

    @Nested
    @DisplayName("getCurrentlyProducingInfo — 진행 정보 반환")
    class GetCurrentlyProducingInfo {

        private Clock clockAt(LocalDateTime dt) {
            return Clock.fixed(dt.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        }

        private Order saveProducingOrder(String orderId, String sampleId, int qty,
                                          LocalDateTime startedAt, int totalMinutes) {
            Order o = new Order(orderId, sampleId, "테스트고객", qty);
            o.transitionTo(OrderStatus.PRODUCING);
            o.startProduction(startedAt, totalMinutes);
            orderRepo.save(o);
            return o;
        }

        @Test
        @DisplayName("PRODUCING 주문 없으면 Optional.empty() 반환")
        void noProducingOrderReturnsEmpty() {
            assertTrue(productionService.getCurrentlyProducingInfo(
                    Clock.systemDefaultZone()).isEmpty());
        }

        @Test
        @DisplayName("progressPercent = elapsed / total * 100 (50%)")
        void progressPercentFiftyPercent() {
            sampleRepo.save(new Sample("S001", "GaAs", 60, 1.0, 0));
            LocalDateTime start = LocalDateTime.of(2026, 6, 12, 9, 0);
            saveProducingOrder("O001", "S001", 10, start, 60);

            Clock clock = clockAt(start.plusMinutes(30));
            var info = productionService.getCurrentlyProducingInfo(clock).get();

            assertEquals(50, info.getProgressPercent());
        }

        @Test
        @DisplayName("elapsed > total 이면 progressPercent는 100으로 캡핑")
        void progressPercentCappedAt100() {
            sampleRepo.save(new Sample("S001", "GaAs", 60, 1.0, 0));
            LocalDateTime start = LocalDateTime.of(2026, 6, 12, 9, 0);
            saveProducingOrder("O001", "S001", 10, start, 60);

            Clock clock = clockAt(start.plusMinutes(90)); // 초과
            var info = productionService.getCurrentlyProducingInfo(clock).get();

            assertEquals(100, info.getProgressPercent());
        }

        @Test
        @DisplayName("estimatedCompletionTime = startedAt + totalProductionMinutes")
        void estimatedCompletionTimeIsCorrect() {
            sampleRepo.save(new Sample("S001", "GaAs", 60, 1.0, 0));
            LocalDateTime start = LocalDateTime.of(2026, 6, 12, 9, 0);
            saveProducingOrder("O001", "S001", 10, start, 60);

            Clock clock = clockAt(start.plusMinutes(10));
            var info = productionService.getCurrentlyProducingInfo(clock).get();

            assertEquals(start.plusMinutes(60), info.getEstimatedCompletionTime());
        }

        @Test
        @DisplayName("sampleName이 시료명으로 채워짐")
        void sampleNameIsPopulated() {
            sampleRepo.save(new Sample("S001", "GaAs 웨이퍼", 60, 1.0, 0));
            LocalDateTime start = LocalDateTime.of(2026, 6, 12, 9, 0);
            saveProducingOrder("O001", "S001", 10, start, 60);

            var info = productionService.getCurrentlyProducingInfo(clockAt(start)).get();

            assertEquals("GaAs 웨이퍼", info.getSampleName());
        }
    }

    // ── Fake Repositories ─────────────────────────────────────────────────────

    static class FakeSampleRepository implements SampleRepository {
        private final Map<String, Sample> store = new LinkedHashMap<>();
        @Override public void save(Sample s) { store.put(s.getId(), s); }
        @Override public Optional<Sample> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Sample> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<Sample> findByName(String k) { return List.of(); }
        @Override public void delete(String id) { store.remove(id); }
    }

    static class InMemoryOrderRepo implements OrderRepository {
        private final Map<String, Order> store = new LinkedHashMap<>();
        @Override public void save(Order o) { store.put(o.getId(), o); }
        @Override public Optional<Order> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Order> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<Order> findByStatus(OrderStatus s) {
            return store.values().stream().filter(o -> o.getStatus() == s).collect(Collectors.toList());
        }
        @Override public void delete(String id) { store.remove(id); }
        @Override public void deleteAll() { store.clear(); }
    }
}
