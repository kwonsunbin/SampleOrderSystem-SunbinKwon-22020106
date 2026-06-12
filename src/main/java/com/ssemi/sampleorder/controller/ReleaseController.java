package com.ssemi.sampleorder.controller;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.service.OrderService;
import com.ssemi.sampleorder.view.ConsoleView;

import java.util.List;

public class ReleaseController {

    private final OrderService orderService;
    private final ConsoleView consoleView;

    public ReleaseController(OrderService orderService, ConsoleView consoleView) {
        this.orderService = orderService;
        this.consoleView = consoleView;
    }

    public void showMenu() {
        handleRelease();
    }

    public void handleRelease() {
        List<Order> confirmed = orderService.listConfirmedOrders();
        if (confirmed.isEmpty()) {
            consoleView.print("출고 대기 중인 주문이 없습니다.");
            return;
        }
        for (Order o : confirmed) {
            consoleView.print("[" + o.getId() + "] 고객: " + o.getCustomerName()
                    + " | 수량: " + o.getQuantity() + " | 상태: " + o.getStatus());
        }
        String orderId = consoleView.readString("출고할 주문 ID: ");
        try {
            Order result = orderService.releaseOrder(orderId);
            consoleView.printSuccess("출고 완료 (" + result.getStatus() + ") - 주문ID: " + result.getId());
        } catch (Exception e) {
            consoleView.printError(e.getMessage());
        }
    }
}
