package com.ssemi.sampleorder.controller;

import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.service.MonitorService;
import com.ssemi.sampleorder.service.ProductionService;
import com.ssemi.sampleorder.service.SampleService;
import com.ssemi.sampleorder.view.ConsoleView;
import com.ssemi.sampleorder.view.MenuView;

import java.time.Clock;
import java.util.Map;

public class MainController {

    private final SampleController sampleController;
    private final OrderController orderController;
    private final ProductionController productionController;
    private final MonitorController monitorController;
    private final ReleaseController releaseController;
    private final ConsoleView consoleView;
    private final MenuView menuView;
    private final SampleService sampleService;         // nullable
    private final MonitorService monitorService;       // nullable
    private final ProductionService productionService; // nullable

    // 하위 호환 생성자 (테스트용)
    public MainController(SampleController sampleController,
                          OrderController orderController,
                          ProductionController productionController,
                          MonitorController monitorController,
                          ReleaseController releaseController,
                          ConsoleView consoleView) {
        this(sampleController, orderController, productionController,
             monitorController, releaseController, consoleView, null, null, null);
    }

    public MainController(SampleController sampleController,
                          OrderController orderController,
                          ProductionController productionController,
                          MonitorController monitorController,
                          ReleaseController releaseController,
                          ConsoleView consoleView,
                          SampleService sampleService,
                          MonitorService monitorService,
                          ProductionService productionService) {
        this.sampleController     = sampleController;
        this.orderController      = orderController;
        this.productionController = productionController;
        this.monitorController    = monitorController;
        this.releaseController    = releaseController;
        this.consoleView          = consoleView;
        this.menuView             = new MenuView(consoleView);
        this.sampleService        = sampleService;
        this.monitorService       = monitorService;
        this.productionService    = productionService;
    }

    public void run() {
        while (true) {
            tickProductionCompletion();
            showMainMenu();
            int choice = consoleView.readInt("선택");
            switch (choice) {
                case 1 -> sampleController.showMenu();
                case 2 -> orderController.showMenu();
                case 3 -> orderController.showApproveMenu();
                case 4 -> monitorController.showMenu();
                case 5 -> productionController.showMenu();
                case 6 -> releaseController.showMenu();
                case 0 -> { return; }
                default -> consoleView.printError("잘못된 선택입니다. 0~6 사이의 숫자를 입력하세요.");
            }
        }
    }

    private void tickProductionCompletion() {
        if (productionService != null) {
            productionService.completeProductionIfReady(Clock.systemDefaultZone());
        }
    }

    private void showMainMenu() {
        if (sampleService != null && monitorService != null) {
            try {
                int sampleCount  = sampleService.listSamples().size();
                int totalStock   = sampleService.listSamples().stream()
                    .mapToInt(s -> s.getStock()).sum();
                Map<OrderStatus, Long> counts = monitorService.getOrderCountByStatus();
                long orderCount    = counts.values().stream().mapToLong(Long::longValue).sum();
                long producingCount = counts.getOrDefault(OrderStatus.PRODUCING, 0L);
                menuView.printMainMenu(sampleCount, orderCount, totalStock, producingCount);
                return;
            } catch (Exception ignored) {
                // stats 실패 시 기본 메뉴로 fallback
            }
        }
        menuView.printMainMenu();
    }
}
