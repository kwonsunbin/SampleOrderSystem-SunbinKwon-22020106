package com.ssemi.sampleorder.view;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.OrderStatus;

import java.util.List;

public class OrderView {

    private final ConsoleView consoleView;

    public OrderView(ConsoleView consoleView) {
        this.consoleView = consoleView;
    }

    public void printOrders(List<Order> orders) {
        consoleView.printBlank();
        consoleView.print(Color.header("  ◆ 주문 목록"));
        consoleView.printDivider();
        if (orders.isEmpty()) {
            consoleView.print(Color.dim("    대기 중인 주문이 없습니다."));
            consoleView.printDivider();
            return;
        }
        for (Order order : orders) {
            printOrder(order);
        }
        consoleView.printDivider();
    }

    public void printOrder(Order order) {
        consoleView.print(
            "  " + Color.YELLOW + "[" + order.getId() + "]" + Color.RESET
            + "  " + Color.dim("고객:") + " " + Color.bold(order.getCustomerName())
            + "  " + Color.dim("시료ID:") + " " + Color.BRIGHT_WHITE + order.getSampleId() + Color.RESET
            + "  " + Color.dim("수량:") + " " + Color.BRIGHT_WHITE + order.getQuantity() + Color.RESET
            + "  " + statusBadge(order.getStatus())
        );
    }

    private String statusBadge(OrderStatus status) {
        switch (status) {
            case RESERVED:   return Color.BRIGHT_YELLOW + "[" + status.name() + "]" + Color.RESET;
            case REJECTED:   return Color.BRIGHT_RED    + "[" + status.name() + "]" + Color.RESET;
            case PRODUCING:  return Color.BRIGHT_CYAN   + "[" + status.name() + "]" + Color.RESET;
            case CONFIRMED:  return Color.BLUE          + "[" + status.name() + "]" + Color.RESET;
            case RELEASED:   return Color.BRIGHT_GREEN  + "[" + status.name() + "]" + Color.RESET;
            default:         return Color.WHITE         + "[" + status.name() + "]" + Color.RESET;
        }
    }
}
