package com.ssemi.sampleorder.controller;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.service.OrderService;
import com.ssemi.sampleorder.view.ConsoleView;
import com.ssemi.sampleorder.view.OrderView;

import java.util.List;

public class ReleaseController {

    private final OrderService orderService;
    private final ConsoleView consoleView;
    private final OrderView orderView;

    public ReleaseController(OrderService orderService, ConsoleView consoleView) {
        this.orderService = orderService;
        this.consoleView  = consoleView;
        this.orderView    = new OrderView(consoleView);
    }

    public void showMenu() {
        handleRelease();
    }

    public void handleRelease() {
        List<Order> confirmed = orderService.listConfirmedOrders();
        orderView.printConfirmedOrders(confirmed);
        if (confirmed.isEmpty()) return;

        int num = consoleView.readInt("출고할 번호");
        if (num < 1 || num > confirmed.size()) {
            consoleView.printError("올바른 번호를 입력하세요.");
            return;
        }
        String orderId = confirmed.get(num - 1).getId();
        try {
            Order result = orderService.releaseOrder(orderId);
            orderView.printReleaseResult(result);
        } catch (Exception e) {
            consoleView.printError(e.getMessage());
        }
    }
}
