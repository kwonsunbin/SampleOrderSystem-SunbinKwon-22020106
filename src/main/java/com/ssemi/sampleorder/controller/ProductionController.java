package com.ssemi.sampleorder.controller;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.service.ProductionService;
import com.ssemi.sampleorder.view.ConsoleView;

import java.util.List;

public class ProductionController {

    private final ProductionService productionService;
    private final ConsoleView consoleView;

    public ProductionController(ProductionService productionService, ConsoleView consoleView) {
        this.productionService = productionService;
        this.consoleView = consoleView;
    }

    public void showMenu() {
        consoleView.print("=== 생산 라인 관리 ===");
        consoleView.print("1. 생산 큐 조회");
        consoleView.print("2. 다음 주문 처리");
        consoleView.print("0. 뒤로가기");
        int choice = consoleView.readInt("선택: ");
        switch (choice) {
            case 1 -> handleViewQueue();
            case 2 -> handleProcessQueue();
            case 0 -> {}
            default -> consoleView.printError("잘못된 선택입니다.");
        }
    }

    public void handleViewQueue() {
        List<Order> queue = productionService.getQueueSnapshot();
        if (queue.isEmpty()) {
            consoleView.print("생산 큐에 대기 중인 주문이 없습니다.");
            return;
        }
        for (Order o : queue) {
            consoleView.print("[" + o.getId() + "] 고객: " + o.getCustomerName()
                    + " | 수량: " + o.getQuantity() + " | 상태: " + o.getStatus());
        }
    }

    public void handleProcessQueue() {
        try {
            Order result = productionService.processNext();
            consoleView.printSuccess("처리 완료 (CONFIRMED) - 주문ID: " + result.getId()
                    + " | 상태: " + result.getStatus());
        } catch (Exception e) {
            consoleView.printError(e.getMessage());
        }
    }
}
