package com.ssemi.sampleorder.service;

import com.ssemi.sampleorder.model.Sample;
import com.ssemi.sampleorder.repository.SampleRepository;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SampleService 테스트")
class SampleServiceTest {

    private FakeSampleRepository sampleRepo;
    private SampleService sampleService;

    @BeforeEach
    void setUp() {
        sampleRepo = new FakeSampleRepository();
        sampleService = new SampleService(sampleRepo);
    }

    // ── Regression: 시료 등록 ─────────────────────────────────────────────────

    @Nested
    @DisplayName("시료 등록")
    class Register {

        @Test
        @DisplayName("유효한 인자로 등록 성공 — 반환된 Sample 필드 일치")
        void registerReturnsSampleWithCorrectFields() {
            Sample result = sampleService.register("GaAs 웨이퍼", 60, 0.85, 50);

            assertNotNull(result.getId());
            assertEquals("GaAs 웨이퍼", result.getName());
            assertEquals(60, result.getAvgProductionTime());
            assertEquals(0.85, result.getYield(), 1e-9);
            assertEquals(50, result.getStock());
        }

        @Test
        @DisplayName("등록 후 listSamples에 포함됨")
        void registeredSampleAppearsInList() {
            sampleService.register("GaAs 웨이퍼", 60, 0.85, 50);

            assertEquals(1, sampleService.listSamples().size());
        }

        @Test
        @DisplayName("등록된 시료 ID는 고유함")
        void registeredSamplesHaveUniqueIds() {
            Sample s1 = sampleService.register("GaAs", 60, 0.85, 10);
            Sample s2 = sampleService.register("InP", 90, 0.9, 20);

            assertNotEquals(s1.getId(), s2.getId());
        }
    }

    // ── Regression: 조회·검색 ─────────────────────────────────────────────────

    @Nested
    @DisplayName("시료 조회 및 검색")
    class QueryAndSearch {

        @Test
        @DisplayName("listSamples — 등록된 전체 목록 반환")
        void listSamplesReturnsAll() {
            sampleService.register("A", 60, 0.9, 10);
            sampleService.register("B", 60, 0.9, 10);
            sampleService.register("C", 60, 0.9, 10);

            assertEquals(3, sampleService.listSamples().size());
        }

        @Test
        @DisplayName("listSamples — 등록 없으면 빈 목록 반환")
        void listSamplesEmptyWhenNoneRegistered() {
            assertTrue(sampleService.listSamples().isEmpty());
        }

        @Test
        @DisplayName("searchSample — 키워드 일치 시료 반환")
        void searchSampleReturnsMatch() {
            sampleService.register("GaAs 웨이퍼", 60, 0.9, 10);
            sampleService.register("InP 기판", 90, 0.85, 20);

            List<Sample> result = sampleService.searchSample("GaAs");

            assertEquals(1, result.size());
            assertEquals("GaAs 웨이퍼", result.get(0).getName());
        }

        @Test
        @DisplayName("searchSample — 미일치 시 빈 목록 반환")
        void searchSampleReturnsEmptyWhenNoMatch() {
            sampleService.register("GaAs 웨이퍼", 60, 0.9, 10);

            assertTrue(sampleService.searchSample("없는시료").isEmpty());
        }

        @Test
        @DisplayName("getStock — 등록된 시료의 재고 수량 정확 반환")
        void getStockReturnsCorrectAmount() {
            Sample s = sampleService.register("GaAs", 60, 0.9, 50);

            assertEquals(50, sampleService.getStock(s.getId()));
        }
    }

    // ── Safety: 유효성 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("등록 유효성 검사")
    class RegisterValidation {

        @Test
        @DisplayName("name null 등록 불가")
        void nullNameThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> sampleService.register(null, 60, 0.85, 10));
        }

        @Test
        @DisplayName("name blank 등록 불가")
        void blankNameThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> sampleService.register("  ", 60, 0.85, 10));
        }

        @Test
        @DisplayName("avgProductionTime ≤ 0 등록 불가")
        void zeroAvgProductionTimeThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> sampleService.register("GaAs", 0, 0.85, 10));
        }

        @Test
        @DisplayName("stock 음수 등록 불가")
        void negativeStockThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> sampleService.register("GaAs", 60, 0.85, -1));
        }

        @Test
        @DisplayName("yield 범위 초과 등록 불가")
        void outOfRangeYieldThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> sampleService.register("GaAs", 60, 1.5, 10));
        }

        @Test
        @DisplayName("getStock(null) → IllegalArgumentException")
        void getStockNullIdThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> sampleService.getStock(null));
        }

        @Test
        @DisplayName("존재하지 않는 ID getStock → IllegalArgumentException")
        void getStockMissingIdThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> sampleService.getStock("NOTEXIST"));
        }

        @Test
        @DisplayName("searchSample(null) → IllegalArgumentException")
        void searchSampleNullThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> sampleService.searchSample(null));
        }
    }

    // ── Fake Repository ───────────────────────────────────────────────────────

    static class FakeSampleRepository implements SampleRepository {
        private final Map<String, Sample> store = new LinkedHashMap<>();

        @Override public void save(Sample s) { store.put(s.getId(), s); }
        @Override public Optional<Sample> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Sample> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<Sample> findByName(String keyword) {
            String lower = keyword.toLowerCase();
            return store.values().stream()
                    .filter(s -> s.getName().toLowerCase().contains(lower))
                    .collect(java.util.stream.Collectors.toList());
        }
        @Override public void delete(String id) { store.remove(id); }
    }
}
