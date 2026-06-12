package com.ssemi.sampleorder.service;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.model.ProductionQueue;
import com.ssemi.sampleorder.model.Sample;
import com.ssemi.sampleorder.model.StockStatus;
import com.ssemi.sampleorder.repository.OrderRepository;
import com.ssemi.sampleorder.repository.SampleRepository;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MonitorService 테스트")
class MonitorServiceTest {

    private FakeSampleRepository sampleRepo;
    private InMemoryOrderRepo orderRepo;
    private MonitorService monitorService;

    @BeforeEach
    void setUp() {
        sampleRepo = new FakeSampleRepository();
        orderRepo  = new InMemoryOrderRepo();
        monitorService = new MonitorService(orderRepo, sampleRepo);
    }

    private Order makeOrder(String id, String sampleId, int qty, OrderStatus status) {
        Order o = new Order(id, sampleId, "고객", qty);
        if (status != OrderStatus.RESERVED) {
            if (status == OrderStatus.CONFIRMED) o.transitionTo(OrderStatus.CONFIRMED);
            else if (status == OrderStatus.PRODUCING) o.transitionTo(OrderStatus.PRODUCING);
            else if (status == OrderStatus.REJECTED)  o.transitionTo(OrderStatus.REJECTED);
            else if (status == OrderStatus.RELEASED) {
                o.transitionTo(OrderStatus.CONFIRMED);
                o.transitionTo(OrderStatus.RELEASED);
            }
        }
        orderRepo.save(o);
        return o;
    }

    // ── Regression: 주문 집계 ─────────────────────────────────────────────────

    @Nested
    @DisplayName("주문 수 집계")
    class OrderCountMonitoring {

        @Test
        @DisplayName("상태별 주문 수 집계 정확성")
        void orderCountByStatusIsAccurate() {
            makeOrder("O1", "S001", 5, OrderStatus.RESERVED);
            makeOrder("O2", "S001", 5, OrderStatus.RESERVED);
            makeOrder("O3", "S001", 5, OrderStatus.CONFIRMED);
            makeOrder("O4", "S001", 5, OrderStatus.PRODUCING);

            Map<OrderStatus, Long> counts = monitorService.getOrderCountByStatus();

            assertEquals(2L, counts.get(OrderStatus.RESERVED));
            assertEquals(1L, counts.get(OrderStatus.CONFIRMED));
            assertEquals(1L, counts.get(OrderStatus.PRODUCING));
        }

        @Test
        @DisplayName("REJECTED 주문은 집계에서 제외")
        void rejectedOrdersExcludedFromMonitoring() {
            makeOrder("O1", "S001", 5, OrderStatus.RESERVED);
            makeOrder("O2", "S001", 5, OrderStatus.REJECTED);
            makeOrder("O3", "S001", 5, OrderStatus.REJECTED);

            Map<OrderStatus, Long> counts = monitorService.getOrderCountByStatus();

            assertEquals(1L, counts.getOrDefault(OrderStatus.RESERVED, 0L));
            assertEquals(0L, counts.getOrDefault(OrderStatus.REJECTED, 0L));
        }

        @Test
        @DisplayName("RELEASED 주문도 집계에 포함")
        void releasedOrdersIncludedInMonitoring() {
            makeOrder("O1", "S001", 5, OrderStatus.RELEASED);
            makeOrder("O2", "S001", 5, OrderStatus.RELEASED);

            Map<OrderStatus, Long> counts = monitorService.getOrderCountByStatus();

            assertEquals(2L, counts.getOrDefault(OrderStatus.RELEASED, 0L));
        }

        @Test
        @DisplayName("getOrdersByStatus → 해당 상태 목록 반환")
        void getOrdersByStatusReturnsCorrectList() {
            makeOrder("O1", "S001", 5, OrderStatus.RESERVED);
            makeOrder("O2", "S001", 5, OrderStatus.RESERVED);
            makeOrder("O3", "S001", 5, OrderStatus.CONFIRMED);

            List<Order> reserved = monitorService.getOrdersByStatus(OrderStatus.RESERVED);

            assertEquals(2, reserved.size());
            assertTrue(reserved.stream().allMatch(o -> o.getStatus() == OrderStatus.RESERVED));
        }

        @Test
        @DisplayName("주문 없을 때 모든 count == 0")
        void emptyRepositoryHasZeroCounts() {
            Map<OrderStatus, Long> counts = monitorService.getOrderCountByStatus();

            for (OrderStatus status : new OrderStatus[]{
                    OrderStatus.RESERVED, OrderStatus.CONFIRMED,
                    OrderStatus.PRODUCING, OrderStatus.RELEASED}) {
                assertEquals(0L, counts.getOrDefault(status, 0L));
            }
        }
    }

    // ── Regression: 재고 현황 ─────────────────────────────────────────────────

    @Nested
    @DisplayName("재고 현황 조회")
    class StockStatusMonitoring {

        @Test
        @DisplayName("stock==0 → DEPLETED")
        void zeroStockIsDepleted() {
            sampleRepo.save(new Sample("S001", "GaAs", 60, 0.9, 0));
            makeOrder("O1", "S001", 5, OrderStatus.PRODUCING);

            List<SampleStockInfo> result = monitorService.getStockStatusBySample();

            assertEquals(StockStatus.DEPLETED,
                    findBySampleId(result, "S001").getStockStatus());
        }

        @Test
        @DisplayName("stock < 총 PRODUCING 주문량 → SHORTAGE")
        void partialStockIsShortage() {
            sampleRepo.save(new Sample("S001", "GaAs", 60, 0.9, 3));
            makeOrder("O1", "S001", 5, OrderStatus.PRODUCING);

            List<SampleStockInfo> result = monitorService.getStockStatusBySample();

            assertEquals(StockStatus.SHORTAGE,
                    findBySampleId(result, "S001").getStockStatus());
        }

        @Test
        @DisplayName("stock >= 총 PRODUCING 주문량 → SUFFICIENT")
        void sufficientStockIsSufficient() {
            sampleRepo.save(new Sample("S001", "GaAs", 60, 0.9, 20));
            makeOrder("O1", "S001", 5, OrderStatus.PRODUCING);

            List<SampleStockInfo> result = monitorService.getStockStatusBySample();

            assertEquals(StockStatus.SUFFICIENT,
                    findBySampleId(result, "S001").getStockStatus());
        }

        @Test
        @DisplayName("다중 시료 각각 StockStatus 정확 판정")
        void multiSampleStockStatusAccuracy() {
            sampleRepo.save(new Sample("S001", "DEPLETED시료", 60, 0.9, 0));
            sampleRepo.save(new Sample("S002", "SHORTAGE시료", 60, 0.9, 3));
            sampleRepo.save(new Sample("S003", "SUFFICIENT시료", 60, 0.9, 20));
            makeOrder("O1", "S001", 5, OrderStatus.PRODUCING);
            makeOrder("O2", "S002", 5, OrderStatus.PRODUCING);
            makeOrder("O3", "S003", 5, OrderStatus.PRODUCING);

            List<SampleStockInfo> result = monitorService.getStockStatusBySample();

            assertEquals(StockStatus.DEPLETED,  findBySampleId(result, "S001").getStockStatus());
            assertEquals(StockStatus.SHORTAGE,   findBySampleId(result, "S002").getStockStatus());
            assertEquals(StockStatus.SUFFICIENT, findBySampleId(result, "S003").getStockStatus());
        }

        private SampleStockInfo findBySampleId(List<SampleStockInfo> list, String sampleId) {
            return list.stream()
                    .filter(i -> i.getSampleId().equals(sampleId))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("SampleStockInfo not found: " + sampleId));
        }
    }

    // ── Safety: 유효성 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("유효성 검사")
    class Validation {

        @Test
        @DisplayName("getOrdersByStatus(null) → IllegalArgumentException")
        void getOrdersByStatusNullThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> monitorService.getOrdersByStatus(null));
        }

        @Test
        @DisplayName("getOrdersByStatus(REJECTED) → IllegalArgumentException (모니터링 대상 외)")
        void getOrdersByStatusRejectedThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> monitorService.getOrdersByStatus(OrderStatus.REJECTED));
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
