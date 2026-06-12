package com.ssemi.sampleorder.controller;

import com.ssemi.sampleorder.model.Sample;
import com.ssemi.sampleorder.service.SampleService;
import com.ssemi.sampleorder.view.ConsoleView;

import java.util.List;

public class SampleController {

    private final SampleService sampleService;
    private final ConsoleView consoleView;

    public SampleController(SampleService sampleService, ConsoleView consoleView) {
        this.sampleService = sampleService;
        this.consoleView = consoleView;
    }

    public void showMenu() {
        consoleView.print("=== 시료 관리 ===");
        consoleView.print("1. 시료 등록");
        consoleView.print("2. 시료 목록");
        consoleView.print("3. 시료 검색");
        consoleView.print("0. 뒤로가기");
        int choice = consoleView.readInt("선택: ");
        switch (choice) {
            case 1 -> handleRegister();
            case 2 -> handleList();
            case 3 -> handleSearch();
            case 0 -> {}
            default -> consoleView.printError("잘못된 선택입니다.");
        }
    }

    public void handleRegister() {
        String name = consoleView.readString("시료명: ");
        int avgProductionTime = consoleView.readInt("평균 생산시간(분): ");
        double yield = consoleView.readDouble("수율(0 초과 1 이하): ");
        int stock = consoleView.readInt("초기 재고: ");
        try {
            Sample sample = sampleService.register(name, avgProductionTime, yield, stock);
            consoleView.printSuccess("시료 등록 완료: " + sample.getName() + " (ID: " + sample.getId() + ")");
        } catch (Exception e) {
            consoleView.printError(e.getMessage());
        }
    }

    public void handleList() {
        List<Sample> samples = sampleService.listSamples();
        if (samples.isEmpty()) {
            consoleView.print("등록된 시료가 없습니다.");
            return;
        }
        for (Sample s : samples) {
            consoleView.print("[" + s.getId() + "] " + s.getName() + " | 재고: " + s.getStock());
        }
    }

    public void handleSearch() {
        String keyword = consoleView.readString("검색 키워드: ");
        List<Sample> results = sampleService.searchSample(keyword);
        if (results.isEmpty()) {
            consoleView.print("검색 결과가 없습니다.");
            return;
        }
        for (Sample s : results) {
            consoleView.print("[" + s.getId() + "] " + s.getName() + " | 재고: " + s.getStock());
        }
    }
}
