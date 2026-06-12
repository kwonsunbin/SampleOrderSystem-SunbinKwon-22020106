package com.ssemi.sampleorder.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Order 도메인 모델 테스트")
class OrderTest {

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order("O001", "S001", "서울대 나노연구소", 10);
    }

    // ── Regression: Happy Path ────────────────────────────────────────────────

    @Nested
    @DisplayName("초기 상태 및 허용 전이")
    class AllowedTransitions {

        @Test
        @DisplayName("주문 생성 시 초기 상태는 RESERVED")
        void initialStatusIsReserved() {
            assertEquals(OrderStatus.RESERVED, order.getStatus());
        }

        @Test
        @DisplayName("RESERVED → CONFIRMED 전이 성공")
        void reservedToConfirmed() {
            order.transitionTo(OrderStatus.CONFIRMED);
            assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        }

        @Test
        @DisplayName("RESERVED → PRODUCING 전이 성공")
        void reservedToProducing() {
            order.transitionTo(OrderStatus.PRODUCING);
            assertEquals(OrderStatus.PRODUCING, order.getStatus());
        }

        @Test
        @DisplayName("RESERVED → REJECTED 전이 성공")
        void reservedToRejected() {
            order.transitionTo(OrderStatus.REJECTED);
            assertEquals(OrderStatus.REJECTED, order.getStatus());
        }

        @Test
        @DisplayName("PRODUCING → CONFIRMED 전이 성공")
        void producingToConfirmed() {
            order.transitionTo(OrderStatus.PRODUCING);
            order.transitionTo(OrderStatus.CONFIRMED);
            assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        }

        @Test
        @DisplayName("CONFIRMED → RELEASED 전이 성공")
        void confirmedToReleased() {
            order.transitionTo(OrderStatus.CONFIRMED);
            order.transitionTo(OrderStatus.RELEASED);
            assertEquals(OrderStatus.RELEASED, order.getStatus());
        }

        @Test
        @DisplayName("전체 흐름 연속 전이: RESERVED → PRODUCING → CONFIRMED → RELEASED")
        void fullLifecycleTransition() {
            order.transitionTo(OrderStatus.PRODUCING);
            assertEquals(OrderStatus.PRODUCING, order.getStatus());

            order.transitionTo(OrderStatus.CONFIRMED);
            assertEquals(OrderStatus.CONFIRMED, order.getStatus());

            order.transitionTo(OrderStatus.RELEASED);
            assertEquals(OrderStatus.RELEASED, order.getStatus());
        }
    }

    // ── Safety: 불허 전이 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("불허 상태 전이 — IllegalStateException 발생")
    class ForbiddenTransitions {

        @Test
        @DisplayName("CONFIRMED → PRODUCING 전이 불가")
        void confirmedToProducingForbidden() {
            order.transitionTo(OrderStatus.CONFIRMED);
            assertThrows(IllegalStateException.class,
                    () -> order.transitionTo(OrderStatus.PRODUCING));
        }

        @Test
        @DisplayName("RELEASED → CONFIRMED 전이 불가")
        void releasedToConfirmedForbidden() {
            order.transitionTo(OrderStatus.CONFIRMED);
            order.transitionTo(OrderStatus.RELEASED);
            assertThrows(IllegalStateException.class,
                    () -> order.transitionTo(OrderStatus.CONFIRMED));
        }

        @Test
        @DisplayName("REJECTED → CONFIRMED 전이 불가")
        void rejectedToConfirmedForbidden() {
            order.transitionTo(OrderStatus.REJECTED);
            assertThrows(IllegalStateException.class,
                    () -> order.transitionTo(OrderStatus.CONFIRMED));
        }

        @Test
        @DisplayName("RELEASED → RESERVED 전이 불가")
        void releasedToReservedForbidden() {
            order.transitionTo(OrderStatus.CONFIRMED);
            order.transitionTo(OrderStatus.RELEASED);
            assertThrows(IllegalStateException.class,
                    () -> order.transitionTo(OrderStatus.RESERVED));
        }

        @Test
        @DisplayName("RELEASED 이후 어떤 전이도 불가")
        void noTransitionAfterReleased() {
            order.transitionTo(OrderStatus.CONFIRMED);
            order.transitionTo(OrderStatus.RELEASED);
            for (OrderStatus next : OrderStatus.values()) {
                assertThrows(IllegalStateException.class,
                        () -> order.transitionTo(next),
                        "RELEASED → " + next + " 전이가 차단되어야 함");
            }
        }
    }

    // ── Safety: 생성 유효성 ───────────────────────────────────────────────────

    @Nested
    @DisplayName("생성 유효성 검사 — IllegalArgumentException 발생")
    class ConstructorValidation {

        @Test
        @DisplayName("customerName이 null이면 예외")
        void nullCustomerNameThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Order("O002", "S001", null, 10));
        }

        @Test
        @DisplayName("customerName이 blank이면 예외")
        void blankCustomerNameThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Order("O002", "S001", "  ", 10));
        }

        @Test
        @DisplayName("quantity가 0이면 예외")
        void zeroQuantityThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Order("O002", "S001", "고객", 0));
        }

        @Test
        @DisplayName("quantity가 음수이면 예외")
        void negativeQuantityThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Order("O002", "S001", "고객", -1));
        }
    }

    // ── NEW: 생산 시간 추적 ───────────────────────────────────────────────────

    @Nested
    @DisplayName("생산 시간 추적 — startProduction")
    class ProductionTimeTracking {

        @Test
        @DisplayName("생성 직후 productionStartedAt은 null")
        void initialProductionStartedAtIsNull() {
            assertNull(order.getProductionStartedAt());
        }

        @Test
        @DisplayName("생성 직후 totalProductionMinutes는 0")
        void initialTotalProductionMinutesIsZero() {
            assertEquals(0, order.getTotalProductionMinutes());
        }

        @Test
        @DisplayName("startProduction → productionStartedAt 기록됨")
        void startProductionRecordsStartedAt() {
            order.transitionTo(OrderStatus.PRODUCING);
            LocalDateTime now = LocalDateTime.of(2026, 6, 12, 9, 0);
            order.startProduction(now, 60);
            assertEquals(now, order.getProductionStartedAt());
        }

        @Test
        @DisplayName("startProduction → totalProductionMinutes 기록됨")
        void startProductionRecordsTotalMinutes() {
            order.transitionTo(OrderStatus.PRODUCING);
            order.startProduction(LocalDateTime.of(2026, 6, 12, 9, 0), 120);
            assertEquals(120, order.getTotalProductionMinutes());
        }

        @Test
        @DisplayName("PRODUCING이 아닌 상태(RESERVED)에서 startProduction → IllegalStateException")
        void startProductionOnReservedThrows() {
            assertThrows(IllegalStateException.class,
                    () -> order.startProduction(LocalDateTime.now(), 60));
        }

        @Test
        @DisplayName("CONFIRMED 상태에서 startProduction → IllegalStateException")
        void startProductionOnConfirmedThrows() {
            order.transitionTo(OrderStatus.CONFIRMED);
            assertThrows(IllegalStateException.class,
                    () -> order.startProduction(LocalDateTime.now(), 60));
        }
    }

    // ── Regression: 불변성 ───────────────────────────────────────────────────

    @Nested
    @DisplayName("상태 전이 후 불변 필드 유지")
    class ImmutableFieldsAfterTransition {

        @Test
        @DisplayName("상태 전이 후 id 변경 없음")
        void idUnchangedAfterTransition() {
            order.transitionTo(OrderStatus.CONFIRMED);
            assertEquals("O001", order.getId());
        }

        @Test
        @DisplayName("상태 전이 후 quantity 변경 없음")
        void quantityUnchangedAfterTransition() {
            order.transitionTo(OrderStatus.CONFIRMED);
            assertEquals(10, order.getQuantity());
        }

        @Test
        @DisplayName("상태 전이 후 customerName 변경 없음")
        void customerNameUnchangedAfterTransition() {
            order.transitionTo(OrderStatus.CONFIRMED);
            assertEquals("서울대 나노연구소", order.getCustomerName());
        }
    }
}
