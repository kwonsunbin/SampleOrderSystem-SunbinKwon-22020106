package com.ssemi.sampleorder.controller;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.service.OrderService;
import com.ssemi.sampleorder.view.ConsoleView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ReleaseController 테스트")
@ExtendWith(MockitoExtension.class)
class ReleaseControllerTest {

    @Mock
    private OrderService orderService;

    // ── Regression: 출고 처리 ─────────────────────────────────────────────────

    @Nested
    @DisplayName("출고 처리")
    class Regression {

        @Test
        @DisplayName("handleRelease() 호출 시 orderService.listConfirmedOrders()가 정확히 1회 호출됨")
        void releaseShowsConfirmedOrders() {
            String input = "O001\n";
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            Order order = mock(Order.class);
            when(order.getId()).thenReturn("O001");
            when(order.getCustomerName()).thenReturn("홍길동");
            when(order.getQuantity()).thenReturn(10);
            when(order.getStatus()).thenReturn(OrderStatus.CONFIRMED);
            when(orderService.listConfirmedOrders()).thenReturn(List.of(order));
            when(orderService.releaseOrder("O001")).thenReturn(order);

            ReleaseController controller = new ReleaseController(orderService, consoleView);
            controller.handleRelease();

            verify(orderService, times(1)).listConfirmedOrders();
        }

        @Test
        @DisplayName("입력한 주문 ID 'O001'로 orderService.releaseOrder('O001')이 정확히 1회 호출됨")
        void releaseCallsReleaseOrderWithId() {
            String input = "O001\n";
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            Order order = mock(Order.class);
            when(order.getId()).thenReturn("O001");
            when(order.getCustomerName()).thenReturn("홍길동");
            when(order.getQuantity()).thenReturn(10);
            when(order.getStatus()).thenReturn(OrderStatus.CONFIRMED);
            when(orderService.listConfirmedOrders()).thenReturn(List.of(order));

            Order releasedOrder = mock(Order.class);
            when(releasedOrder.getId()).thenReturn("O001");
            when(releasedOrder.getStatus()).thenReturn(OrderStatus.RELEASED);
            when(orderService.releaseOrder("O001")).thenReturn(releasedOrder);

            ReleaseController controller = new ReleaseController(orderService, consoleView);
            controller.handleRelease();

            verify(orderService, times(1)).releaseOrder("O001");
        }

        @Test
        @DisplayName("출고 처리 성공 시 출력에 '출고' 또는 'RELEASED'가 포함됨")
        void releasePrintsReleasedMessage() {
            String input = "O001\n";
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            Order confirmedOrder = mock(Order.class);
            when(confirmedOrder.getId()).thenReturn("O001");
            when(confirmedOrder.getCustomerName()).thenReturn("홍길동");
            when(confirmedOrder.getQuantity()).thenReturn(10);
            when(confirmedOrder.getStatus()).thenReturn(OrderStatus.CONFIRMED);
            when(orderService.listConfirmedOrders()).thenReturn(List.of(confirmedOrder));

            Order releasedOrder = mock(Order.class);
            when(releasedOrder.getId()).thenReturn("O001");
            when(releasedOrder.getStatus()).thenReturn(OrderStatus.RELEASED);
            when(orderService.releaseOrder("O001")).thenReturn(releasedOrder);

            ReleaseController controller = new ReleaseController(orderService, consoleView);
            controller.handleRelease();

            String output = outputStream.toString(StandardCharsets.UTF_8);
            assertTrue(
                output.contains("출고") || output.contains("RELEASED"),
                "출고 처리 성공 시 출력에 '출고' 또는 'RELEASED'가 포함되어야 합니다. 실제 출력: " + output
            );
        }
    }

    // ── Safety: 예외 처리 ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("안전성")
    class Safety {

        @Test
        @DisplayName("CONFIRMED 주문이 없을 때 출력에 '없'이 포함되고 예외가 전파되지 않음")
        void releaseShowsMessageWhenNoConfirmedOrders() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            when(orderService.listConfirmedOrders()).thenReturn(Collections.emptyList());

            ReleaseController controller = new ReleaseController(orderService, consoleView);

            assertDoesNotThrow(controller::handleRelease,
                "CONFIRMED 주문이 없어도 handleRelease()는 예외를 외부로 전파하지 않아야 합니다.");

            String output = outputStream.toString(StandardCharsets.UTF_8);
            assertTrue(
                output.contains("없"),
                "CONFIRMED 주문이 없을 때 출력에 '없'이 포함되어야 합니다. 실제 출력: " + output
            );
        }

        @Test
        @DisplayName("releaseOrder()가 예외를 던져도 오류 메시지를 출력하고 예외가 외부로 전파되지 않음")
        void releaseShowsErrorWhenServiceThrows() {
            String input = "O999\n";
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            Order order = mock(Order.class);
            when(order.getId()).thenReturn("O999");
            when(order.getCustomerName()).thenReturn("홍길동");
            when(order.getQuantity()).thenReturn(5);
            when(order.getStatus()).thenReturn(OrderStatus.CONFIRMED);
            when(orderService.listConfirmedOrders()).thenReturn(List.of(order));
            when(orderService.releaseOrder("O999"))
                .thenThrow(new IllegalArgumentException("존재하지 않는 주문입니다."));

            ReleaseController controller = new ReleaseController(orderService, consoleView);

            assertDoesNotThrow(controller::handleRelease,
                "서비스에서 예외가 발생해도 handleRelease()는 예외를 외부로 전파하지 않아야 합니다.");

            String output = outputStream.toString(StandardCharsets.UTF_8);
            assertFalse(output.isBlank(),
                "오류 발생 시 출력이 비어 있으면 안 됩니다.");
        }
    }
}
