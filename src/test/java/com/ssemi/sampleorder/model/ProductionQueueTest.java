package com.ssemi.sampleorder.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProductionQueue 도메인 모델 테스트")
class ProductionQueueTest {

    private ProductionQueue queue;

    private Order makeOrder(String id) {
        Order o = new Order(id, "S001", "테스트고객", 5);
        o.transitionTo(OrderStatus.PRODUCING);
        return o;
    }

    @BeforeEach
    void setUp() {
        queue = new ProductionQueue();
    }

    // ── Regression: Happy Path ────────────────────────────────────────────────

    @Nested
    @DisplayName("기본 동작")
    class BasicBehavior {

        @Test
        @DisplayName("초기 상태는 비어 있음")
        void initiallyEmpty() {
            assertTrue(queue.isEmpty());
            assertEquals(0, queue.size());
        }

        @Test
        @DisplayName("enqueue 후 isEmpty false, size 1")
        void enqueueMakesNonEmpty() {
            queue.enqueue(makeOrder("O001"));
            assertFalse(queue.isEmpty());
            assertEquals(1, queue.size());
        }

        @Test
        @DisplayName("FIFO 순서 보장 — 먼저 넣은 것이 먼저 나옴")
        void fifoOrder() {
            Order first  = makeOrder("O001");
            Order second = makeOrder("O002");
            Order third  = makeOrder("O003");

            queue.enqueue(first);
            queue.enqueue(second);
            queue.enqueue(third);

            assertSame(first,  queue.dequeue());
            assertSame(second, queue.dequeue());
            assertSame(third,  queue.dequeue());
        }

        @Test
        @DisplayName("dequeue 후 size 감소")
        void dequeueReducesSize() {
            queue.enqueue(makeOrder("O001"));
            queue.enqueue(makeOrder("O002"));
            queue.dequeue();
            assertEquals(1, queue.size());
        }

        @Test
        @DisplayName("peek은 꺼내지 않고 조회 — size 불변")
        void peekDoesNotRemove() {
            Order order = makeOrder("O001");
            queue.enqueue(order);

            Order peeked = queue.peek();

            assertSame(order, peeked);
            assertEquals(1, queue.size());
        }

        @Test
        @DisplayName("반복 enqueue/dequeue 후 size 일관성")
        void sizeConsistencyAfterMixedOperations() {
            for (int i = 1; i <= 5; i++) queue.enqueue(makeOrder("O00" + i));
            queue.dequeue();
            queue.dequeue();
            queue.dequeue();
            queue.enqueue(makeOrder("O006"));
            queue.enqueue(makeOrder("O007"));

            assertEquals(4, queue.size());
        }
    }

    // ── Safety: 경계값 / 예외 ─────────────────────────────────────────────────

    @Nested
    @DisplayName("경계값 및 예외 처리")
    class BoundaryAndExceptions {

        @Test
        @DisplayName("빈 큐에서 dequeue 시 NoSuchElementException")
        void dequeueFromEmptyQueueThrows() {
            assertThrows(NoSuchElementException.class, () -> queue.dequeue());
        }

        @Test
        @DisplayName("빈 큐에서 peek 시 NoSuchElementException")
        void peekFromEmptyQueueThrows() {
            assertThrows(NoSuchElementException.class, () -> queue.peek());
        }

        @Test
        @DisplayName("null 주문 enqueue 시 IllegalArgumentException")
        void enqueueNullThrows() {
            assertThrows(IllegalArgumentException.class, () -> queue.enqueue(null));
        }
    }
}
