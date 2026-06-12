package com.ssemi.sampleorder.view;

import com.ssemi.sampleorder.model.Order;

import java.util.List;

public class OrderView {

    private final ConsoleView consoleView;

    public OrderView(ConsoleView consoleView) {
        this.consoleView = consoleView;
    }

    public void printOrders(List<Order> orders) {
        if (orders.isEmpty()) {
            consoleView.print("대기 중인 주문이 없습니다.");
            return;
        }
        for (Order order : orders) {
            printOrder(order);
        }
    }

    public void printOrder(Order order) {
        consoleView.print("[" + order.getId() + "] 고객: " + order.getCustomerName()
                + " | 시료ID: " + order.getSampleId()
                + " | 수량: " + order.getQuantity()
                + " | 상태: " + order.getStatus());
    }
}
