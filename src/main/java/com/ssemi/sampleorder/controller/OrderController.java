package com.ssemi.sampleorder.controller;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.service.OrderService;
import com.ssemi.sampleorder.view.ConsoleView;

import java.util.List;

public class OrderController {

    private final OrderService orderService;
    private final ConsoleView consoleView;

    public OrderController(OrderService orderService, ConsoleView consoleView) {
        this.orderService = orderService;
        this.consoleView = consoleView;
    }

    public void showMenu() {
        consoleView.print("=== 주문 관리 ===");
        consoleView.print("1. 주문 접수");
        consoleView.print("2. 주문 승인");
        consoleView.print("3. 주문 거절");
        consoleView.print("0. 뒤로가기");
        int choice = consoleView.readInt("선택: ");
        switch (choice) {
            case 1 -> handleReserve();
            case 2 -> handleApprove();
            case 3 -> handleReject();
            case 0 -> {}
            default -> consoleView.printError("잘못된 선택입니다.");
        }
    }

    public void handleReserve() {
        String sampleId = consoleView.readString("시료 ID: ");
        String customerName = consoleView.readString("고객명: ");
        int quantity = consoleView.readInt("수량: ");
        try {
            Order order = orderService.reserveOrder(sampleId, customerName, quantity);
            consoleView.printSuccess("주문 접수 완료 - 주문ID: " + order.getId() + " | 상태: " + order.getStatus());
        } catch (Exception e) {
            consoleView.printError(e.getMessage());
        }
    }

    public void handleApprove() {
        List<Order> reserved = orderService.listReservedOrders();
        if (reserved.isEmpty()) {
            consoleView.print("대기 중인 주문이 없습니다.");
            return;
        }
        for (Order o : reserved) {
            consoleView.print("[" + o.getId() + "] 고객: " + o.getCustomerName()
                    + " | 수량: " + o.getQuantity() + " | 상태: " + o.getStatus());
        }
        String orderId = consoleView.readString("승인할 주문 ID: ");
        try {
            Order result = orderService.approveOrder(orderId);
            if (result.getStatus() == OrderStatus.CONFIRMED) {
                consoleView.printSuccess("주문 승인 완료 (CONFIRMED) - 주문ID: " + result.getId());
            } else {
                consoleView.printSuccess("주문 승인 - 생산 대기 중 (PRODUCING) - 주문ID: " + result.getId());
            }
        } catch (Exception e) {
            consoleView.printError(e.getMessage());
        }
    }

    public void handleReject() {
        List<Order> reserved = orderService.listReservedOrders();
        if (reserved.isEmpty()) {
            consoleView.print("대기 중인 주문이 없습니다.");
            return;
        }
        for (Order o : reserved) {
            consoleView.print("[" + o.getId() + "] 고객: " + o.getCustomerName()
                    + " | 수량: " + o.getQuantity() + " | 상태: " + o.getStatus());
        }
        String orderId = consoleView.readString("거절할 주문 ID: ");
        try {
            Order result = orderService.rejectOrder(orderId);
            consoleView.printSuccess("주문 거절 완료 (" + result.getStatus() + ") - 주문ID: " + result.getId());
        } catch (Exception e) {
            consoleView.printError(e.getMessage());
        }
    }
}
