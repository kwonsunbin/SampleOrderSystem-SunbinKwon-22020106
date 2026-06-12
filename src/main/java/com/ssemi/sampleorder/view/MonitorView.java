package com.ssemi.sampleorder.view;

import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.model.StockStatus;
import com.ssemi.sampleorder.service.SampleStockInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MonitorView {

    private final ConsoleView cv;

    public MonitorView(ConsoleView cv) {
        this.cv = cv;
    }

    public void printOrderCounts(Map<OrderStatus, Long> counts) {
        cv.printBlank();
        cv.printSectionHeader("상태별 주문 현황");
        if (counts.isEmpty()) {
            cv.print(Color.dim("    주문 데이터가 없습니다."));
            cv.printBlank();
            return;
        }

        OrderStatus[] order = {
            OrderStatus.RESERVED, OrderStatus.PRODUCING,
            OrderStatus.CONFIRMED, OrderStatus.RELEASED
        };
        for (OrderStatus status : order) {
            long cnt = counts.getOrDefault(status, 0L);
            String badge = statusBadge(status);
            String count = Color.BRIGHT_WHITE + String.format("%3d 건", cnt) + Color.RESET;
            String note  = statusNote(status, cnt);
            cv.print("    " + badge + "  " + count + (note.isEmpty() ? "" : "  " + note));
        }
        cv.printBlank();
    }

    public void printStockStatus(List<SampleStockInfo> infos) {
        cv.printBlank();
        cv.printSectionHeader("재고 현황");
        if (infos.isEmpty()) {
            cv.print(Color.dim("    등록된 시료가 없습니다."));
            cv.printBlank();
            return;
        }

        int[] cols = {10, 20, 8, 8, 6};
        String[] hdr = {"ID", "시료명", "재고", "수요", "상태"};
        List<String[]> rows = new ArrayList<>();
        for (SampleStockInfo info : infos) {
            String id = info.getSampleId();
            rows.add(new String[]{
                id.length() > 10 ? "…" + id.substring(id.length() - 9) : id,
                info.getSampleName(),
                info.getStock() + " ea",
                info.getDemand() > 0 ? info.getDemand() + " ea" : "-",
                stockLabel(info.getStockStatus())
            });
        }
        cv.printTable(cols, hdr, rows);
        cv.printBlank();
    }

    private String statusBadge(OrderStatus s) {
        switch (s) {
            case RESERVED:  return Color.BRIGHT_YELLOW + "RESERVED " + Color.RESET;
            case PRODUCING: return Color.BRIGHT_CYAN   + "PRODUCING" + Color.RESET;
            case CONFIRMED: return Color.BLUE          + "CONFIRMED" + Color.RESET;
            case RELEASED:  return Color.BRIGHT_GREEN  + "RELEASED " + Color.RESET;
            default:        return Color.WHITE + s.name() + Color.RESET;
        }
    }

    private String statusNote(OrderStatus s, long cnt) {
        if (s == OrderStatus.PRODUCING && cnt > 0)
            return Color.dim("← 생산라인 대기");
        return "";
    }

    private String stockLabel(StockStatus s) {
        switch (s) {
            case SUFFICIENT: return Color.BRIGHT_GREEN  + "여유" + Color.RESET;
            case SHORTAGE:   return Color.BRIGHT_YELLOW + "부족" + Color.RESET;
            case DEPLETED:   return Color.BRIGHT_RED    + "고갈" + Color.RESET;
            default:         return s.name();
        }
    }
}
