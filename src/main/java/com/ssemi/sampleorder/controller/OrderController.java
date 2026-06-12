package com.ssemi.sampleorder.controller;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.model.Sample;
import com.ssemi.sampleorder.service.OrderService;
import com.ssemi.sampleorder.service.ProductionService;
import com.ssemi.sampleorder.service.SampleService;
import com.ssemi.sampleorder.view.ConsoleView;
import com.ssemi.sampleorder.view.MenuView;
import com.ssemi.sampleorder.view.OrderView;
import com.ssemi.sampleorder.view.SampleView;

import java.util.List;

public class OrderController {

    private final OrderService orderService;
    private final SampleService sampleService;      // nullable — 없으면 fallback
    private final ProductionService productionService; // nullable
    private final ConsoleView consoleView;
    private final OrderView orderView;
    private final SampleView sampleView;
    private final MenuView menuView;

    // 하위 호환 생성자 (테스트용)
    public OrderController(OrderService orderService, ConsoleView consoleView) {
        this(orderService, null, null, consoleView);
    }

    public OrderController(OrderService orderService,
                           SampleService sampleService,
                           ProductionService productionService,
                           ConsoleView consoleView) {
        this.orderService      = orderService;
        this.sampleService     = sampleService;
        this.productionService = productionService;
        this.consoleView       = consoleView;
        this.orderView         = new OrderView(consoleView);
        this.sampleView        = new SampleView(consoleView);
        this.menuView          = new MenuView(consoleView);
    }

    // ── 메인 진입 (하위 호환 — [2] 주문 접수) ────────────────────────────────

    public void showMenu() {
        showReserveMenu();
    }

    // ── [2] 시료 주문 ─────────────────────────────────────────────────────────

    public void showReserveMenu() {
        handleReserve();
    }

    // ── [3] 주문 승인/거절 ────────────────────────────────────────────────────

    public void showApproveMenu() {
        menuView.printSubMenu("[3] 주문 승인/거절", "승인 처리", "주문 거절");
        int choice = consoleView.readInt("선택");
        switch (choice) {
            case 1 -> handleApprove();
            case 2 -> handleReject();
            case 0 -> {}
            default -> consoleView.printError("잘못된 선택입니다.");
        }
    }

    // ── 주문 접수 ─────────────────────────────────────────────────────────────

    public void handleReserve() {
        consoleView.printBlank();
        consoleView.printSectionHeader("시료 주문");

        String sampleId;
        String sampleName;

        if (sampleService != null) {
            // 시료 목록 표시 후 번호 선택
            List<Sample> samples = sampleService.listSamples();
            sampleView.printSamplesTable(samples);
            if (samples.isEmpty()) return;

            int num = consoleView.readInt("시료 번호 선택");
            if (num < 1 || num > samples.size()) {
                consoleView.printError("올바른 번호를 입력하세요.");
                return;
            }
            Sample selected = samples.get(num - 1);
            sampleId   = selected.getId();
            sampleName = selected.getName();
        } else {
            sampleId   = consoleView.readString("시료 ID");
            sampleName = sampleId;
        }

        String customerName = consoleView.readString("고객명");
        int quantity = consoleView.readInt("주문 수량 (ea)");
        if (quantity <= 0) {
            consoleView.printError("수량은 1 이상이어야 합니다.");
            return;
        }

        orderView.printOrderConfirm(sampleName, sampleId, customerName, quantity);
        if (!consoleView.readYN("선택")) return;

        try {
            Order order = orderService.reserveOrder(sampleId, customerName, quantity);
            orderView.printOrderCreated(order);
        } catch (Exception e) {
            consoleView.printError(e.getMessage());
        }
    }

    // ── 주문 승인 ─────────────────────────────────────────────────────────────

    public void handleApprove() {
        List<Order> reserved = orderService.listReservedOrders();
        orderView.printReservedOrders(reserved);
        if (reserved.isEmpty()) return;

        int num = consoleView.readInt("승인할 번호");
        if (num < 1 || num > reserved.size()) {
            consoleView.printError("올바른 번호를 입력하세요.");
            return;
        }
        Order target = reserved.get(num - 1);

        // 재고 확인 정보 표시
        if (sampleService != null && productionService != null) {
            try {
                Sample sample = sampleService.getSample(target.getSampleId());
                int shortage   = Math.max(0, target.getQuantity() - sample.getStock());
                int actualProd = shortage > 0
                    ? productionService.calculateActualProduction(shortage, sample.getYield()) : 0;
                int totalTime  = productionService.calculateTotalProductionTime(
                    sample.getAvgProductionTime(), actualProd > 0 ? actualProd : target.getQuantity());

                orderView.printApprovalInfo(sample.getName(), target.getQuantity(),
                    sample.getStock(), shortage, actualProd, totalTime);
            } catch (Exception e) {
                consoleView.printWarn("재고 정보 조회 실패: " + e.getMessage());
                consoleView.print("  계속 승인하시겠습니까?");
                consoleView.print("  " + "  [Y] 승인   [N] 취소");
            }
        } else {
            consoleView.print("  주문 #" + num + " 을 승인하시겠습니까?");
            consoleView.print("  " + "  [Y] 승인   [N] 거절");
        }

        if (!consoleView.readYN("선택")) {
            try {
                Order rejected = orderService.rejectOrder(target.getId());
                consoleView.printWarn("주문 거절 처리됨  " + rejected.getId());
            } catch (Exception e) {
                consoleView.printError(e.getMessage());
            }
            return;
        }

        try {
            Order result = orderService.approveOrder(target.getId());
            orderView.printApprovalResult(result);
        } catch (Exception e) {
            consoleView.printError(e.getMessage());
        }
    }

    // ── 주문 거절 ─────────────────────────────────────────────────────────────

    public void handleReject() {
        List<Order> reserved = orderService.listReservedOrders();
        orderView.printReservedOrders(reserved);
        if (reserved.isEmpty()) return;

        int num = consoleView.readInt("거절할 번호");
        if (num < 1 || num > reserved.size()) {
            consoleView.printError("올바른 번호를 입력하세요.");
            return;
        }
        Order target = reserved.get(num - 1);
        try {
            Order result = orderService.rejectOrder(target.getId());
            consoleView.printSuccess("주문 거절 완료  " + result.getStatus() + "  " + result.getId());
        } catch (Exception e) {
            consoleView.printError(e.getMessage());
        }
    }
}
