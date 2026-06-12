package com.ssemi.sampleorder.view;

import com.ssemi.sampleorder.model.Order;

import java.util.List;

public class ReleaseView {

    private final ConsoleView consoleView;

    public ReleaseView(ConsoleView consoleView) {
        this.consoleView = consoleView;
    }

    public void printConfirmedOrders(List<Order> orders) {
        if (orders.isEmpty()) {
            consoleView.print("출고 대기 중인 주문이 없습니다.");
            return;
        }
        for (Order order : orders) {
            consoleView.print("[" + order.getId() + "] 고객: " + order.getCustomerName()
                    + " | 수량: " + order.getQuantity()
                    + " | 상태: " + order.getStatus());
        }
    }

    public void printReleaseResult(Order order) {
        consoleView.printSuccess("출고 완료 - 주문ID: " + order.getId() + " | 상태: " + order.getStatus());
    }
}
