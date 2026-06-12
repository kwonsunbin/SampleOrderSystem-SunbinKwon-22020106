package com.ssemi.sampleorder.view;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

public class ConsoleView {

    private final Scanner scanner;
    private final PrintStream out;

    public ConsoleView(InputStream in, PrintStream out) {
        this.scanner = new Scanner(in);
        this.out = out;
    }

    // CJK 문자(한글 등) 포함 시 터미널 표시 너비 계산
    public static int dw(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            w += (c >= 0xAC00 && c <= 0xD7FF) || (c >= 0x1100 && c <= 0x11FF)
                 || (c >= 0x3000 && c <= 0x9FFF) || (c >= 0xF900 && c <= 0xFAFF)
                 || (c >= 0xFF00 && c <= 0xFF60) ? 2 : 1;
        }
        return w;
    }

    // 표시 너비 기준 오른쪽 패딩
    public static String pr(String s, int target) {
        int cur = dw(s);
        return cur >= target ? s : s + " ".repeat(target - cur);
    }

    // 표시 너비 기준 왼쪽 패딩
    public static String pl(String s, int target) {
        int cur = dw(s);
        return cur >= target ? s : " ".repeat(target - cur) + s;
    }

    // 박스 테이블 그리기 (col widths = 내용 표시 너비)
    public void printTable(int[] ws, String[] headers, List<String[]> rows) {
        out.println("  " + Color.DIM + borderRow(ws, "┌", "┬", "┐", "─") + Color.RESET);
        out.println("  " + dataRow(ws, headers, Color.BOLD + Color.BRIGHT_CYAN));
        out.println("  " + Color.DIM + borderRow(ws, "├", "┼", "┤", "─") + Color.RESET);
        for (String[] row : rows) {
            out.println("  " + dataRow(ws, row, Color.BRIGHT_WHITE));
        }
        out.println("  " + Color.DIM + borderRow(ws, "└", "┴", "┘", "─") + Color.RESET);
    }

    private String borderRow(int[] ws, String l, String m, String r, String f) {
        StringBuilder sb = new StringBuilder(l);
        for (int i = 0; i < ws.length; i++) {
            sb.append(f.repeat(ws[i] + 2));
            if (i < ws.length - 1) sb.append(m);
        }
        return sb.append(r).toString();
    }

    private String dataRow(int[] ws, String[] cells, String color) {
        StringBuilder sb = new StringBuilder();
        sb.append(Color.DIM).append("│").append(Color.RESET);
        for (int i = 0; i < ws.length; i++) {
            String cell = i < cells.length ? cells[i] : "";
            sb.append(" ").append(color).append(pr(cell, ws[i])).append(Color.RESET).append(" ");
            sb.append(Color.DIM).append("│").append(Color.RESET);
        }
        return sb.toString();
    }

    public String readString(String prompt) {
        out.print(Color.CYAN + "  ▶ " + Color.RESET + prompt + " > ");
        return scanner.nextLine();
    }

    public int readInt(String prompt) {
        out.print(Color.CYAN + "  ▶ " + Color.RESET + prompt + " > ");
        String line = scanner.nextLine();
        try {
            return Integer.parseInt(line.trim());
        } catch (NumberFormatException e) {
            out.println(Color.error("  [오류] 숫자를 입력해 주세요."));
            return -1;
        }
    }

    public double readDouble(String prompt) {
        out.print(Color.CYAN + "  ▶ " + Color.RESET + prompt + " > ");
        String line = scanner.nextLine();
        try {
            return Double.parseDouble(line.trim());
        } catch (NumberFormatException e) {
            out.println(Color.error("  [오류] 숫자를 입력해 주세요."));
            return -1.0;
        }
    }

    public boolean readYN(String prompt) {
        out.print(Color.CYAN + "  ▶ " + Color.RESET + prompt + "  " + Color.YELLOW + "[Y/N]" + Color.RESET + " > ");
        return scanner.nextLine().trim().equalsIgnoreCase("Y");
    }

    public void printSuccess(String msg) {
        out.println();
        out.println(Color.success("  ✔  " + msg));
        out.println();
    }

    public void printError(String msg) {
        out.println();
        out.println(Color.error("  ✖  " + msg));
        out.println();
    }

    public void printWarn(String msg) {
        out.println();
        out.println(Color.warn("  ⚠  " + msg));
        out.println();
    }

    public void print(String msg) {
        out.println(msg);
    }

    public void printBlank() {
        out.println();
    }

    public void printDivider() {
        out.println(Color.dim("  " + "─".repeat(60)));
    }

    public void printSectionHeader(String title) {
        out.println(Color.BOLD + Color.BRIGHT_CYAN + "  ◆ " + title + Color.RESET);
        printDivider();
    }
}
