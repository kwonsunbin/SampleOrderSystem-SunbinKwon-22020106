package com.ssemi.sampleorder.view;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.service.ProductionProgressInfo;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProductionView {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // 컬럼 너비: 순서, 주문번호, 시료명, 주문량, 부족분, 실생산량, 예상시간
    private static final int[] COLS = {4, 13, 18, 7, 7, 8, 8};
    private static final String[] HDR = {"순서", "주문번호", "시료명", "주문량", "부족분", "실생산량", "예상시간"};

    // 대기 큐 + 예상 완료 시각 컬럼
    private static final int[] ETA_COLS = {4, 13, 18, 7, 7, 8, 8};
    private static final String[] ETA_HDR = {"순서", "주문번호", "시료명", "주문량", "부족분", "실생산량", "예상 완료"};

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

    public void printCurrentlyProducing(ProductionProgressInfo info) {
        cv.printBlank();
        cv.print(Color.BOLD + Color.BRIGHT_WHITE + "  ▶ 현재 처리 중" + Color.RESET);
        cv.printBlank();
        cv.print("  주문번호 " + Color.BOLD + shortId(info.getOrder().getId()) + Color.RESET
                + "  시료 " + Color.BOLD + info.getSampleName() + Color.RESET);
        cv.print(String.format("  재고 %d ea → 부족 %d ea → 실생산량 %d ea  (수율 %.2f / %d sec)",
                info.getCurrentStock(), info.getShortage(), info.getActualProduction(),
                info.getYield(), info.getAvgProductionTime()));
        cv.printBlank();
        String eta = info.getEstimatedCompletionTime().format(TIME_FMT);
        cv.print(String.format("  %d%% 완료  예정 %s", info.getProgressPercent(), eta));
        cv.print("  " + buildProgressBar(info.getProgressPercent()));
        cv.printBlank();
    }

    public void printQueueWithEta(List<String[]> entries) {
        cv.printBlank();
        cv.printSectionHeader("생산라인 조회  — FIFO 방식");
        if (entries.isEmpty()) {
            cv.print(Color.dim("    대기 중인 주문이 없습니다."));
            cv.printBlank();
            return;
        }
        cv.print(Color.BOLD + Color.BRIGHT_WHITE + "  대기 중인 주문  (" + entries.size() + " 건)" + Color.RESET);
        cv.printBlank();
        cv.printTable(ETA_COLS, ETA_HDR, entries);
        cv.printBlank();
        cv.print(Color.dim("  ※ 부족분 = 주문량 - 재고,  실생산량 = ⌈부족분 / (수율 × 0.9)⌉"));
        cv.print(Color.dim("  ※ 선입선출(FIFO) 방식으로 처리됩니다."));
        cv.printBlank();
    }

    private String buildProgressBar(int percent) {
        int total = 20;
        int filled = (int) Math.round(percent / 100.0 * total);
        return "[" + "█".repeat(filled) + "░".repeat(total - filled) + "]";
    }

    private String shortId(String id) {
        return id.length() > 13 ? "…" + id.substring(id.length() - 12) : id;
    }
}
