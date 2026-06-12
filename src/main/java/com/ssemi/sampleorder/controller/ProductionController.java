package com.ssemi.sampleorder.controller;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.Sample;
import com.ssemi.sampleorder.service.ProductionProgressInfo;
import com.ssemi.sampleorder.service.ProductionService;
import com.ssemi.sampleorder.service.SampleService;
import com.ssemi.sampleorder.view.Color;
import com.ssemi.sampleorder.view.ConsoleView;
import com.ssemi.sampleorder.view.MenuView;
import com.ssemi.sampleorder.view.ProductionView;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductionController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ProductionService productionService;
    private final SampleService sampleService;
    private final ConsoleView consoleView;
    private final ProductionView productionView;
    private final MenuView menuView;

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
        Clock clock = Clock.systemDefaultZone();
        productionService.completeProductionIfReady(clock);

        // 생산 중이면 완료될 때까지 1초마다 진행률 갱신
        if (productionService.getCurrentlyProducingInfo(clock).isPresent()) {
            watchLiveProgress(clock);
        }

        handleViewProductionLine(clock);
        menuView.printSubMenu("[5] 생산라인 조회", "다음 주문 생산 시작");
        int choice = consoleView.readInt("선택");
        switch (choice) {
            case 1 -> handleStartProduction(clock);
            case 0 -> {}
            default -> consoleView.printError("잘못된 선택입니다.");
        }
    }

    private void watchLiveProgress(Clock clock) {
        while (true) {
            productionService.completeProductionIfReady(clock);
            Optional<ProductionProgressInfo> info = productionService.getCurrentlyProducingInfo(clock);
            if (info.isEmpty()) {
                consoleView.clearScreen();
                consoleView.printSuccess("생산 완료  CONFIRMED");
                break;
            }
            consoleView.clearScreen();
            productionView.printCurrentlyProducing(info.get());
            consoleView.print(Color.dim("  생산 완료 시 자동으로 메뉴가 표시됩니다..."));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // 현재 생산 중 + 대기 큐(ETA 포함) 화면
    public void handleViewProductionLine(Clock clock) {
        Optional<ProductionProgressInfo> producing = productionService.getCurrentlyProducingInfo(clock);
        producing.ifPresent(productionView::printCurrentlyProducing);

        List<Order> queue = productionService.getQueueSnapshot();
        if (sampleService != null) {
            LocalDateTime startAfter = producing
                    .map(ProductionProgressInfo::getEstimatedCompletionTime)
                    .orElse(LocalDateTime.now(clock));
            List<String[]> rows = buildEtaRows(queue, startAfter);
            productionView.printQueueWithEta(rows);
        } else {
            productionView.printQueue(queue);
        }
    }

    // 새 생산 시작: RESERVED → PRODUCING
    public void handleStartProduction(Clock clock) {
        try {
            Order result = productionService.startNextProduction(clock);
            consoleView.printSuccess("생산 시작  " + result.getId());
        } catch (Exception e) {
            consoleView.printError(e.getMessage());
        }
    }

    // ── 하위 호환 메서드 (기존 테스트 및 직접 호출용) ─────────────────────

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

    // ── 내부 헬퍼 ────────────────────────────────────────────────────────────

    private List<String[]> buildEtaRows(List<Order> queue, LocalDateTime startAfter) {
        List<String[]> rows = new ArrayList<>();
        LocalDateTime cursor = startAfter;
        for (int i = 0; i < queue.size(); i++) {
            Order o = queue.get(i);
            try {
                Sample s = sampleService.getSample(o.getSampleId());
                int shortage   = Math.max(0, o.getQuantity() - s.getStock());
                int actualProd = shortage > 0
                        ? productionService.calculateActualProduction(shortage, s.getYield()) : 0;
                int unitsToMake = actualProd > 0 ? actualProd : o.getQuantity();
                int totalTime   = productionService.calculateTotalProductionTime(
                        s.getAvgProductionTime(), unitsToMake);
                cursor = cursor.plusSeconds(totalTime);
                rows.add(new String[]{
                        String.valueOf(i + 1),
                        shortId(o.getId()),
                        s.getName(),
                        o.getQuantity() + " ea",
                        shortage > 0 ? shortage + " ea" : "-",
                        actualProd  > 0 ? actualProd + " ea" : "-",
                        cursor.format(TIME_FMT)
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
                        totalTime + " 초"
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
