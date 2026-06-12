package com.ssemi.sampleorder.view;

import com.ssemi.sampleorder.model.Sample;

import java.util.List;

public class SampleView {

    private final ConsoleView consoleView;

    public SampleView(ConsoleView consoleView) {
        this.consoleView = consoleView;
    }

    public void printSamples(List<Sample> samples) {
        if (samples.isEmpty()) {
            consoleView.print("등록된 시료가 없습니다.");
            return;
        }
        for (Sample sample : samples) {
            printSample(sample);
        }
    }

    public void printSample(Sample sample) {
        consoleView.print("[" + sample.getId() + "] " + sample.getName()
                + " | 재고: " + sample.getStock()
                + " | 평균 생산시간: " + sample.getAvgProductionTime() + "분"
                + " | 수율: " + sample.getYield());
    }
}
