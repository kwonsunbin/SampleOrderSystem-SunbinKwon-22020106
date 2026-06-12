package com.ssemi.sampleorder.view;

import com.ssemi.sampleorder.model.Sample;

import java.util.List;

public class SampleView {

    private final ConsoleView consoleView;

    public SampleView(ConsoleView consoleView) {
        this.consoleView = consoleView;
    }

    public void printSamples(List<Sample> samples) {
        consoleView.printBlank();
        consoleView.print(Color.header("  ◆ 시료 목록"));
        consoleView.printDivider();
        if (samples.isEmpty()) {
            consoleView.print(Color.dim("    등록된 시료가 없습니다."));
            consoleView.printDivider();
            return;
        }
        for (Sample sample : samples) {
            printSample(sample);
        }
        consoleView.printDivider();
    }

    public void printSample(Sample sample) {
        consoleView.print(
            "  " + Color.YELLOW + "[" + sample.getId() + "]" + Color.RESET
            + "  " + Color.bold(sample.getName())
            + "  " + Color.dim("재고:") + " " + Color.BRIGHT_WHITE + sample.getStock() + Color.RESET
            + "  " + Color.dim("생산시간:") + " " + Color.BRIGHT_WHITE + sample.getAvgProductionTime() + "분" + Color.RESET
            + "  " + Color.dim("수율:") + " " + Color.BRIGHT_GREEN + sample.getYield() + Color.RESET
        );
    }
}
