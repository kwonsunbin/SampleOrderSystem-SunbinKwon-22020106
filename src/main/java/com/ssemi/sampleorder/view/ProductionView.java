package com.ssemi.sampleorder.view;

import com.ssemi.sampleorder.model.Order;

import java.util.ArrayList;
import java.util.List;

public class ProductionView {

    // 컬럼 너비: 순서, 주문번호, 시료명, 주문량, 부족분, 실생산량, 예상시간
    private static final int[] COLS = {4, 13, 18, 7, 7, 8, 8};
    private static final String[] HDR = {"순서", "주문번호", "시료명", "주문량", "부족분", "실생산량", "예상시간"};

    private final ConsoleView cv;

    public ProductionView(ConsoleView cv) {
        this.cv = cv;
    }

    public void printQueue(List<Order> orders) {
        cv.printBlank();
        cv.printSectionHeader("생산라인 조회  — FIFO 방식");
        if (orders.isEmpty()) {
            cv.print(Color.dim("    생산 큐에 대기 중인 주문이 없습니다."));
            cv.printBlank();
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            rows.add(new String[]{
                String.valueOf(i + 1),
                shortId(o.getId()),
                o.getSampleId(),
                o.getQuantity() + " ea",
                "-", "-", "-"
            });
        }
        cv.printTable(COLS, HDR, rows);
        cv.printBlank();
        cv.print(Color.dim("  ※ 선입선출(FIFO) 방식으로 처리됩니다."));
        cv.printBlank();
    }

    public void printQueueEnriched(List<String[]> entries) {
        cv.printBlank();
        cv.printSectionHeader("생산라인 조회  — FIFO 방식");
        if (entries.isEmpty()) {
            cv.print(Color.dim("    생산 큐에 대기 중인 주문이 없습니다."));
            cv.printBlank();
            return;
        }
        cv.print(Color.BOLD + Color.BRIGHT_WHITE + "  대기 중인 주문  (" + entries.size() + " 건)" + Color.RESET);
        cv.printBlank();
        cv.printTable(COLS, HDR, entries);
        cv.printBlank();
        cv.print(Color.dim("  ※ 부족분 = 주문량 - 재고,  실생산량 = ⌈부족분 / (수율 × 0.9)⌉"));
        cv.print(Color.dim("  ※ 선입선출(FIFO) 방식으로 처리됩니다."));
        cv.printBlank();
    }

    public void printProcessResult(Order order) {
        cv.printSuccess("생산 처리 완료  " + Color.BOLD + "CONFIRMED" + Color.RESET
                + "  " + Color.dim(shortId(order.getId())));
    }

    private String shortId(String id) {
        return id.length() > 13 ? "…" + id.substring(id.length() - 12) : id;
    }
}
