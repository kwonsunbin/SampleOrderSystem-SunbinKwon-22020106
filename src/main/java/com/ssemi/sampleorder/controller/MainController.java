package com.ssemi.sampleorder.controller;

import com.ssemi.sampleorder.view.ConsoleView;

public class MainController {

    private final SampleController sampleController;
    private final OrderController orderController;
    private final ProductionController productionController;
    private final MonitorController monitorController;
    private final ReleaseController releaseController;
    private final ConsoleView consoleView;

    public MainController(SampleController sampleController,
                          OrderController orderController,
                          ProductionController productionController,
                          MonitorController monitorController,
                          ReleaseController releaseController,
                          ConsoleView consoleView) {
        this.sampleController = sampleController;
        this.orderController = orderController;
        this.productionController = productionController;
        this.monitorController = monitorController;
        this.releaseController = releaseController;
        this.consoleView = consoleView;
    }

    public void run() {
        while (true) {
            consoleView.print("=== 반도체 시료 생산주문관리 시스템 ===");
            consoleView.print("1. 시료 관리");
            consoleView.print("2. 주문 접수");
            consoleView.print("3. 주문 승인/거절");
            consoleView.print("4. 생산 라인 관리");
            consoleView.print("5. 모니터링");
            consoleView.print("6. 출고 처리");
            consoleView.print("0. 종료");
            int choice = consoleView.readInt("선택: ");
            switch (choice) {
                case 1 -> sampleController.showMenu();
                case 2 -> orderController.showMenu();
                case 3 -> orderController.showMenu();
                case 4 -> productionController.showMenu();
                case 5 -> monitorController.showMenu();
                case 6 -> releaseController.showMenu();
                case 0 -> { return; }
                default -> consoleView.printError("잘못된 선택입니다. 0~6 사이의 숫자를 입력하세요.");
            }
        }
    }
}
