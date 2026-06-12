package com.ssemi.sampleorder.view;

import com.ssemi.sampleorder.model.Order;

import java.util.List;

public class ReleaseView {

    private final ConsoleView cv;

    public ReleaseView(ConsoleView cv) {
        this.cv = cv;
    }

    public void printConfirmedOrders(List<Order> orders) {
        // OrderView.printConfirmedOrders 로 통합 — 하위 호환용 유지
        cv.printBlank();
        cv.printSectionHeader("출고 가능 주문  (CONFIRMED)");
        if (orders.isEmpty()) {
            cv.print(Color.dim("    출고 대기 중인 주문이 없습니다."));
            cv.printBlank();
            return;
        }
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            cv.print("  " + Color.YELLOW + "[" + (i + 1) + "]" + Color.RESET
                    + "  " + Color.dim("주문: ") + Color.BRIGHT_WHITE + shortId(o.getId()) + Color.RESET
                    + "  " + Color.dim("고객: ") + Color.BOLD + o.getCustomerName() + Color.RESET
                    + "  " + Color.dim("수량: ") + Color.BRIGHT_WHITE + o.getQuantity() + " ea" + Color.RESET);
        }
        cv.printBlank();
    }

    public void printReleaseResult(Order order) {
        cv.printSuccess("출고 완료  " + Color.BOLD + "RELEASED" + Color.RESET
                + "  " + Color.dim(shortId(order.getId())));
    }

    private String shortId(String id) {
        return id.length() > 13 ? "…" + id.substring(id.length() - 12) : id;
    }
}
