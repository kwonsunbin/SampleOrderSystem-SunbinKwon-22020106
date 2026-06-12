package com.ssemi.sampleorder.view;

import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.model.StockStatus;
import com.ssemi.sampleorder.service.SampleStockInfo;

import java.util.List;
import java.util.Map;

public class MonitorView {

    private final ConsoleView consoleView;

    public MonitorView(ConsoleView consoleView) {
        this.consoleView = consoleView;
    }

    public void printOrderCounts(Map<OrderStatus, Long> counts) {
        consoleView.printBlank();
        consoleView.print(Color.header("  ◆ 상태별 주문 현황"));
        consoleView.printDivider();
        if (counts.isEmpty()) {
            consoleView.print(Color.dim("    주문 데이터가 없습니다."));
            consoleView.printDivider();
            return;
        }
        for (Map.Entry<OrderStatus, Long> entry : counts.entrySet()) {
            consoleView.print(
                "    " + statusBadge(entry.getKey())
                + "  " + Color.BRIGHT_WHITE + entry.getValue() + "건" + Color.RESET
            );
        }
        consoleView.printDivider();
    }

    public void printStockStatus(List<SampleStockInfo> infos) {
        consoleView.printBlank();
        consoleView.print(Color.header("  ◆ 재고 현황"));
        consoleView.printDivider();
        if (infos.isEmpty()) {
            consoleView.print(Color.dim("    등록된 시료가 없습니다."));
            consoleView.printDivider();
            return;
        }
        for (SampleStockInfo info : infos) {
            consoleView.print(
                "  " + Color.YELLOW + "[" + info.getSampleId() + "]" + Color.RESET
                + "  " + Color.bold(info.getSampleName())
                + "  " + Color.dim("재고:") + " " + Color.BRIGHT_WHITE + info.getStock() + Color.RESET
                + "  " + Color.dim("수요:") + " " + Color.BRIGHT_WHITE + info.getDemand() + Color.RESET
                + "  " + stockStatusBadge(info.getStockStatus())
            );
        }
        consoleView.printDivider();
    }

    private String statusBadge(OrderStatus status) {
        switch (status) {
            case RESERVED:   return Color.BRIGHT_YELLOW + "[RESERVED ]" + Color.RESET;
            case REJECTED:   return Color.BRIGHT_RED    + "[REJECTED ]" + Color.RESET;
            case PRODUCING:  return Color.BRIGHT_CYAN   + "[PRODUCING]" + Color.RESET;
            case CONFIRMED:  return Color.BLUE          + "[CONFIRMED]" + Color.RESET;
            case RELEASED:   return Color.BRIGHT_GREEN  + "[RELEASED ]" + Color.RESET;
            default:         return Color.WHITE         + "[" + status.name() + "]" + Color.RESET;
        }
    }

    private String stockStatusBadge(StockStatus status) {
        switch (status) {
            case SUFFICIENT:  return Color.BRIGHT_GREEN  + "[충분]" + Color.RESET;
            case SHORTAGE:    return Color.BRIGHT_YELLOW + "[부족]" + Color.RESET;
            case DEPLETED:    return Color.BRIGHT_RED    + "[고갈]" + Color.RESET;
            default:          return Color.WHITE         + "[" + status.name() + "]" + Color.RESET;
        }
    }
}
