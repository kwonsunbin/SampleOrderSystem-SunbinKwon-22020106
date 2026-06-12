package com.ssemi.sampleorder.view;

import com.ssemi.sampleorder.model.Sample;

import java.util.ArrayList;
import java.util.List;

public class SampleView {

    // 컬럼 표시 너비: #, ID, 시료명, 생산시간, 수율, 재고
    private static final int[] COLS    = {3, 10, 22, 9, 6, 8};
    private static final String[] HDR  = {"#", "ID", "시료명", "생산시간", "수율", "재고"};
    private static final int PAGE_SIZE = 5;

    private final ConsoleView cv;

    public SampleView(ConsoleView cv) {
        this.cv = cv;
    }

    public void printSamplesTable(List<Sample> samples, int page) {
        cv.printBlank();
        int total = samples.size();
        int from  = page * PAGE_SIZE;
        int to    = Math.min(from + PAGE_SIZE, total);
        cv.printSectionHeader("등록 시료 목록  (총 " + total + " 종)");

        if (total == 0) {
            cv.print(Color.dim("    등록된 시료가 없습니다."));
            cv.printBlank();
            return;
        }

        List<String[]> rows = new ArrayList<>();
        for (int i = from; i < to; i++) {
            Sample s = samples.get(i);
            rows.add(new String[]{
                String.valueOf(i + 1),
                shortId(s.getId()),
                s.getName(),
                s.getAvgProductionTime() + " m/ea",
                String.format("%.2f", s.getYield()),
                s.getStock() + " ea"
            });
        }
        cv.printTable(COLS, HDR, rows);

        if (to < total) {
            cv.print("  " + Color.YELLOW + "[N]" + Color.RESET + Color.dim(" 다음 페이지")
                    + Color.dim("  (" + to + "/" + total + ")"));
        }
        cv.printBlank();
    }

    public void printSamplesTable(List<Sample> samples) {
        printSamplesTable(samples, 0);
    }

    public void printSearchResults(List<Sample> results) {
        cv.printBlank();
        cv.printSectionHeader("검색 결과  (" + results.size() + " 종)");
        if (results.isEmpty()) {
            cv.print(Color.dim("    검색 결과가 없습니다."));
            cv.printBlank();
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Sample s = results.get(i);
            rows.add(new String[]{
                String.valueOf(i + 1),
                shortId(s.getId()),
                s.getName(),
                s.getAvgProductionTime() + " m/ea",
                String.format("%.2f", s.getYield()),
                s.getStock() + " ea"
            });
        }
        cv.printTable(COLS, HDR, rows);
        cv.printBlank();
    }

    public void printRegisterSuccess(Sample sample) {
        cv.printSuccess("시료 등록 완료  " + Color.BOLD + sample.getName() + Color.RESET
                + "  " + Color.dim("ID: ") + shortId(sample.getId()));
    }

    private String shortId(String id) {
        return id.length() > 10 ? "…" + id.substring(id.length() - 9) : id;
    }
}
