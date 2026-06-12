package com.ssemi.sampleorder.view;

public class MenuView {

    private static final int BANNER_WIDTH = 58;

    private final ConsoleView consoleView;

    public MenuView(ConsoleView consoleView) {
        this.consoleView = consoleView;
    }

    public void printMainMenu() {
        consoleView.printBlank();
        printBanner();
        consoleView.printBlank();
        consoleView.print(Color.BOLD + Color.BRIGHT_WHITE + "  메인 메뉴" + Color.RESET);
        consoleView.printDivider();
        printMenuItem("1", "시료 관리");
        printMenuItem("2", "주문 접수");
        printMenuItem("3", "주문 승인/거절");
        printMenuItem("4", "생산 라인 관리");
        printMenuItem("5", "모니터링");
        printMenuItem("6", "출고 처리");
        consoleView.printDivider();
        printMenuItem("0", "종료");
        consoleView.printBlank();
    }

    public void printSubMenu(String title, String... options) {
        consoleView.printBlank();
        printSectionHeader(title);
        consoleView.printDivider();
        for (int i = 0; i < options.length; i++) {
            printMenuItem(String.valueOf(i + 1), options[i]);
        }
        consoleView.printDivider();
        printMenuItem("0", "뒤로가기");
        consoleView.printBlank();
    }

    private void printBanner() {
        String border = "═".repeat(BANNER_WIDTH);
        String title  = "  반도체 시료 생산주문관리 시스템  ";
        int pad = (BANNER_WIDTH - title.length()) / 2;
        String padded = " ".repeat(Math.max(0, pad)) + title;

        consoleView.print(Color.BOLD + Color.BRIGHT_CYAN + "  ╔" + border + "╗" + Color.RESET);
        consoleView.print(Color.BOLD + Color.BRIGHT_CYAN + "  ║" + padded
                + " ".repeat(Math.max(0, BANNER_WIDTH - padded.length())) + "║" + Color.RESET);
        consoleView.print(Color.BOLD + Color.BRIGHT_CYAN + "  ╚" + border + "╝" + Color.RESET);
    }

    private void printSectionHeader(String title) {
        consoleView.print(Color.bold(Color.BRIGHT_CYAN + "  ◆ " + title + Color.RESET));
    }

    private void printMenuItem(String key, String label) {
        consoleView.print("    " + Color.YELLOW + "[" + key + "]" + Color.RESET
                + "  " + Color.BRIGHT_WHITE + label + Color.RESET);
    }
}
