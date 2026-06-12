package com.ssemi.sampleorder.service;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.model.Sample;
import com.ssemi.sampleorder.repository.OrderRepository;
import com.ssemi.sampleorder.repository.SampleRepository;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DataInitializer 테스트")
class DataInitializerTest {

    private FakeSampleRepository sampleRepo;
    private FakeOrderRepository  orderRepo;
    private DataInitializer      initializer;

    @BeforeEach
    void setUp() {
        sampleRepo  = new FakeSampleRepository();
        orderRepo   = new FakeOrderRepository();
        initializer = new DataInitializer(sampleRepo, orderRepo);
    }

    @Nested
    @DisplayName("seedSamples")
    class SeedSamples {

        @Test
        @DisplayName("samples가 이미 존재하면 시드를 삽입하지 않는다")
        void seedSamples_doesNothing_whenSamplesAlreadyExist() {
            sampleRepo.save(new Sample("X001", "기존시료", 60, 0.9, 10));

            initializer.seedSamples();

            assertEquals(1, sampleRepo.findAll().size());
        }

        @Test
        @DisplayName("samples가 비어 있으면 5개 시드를 삽입한다")
        void seedSamples_inserts5Seeds_whenSamplesEmpty() {
            initializer.seedSamples();

            List<Sample> all = sampleRepo.findAll();
            assertEquals(5, all.size());
        }

        @Test
        @DisplayName("시드 ID는 S001~S005 이다")
        void seedSamples_hasCorrectIds() {
            initializer.seedSamples();

            List<String> ids = sampleRepo.findAll().stream()
                    .map(Sample::getId)
                    .sorted()
                    .collect(Collectors.toList());
            assertEquals(List.of("S001", "S002", "S003", "S004", "S005"), ids);
        }
    }

    @Nested
    @DisplayName("clearOrders")
    class ClearOrders {

        @Test
        @DisplayName("orders가 있으면 전부 삭제한다")
        void clearOrders_removesAll_whenOrdersExist() {
            orderRepo.save(new Order("ORD-0001", "S001", "고객A", 10));
            orderRepo.save(new Order("ORD-0002", "S002", "고객B", 5));
            orderRepo.save(new Order("ORD-0003", "S003", "고객C", 3));

            initializer.clearOrders();

            assertTrue(orderRepo.findAll().isEmpty());
        }

        @Test
        @DisplayName("orders가 이미 비어 있어도 예외 없이 완료된다")
        void clearOrders_noOp_whenOrdersAlreadyEmpty() {
            assertDoesNotThrow(() -> initializer.clearOrders());
            assertTrue(orderRepo.findAll().isEmpty());
        }
    }

    // ── Fake Repositories ────────────────────────────────────────────────────

    static class FakeSampleRepository implements SampleRepository {
        private final Map<String, Sample> store = new LinkedHashMap<>();
        @Override public void save(Sample s) { store.put(s.getId(), s); }
        @Override public Optional<Sample> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Sample> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<Sample> findByName(String k) { return List.of(); }
        @Override public void delete(String id) { store.remove(id); }
    }

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
}
