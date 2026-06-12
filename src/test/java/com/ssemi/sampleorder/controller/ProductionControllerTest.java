package com.ssemi.sampleorder.controller;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.service.ProductionService;
import com.ssemi.sampleorder.view.ConsoleView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductionController 테스트")
class ProductionControllerTest {

    @Mock
    private ProductionService productionService;

    private ConsoleView buildConsoleView(ByteArrayOutputStream outputStream) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
        return new ConsoleView(inputStream, new PrintStream(outputStream));
    }

    private Order buildConfirmedOrder() {
        Order order = new Order("ORD-001", "SAMPLE-001", "테스트고객", 10);
        order.transitionTo(OrderStatus.PRODUCING);
        order.transitionTo(OrderStatus.CONFIRMED);
        return order;
    }

    @Nested
    @DisplayName("Regression: 생산 큐 처리")
    class ProcessQueueTests {

        @Test
        @DisplayName("handleProcessQueue 호출 시 productionService.processNext()가 1회 호출된다")
        void processQueueCallsProcessNext() {
            // given
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ConsoleView consoleView = buildConsoleView(outputStream);
            Order confirmedOrder = buildConfirmedOrder();
            when(productionService.processNext()).thenReturn(confirmedOrder);

            ProductionController controller = new ProductionController(productionService, consoleView);

            // when
            controller.handleProcessQueue();

            // then
            verify(productionService, times(1)).processNext();
        }

        @Test
        @DisplayName("handleProcessQueue 호출 후 완료 메시지(완료 또는 CONFIRMED)가 출력된다")
        void processQueuePrintsCompletionMessage() {
            // given
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ConsoleView consoleView = buildConsoleView(outputStream);
            Order confirmedOrder = buildConfirmedOrder();
            when(productionService.processNext()).thenReturn(confirmedOrder);

            ProductionController controller = new ProductionController(productionService, consoleView);

            // when
            controller.handleProcessQueue();

            // then
            String output = outputStream.toString();
            boolean hasCompletionKeyword = output.contains("완료") || output.contains("CONFIRMED");
            assertTrue(hasCompletionKeyword,
                    "출력에 '완료' 또는 'CONFIRMED'가 포함되어야 한다. 실제 출력: " + output);
        }
    }

    @Nested
    @DisplayName("Regression: 생산 큐 조회")
    class ViewQueueTests {

        @Test
        @DisplayName("handleViewQueue 호출 시 productionService.getQueueSnapshot()이 1회 호출된다")
        void viewQueueCallsGetQueueSnapshot() {
            // given
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ConsoleView consoleView = buildConsoleView(outputStream);
            when(productionService.getQueueSnapshot()).thenReturn(Collections.emptyList());

            ProductionController controller = new ProductionController(productionService, consoleView);

            // when
            controller.handleViewQueue();

            // then
            verify(productionService, times(1)).getQueueSnapshot();
        }

        @Test
        @DisplayName("getQueueSnapshot이 빈 리스트를 반환하면 '없' 또는 '비어' 메시지가 출력된다")
        void viewQueueShowsEmptyMessageWhenQueueEmpty() {
            // given
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ConsoleView consoleView = buildConsoleView(outputStream);
            when(productionService.getQueueSnapshot()).thenReturn(List.of());

            ProductionController controller = new ProductionController(productionService, consoleView);

            // when
            controller.handleViewQueue();

            // then
            String output = outputStream.toString();
            boolean hasEmptyKeyword = output.contains("없") || output.contains("비어");
            assertTrue(hasEmptyKeyword,
                    "큐가 비어 있을 때 '없' 또는 '비어'가 출력에 포함되어야 한다. 실제 출력: " + output);
        }
    }

    @Nested
    @DisplayName("Safety: 예외 처리")
    class SafetyTests {

        @Test
        @DisplayName("processNext()가 IllegalStateException을 던지면 오류 메시지를 출력하고 예외가 전파되지 않는다")
        void processQueueShowsErrorWhenQueueEmpty() {
            // given
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ConsoleView consoleView = buildConsoleView(outputStream);
            when(productionService.processNext())
                    .thenThrow(new IllegalStateException("생산 큐가 비어 있습니다."));

            ProductionController controller = new ProductionController(productionService, consoleView);

            // when & then: 예외가 밖으로 전파되지 않아야 한다
            assertDoesNotThrow(controller::handleProcessQueue,
                    "IllegalStateException이 컨트롤러 밖으로 전파되면 안 된다.");

            String output = outputStream.toString();
            assertFalse(output.isBlank(),
                    "오류 상황에서도 출력이 비어 있으면 안 된다.");
            assertTrue(output.contains("생산 큐가 비어 있습니다.") || output.contains("오류") || output.contains("에러") || output.contains("비어"),
                    "오류 메시지가 출력에 포함되어야 한다. 실제 출력: " + output);
        }
    }
}
