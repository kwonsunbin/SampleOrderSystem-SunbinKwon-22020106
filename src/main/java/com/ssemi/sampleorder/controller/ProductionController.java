package com.ssemi.sampleorder.controller;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.Sample;
import com.ssemi.sampleorder.service.ProductionService;
import com.ssemi.sampleorder.service.SampleService;
import com.ssemi.sampleorder.view.ConsoleView;
import com.ssemi.sampleorder.view.MenuView;
import com.ssemi.sampleorder.view.ProductionView;

import java.util.ArrayList;
import java.util.List;

public class ProductionController {

    private final ProductionService productionService;
    private final SampleService sampleService;   // nullable
    private final ConsoleView consoleView;
    private final ProductionView productionView;
    private final MenuView menuView;

    // 하위 호환 생성자 (테스트용)
    public ProductionController(ProductionService productionService, ConsoleView consoleView) {
        this(productionService, null, consoleView);
    }

    public ProductionController(ProductionService productionService,
                                SampleService sampleService,
                                ConsoleView consoleView) {
        this.productionService = productionService;
        this.sampleService     = sampleService;
        this.consoleView       = consoleView;
        this.productionView    = new ProductionView(consoleView);
        this.menuView          = new MenuView(consoleView);
    }

    public void showMenu() {
        handleViewQueue();
        menuView.printSubMenu("[5] 생산라인 조회", "다음 주문 처리");
        int choice = consoleView.readInt("선택");
        switch (choice) {
            case 1 -> handleProcessQueue();
            case 0 -> {}
            default -> consoleView.printError("잘못된 선택입니다.");
        }
    }

    public void handleViewQueue() {
        List<Order> queue = productionService.getQueueSnapshot();
        if (sampleService != null) {
            List<String[]> enriched = buildEnrichedRows(queue);
            productionView.printQueueEnriched(enriched);
        } else {
            productionView.printQueue(queue);
        }
    }

    public void handleProcessQueue() {
        try {
            Order result = productionService.processNext();
            productionView.printProcessResult(result);
        } catch (Exception e) {
            consoleView.printError(e.getMessage());
        }
    }

    private List<String[]> buildEnrichedRows(List<Order> queue) {
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) {
            Order o = queue.get(i);
            try {
                Sample s = sampleService.getSample(o.getSampleId());
                int shortage   = Math.max(0, o.getQuantity() - s.getStock());
                int actualProd = shortage > 0
                    ? productionService.calculateActualProduction(shortage, s.getYield()) : 0;
                int totalTime  = productionService.calculateTotalProductionTime(
                    s.getAvgProductionTime(), actualProd > 0 ? actualProd : o.getQuantity());
                rows.add(new String[]{
                    String.valueOf(i + 1),
                    shortId(o.getId()),
                    s.getName(),
                    o.getQuantity() + " ea",
                    shortage > 0 ? shortage + " ea" : "-",
                    actualProd > 0 ? actualProd + " ea" : "-",
                    totalTime + " 분"
                });
            } catch (Exception e) {
                rows.add(new String[]{
                    String.valueOf(i + 1), shortId(o.getId()),
                    o.getSampleId(), o.getQuantity() + " ea", "?", "?", "?"
                });
            }
        }
        return rows;
    }

    private String shortId(String id) {
        return id.length() > 13 ? "…" + id.substring(id.length() - 12) : id;
    }
}
