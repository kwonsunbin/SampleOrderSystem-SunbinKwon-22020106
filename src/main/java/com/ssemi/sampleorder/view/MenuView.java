package com.ssemi.sampleorder.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MenuView {

    private static final int BW = 60;
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConsoleView cv;

    public MenuView(ConsoleView cv) {
        this.cv = cv;
    }

    public void printMainMenu(int sampleCount, long orderCount, int totalStock, long producingCount) {
        cv.printBlank();
        printBanner();
        cv.printBlank();
        printStats(sampleCount, orderCount, totalStock, producingCount);
        cv.printBlank();
        printMenuGrid();
        cv.printBlank();
    }

    // 하위 호환: 통계 없는 메인 메뉴
    public void printMainMenu() {
        cv.printBlank();
        printBanner();
        cv.printBlank();
        printMenuGrid();
        cv.printBlank();
    }

    public void printSubMenu(String title, String... options) {
        cv.printBlank();
        cv.printSectionHeader(title);
        for (int i = 0; i < options.length; i++) {
            cv.print("  " + Color.YELLOW + "[" + (i + 1) + "]" + Color.RESET
                    + "  " + Color.BRIGHT_WHITE + options[i] + Color.RESET);
        }
        cv.printDivider();
        cv.print("  " + Color.YELLOW + "[0]" + Color.RESET + "  뒤로가기");
        cv.printBlank();
    }

    private void printBanner() {
        String title = "반도체 시료 생산주문관리 시스템";
        int pad = (BW - ConsoleView.dw(title)) / 2;
        cv.print(Color.BOLD + Color.BRIGHT_CYAN + "  ╔" + "═".repeat(BW) + "╗" + Color.RESET);
        cv.print(Color.BOLD + Color.BRIGHT_CYAN + "  ║" + " ".repeat(pad) + title
                + " ".repeat(BW - pad - ConsoleView.dw(title)) + "║" + Color.RESET);
        cv.print(Color.BOLD + Color.BRIGHT_CYAN + "  ╚" + "═".repeat(BW) + "╝" + Color.RESET);
    }

    private void printStats(int sampleCount, long orderCount, int totalStock, long producingCount) {
        String now = LocalDateTime.now().format(DT);
        String label = "시스템 현황";
        int gap = BW - ConsoleView.dw(label) - ConsoleView.dw(now) + 2;
        cv.print("  " + Color.BOLD + label + Color.RESET
                + " ".repeat(Math.max(2, gap)) + Color.DIM + now + Color.RESET);

        int hw = 29;
        String la = "  등록 시료 " + String.format("%4d 종", sampleCount);
        String ra = "  전체 주문 " + String.format("%4d 건", orderCount);
        String lb = "  재고 합계 " + String.format("%,6d ea", totalStock);
        String rb = "  생산 대기 " + String.format("%4d 건", producingCount);

        cv.print("  " + Color.DIM + "┌" + "─".repeat(hw) + "┬" + "─".repeat(hw) + "┐" + Color.RESET);
        cv.print("  " + Color.DIM + "│" + Color.RESET
                + Color.BRIGHT_WHITE + ConsoleView.pr(la, hw) + Color.RESET
                + Color.DIM + "│" + Color.RESET
                + Color.BRIGHT_YELLOW + ConsoleView.pr(ra, hw) + Color.RESET
                + Color.DIM + "│" + Color.RESET);
        cv.print("  " + Color.DIM + "│" + Color.RESET
                + Color.BRIGHT_CYAN + ConsoleView.pr(lb, hw) + Color.RESET
                + Color.DIM + "│" + Color.RESET
                + (producingCount > 0 ? Color.BRIGHT_GREEN : Color.DIM)
                + ConsoleView.pr(rb, hw) + Color.RESET
                + Color.DIM + "│" + Color.RESET);
        cv.print("  " + Color.DIM + "└" + "─".repeat(hw) + "┴" + "─".repeat(hw) + "┘" + Color.RESET);
    }

    private void printMenuGrid() {
        String[][] grid = {
            {"[1] 시료 관리",      "[2] 시료 주문"},
            {"[3] 주문 승인/거절",  "[4] 모니터링"},
            {"[5] 생산라인 조회",   "[6] 출고 처리"},
        };
        for (String[] row : grid) {
            int gap = 32 - ConsoleView.dw(row[0]);
            cv.print("  " + menuItem(row[0]) + " ".repeat(Math.max(2, gap)) + menuItem(row[1]));
        }
        cv.printDivider();
        cv.print("  " + menuItem("[0] 종료"));
    }

    private String menuItem(String s) {
        int close = s.indexOf(']');
        if (close < 0) return Color.BRIGHT_WHITE + s + Color.RESET;
        return Color.YELLOW + s.substring(0, close + 1) + Color.RESET
                + Color.BRIGHT_WHITE + s.substring(close + 1) + Color.RESET;
    }
}
