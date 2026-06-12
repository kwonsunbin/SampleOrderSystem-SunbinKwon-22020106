package com.ssemi.sampleorder.view;

public final class Color {

    public static final String RESET  = "[0m";
    public static final String BOLD   = "[1m";
    public static final String DIM    = "[2m";

    public static final String BLACK   = "[30m";
    public static final String RED     = "[31m";
    public static final String GREEN   = "[32m";
    public static final String YELLOW  = "[33m";
    public static final String BLUE    = "[34m";
    public static final String MAGENTA = "[35m";
    public static final String CYAN    = "[36m";
    public static final String WHITE   = "[37m";

    public static final String BRIGHT_RED    = "[91m";
    public static final String BRIGHT_GREEN  = "[92m";
    public static final String BRIGHT_YELLOW = "[93m";
    public static final String BRIGHT_CYAN   = "[96m";
    public static final String BRIGHT_WHITE  = "[97m";

    private Color() {}

    public static String apply(String color, String text) {
        return color + text + RESET;
    }

    public static String bold(String text) {
        return BOLD + text + RESET;
    }

    public static String success(String text) {
        return BRIGHT_GREEN + text + RESET;
    }

    public static String error(String text) {
        return BRIGHT_RED + text + RESET;
    }

    public static String warn(String text) {
        return BRIGHT_YELLOW + text + RESET;
    }

    public static String header(String text) {
        return BOLD + BRIGHT_CYAN + text + RESET;
    }

    public static String dim(String text) {
        return DIM + text + RESET;
    }
}
