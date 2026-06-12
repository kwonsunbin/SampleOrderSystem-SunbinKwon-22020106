package com.ssemi.sampleorder.controller;

import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.service.MonitorService;
import com.ssemi.sampleorder.service.SampleStockInfo;
import com.ssemi.sampleorder.view.ConsoleView;

import java.util.List;
import java.util.Map;

public class MonitorController {

    private final MonitorService monitorService;
    private final ConsoleView consoleView;

    public MonitorController(MonitorService monitorService, ConsoleView consoleView) {
        this.monitorService = monitorService;
        this.consoleView = consoleView;
    }

    public void showMenu() {
        consoleView.print("=== 모니터링 ===");
        consoleView.print("1. 주문 집계");
        consoleView.print("2. 재고 현황");
        consoleView.print("0. 뒤로가기");
        int choice = consoleView.readInt("선택: ");
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
            consoleView.print("=== 상태별 주문 현황 ===");
            for (Map.Entry<OrderStatus, Long> entry : counts.entrySet()) {
                consoleView.print(entry.getKey().name() + ": " + entry.getValue() + "건");
            }
        } catch (Exception e) {
            consoleView.printError(e.getMessage());
        }
    }

    public void handleStockMonitor() {
        try {
            List<SampleStockInfo> infos = monitorService.getStockStatusBySample();
            if (infos.isEmpty()) {
                consoleView.print("등록된 시료가 없습니다.");
                return;
            }
            consoleView.print("=== 재고 현황 ===");
            for (SampleStockInfo info : infos) {
                consoleView.print("[" + info.getSampleId() + "] " + info.getSampleName()
                        + " | 재고: " + info.getStock()
                        + " | 수요: " + info.getDemand()
                        + " | 상태: " + info.getStockStatus());
            }
        } catch (Exception e) {
            consoleView.printError(e.getMessage());
        }
    }
}
