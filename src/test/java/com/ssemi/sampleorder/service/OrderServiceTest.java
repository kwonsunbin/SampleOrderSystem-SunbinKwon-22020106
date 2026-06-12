package com.ssemi.sampleorder.service;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.model.ProductionQueue;
import com.ssemi.sampleorder.model.Sample;
import com.ssemi.sampleorder.model.StockStatus;
import com.ssemi.sampleorder.repository.OrderRepository;
import com.ssemi.sampleorder.repository.SampleRepository;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderService 테스트")
class OrderServiceTest {

    private FakeSampleRepository sampleRepo;
    private InMemoryOrderRepo orderRepo;
    private ProductionQueue queue;
    private OrderService orderService;

    private String sampleId;

    @BeforeEach
    void setUp() {
        sampleRepo = new FakeSampleRepository();
        orderRepo  = new InMemoryOrderRepo();
        queue      = new ProductionQueue();
        orderService = new OrderService(orderRepo, sampleRepo, queue);

        // 기본 시료 등록 (stock=50)
        Sample sample = new Sample("S001", "GaAs 웨이퍼", 60, 0.9, 50);
        sampleRepo.save(sample);
        sampleId = "S001";
    }

    // ── Regression: 주문 접수 ─────────────────────────────────────────────────

    @Nested
    @DisplayName("주문 접수")
    class ReserveOrder {

        @Test
        @DisplayName("주문 접수 성공 → status == RESERVED")
        void reserveOrderStatusIsReserved() {
            Order order = orderService.reserveOrder(sampleId, "고객A", 10);

            assertEquals(OrderStatus.RESERVED, order.getStatus());
        }

        @Test
        @DisplayName("접수된 주문은 Repository에 저장됨")
        void reserveOrderSavedToRepository() {
            Order order = orderService.reserveOrder(sampleId, "고객A", 10);

            assertTrue(orderRepo.findById(order.getId()).isPresent());
        }

        @Test
        @DisplayName("접수 주문 ID는 고유함")
        void reserveOrderGeneratesUniqueIds() {
            Order o1 = orderService.reserveOrder(sampleId, "고객A", 5);
            Order o2 = orderService.reserveOrder(sampleId, "고객B", 5);

            assertNotEquals(o1.getId(), o2.getId());
        }

        @Test
        @DisplayName("listReservedOrders → RESERVED 상태 주문만 반환")
        void listReservedOrdersReturnsOnlyReserved() {
            orderService.reserveOrder(sampleId, "고객A", 5);
            orderService.reserveOrder(sampleId, "고객B", 5);
            Order o3 = orderService.reserveOrder(sampleId, "고객C", 5);
            orderService.approveOrder(o3.getId()); // CONFIRMED 로 전환

            List<Order> reserved = orderService.listReservedOrders();

            assertEquals(2, reserved.size());
            assertTrue(reserved.stream().allMatch(o -> o.getStatus() == OrderStatus.RESERVED));
        }
    }

    // ── Regression: 재고 충분 시 승인 ─────────────────────────────────────────

    @Nested
    @DisplayName("재고 충분 시 승인 (즉시 CONFIRMED)")
    class ApproveWithSufficientStock {

        @Test
        @DisplayName("재고 충분 → status == CONFIRMED")
        void approveWithSufficientStockConfirms() {
            Order order = orderService.reserveOrder(sampleId, "고객A", 10);

            orderService.approveOrder(order.getId());

            assertEquals(OrderStatus.CONFIRMED,
                    orderRepo.findById(order.getId()).get().getStatus());
        }

        @Test
        @DisplayName("재고 충분 → 재고 정확히 차감됨")
        void approveDeductsStockExactly() {
            Order order = orderService.reserveOrder(sampleId, "고객A", 10);

            orderService.approveOrder(order.getId());

            assertEquals(40, sampleRepo.findById(sampleId).get().getStock());
        }

        @Test
        @DisplayName("재고 == 주문량 경계값 → CONFIRMED + stock == 0")
        void approveWhenStockExactlyEqualsDemand() {
            sampleRepo.save(new Sample("S002", "InP", 60, 0.9, 10));
            Order order = orderService.reserveOrder("S002", "고객A", 10);

            orderService.approveOrder(order.getId());

            assertEquals(OrderStatus.CONFIRMED,
                    orderRepo.findById(order.getId()).get().getStatus());
            assertEquals(0, sampleRepo.findById("S002").get().getStock());
        }

        @Test
        @DisplayName("첫 승인 후 재고 소진 → 두 번째 동일 시료 승인은 PRODUCING")
        void secondApprovalAfterStockDepletedGoesToProducing() {
            sampleRepo.save(new Sample("S002", "InP", 60, 0.9, 10));
            Order o1 = orderService.reserveOrder("S002", "고객A", 10);
            Order o2 = orderService.reserveOrder("S002", "고객B", 5);

            orderService.approveOrder(o1.getId()); // stock 0 됨
            orderService.approveOrder(o2.getId()); // 재고 부족 → PRODUCING

            assertEquals(OrderStatus.CONFIRMED,
                    orderRepo.findById(o1.getId()).get().getStatus());
            assertEquals(OrderStatus.PRODUCING,
                    orderRepo.findById(o2.getId()).get().getStatus());
        }
    }

    // ── Regression: 재고 부족 시 승인 ─────────────────────────────────────────

    @Nested
    @DisplayName("재고 부족 시 승인 (PRODUCING 전환)")
    class ApproveWithInsufficientStock {

        @Test
        @DisplayName("재고 부족 → status == PRODUCING")
        void approveWithInsufficientStockProduces() {
            sampleRepo.save(new Sample("S002", "InP", 60, 0.9, 5));
            Order order = orderService.reserveOrder("S002", "고객A", 10);

            orderService.approveOrder(order.getId());

            assertEquals(OrderStatus.PRODUCING,
                    orderRepo.findById(order.getId()).get().getStatus());
        }

        @Test
        @DisplayName("재고 부족 → ProductionQueue에 진입")
        void approveWithInsufficientStockEnqueues() {
            sampleRepo.save(new Sample("S002", "InP", 60, 0.9, 5));
            Order order = orderService.reserveOrder("S002", "고객A", 10);

            orderService.approveOrder(order.getId());

            assertEquals(1, queue.size());
        }

        @Test
        @DisplayName("재고 == 0 경계값 → PRODUCING")
        void approveWhenStockIsZeroProduces() {
            sampleRepo.save(new Sample("S002", "InP", 60, 0.9, 0));
            Order order = orderService.reserveOrder("S002", "고객A", 10);

            orderService.approveOrder(order.getId());

            assertEquals(OrderStatus.PRODUCING,
                    orderRepo.findById(order.getId()).get().getStatus());
        }

        @Test
        @DisplayName("재고 부족 시 재고 차감 없음")
        void approveWithInsufficientStockDoesNotDeductStock() {
            sampleRepo.save(new Sample("S002", "InP", 60, 0.9, 5));
            Order order = orderService.reserveOrder("S002", "고객A", 10);

            orderService.approveOrder(order.getId());

            assertEquals(5, sampleRepo.findById("S002").get().getStock());
        }
    }

    // ── Regression: 주문 거절 ─────────────────────────────────────────────────

    @Nested
    @DisplayName("주문 거절")
    class RejectOrder {

        @Test
        @DisplayName("거절 → status == REJECTED")
        void rejectOrderSetsRejected() {
            Order order = orderService.reserveOrder(sampleId, "고객A", 10);

            orderService.rejectOrder(order.getId());

            assertEquals(OrderStatus.REJECTED,
                    orderRepo.findById(order.getId()).get().getStatus());
        }

        @Test
        @DisplayName("거절 시 재고 변화 없음")
        void rejectOrderDoesNotChangeStock() {
            Order order = orderService.reserveOrder(sampleId, "고객A", 10);

            orderService.rejectOrder(order.getId());

            assertEquals(50, sampleRepo.findById(sampleId).get().getStock());
        }
    }

    // ── Safety: 유효성 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("유효성 검사")
    class Validation {

        @Test
        @DisplayName("reserveOrder null customerName → IllegalArgumentException")
        void reserveWithNullCustomerNameThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> orderService.reserveOrder(sampleId, null, 10));
        }

        @Test
        @DisplayName("reserveOrder quantity ≤ 0 → IllegalArgumentException")
        void reserveWithZeroQuantityThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> orderService.reserveOrder(sampleId, "고객A", 0));
        }

        @Test
        @DisplayName("approveOrder 존재하지 않는 ID → IllegalArgumentException")
        void approveNonExistentOrderThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> orderService.approveOrder("NOTEXIST"));
        }

        @Test
        @DisplayName("approveOrder(null) → IllegalArgumentException")
        void approveNullIdThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> orderService.approveOrder(null));
        }

        @Test
        @DisplayName("rejectOrder 존재하지 않는 ID → IllegalArgumentException")
        void rejectNonExistentOrderThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> orderService.rejectOrder("NOTEXIST"));
        }

        @Test
        @DisplayName("rejectOrder(null) → IllegalArgumentException")
        void rejectNullIdThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> orderService.rejectOrder(null));
        }

        @Test
        @DisplayName("RESERVED가 아닌 주문 승인 시도 → IllegalStateException")
        void approveNonReservedOrderThrows() {
            Order order = orderService.reserveOrder(sampleId, "고객A", 10);
            orderService.approveOrder(order.getId()); // CONFIRMED 로 전환

            assertThrows(IllegalStateException.class,
                    () -> orderService.approveOrder(order.getId()));
        }

        @Test
        @DisplayName("RESERVED가 아닌 주문 거절 시도 → IllegalStateException")
        void rejectNonReservedOrderThrows() {
            Order order = orderService.reserveOrder(sampleId, "고객A", 10);
            orderService.rejectOrder(order.getId()); // REJECTED 로 전환

            assertThrows(IllegalStateException.class,
                    () -> orderService.rejectOrder(order.getId()));
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
