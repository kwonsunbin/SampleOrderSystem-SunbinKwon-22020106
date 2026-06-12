package com.ssemi.sampleorder.view;

import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.service.SampleStockInfo;

import java.util.List;
import java.util.Map;

public class MonitorView {

    private final ConsoleView consoleView;

    public MonitorView(ConsoleView consoleView) {
        this.consoleView = consoleView;
    }

    public void printOrderCounts(Map<OrderStatus, Long> counts) {
        consoleView.print("=== 상태별 주문 현황 ===");
        for (Map.Entry<OrderStatus, Long> entry : counts.entrySet()) {
            consoleView.print(entry.getKey().name() + ": " + entry.getValue() + "건");
        }
    }

    public void printStockStatus(List<SampleStockInfo> infos) {
        if (infos.isEmpty()) {
            consoleView.print("등록된 시료가 없습니다.");
            return;
        }
        consoleView.print("=== 재고 현황 ===");
        for (SampleStockInfo info : infos) {
            consoleView.print("[" + info.getSampleId() + "] " + info.getSampleName()
                    + " | 재고: " + info.getStock()
                    + " | 수요: " + info.getDemand()
                    + " | 상태: " + info.getStockStatus());
        }
    }
}
