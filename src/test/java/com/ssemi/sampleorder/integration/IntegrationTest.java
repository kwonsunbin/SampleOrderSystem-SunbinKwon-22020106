package com.ssemi.sampleorder.integration;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.model.ProductionQueue;
import com.ssemi.sampleorder.model.Sample;
import com.ssemi.sampleorder.repository.OrderRepository;
import com.ssemi.sampleorder.repository.SampleRepository;
import com.ssemi.sampleorder.repository.json.JsonOrderRepository;
import com.ssemi.sampleorder.repository.json.JsonSampleRepository;
import com.ssemi.sampleorder.service.MonitorService;
import com.ssemi.sampleorder.service.OrderService;
import com.ssemi.sampleorder.service.ProductionService;
import com.ssemi.sampleorder.service.SampleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {

    private static final String TEST_DIR = "data/test/integration/";

    private SampleRepository sampleRepo;
    private OrderRepository orderRepo;
    private SampleService sampleService;
    private OrderService orderService;
    private ProductionService productionService;
    private MonitorService monitorService;
    private ProductionQueue productionQueue;

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(Path.of(TEST_DIR));

        sampleRepo = new JsonSampleRepository(TEST_DIR + "samples.json");
        orderRepo = new JsonOrderRepository(TEST_DIR + "orders.json");
        productionQueue = new ProductionQueue();

        sampleService = new SampleService(sampleRepo);
        orderService = new OrderService(orderRepo, sampleRepo, productionQueue);
        productionService = new ProductionService(orderRepo, sampleRepo, productionQueue);
        monitorService = new MonitorService(orderRepo, sampleRepo);
    }

    @AfterEach
    void tearDown() throws Exception {
        Path dir = Path.of(TEST_DIR);
        if (Files.exists(dir)) {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    // =========================================================================
    // Scenario 1: 재고 부족 → PRODUCING 진입
    // =========================================================================

    @Nested
    @DisplayName("Scenario1: 재고 부족 → PRODUCING 진입")
    class Scenario1_StockZeroToProducing {

        Sample sample;
        Order order;

        @BeforeEach
        void setUpScenario() {
            sample = sampleService.register("GaAs", 120, 0.9, 0);
            order = orderService.reserveOrder(sample.getId(), "고객A", 5);
        }

        @Test
        @DisplayName("재고 0에서 승인 시 PRODUCING 상태로 전이된다")
        void stockZeroApproveTransitionsToProducing() {
            Order result = orderService.approveOrder(order.getId());
            assertEquals(OrderStatus.PRODUCING, result.getStatus());
        }

        @Test
        @DisplayName("재고 0에서 승인 시 주문이 생산 큐에 삽입된다")
        void stockZeroApproveEnqueuesOrderInQueue() {
            orderService.approveOrder(order.getId());
            assertEquals(1, productionQueue.size());
        }

        @Test
        @DisplayName("재고 0에서 승인 후 시료 재고는 0으로 유지된다")
        void stockZeroApproveDoesNotReduceStock() {
            orderService.approveOrder(order.getId());
            assertEquals(0, sampleService.listSamples().get(0).getStock());
        }
    }

    // =========================================================================
    // Scenario 2: 생산라인 실행 → 출고 → RELEASED
    // =========================================================================

    @Nested
    @DisplayName("Scenario2: 생산라인 실행 → 출고 → RELEASED")
    class Scenario2_ProductionToRelease {

        Sample sample;
        Order order;

        @BeforeEach
        void setUpScenario() {
            sample = sampleService.register("GaAs", 120, 0.9, 0);
            order = orderService.reserveOrder(sample.getId(), "고객B", 5);
            orderService.approveOrder(order.getId());
        }

        @Test
        @DisplayName("processNext() 호출 시 주문이 CONFIRMED 상태로 전이된다")
        void processNextTransitionsToConfirmed() {
            Order result = productionService.processNext();
            assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        }

        @Test
        @DisplayName("processNext() 호출 후 시료 재고는 음수가 아니다")
        void processNextUpdatesStockNonNegative() {
            productionService.processNext();
            assertTrue(sampleRepo.findById(sample.getId()).get().getStock() >= 0);
        }

        @Test
        @DisplayName("CONFIRMED 주문을 출고 처리하면 RELEASED 상태로 전이된다")
        void releaseOrderTransitionsToReleased() {
            productionService.processNext();
            Order released = orderService.releaseOrder(order.getId());
            assertEquals(OrderStatus.RELEASED, released.getStatus());
        }

        @Test
        @DisplayName("출고된 주문은 Repository에 RELEASED 상태로 영속된다")
        void releasedOrderPersistedInRepository() {
            productionService.processNext();
            orderService.releaseOrder(order.getId());
            assertEquals(OrderStatus.RELEASED, orderRepo.findById(order.getId()).get().getStatus());
        }
    }

    // =========================================================================
    // Scenario 3: 재고 충분 → 즉시 CONFIRMED
    // =========================================================================

    @Nested
    @DisplayName("Scenario3: 재고 충분 → 즉시 CONFIRMED")
    class Scenario3_SufficientStockToConfirmed {

        Sample sample;
        Order order;

        @BeforeEach
        void setUpScenario() {
            sample = sampleService.register("GaAs", 120, 0.9, 100);
            order = orderService.reserveOrder(sample.getId(), "고객C", 10);
        }

        @Test
        @DisplayName("재고 충분 시 승인하면 즉시 CONFIRMED 상태로 전이된다")
        void sufficientStockApproveTransitionsToConfirmed() {
            Order result = orderService.approveOrder(order.getId());
            assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        }

        @Test
        @DisplayName("재고 충분 시 승인하면 시료 재고가 주문 수량만큼 차감된다")
        void sufficientStockApproveReducesStock() {
            orderService.approveOrder(order.getId());
            assertEquals(90, sampleRepo.findById(sample.getId()).get().getStock());
        }

        @Test
        @DisplayName("재고 충분 시 승인하면 생산 큐에 주문이 삽입되지 않는다")
        void sufficientStockApproveDoesNotEnqueueQueue() {
            orderService.approveOrder(order.getId());
            assertTrue(productionQueue.isEmpty());
        }
    }

    // =========================================================================
    // Scenario 4: 주문 거절 → 모니터링 미표시
    // =========================================================================

    @Nested
    @DisplayName("Scenario4: 주문 거절 → 모니터링 미표시")
    class Scenario4_RejectedOrderNotInMonitoring {

        Sample sample;
        Order order;

        @BeforeEach
        void setUpScenario() {
            sample = sampleService.register("GaAs", 120, 0.9, 0);
            order = orderService.reserveOrder(sample.getId(), "고객D", 1);
        }

        @Test
        @DisplayName("거절된 주문은 REJECTED 상태로 전이된다")
        void rejectOrderTransitionsToRejected() {
            Order result = orderService.rejectOrder(order.getId());
            assertEquals(OrderStatus.REJECTED, result.getStatus());
        }

        @Test
        @DisplayName("거절된 주문은 모니터링 상태 맵의 키로 포함되지 않는다")
        void rejectedOrderNotInMonitoringMap() {
            orderService.rejectOrder(order.getId());
            assertFalse(monitorService.getOrderCountByStatus().containsKey(OrderStatus.REJECTED));
        }

        @Test
        @DisplayName("거절된 주문만 있을 때 모니터링 총 건수는 0이다")
        void monitoringTotalIsZeroWhenOnlyRejected() {
            orderService.rejectOrder(order.getId());
            long total = monitorService.getOrderCountByStatus()
                    .values().stream()
                    .mapToLong(Long::longValue)
                    .sum();
            assertEquals(0L, total);
        }
    }

    // =========================================================================
    // Scenario 5: 영속성 — 재시작 후 데이터 유지
    // =========================================================================

    @Nested
    @DisplayName("Scenario5: 영속성 — 재시작 후 데이터 유지")
    class Scenario5_Persistence {

        @Test
        @DisplayName("시료 저장 후 동일 경로의 새 Repository로 조회하면 모든 필드가 동일하다")
        void samplePersistsAfterRestart() {
            SampleRepository sampleRepo1 = new JsonSampleRepository(TEST_DIR + "persist_samples.json");
            SampleService sampleService1 = new SampleService(sampleRepo1);
            Sample saved = sampleService1.register("GaAs", 120, 0.9, 50);

            SampleRepository sampleRepo2 = new JsonSampleRepository(TEST_DIR + "persist_samples.json");
            Sample loaded = sampleRepo2.findById(saved.getId()).orElseThrow();

            assertEquals(saved.getName(), loaded.getName());
            assertEquals(saved.getStock(), loaded.getStock());
            assertEquals(saved.getYield(), loaded.getYield(), 1e-9);
            assertEquals(saved.getAvgProductionTime(), loaded.getAvgProductionTime());
        }

        @Test
        @DisplayName("주문 저장 후 동일 경로의 새 Repository로 조회하면 customerName/quantity/status가 동일하다")
        void orderPersistsAfterRestart() {
            SampleRepository sampleRepo1 = new JsonSampleRepository(TEST_DIR + "persist_samples2.json");
            OrderRepository orderRepo1 = new JsonOrderRepository(TEST_DIR + "persist_orders2.json");
            ProductionQueue pq1 = new ProductionQueue();
            SampleService ss1 = new SampleService(sampleRepo1);
            OrderService os1 = new OrderService(orderRepo1, sampleRepo1, pq1);

            Sample s = ss1.register("GaAs", 120, 0.9, 0);
            Order reserved = os1.reserveOrder(s.getId(), "고객E", 3);

            OrderRepository orderRepo2 = new JsonOrderRepository(TEST_DIR + "persist_orders2.json");
            Order loaded = orderRepo2.findById(reserved.getId()).orElseThrow();

            assertEquals(reserved.getCustomerName(), loaded.getCustomerName());
            assertEquals(reserved.getQuantity(), loaded.getQuantity());
            assertEquals(reserved.getStatus(), loaded.getStatus());
        }

        @Test
        @DisplayName("CONFIRMED 상태의 주문은 동일 경로의 새 Repository에서도 CONFIRMED로 조회된다")
        void confirmedOrderStatusPersists() {
            SampleRepository sampleRepo1 = new JsonSampleRepository(TEST_DIR + "persist_samples3.json");
            OrderRepository orderRepo1 = new JsonOrderRepository(TEST_DIR + "persist_orders3.json");
            ProductionQueue pq1 = new ProductionQueue();
            SampleService ss1 = new SampleService(sampleRepo1);
            OrderService os1 = new OrderService(orderRepo1, sampleRepo1, pq1);

            Sample s = ss1.register("GaAs", 120, 0.9, 100);
            Order reserved = os1.reserveOrder(s.getId(), "고객F", 10);
            os1.approveOrder(reserved.getId());

            OrderRepository orderRepo2 = new JsonOrderRepository(TEST_DIR + "persist_orders3.json");
            Order loaded = orderRepo2.findById(reserved.getId()).orElseThrow();

            assertEquals(OrderStatus.CONFIRMED, loaded.getStatus());
        }
    }

    // =========================================================================
    // Scenario 6: 수율 계산 하드 케이스
    // =========================================================================

    @Nested
    @DisplayName("Scenario6: 수율 하드 케이스")
    class Scenario6_YieldEdgeCases {

        // H1: overflow 버그 노출 — 현재 구현은 ArithmeticException을 던지지 않아 RED
        @Test
        @DisplayName("H1: 매우 낮은 수율과 큰 부족분에서 overflow 발생 시 ArithmeticException을 던진다 (RED)")
        void calculateActualProductionOverflowDetection() {
            assertThrows(ArithmeticException.class,
                    () -> productionService.calculateActualProduction(20_000_000, 0.01));
        }

        // H2: overflow 상황에서 processNext()가 예외를 던지고, 주문 상태는 PRODUCING 유지
        @Test
        @DisplayName("H2: overflow 발생 시 processNext()는 예외를 던지고 주문 상태는 PRODUCING으로 유지된다 (RED)")
        void processNextWithOverflowDoesNotTransitionToConfirmed() {
            Sample s = sampleService.register("LowYield", 60, 0.01, 0);
            Order o = orderService.reserveOrder(s.getId(), "고객G", 20_000_000);
            orderService.approveOrder(o.getId());

            assertThrows(Exception.class, () -> productionService.processNext());

            Order persisted = orderRepo.findById(o.getId()).orElseThrow();
            assertEquals(OrderStatus.PRODUCING, persisted.getStatus());
        }

        // H3: GREEN fix 후 ArithmeticException을 정식으로 던지도록 수정된 회귀 검증 (RED → GREEN)
        @Test
        @DisplayName("H3: calculateActualProduction은 overflow 발생 시 ArithmeticException을 던진다 (GREEN fix 후 통과)")
        void calculateActualProductionThrowsArithmeticExceptionOnOverflow() {
            assertThrows(ArithmeticException.class,
                    () -> productionService.calculateActualProduction(20_000_000, 0.01));
        }

        // H4: 정상 케이스는 fix 후에도 올바른 값을 반환한다
        @Test
        @DisplayName("H4: 정상 수율/부족분에서 ceil(10 / 0.81) = 13을 반환한다")
        void calculateActualProductionNormalCaseRemainsCorrect() {
            assertEquals(13, productionService.calculateActualProduction(10, 0.9));
        }

        // H5: 부족분=1, 최대 수율(1.0) 경계
        @Test
        @DisplayName("H5: 부족분 1, 수율 1.0일 때 실 생산량은 1 이상이다")
        void calculateActualProductionShortageOneMaxYield() {
            int result = productionService.calculateActualProduction(1, 1.0);
            assertTrue(result >= 1, "실 생산량은 부족분 이상이어야 함");
        }

        // H6: 부족분=1, 최소 수율(0.01) 조건에서 processNext()가 데드락 없이 CONFIRMED 반환
        @Test
        @DisplayName("H6: 부족분 1, 수율 0.01 조건에서 processNext()가 CONFIRMED를 반환하고 재고가 음수가 아니다")
        void processNextShortageOneMinYieldNoDeadlock() {
            Sample s = sampleService.register("MinYield", 60, 0.01, 0);
            Order o = orderService.reserveOrder(s.getId(), "고객H", 1);
            orderService.approveOrder(o.getId());

            Order processed = productionService.processNext();

            assertEquals(OrderStatus.CONFIRMED, processed.getStatus());
            assertTrue(sampleRepo.findById(s.getId()).get().getStock() >= 0);
        }

        // H7: 재고가 음수로 떨어지지 않는다
        @Test
        @DisplayName("H7: processNext() 처리 후 시료 재고는 절대 음수가 아니다")
        void processNextStockNeverGoesNegative() {
            Sample s = sampleService.register("MidYield", 60, 0.5, 0);
            Order o = orderService.reserveOrder(s.getId(), "고객I", 5);
            orderService.approveOrder(o.getId());

            productionService.processNext();

            assertTrue(sampleRepo.findById(s.getId()).get().getStock() >= 0);
        }
    }
}
