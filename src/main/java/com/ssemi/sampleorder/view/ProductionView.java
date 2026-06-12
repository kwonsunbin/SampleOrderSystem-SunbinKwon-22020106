package com.ssemi.sampleorder.view;

import com.ssemi.sampleorder.model.Order;

import java.util.List;

public class ProductionView {

    private final ConsoleView consoleView;

    public ProductionView(ConsoleView consoleView) {
        this.consoleView = consoleView;
    }

    public void printQueue(List<Order> orders) {
        if (orders.isEmpty()) {
            consoleView.print("생산 큐에 대기 중인 주문이 없습니다.");
            return;
        }
        for (Order order : orders) {
            consoleView.print("[" + order.getId() + "] 고객: " + order.getCustomerName()
                    + " | 시료ID: " + order.getSampleId()
                    + " | 수량: " + order.getQuantity()
                    + " | 상태: " + order.getStatus());
        }
    }

    public void printProcessResult(Order order) {
        consoleView.print("처리 완료 - 주문ID: " + order.getId()
                + " | 상태: " + order.getStatus());
    }
}
