package com.ssemi.sampleorder.repository;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.repository.json.JsonOrderRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonOrderRepository 테스트")
class JsonOrderRepositoryTest {

    @TempDir
    Path tempDir;

    private JsonOrderRepository repository;

    private Order makeOrder(String id, String sampleId) {
        return new Order(id, sampleId, "테스트고객", 10);
    }

    @BeforeEach
    void setUp() {
        repository = new JsonOrderRepository(tempDir.resolve("orders.json").toString());
    }

    // ── Regression: Round-trip 직렬화 ────────────────────────────────────────

    @Nested
    @DisplayName("Round-trip 직렬화 및 영속성")
    class RoundTripSerialization {

        @Test
        @DisplayName("save 후 findById로 모든 필드 일치 조회")
        void saveAndFindById() {
            Order order = makeOrder("O001", "S001");
            repository.save(order);

            Optional<Order> found = repository.findById("O001");

            assertTrue(found.isPresent());
            assertEquals("O001", found.get().getId());
            assertEquals("S001", found.get().getSampleId());
            assertEquals("테스트고객", found.get().getCustomerName());
            assertEquals(10, found.get().getQuantity());
            assertEquals(OrderStatus.RESERVED, found.get().getStatus());
        }

        @Test
        @DisplayName("save 후 findAll에 포함됨")
        void saveAndFindAll() {
            repository.save(makeOrder("O001", "S001"));

            List<Order> all = repository.findAll();

            assertEquals(1, all.size());
            assertEquals("O001", all.get(0).getId());
        }

        @Test
        @DisplayName("복수 save 후 findAll 전체 반환")
        void saveMultipleAndFindAll() {
            repository.save(makeOrder("O001", "S001"));
            repository.save(makeOrder("O002", "S001"));
            repository.save(makeOrder("O003", "S002"));

            assertEquals(3, repository.findAll().size());
        }

        @Test
        @DisplayName("findAll 반환 순서는 저장 순서와 일치")
        void findAllPreservesInsertionOrder() {
            repository.save(makeOrder("O001", "S001"));
            repository.save(makeOrder("O002", "S001"));
            repository.save(makeOrder("O003", "S001"));

            List<Order> all = repository.findAll();

            assertEquals("O001", all.get(0).getId());
            assertEquals("O002", all.get(1).getId());
            assertEquals("O003", all.get(2).getId());
        }

        @Test
        @DisplayName("재시작 시뮬레이션 — 새 인스턴스로 findById 시 동일 필드 반환")
        void persistenceAcrossNewInstance() {
            String filePath = tempDir.resolve("orders.json").toString();
            JsonOrderRepository repo1 = new JsonOrderRepository(filePath);
            repo1.save(makeOrder("O001", "S001"));

            JsonOrderRepository repo2 = new JsonOrderRepository(filePath);
            Optional<Order> found = repo2.findById("O001");

            assertTrue(found.isPresent());
            assertEquals("테스트고객", found.get().getCustomerName());
            assertEquals(10, found.get().getQuantity());
            assertEquals(OrderStatus.RESERVED, found.get().getStatus());
        }

        @Test
        @DisplayName("상태 변경 후 save → 새 인스턴스에서 변경된 status 영속화 확인")
        void statusChangePersistedAfterSave() {
            String filePath = tempDir.resolve("orders.json").toString();
            JsonOrderRepository repo1 = new JsonOrderRepository(filePath);
            Order order = makeOrder("O001", "S001");
            order.transitionTo(OrderStatus.CONFIRMED);
            repo1.save(order);

            JsonOrderRepository repo2 = new JsonOrderRepository(filePath);
            Optional<Order> found = repo2.findById("O001");

            assertTrue(found.isPresent());
            assertEquals(OrderStatus.CONFIRMED, found.get().getStatus());
        }
    }

    // ── Regression: 상태별 필터 조회 ─────────────────────────────────────────

    @Nested
    @DisplayName("상태별 필터 조회")
    class StatusFilter {

        @Test
        @DisplayName("findByStatus(RESERVED) — RESERVED 상태만 반환")
        void findByStatusReserved() {
            repository.save(makeOrder("O001", "S001"));
            repository.save(makeOrder("O002", "S001"));
            Order confirmed = makeOrder("O003", "S001");
            confirmed.transitionTo(OrderStatus.CONFIRMED);
            repository.save(confirmed);

            List<Order> result = repository.findByStatus(OrderStatus.RESERVED);

            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(o -> o.getStatus() == OrderStatus.RESERVED));
        }

        @Test
        @DisplayName("findByStatus(CONFIRMED) — CONFIRMED 상태만 반환")
        void findByStatusConfirmed() {
            repository.save(makeOrder("O001", "S001"));
            Order c1 = makeOrder("O002", "S001");
            c1.transitionTo(OrderStatus.CONFIRMED);
            Order c2 = makeOrder("O003", "S001");
            c2.transitionTo(OrderStatus.CONFIRMED);
            repository.save(c1);
            repository.save(c2);

            List<Order> result = repository.findByStatus(OrderStatus.CONFIRMED);

            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(o -> o.getStatus() == OrderStatus.CONFIRMED));
        }

        @Test
        @DisplayName("혼합 상태 저장 후 각 findByStatus 카운트 정확성")
        void mixedStatusFindByStatusCounts() {
            Order reserved  = makeOrder("O001", "S001");
            Order producing = makeOrder("O002", "S001");
            producing.transitionTo(OrderStatus.PRODUCING);
            Order confirmed = makeOrder("O003", "S001");
            confirmed.transitionTo(OrderStatus.CONFIRMED);

            repository.save(reserved);
            repository.save(producing);
            repository.save(confirmed);

            assertEquals(1, repository.findByStatus(OrderStatus.RESERVED).size());
            assertEquals(1, repository.findByStatus(OrderStatus.PRODUCING).size());
            assertEquals(1, repository.findByStatus(OrderStatus.CONFIRMED).size());
            assertEquals(0, repository.findByStatus(OrderStatus.RELEASED).size());
        }

        @Test
        @DisplayName("findByStatus 결과 없으면 빈 목록 반환")
        void findByStatusNoMatchReturnsEmpty() {
            repository.save(makeOrder("O001", "S001"));

            assertTrue(repository.findByStatus(OrderStatus.RELEASED).isEmpty());
        }
    }

    // ── Regression: 삭제 ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("삭제")
    class DeleteOperations {

        @Test
        @DisplayName("delete 후 findById는 Optional.empty() 반환")
        void deleteAndFindByIdReturnsEmpty() {
            repository.save(makeOrder("O001", "S001"));
            repository.delete("O001");

            assertTrue(repository.findById("O001").isEmpty());
        }

        @Test
        @DisplayName("delete 후 findAll에서 제거됨")
        void deleteReducesFindAllSize() {
            repository.save(makeOrder("O001", "S001"));
            repository.save(makeOrder("O002", "S001"));
            repository.save(makeOrder("O003", "S001"));
            repository.delete("O002");

            List<Order> all = repository.findAll();
            assertEquals(2, all.size());
            assertTrue(all.stream().noneMatch(o -> o.getId().equals("O002")));
        }
    }

    // ── Safety: null 입력 ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("null 입력 방어")
    class NullInputGuards {

        @Test
        @DisplayName("save(null) → IllegalArgumentException")
        void saveNullThrows() {
            assertThrows(IllegalArgumentException.class, () -> repository.save(null));
        }

        @Test
        @DisplayName("findById(null) → IllegalArgumentException")
        void findByIdNullThrows() {
            assertThrows(IllegalArgumentException.class, () -> repository.findById(null));
        }

        @Test
        @DisplayName("delete(null) → IllegalArgumentException")
        void deleteNullThrows() {
            assertThrows(IllegalArgumentException.class, () -> repository.delete(null));
        }

        @Test
        @DisplayName("findByStatus(null) → IllegalArgumentException")
        void findByStatusNullThrows() {
            assertThrows(IllegalArgumentException.class, () -> repository.findByStatus(null));
        }
    }

    // ── Safety: 파일/디렉터리 부재 ──────────────────────────────────────────────

    @Nested
    @DisplayName("파일 및 디렉터리 부재 처리")
    class MissingFileHandling {

        @Test
        @DisplayName("파일 없을 때 findAll → 빈 목록 반환 (예외 없음)")
        void findAllWhenFileAbsentReturnsEmpty() {
            JsonOrderRepository freshRepo = new JsonOrderRepository(
                    tempDir.resolve("nonexistent.json").toString());

            List<Order> result = freshRepo.findAll();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("존재하지 않는 id findById → Optional.empty()")
        void findByIdMissingReturnsEmpty() {
            assertTrue(repository.findById("NOTEXIST").isEmpty());
        }

        @Test
        @DisplayName("존재하지 않는 id delete → 예외 없이 무시")
        void deleteMissingIdDoesNotThrow() {
            assertDoesNotThrow(() -> repository.delete("NOTEXIST"));
        }

        @Test
        @DisplayName("createdAt 직렬화 — LocalDateTime 왕복 시 초 단위 정확성 유지")
        void createdAtRoundTrip() {
            String filePath = tempDir.resolve("orders.json").toString();
            JsonOrderRepository repo1 = new JsonOrderRepository(filePath);
            Order order = makeOrder("O001", "S001");
            LocalDateTime before = order.getCreatedAt().withNano(0);
            repo1.save(order);

            JsonOrderRepository repo2 = new JsonOrderRepository(filePath);
            Optional<Order> found = repo2.findById("O001");

            assertTrue(found.isPresent());
            assertEquals(before, found.get().getCreatedAt().withNano(0));
        }

        @Test
        @DisplayName("data 디렉터리 미존재 시 save → 디렉터리 자동 생성 후 저장 성공")
        void saveCreatesDirectoryIfAbsent() {
            Path nestedPath = tempDir.resolve("newdir/subdir/orders.json");
            JsonOrderRepository nestedRepo = new JsonOrderRepository(nestedPath.toString());

            assertDoesNotThrow(() -> nestedRepo.save(makeOrder("O001", "S001")));
            assertTrue(nestedRepo.findById("O001").isPresent());
        }
    }
}
