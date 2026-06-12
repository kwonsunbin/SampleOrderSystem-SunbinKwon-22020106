package com.ssemi.sampleorder.controller;

import com.ssemi.sampleorder.model.Sample;
import com.ssemi.sampleorder.service.SampleService;
import com.ssemi.sampleorder.view.ConsoleView;
import com.ssemi.sampleorder.view.MenuView;
import com.ssemi.sampleorder.view.SampleView;

import java.util.List;

public class SampleController {

    private final SampleService sampleService;
    private final ConsoleView consoleView;
    private final SampleView sampleView;
    private final MenuView menuView;

    public SampleController(SampleService sampleService, ConsoleView consoleView) {
        this.sampleService = sampleService;
        this.consoleView   = consoleView;
        this.sampleView    = new SampleView(consoleView);
        this.menuView      = new MenuView(consoleView);
    }

    public void showMenu() {
        menuView.printSubMenu("[1] 시료 관리", "시료 등록", "시료 목록", "시료 검색");
        int choice = consoleView.readInt("선택");
        switch (choice) {
            case 1 -> handleRegister();
            case 2 -> handleList();
            case 3 -> handleSearch();
            case 0 -> {}
            default -> consoleView.printError("잘못된 선택입니다.");
        }
    }

    public void handleRegister() {
        consoleView.printBlank();
        consoleView.printSectionHeader("시료 등록");
        String name = consoleView.readString("시료명");
        int avgTime = consoleView.readInt("평균 생산시간 (초/개)");
        double yield = consoleView.readDouble("수율 (0 초과 ~ 1.0 이하)");
        int stock = consoleView.readInt("초기 재고 (ea)");
        try {
            Sample sample = sampleService.register(name, avgTime, yield, stock);
            sampleView.printRegisterSuccess(sample);
        } catch (Exception e) {
            consoleView.printError(e.getMessage());
        }
    }

    public void handleList() {
        List<Sample> samples = sampleService.listSamples();
        int page = 0;
        while (true) {
            sampleView.printSamplesTable(samples, page);
            int total = samples.size();
            int maxPage = (total - 1) / 5;
            if (page < maxPage) {
                String input = consoleView.readString("N=다음 페이지, 0=뒤로가기").trim().toUpperCase();
                if ("N".equals(input)) { page++; continue; }
            }
            break;
        }
    }

    public void handleSearch() {
        consoleView.printBlank();
        consoleView.printSectionHeader("시료 검색");
        String keyword = consoleView.readString("검색어");
        List<Sample> results = sampleService.searchSample(keyword);
        sampleView.printSearchResults(results);
    }
}
