package com.ssemi.sampleorder.view;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

public class ConsoleView {

    private final Scanner scanner;
    private final PrintStream out;

    public ConsoleView(InputStream in, PrintStream out) {
        this.scanner = new Scanner(in);
        this.out = out;
    }

    public String readString(String prompt) {
        out.print(Color.CYAN + "  ▶ " + Color.RESET + prompt + " : ");
        return scanner.nextLine();
    }

    public int readInt(String prompt) {
        out.print(Color.CYAN + "  ▶ " + Color.RESET + prompt + " : ");
        String line = scanner.nextLine();
        try {
            return Integer.parseInt(line.trim());
        } catch (NumberFormatException e) {
            out.println(Color.error("  [오류] 숫자를 입력해 주세요."));
            return -1;
        }
    }

    public double readDouble(String prompt) {
        out.print(Color.CYAN + "  ▶ " + Color.RESET + prompt + " : ");
        String line = scanner.nextLine();
        try {
            return Double.parseDouble(line.trim());
        } catch (NumberFormatException e) {
            out.println(Color.error("  [오류] 숫자를 입력해 주세요."));
            return -1.0;
        }
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

    public void print(String msg) {
        out.println(msg);
    }

    public void printBlank() {
        out.println();
    }

    public void printDivider() {
        out.println(Color.dim("  " + "─".repeat(56)));
    }
}
