package com.ssemi.sampleorder.controller;

import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.model.StockStatus;
import com.ssemi.sampleorder.service.MonitorService;
import com.ssemi.sampleorder.service.SampleStockInfo;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@DisplayName("MonitorController 테스트")
@ExtendWith(MockitoExtension.class)
class MonitorControllerTest {

    @Mock
    private MonitorService monitorService;

    // ── Regression: 주문 집계 조회 ────────────────────────────────────────────

    @Nested
    @DisplayName("주문 집계 조회")
    class OrderMonitor {

        @Test
        @DisplayName("handleOrderMonitor() 호출 시 getOrderCountByStatus()가 정확히 1회 호출됨")
        void orderMonitorCallsGetOrderCountByStatus() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            when(monitorService.getOrderCountByStatus()).thenReturn(Collections.emptyMap());

            MonitorController controller = new MonitorController(monitorService, consoleView);
            controller.handleOrderMonitor();

            verify(monitorService, times(1)).getOrderCountByStatus();
        }

        @Test
        @DisplayName("getOrderCountByStatus()가 상태별 건수를 반환하면 출력에 상태명과 숫자가 포함됨")
        void orderMonitorPrintsStatusCounts() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            Map<OrderStatus, Long> fakeCounts = Map.of(
                OrderStatus.RESERVED,  2L,
                OrderStatus.CONFIRMED, 1L,
                OrderStatus.PRODUCING, 0L,
                OrderStatus.RELEASED,  3L
            );
            when(monitorService.getOrderCountByStatus()).thenReturn(fakeCounts);

            MonitorController controller = new MonitorController(monitorService, consoleView);
            controller.handleOrderMonitor();

            String output = outputStream.toString(StandardCharsets.UTF_8);
            boolean containsStatus = output.contains("RESERVED") || output.contains("접수");
            boolean containsNumber = output.contains("2") || output.contains("3");
            assertTrue(containsStatus,
                "출력에 상태명('RESERVED' 또는 '접수')이 포함되어야 합니다. 실제 출력: " + output);
            assertTrue(containsNumber,
                "출력에 건수 숫자가 포함되어야 합니다. 실제 출력: " + output);
        }
    }

    // ── Regression: 재고 현황 조회 ────────────────────────────────────────────

    @Nested
    @DisplayName("재고 현황 조회")
    class StockMonitor {

        @Test
        @DisplayName("handleStockMonitor() 호출 시 getStockStatusBySample()이 정확히 1회 호출됨")
        void stockMonitorCallsGetStockStatusBySample() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            when(monitorService.getStockStatusBySample()).thenReturn(Collections.emptyList());

            MonitorController controller = new MonitorController(monitorService, consoleView);
            controller.handleStockMonitor();

            verify(monitorService, times(1)).getStockStatusBySample();
        }

        @Test
        @DisplayName("재고 정보가 있으면 출력에 시료명과 재고 수량이 포함됨")
        void stockMonitorPrintsStockInfo() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            SampleStockInfo fakeInfo = mock(SampleStockInfo.class);
            when(fakeInfo.getSampleId()).thenReturn("S001");
            when(fakeInfo.getSampleName()).thenReturn("GaAs");
            when(fakeInfo.getStock()).thenReturn(50);
            when(fakeInfo.getDemand()).thenReturn(10);
            when(fakeInfo.getStockStatus()).thenReturn(StockStatus.SUFFICIENT);

            when(monitorService.getStockStatusBySample()).thenReturn(List.of(fakeInfo));

            MonitorController controller = new MonitorController(monitorService, consoleView);
            controller.handleStockMonitor();

            String output = outputStream.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("GaAs"),
                "출력에 시료명 'GaAs'가 포함되어야 합니다. 실제 출력: " + output);
            assertTrue(output.contains("50"),
                "출력에 재고 수량 '50'이 포함되어야 합니다. 실제 출력: " + output);
        }

        @Test
        @DisplayName("등록된 시료가 없으면 출력에 '없'이 포함됨")
        void stockMonitorShowsEmptyMessageWhenNoSamples() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            when(monitorService.getStockStatusBySample()).thenReturn(Collections.emptyList());

            MonitorController controller = new MonitorController(monitorService, consoleView);
            controller.handleStockMonitor();

            String output = outputStream.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("없"),
                "빈 재고 목록일 때 출력에 '없'이 포함되어야 합니다. 실제 출력: " + output);
        }
    }

    // ── Safety: 예외 처리 ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Safety: 예외 처리")
    class Safety {

        @Test
        @DisplayName("getOrderCountByStatus()가 RuntimeException을 던져도 오류 메시지를 출력하고 예외가 전파되지 않음")
        void orderMonitorShowsErrorWhenServiceThrows() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            when(monitorService.getOrderCountByStatus()).thenThrow(new RuntimeException("서비스 오류"));

            MonitorController controller = new MonitorController(monitorService, consoleView);
            assertDoesNotThrow(controller::handleOrderMonitor,
                "서비스 예외가 외부로 전파되어서는 안 됩니다.");

            String output = outputStream.toString(StandardCharsets.UTF_8);
            assertTrue(output.length() > 0,
                "예외 발생 시 오류 메시지가 출력되어야 합니다.");
        }

        @Test
        @DisplayName("getStockStatusBySample()이 RuntimeException을 던져도 오류 메시지를 출력하고 예외가 전파되지 않음")
        void stockMonitorShowsErrorWhenServiceThrows() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            when(monitorService.getStockStatusBySample()).thenThrow(new RuntimeException("서비스 오류"));

            MonitorController controller = new MonitorController(monitorService, consoleView);
            assertDoesNotThrow(controller::handleStockMonitor,
                "서비스 예외가 외부로 전파되어서는 안 됩니다.");

            String output = outputStream.toString(StandardCharsets.UTF_8);
            assertTrue(output.length() > 0,
                "예외 발생 시 오류 메시지가 출력되어야 합니다.");
        }
    }
}
