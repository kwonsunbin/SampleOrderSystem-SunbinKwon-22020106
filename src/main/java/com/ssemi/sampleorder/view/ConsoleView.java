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
        out.println(prompt);
        return scanner.nextLine();
    }

    public int readInt(String prompt) {
        out.println(prompt);
        String line = scanner.nextLine();
        try {
            return Integer.parseInt(line.trim());
        } catch (NumberFormatException e) {
            out.println("잘못된 입력입니다.");
            return -1;
        }
    }

    public double readDouble(String prompt) {
        out.println(prompt);
        String line = scanner.nextLine();
        try {
            return Double.parseDouble(line.trim());
        } catch (NumberFormatException e) {
            out.println("잘못된 입력입니다.");
            return -1.0;
        }
    }

    public void printSuccess(String msg) {
        out.println("[완료] " + msg);
    }

    public void printError(String msg) {
        out.println("[오류] " + msg);
    }

    public void print(String msg) {
        out.println(msg);
    }
}
