package com.ssemi.sampleorder.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Sample 도메인 모델 테스트")
class SampleTest {

    private Sample sample;

    @BeforeEach
    void setUp() {
        sample = new Sample("S001", "GaAs 웨이퍼 A급", 120, 0.85, 50);
    }

    // ── Regression: Happy Path ────────────────────────────────────────────────

    @Nested
    @DisplayName("정상 생성 및 재고 연산")
    class NormalOperations {

        @Test
        @DisplayName("유효한 인자로 생성 시 필드 값 일치")
        void createWithValidArgs() {
            assertEquals("S001", sample.getId());
            assertEquals("GaAs 웨이퍼 A급", sample.getName());
            assertEquals(120, sample.getAvgProductionTime());
            assertEquals(0.85, sample.getYield());
            assertEquals(50, sample.getStock());
        }

        @Test
        @DisplayName("addStock 후 재고 증가")
        void addStockIncreasesStock() {
            sample.addStock(5);
            assertEquals(55, sample.getStock());
        }

        @Test
        @DisplayName("reduceStock 후 재고 감소")
        void reduceStockDecreasesStock() {
            sample.reduceStock(3);
            assertEquals(47, sample.getStock());
        }

        @Test
        @DisplayName("addStock 후 reduceStock 연속 연산 일관성")
        void stockConsistencyAfterMixedOperations() {
            sample.addStock(5);      // 50 + 5 = 55
            sample.reduceStock(8);   // 55 - 8 = 47
            assertEquals(47, sample.getStock());
        }

        @Test
        @DisplayName("reduceStock으로 재고를 정확히 0으로 만들 수 있음")
        void reduceStockToZero() {
            sample.reduceStock(50);
            assertEquals(0, sample.getStock());
        }
    }

    // ── Safety: 생성 유효성 ───────────────────────────────────────────────────

    @Nested
    @DisplayName("생성자 유효성 검사 — IllegalArgumentException 발생")
    class ConstructorValidation {

        @Test
        @DisplayName("yield가 0.0이면 예외")
        void zeroYieldThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Sample("S002", "샘플", 60, 0.0, 10));
        }

        @Test
        @DisplayName("yield가 1.0 초과이면 예외")
        void yieldAboveOneThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Sample("S002", "샘플", 60, 1.1, 10));
        }

        @Test
        @DisplayName("stock이 음수이면 예외")
        void negativeStockThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Sample("S002", "샘플", 60, 0.9, -1));
        }

        @Test
        @DisplayName("avgProductionTime이 0이면 예외")
        void zeroAvgProductionTimeThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Sample("S002", "샘플", 0, 0.9, 10));
        }

        @Test
        @DisplayName("name이 null이면 예외")
        void nullNameThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Sample("S002", null, 60, 0.9, 10));
        }

        @Test
        @DisplayName("name이 blank이면 예외")
        void blankNameThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Sample("S002", "  ", 60, 0.9, 10));
        }
    }

    // ── Safety: 연산 유효성 ───────────────────────────────────────────────────

    @Nested
    @DisplayName("재고 연산 유효성 검사 — IllegalArgumentException 발생")
    class StockOperationValidation {

        @Test
        @DisplayName("재고보다 많이 reduceStock 시 예외")
        void reduceStockExceedingCurrentStockThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> sample.reduceStock(51));
        }

        @Test
        @DisplayName("addStock에 음수 전달 시 예외")
        void negativeAddStockThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> sample.addStock(-1));
        }

        @Test
        @DisplayName("reduceStock에 음수 전달 시 예외")
        void negativeReduceStockThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> sample.reduceStock(-1));
        }
    }
}
