package com.ssemi.sampleorder.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StockStatus 판정 로직 테스트")
class StockStatusTest {

    // ── Regression: 판정 로직 ────────────────────────────────────────────────

    @Nested
    @DisplayName("재고 상태 판정")
    class StockStatusDetermination {

        @Test
        @DisplayName("stock == 0 → DEPLETED (고갈)")
        void zeroStockIsDepleted() {
            assertEquals(StockStatus.DEPLETED, StockStatus.of(0, 5));
        }

        @Test
        @DisplayName("0 < stock < demand → SHORTAGE (부족)")
        void partialStockIsShortage() {
            assertEquals(StockStatus.SHORTAGE, StockStatus.of(3, 5));
        }

        @Test
        @DisplayName("stock == demand → SUFFICIENT (여유)")
        void stockEqualsDemandIsSufficient() {
            assertEquals(StockStatus.SUFFICIENT, StockStatus.of(5, 5));
        }

        @Test
        @DisplayName("stock > demand → SUFFICIENT (여유)")
        void stockExceedsDemandIsSufficient() {
            assertEquals(StockStatus.SUFFICIENT, StockStatus.of(10, 5));
        }

        @Test
        @DisplayName("demand == 0, stock == 0 → SUFFICIENT (주문 없음)")
        void zeroDemandZeroStockIsSufficient() {
            assertEquals(StockStatus.SUFFICIENT, StockStatus.of(0, 0));
        }

        @Test
        @DisplayName("demand == 0, stock > 0 → SUFFICIENT")
        void zeroDemandPositiveStockIsSufficient() {
            assertEquals(StockStatus.SUFFICIENT, StockStatus.of(5, 0));
        }
    }

    // ── Safety: 경계값 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("유효하지 않은 입력 — IllegalArgumentException 발생")
    class InvalidInputs {

        @Test
        @DisplayName("stock 음수 시 예외")
        void negativeStockThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> StockStatus.of(-1, 5));
        }

        @Test
        @DisplayName("demand 음수 시 예외")
        void negativeDemandThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> StockStatus.of(5, -1));
        }
    }
}
