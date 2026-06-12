package com.ssemi.sampleorder.controller;

import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.service.MonitorService;
import com.ssemi.sampleorder.service.SampleStockInfo;
import com.ssemi.sampleorder.view.ConsoleView;
import com.ssemi.sampleorder.view.MenuView;
import com.ssemi.sampleorder.view.MonitorView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class MonitorController {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MonitorService monitorService;
    private final ConsoleView consoleView;
    private final MonitorView monitorView;
    private final MenuView menuView;

    public MonitorController(MonitorService monitorService, ConsoleView consoleView) {
        this.monitorService = monitorService;
        this.consoleView    = consoleView;
        this.monitorView    = new MonitorView(consoleView);
        this.menuView       = new MenuView(consoleView);
    }

    public void showMenu() {
        consoleView.printBlank();
        consoleView.print(
            "  " + "─".repeat(20)
            + "  " + "모니터링" + "  "
            + LocalDateTime.now().format(DT)
            + "  " + "─".repeat(6));
        menuView.printSubMenu("[4] 모니터링", "주문량 확인", "재고량 확인");
        int choice = consoleView.readInt("선택");
        switch (choice) {
            case 1 -> handleOrderMonitor();
            case 2 -> handleStockMonitor();
            case 0 -> {}
            default -> consoleView.printError("잘못된 선택입니다.");
        }
    }

    public void handleOrderMonitor() {
        try {
            Map<OrderStatus, Long> counts = monitorService.getOrderCountByStatus();
            monitorView.printOrderCounts(counts);
        } catch (Exception e) {
            consoleView.printError(e.getMessage());
        }
    }

    public void handleStockMonitor() {
        try {
            List<SampleStockInfo> infos = monitorService.getStockStatusBySample();
            monitorView.printStockStatus(infos);
        } catch (Exception e) {
            consoleView.printError(e.getMessage());
        }
    }
}
