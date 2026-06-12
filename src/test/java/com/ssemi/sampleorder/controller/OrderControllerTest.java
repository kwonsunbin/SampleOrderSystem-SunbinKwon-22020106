package com.ssemi.sampleorder.controller;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.service.OrderService;
import com.ssemi.sampleorder.view.ConsoleView;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("OrderController 테스트")
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    OrderService orderService;

    // ── 공통 헬퍼 ────────────────────────────────────────────────────────────

    private OrderController buildController(String inputText, ByteArrayOutputStream outputStream) {
        InputStream inputStream = new ByteArrayInputStream(inputText.getBytes());
        ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));
        return new OrderController(orderService, consoleView);
    }

    private Order mockOrder(String id, OrderStatus status) {
        Order order = mock(Order.class);
        when(order.getId()).thenReturn(id);
        when(order.getStatus()).thenReturn(status);
        return order;
    }

    // ── Regression: 주문 접수 ─────────────────────────────────────────────────

    @Nested
    @DisplayName("주문 접수")
    class HandleReserve {

        @Test
        @DisplayName("입력값으로 reserveOrder() 가 정확한 인수와 함께 1회 호출됨")
        void reserveCallsServiceWithCorrectArgs() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // 시료 ID, 고객명, 수량, 확인(Y)
            OrderController controller = buildController("S001\n서울대\n10\nY\n", outputStream);

            Order mockOrder = mockOrder("O001", OrderStatus.RESERVED);
            when(orderService.reserveOrder("S001", "서울대", 10)).thenReturn(mockOrder);

            controller.handleReserve();

            verify(orderService, times(1)).reserveOrder("S001", "서울대", 10);
        }

        @Test
        @DisplayName("접수 성공 시 출력에 '접수' 포함")
        void reservePrintsSuccessMessage() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OrderController controller = buildController("S001\n서울대\n10\nY\n", outputStream);

            Order mockOrder = mockOrder("O001", OrderStatus.RESERVED);
            when(orderService.reserveOrder("S001", "서울대", 10)).thenReturn(mockOrder);

            controller.handleReserve();

            String output = outputStream.toString();
            assertTrue(output.contains("접수"),
                    "출력에 '접수' 가 포함되어야 합니다. 실제 출력: " + output);
        }
    }

    // ── Regression: 주문 승인 ─────────────────────────────────────────────────

    @Nested
    @DisplayName("주문 승인")
    class HandleApprove {

        @Test
        @DisplayName("선택한 번호에 해당하는 orderId 로 approveOrder() 가 1회 호출됨")
        void approveCallsServiceWithOrderId() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Order reservedOrder = mockOrder("O001", OrderStatus.RESERVED);
            when(orderService.listReservedOrders()).thenReturn(List.of(reservedOrder));

            Order approvedOrder = mockOrder("O001", OrderStatus.CONFIRMED);
            when(orderService.approveOrder("O001")).thenReturn(approvedOrder);

            // 번호 1 선택, Y 확인
            OrderController controller = buildController("1\nY\n", outputStream);
            controller.handleApprove();

            verify(orderService, times(1)).approveOrder("O001");
        }

        @Test
        @DisplayName("승인 결과가 CONFIRMED 일 때 출력에 '승인' 또는 'CONFIRMED' 포함")
        void approveShowsConfirmedMessage() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Order reservedOrder = mockOrder("O001", OrderStatus.RESERVED);
            when(orderService.listReservedOrders()).thenReturn(List.of(reservedOrder));

            Order approvedOrder = mockOrder("O001", OrderStatus.CONFIRMED);
            when(orderService.approveOrder("O001")).thenReturn(approvedOrder);

            OrderController controller = buildController("1\nY\n", outputStream);
            controller.handleApprove();

            String output = outputStream.toString();
            assertTrue(output.contains("승인") || output.contains("CONFIRMED"),
                    "출력에 '승인' 또는 'CONFIRMED' 가 포함되어야 합니다. 실제 출력: " + output);
        }

        @Test
        @DisplayName("승인 결과가 PRODUCING 일 때 출력에 '생산' 또는 'PRODUCING' 포함")
        void approveShowsProducingMessage() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Order reservedOrder = mockOrder("O001", OrderStatus.RESERVED);
            when(orderService.listReservedOrders()).thenReturn(List.of(reservedOrder));

            Order producingOrder = mockOrder("O001", OrderStatus.PRODUCING);
            when(orderService.approveOrder("O001")).thenReturn(producingOrder);

            OrderController controller = buildController("1\nY\n", outputStream);
            controller.handleApprove();

            String output = outputStream.toString();
            assertTrue(output.contains("생산") || output.contains("PRODUCING"),
                    "출력에 '생산' 또는 'PRODUCING' 이 포함되어야 합니다. 실제 출력: " + output);
        }
    }

    // ── Regression: 주문 거절 ─────────────────────────────────────────────────

    @Nested
    @DisplayName("주문 거절")
    class HandleReject {

        @Test
        @DisplayName("선택한 번호에 해당하는 orderId 로 rejectOrder() 가 1회 호출됨")
        void rejectCallsServiceWithOrderId() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Order reservedOrder = mockOrder("O001", OrderStatus.RESERVED);
            when(orderService.listReservedOrders()).thenReturn(List.of(reservedOrder));

            Order rejectedOrder = mockOrder("O001", OrderStatus.REJECTED);
            when(orderService.rejectOrder("O001")).thenReturn(rejectedOrder);

            // 번호 1 선택
            OrderController controller = buildController("1\n", outputStream);
            controller.handleReject();

            verify(orderService, times(1)).rejectOrder("O001");
        }

        @Test
        @DisplayName("거절 성공 시 출력에 '거절' 또는 'REJECTED' 포함")
        void rejectPrintsRejectedMessage() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Order reservedOrder = mockOrder("O001", OrderStatus.RESERVED);
            when(orderService.listReservedOrders()).thenReturn(List.of(reservedOrder));

            Order rejectedOrder = mockOrder("O001", OrderStatus.REJECTED);
            when(orderService.rejectOrder("O001")).thenReturn(rejectedOrder);

            OrderController controller = buildController("1\n", outputStream);
            controller.handleReject();

            String output = outputStream.toString();
            assertTrue(output.contains("거절") || output.contains("REJECTED"),
                    "출력에 '거절' 또는 'REJECTED' 가 포함되어야 합니다. 실제 출력: " + output);
        }
    }

    // ── Safety ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Safety")
    class Safety {

        @Test
        @DisplayName("RESERVED 주문이 없을 때 handleApprove() 는 출력에 '없' 포함하고 예외 없이 종료됨")
        void approveShowsMessageWhenNoReservedOrders() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            when(orderService.listReservedOrders()).thenReturn(Collections.emptyList());

            OrderController controller = buildController("", outputStream);

            assertDoesNotThrow(controller::handleApprove);

            String output = outputStream.toString();
            assertTrue(output.contains("없"),
                    "출력에 '없' 이 포함되어야 합니다. 실제 출력: " + output);
        }

        @Test
        @DisplayName("approveOrder() 가 IllegalArgumentException 을 던질 때 출력에 오류 메시지 포함")
        void approveShowsErrorWhenServiceThrows() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Order reservedOrder = mockOrder("O001", OrderStatus.RESERVED);
            when(orderService.listReservedOrders()).thenReturn(List.of(reservedOrder));
            when(orderService.approveOrder("O001"))
                    .thenThrow(new IllegalArgumentException("존재하지 않는 주문입니다: O001"));

            // 번호 1 선택, Y 확인
            OrderController controller = buildController("1\nY\n", outputStream);

            assertDoesNotThrow(controller::handleApprove);

            String output = outputStream.toString();
            assertFalse(output.isBlank(),
                    "오류 발생 시 오류 메시지가 출력되어야 합니다.");
        }
    }
}
