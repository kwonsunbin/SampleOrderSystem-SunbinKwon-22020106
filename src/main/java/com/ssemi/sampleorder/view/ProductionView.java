package com.ssemi.sampleorder.view;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.OrderStatus;

import java.util.List;

public class ProductionView {

    private final ConsoleView consoleView;

    public ProductionView(ConsoleView consoleView) {
        this.consoleView = consoleView;
    }

    public void printQueue(List<Order> orders) {
        consoleView.printBlank();
        consoleView.print(Color.header("  ◆ 생산 큐"));
        consoleView.printDivider();
        if (orders.isEmpty()) {
            consoleView.print(Color.dim("    생산 큐에 대기 중인 주문이 없습니다."));
            consoleView.printDivider();
            return;
        }
        for (Order order : orders) {
            consoleView.print(
                "  " + Color.BRIGHT_CYAN + "[" + order.getId() + "]" + Color.RESET
                + "  " + Color.dim("고객:") + " " + Color.bold(order.getCustomerName())
                + "  " + Color.dim("시료ID:") + " " + Color.BRIGHT_WHITE + order.getSampleId() + Color.RESET
                + "  " + Color.dim("수량:") + " " + Color.BRIGHT_WHITE + order.getQuantity() + Color.RESET
                + "  " + statusBadge(order.getStatus())
            );
        }
        consoleView.printDivider();
    }

    public void printProcessResult(Order order) {
        consoleView.printSuccess("생산 처리 완료  주문ID: " + order.getId()
                + "  상태: " + order.getStatus().name());
    }

    private String statusBadge(OrderStatus status) {
        switch (status) {
            case RESERVED:   return Color.BRIGHT_YELLOW + "[" + status.name() + "]" + Color.RESET;
            case PRODUCING:  return Color.BRIGHT_CYAN   + "[" + status.name() + "]" + Color.RESET;
            case CONFIRMED:  return Color.BLUE          + "[" + status.name() + "]" + Color.RESET;
            default:         return Color.WHITE         + "[" + status.name() + "]" + Color.RESET;
        }
    }
}
