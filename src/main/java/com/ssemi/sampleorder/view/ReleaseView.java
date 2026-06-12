package com.ssemi.sampleorder.view;

import com.ssemi.sampleorder.model.Order;

import java.util.List;

public class ReleaseView {

    private final ConsoleView consoleView;

    public ReleaseView(ConsoleView consoleView) {
        this.consoleView = consoleView;
    }

    public void printConfirmedOrders(List<Order> orders) {
        consoleView.printBlank();
        consoleView.print(Color.header("  ◆ 출고 대기 주문"));
        consoleView.printDivider();
        if (orders.isEmpty()) {
            consoleView.print(Color.dim("    출고 대기 중인 주문이 없습니다."));
            consoleView.printDivider();
            return;
        }
        for (Order order : orders) {
            consoleView.print(
                "  " + Color.BLUE + "[" + order.getId() + "]" + Color.RESET
                + "  " + Color.dim("고객:") + " " + Color.bold(order.getCustomerName())
                + "  " + Color.dim("수량:") + " " + Color.BRIGHT_WHITE + order.getQuantity() + Color.RESET
                + "  " + Color.BLUE + "[" + order.getStatus().name() + "]" + Color.RESET
            );
        }
        consoleView.printDivider();
    }

    public void printReleaseResult(Order order) {
        consoleView.printSuccess("출고 완료  주문ID: " + order.getId()
                + "  상태: " + order.getStatus().name());
    }
}
