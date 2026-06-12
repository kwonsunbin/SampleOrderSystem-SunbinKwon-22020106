package com.ssemi.sampleorder.view;

public class MenuView {

    private final ConsoleView consoleView;

    public MenuView(ConsoleView consoleView) {
        this.consoleView = consoleView;
    }

    public void printMainMenu() {
        consoleView.print("=== 반도체 시료 생산주문관리 시스템 ===");
        consoleView.print("1. 시료 관리");
        consoleView.print("2. 주문 접수");
        consoleView.print("3. 주문 승인/거절");
        consoleView.print("4. 생산 라인 관리");
        consoleView.print("5. 모니터링");
        consoleView.print("6. 출고 처리");
        consoleView.print("0. 종료");
    }

    public void printSubMenu(String title, String... options) {
        consoleView.print("=== " + title + " ===");
        for (int i = 0; i < options.length; i++) {
            consoleView.print((i + 1) + ". " + options[i]);
        }
        consoleView.print("0. 뒤로가기");
    }
}
