package com.ssemi.sampleorder.service;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.repository.OrderRepository;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderIdGenerator 테스트")
class OrderIdGeneratorTest {

    private FakeOrderRepository orderRepo;
    private OrderIdGenerator    generator;

    @BeforeEach
    void setUp() {
        orderRepo = new FakeOrderRepository();
        generator = new OrderIdGenerator(orderRepo);
    }

    @Test
    @DisplayName("orders가 없으면 ORD-0001을 반환한다")
    void generate_returnsORD0001_whenNoOrders() {
        assertEquals("ORD-0001", generator.generate());
    }

    @Test
    @DisplayName("기존 최대 번호에서 1 증가한 ID를 반환한다")
    void generate_incrementsFromExisting() {
        orderRepo.save(new Order("ORD-0001", "S001", "고객A", 10));
        orderRepo.save(new Order("ORD-0003", "S002", "고객B", 5));

        assertEquals("ORD-0004", generator.generate());
    }

    @Test
    @DisplayName("UUID 형식 ID가 혼재해도 ORD 기준으로 증분한다")
    void generate_handlesNonORDIds_gracefully() {
        orderRepo.save(new Order("c3bcd5f2-434d-48f3-a7d1-397b3f9eb0b1", "S001", "고객A", 10));
        orderRepo.save(new Order("ORD-0002", "S002", "고객B", 5));

        assertEquals("ORD-0003", generator.generate());
    }

    @Test
    @DisplayName("UUID만 있고 ORD 형식이 없으면 ORD-0001을 반환한다")
    void generate_returnsORD0001_whenOnlyUUIDsExist() {
        orderRepo.save(new Order("c3bcd5f2-434d-48f3-a7d1-397b3f9eb0b1", "S001", "고객A", 10));

        assertEquals("ORD-0001", generator.generate());
    }

    @Test
    @DisplayName("reserveOrder가 반환하는 Order ID는 ORD-NNNN 패턴이다")
    void reserveOrder_usesShortId() {
        OrderService orderService = new OrderService(
                orderRepo,
                new FakeSampleRepository(),
                new com.ssemi.sampleorder.model.ProductionQueue(),
                generator
        );
        com.ssemi.sampleorder.model.Sample sample =
                new com.ssemi.sampleorder.model.Sample("S001", "웨이퍼", 60, 0.9, 50);
        FakeSampleRepository sampleRepo = new FakeSampleRepository();
        sampleRepo.save(sample);

        OrderService svc = new OrderService(orderRepo, sampleRepo,
                new com.ssemi.sampleorder.model.ProductionQueue(), generator);
        Order order = svc.reserveOrder("S001", "고객A", 5);

        assertTrue(order.getId().matches("ORD-\\d{4,}"),
                "ID는 ORD-NNNN 형식이어야 합니다. 실제: " + order.getId());
    }

    // ── Fake Repositories ────────────────────────────────────────────────────

    static class FakeOrderRepository implements OrderRepository {
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

    static class FakeSampleRepository implements com.ssemi.sampleorder.repository.SampleRepository {
        private final Map<String, com.ssemi.sampleorder.model.Sample> store = new LinkedHashMap<>();
        @Override public void save(com.ssemi.sampleorder.model.Sample s) { store.put(s.getId(), s); }
        @Override public Optional<com.ssemi.sampleorder.model.Sample> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<com.ssemi.sampleorder.model.Sample> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<com.ssemi.sampleorder.model.Sample> findByName(String k) { return List.of(); }
        @Override public void delete(String id) { store.remove(id); }
    }
}
