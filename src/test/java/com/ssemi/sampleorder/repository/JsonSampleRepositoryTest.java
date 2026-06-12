package com.ssemi.sampleorder.repository;

import com.ssemi.sampleorder.model.Sample;
import com.ssemi.sampleorder.repository.json.JsonSampleRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonSampleRepository 테스트")
class JsonSampleRepositoryTest {

    @TempDir
    Path tempDir;

    private JsonSampleRepository repository;

    private Sample makeSample(String id, String name, int stock) {
        return new Sample(id, name, 60, 0.85, stock);
    }

    @BeforeEach
    void setUp() {
        repository = new JsonSampleRepository(tempDir.resolve("samples.json").toString());
    }

    // ── Regression: Round-trip 직렬화 ────────────────────────────────────────

    @Nested
    @DisplayName("Round-trip 직렬화 및 영속성")
    class RoundTripSerialization {

        @Test
        @DisplayName("save 후 findById로 모든 필드 일치 조회")
        void saveAndFindById() {
            Sample sample = makeSample("S001", "GaAs 웨이퍼", 50);
            repository.save(sample);

            Optional<Sample> found = repository.findById("S001");

            assertTrue(found.isPresent());
            assertEquals("S001", found.get().getId());
            assertEquals("GaAs 웨이퍼", found.get().getName());
            assertEquals(60, found.get().getAvgProductionTime());
            assertEquals(0.85, found.get().getYield(), 1e-9);
            assertEquals(50, found.get().getStock());
        }

        @Test
        @DisplayName("save 후 findAll에 포함됨")
        void saveAndFindAll() {
            repository.save(makeSample("S001", "GaAs 웨이퍼", 50));

            List<Sample> all = repository.findAll();

            assertEquals(1, all.size());
            assertEquals("S001", all.get(0).getId());
        }

        @Test
        @DisplayName("복수 save 후 findAll 전체 반환")
        void saveMultipleAndFindAll() {
            repository.save(makeSample("S001", "GaAs 웨이퍼", 50));
            repository.save(makeSample("S002", "InP 기판", 30));
            repository.save(makeSample("S003", "SiC 에피", 20));

            assertEquals(3, repository.findAll().size());
        }

        @Test
        @DisplayName("findAll 반환 순서는 저장 순서와 일치")
        void findAllPreservesInsertionOrder() {
            repository.save(makeSample("S001", "첫번째", 10));
            repository.save(makeSample("S002", "두번째", 20));
            repository.save(makeSample("S003", "세번째", 30));

            List<Sample> all = repository.findAll();

            assertEquals("S001", all.get(0).getId());
            assertEquals("S002", all.get(1).getId());
            assertEquals("S003", all.get(2).getId());
        }

        @Test
        @DisplayName("재시작 시뮬레이션 — 새 인스턴스로 findById 시 동일 필드 반환")
        void persistenceAcrossNewInstance() {
            String filePath = tempDir.resolve("samples.json").toString();
            JsonSampleRepository repo1 = new JsonSampleRepository(filePath);
            repo1.save(makeSample("S001", "GaAs 웨이퍼", 50));

            JsonSampleRepository repo2 = new JsonSampleRepository(filePath);
            Optional<Sample> found = repo2.findById("S001");

            assertTrue(found.isPresent());
            assertEquals("GaAs 웨이퍼", found.get().getName());
            assertEquals(50, found.get().getStock());
        }

        @Test
        @DisplayName("동일 id로 save 시 기존 데이터 덮어쓰기(update)")
        void saveWithSameIdOverwrites() {
            repository.save(makeSample("S001", "구버전", 10));
            repository.save(new Sample("S001", "신버전", 90, 0.9, 99));

            Optional<Sample> found = repository.findById("S001");

            assertTrue(found.isPresent());
            assertEquals("신버전", found.get().getName());
            assertEquals(99, found.get().getStock());
        }

        @Test
        @DisplayName("stock 변경 후 save → 새 인스턴스에서 변경된 stock 영속화 확인")
        void stockChangePersistedAfterSave() {
            String filePath = tempDir.resolve("samples.json").toString();
            JsonSampleRepository repo1 = new JsonSampleRepository(filePath);
            Sample sample = makeSample("S001", "GaAs 웨이퍼", 50);
            sample.addStock(20);
            repo1.save(sample);

            JsonSampleRepository repo2 = new JsonSampleRepository(filePath);
            Optional<Sample> found = repo2.findById("S001");

            assertTrue(found.isPresent());
            assertEquals(70, found.get().getStock());
        }
    }

    // ── Regression: 검색 ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("이름 검색")
    class NameSearch {

        @Test
        @DisplayName("findByName 정확 일치 검색")
        void findByNameExact() {
            repository.save(makeSample("S001", "GaAs", 10));
            repository.save(makeSample("S002", "InP", 20));

            List<Sample> result = repository.findByName("GaAs");

            assertEquals(1, result.size());
            assertEquals("S001", result.get(0).getId());
        }

        @Test
        @DisplayName("findByName 부분 일치 검색")
        void findByNamePartial() {
            repository.save(makeSample("S001", "GaAs 웨이퍼 A급", 10));
            repository.save(makeSample("S002", "InP 기판", 20));

            List<Sample> result = repository.findByName("웨이퍼");

            assertEquals(1, result.size());
            assertEquals("S001", result.get(0).getId());
        }

        @Test
        @DisplayName("findByName 대소문자 무관 검색")
        void findByNameCaseInsensitive() {
            repository.save(makeSample("S001", "GaAs 웨이퍼", 10));

            List<Sample> result = repository.findByName("gaas");

            assertEquals(1, result.size());
        }
    }

    // ── Regression: 삭제 ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("삭제")
    class DeleteOperations {

        @Test
        @DisplayName("delete 후 findById는 Optional.empty() 반환")
        void deleteAndFindByIdReturnsEmpty() {
            repository.save(makeSample("S001", "GaAs 웨이퍼", 10));
            repository.delete("S001");

            assertTrue(repository.findById("S001").isEmpty());
        }

        @Test
        @DisplayName("delete 후 findAll에서 제거됨")
        void deleteReducesFindAllSize() {
            repository.save(makeSample("S001", "GaAs 웨이퍼", 10));
            repository.save(makeSample("S002", "InP 기판", 20));
            repository.delete("S001");

            assertEquals(1, repository.findAll().size());
            assertEquals("S002", repository.findAll().get(0).getId());
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
        @DisplayName("findByName(null) → IllegalArgumentException")
        void findByNameNullThrows() {
            assertThrows(IllegalArgumentException.class, () -> repository.findByName(null));
        }
    }

    // ── Safety: 파일/디렉터리 부재 ──────────────────────────────────────────────

    @Nested
    @DisplayName("파일 및 디렉터리 부재 처리")
    class MissingFileHandling {

        @Test
        @DisplayName("파일 없을 때 findAll → 빈 목록 반환 (예외 없음)")
        void findAllWhenFileAbsentReturnsEmpty() {
            JsonSampleRepository freshRepo = new JsonSampleRepository(
                    tempDir.resolve("nonexistent.json").toString());

            List<Sample> result = freshRepo.findAll();

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
        @DisplayName("findByName 결과 없음 → 빈 목록 반환")
        void findByNameNoMatchReturnsEmpty() {
            repository.save(makeSample("S001", "GaAs 웨이퍼", 10));

            assertTrue(repository.findByName("없는시료").isEmpty());
        }

        @Test
        @DisplayName("data 디렉터리 미존재 시 save → 디렉터리 자동 생성 후 저장 성공")
        void saveCreatesDirectoryIfAbsent() {
            Path nestedPath = tempDir.resolve("newdir/subdir/samples.json");
            JsonSampleRepository nestedRepo = new JsonSampleRepository(nestedPath.toString());

            assertDoesNotThrow(() -> nestedRepo.save(makeSample("S001", "GaAs", 10)));
            assertTrue(nestedRepo.findById("S001").isPresent());
        }
    }
}
