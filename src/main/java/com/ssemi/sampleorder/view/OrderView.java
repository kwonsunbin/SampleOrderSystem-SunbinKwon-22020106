package com.ssemi.sampleorder.view;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.OrderStatus;

import java.util.ArrayList;
import java.util.List;

public class OrderView {

    // 컬럼 너비: #, 주문번호, 고객, 시료ID, 수량, 상태
    private static final int[] RESERVE_COLS  = {3, 13, 16, 10, 7, 11};
    private static final String[] RESERVE_HDR = {"#", "주문번호", "고객", "시료ID", "수량", "상태"};

    private final ConsoleView cv;

    public OrderView(ConsoleView cv) {
        this.cv = cv;
    }

    // ── 예약 대기 목록 (승인/거절 화면) ──────────────────────────────────────

    public void printReservedOrders(List<Order> orders) {
        cv.printBlank();
        cv.printSectionHeader("승인 대기 중인 예약 목록  (RESERVED)");
        if (orders.isEmpty()) {
            cv.print(Color.dim("    대기 중인 주문이 없습니다."));
            cv.printBlank();
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            String sid = o.getSampleId() != null ? o.getSampleId() : "-";
            rows.add(new String[]{
                String.valueOf(i + 1),
                shortId(o.getId()),
                o.getCustomerName() != null ? o.getCustomerName() : "-",
                sid.length() > 10 ? "…" + sid.substring(sid.length() - 9) : sid,
                o.getQuantity() + " ea",
                statusLabel(o.getStatus())
            });
        }
        cv.printTable(RESERVE_COLS, RESERVE_HDR, rows);
        cv.printBlank();
    }

    // ── 주문 접수 확인 박스 ───────────────────────────────────────────────────

    public void printOrderConfirm(String sampleName, String sampleId, String customerName, int qty) {
        cv.printBlank();
        cv.print(Color.BOLD + Color.BRIGHT_WHITE + "  입력 내용 확인" + Color.RESET);
        int bw = 44;
        cv.print("  " + Color.DIM + "┌" + "─".repeat(bw) + "┐" + Color.RESET);
        printBoxLine("시료",     sampleName + "  " + Color.dim("(" + shortId(sampleId) + ")"), bw);
        printBoxLine("고객",     customerName, bw);
        printBoxLine("주문 수량", qty + " ea", bw);
        cv.print("  " + Color.DIM + "└" + "─".repeat(bw) + "┘" + Color.RESET);
        cv.printBlank();
        cv.print("  " + Color.YELLOW + "[Y]" + Color.RESET + " 예약 접수   "
                + Color.DIM + "[N]" + Color.RESET + " 취소");
    }

    // ── 주문 접수 결과 ────────────────────────────────────────────────────────

    public void printOrderCreated(Order order) {
        cv.printBlank();
        cv.printSuccess("예약 접수 완료");
        int bw = 44;
        cv.print("  " + Color.DIM + "┌" + "─".repeat(bw) + "┐" + Color.RESET);
        printBoxLine("주문번호", shortId(order.getId()), bw);
        printBoxLine("현재 상태", statusLabel(order.getStatus()), bw);
        cv.print("  " + Color.DIM + "└" + "─".repeat(bw) + "┘" + Color.RESET);
        cv.print(Color.dim("  ※ 재고 확인은 [3] 승인 메뉴에서 직접 진행하세요."));
        cv.printBlank();
    }

    // ── 승인 재고 정보 ────────────────────────────────────────────────────────

    public void printApprovalInfo(String sampleName, int orderQty, int currentStock,
                                  int shortage, int actualProd, int totalTime) {
        cv.printBlank();
        cv.print(Color.DIM + "  재고 확인 중…" + Color.RESET);
        cv.printBlank();
        int bw = 50;
        cv.print("  " + Color.DIM + "┌" + "─".repeat(bw) + "┐" + Color.RESET);
        printBoxLine("시료",      sampleName, bw);
        printBoxLine("주문 수량", orderQty + " ea", bw);
        printBoxLine("현재 재고", currentStock + " ea", bw);

        if (shortage <= 0) {
            printBoxLine("재고 상태", Color.BRIGHT_GREEN + "충분" + Color.RESET, bw);
        } else {
            printBoxLine("재고 부족", Color.BRIGHT_RED + shortage + " ea 부족" + Color.RESET, bw);
            printBoxLine("실 생산량", Color.BRIGHT_CYAN + actualProd + " ea" + Color.RESET
                    + Color.dim("  (수율 보정 생산)"), bw);
            printBoxLine("예상 시간", Color.BRIGHT_YELLOW + totalTime + " 초" + Color.RESET, bw);
        }
        cv.print("  " + Color.DIM + "└" + "─".repeat(bw) + "┘" + Color.RESET);
        cv.printBlank();

        if (shortage > 0) {
            cv.print("  " + Color.warn("재고 부족") + "  생산을 포함하여 승인하시겠습니까?");
        } else {
            cv.print("  " + Color.success("재고 충분") + "  승인하시겠습니까?");
        }
        cv.print("  " + Color.YELLOW + "[Y]" + Color.RESET + " 승인   "
                + Color.DIM + "[N]" + Color.RESET + " 주문 거절");
    }

    // ── 승인 결과 ─────────────────────────────────────────────────────────────

    public void printApprovalResult(Order order) {
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            cv.printSuccess("승인 완료  " + Color.BOLD + "CONFIRMED" + Color.RESET
                    + "  " + Color.dim(shortId(order.getId())));
        } else {
            cv.printSuccess("승인 완료  생산 대기  " + Color.BRIGHT_CYAN + "PRODUCING" + Color.RESET
                    + "  " + Color.dim(shortId(order.getId())));
        }
    }

    // ── 출고 대기 목록 ────────────────────────────────────────────────────────

    private static final int[] RELEASE_COLS = {3, 13, 14, 12, 7, 11};
    private static final String[] RELEASE_HDR = {"#", "주문번호", "고객", "시료ID", "수량", "상태"};

    public void printConfirmedOrders(List<Order> orders) {
        cv.printBlank();
        cv.printSectionHeader("출고 가능 주문  (CONFIRMED)");
        if (orders.isEmpty()) {
            cv.print(Color.dim("    출고 대기 중인 주문이 없습니다."));
            cv.printBlank();
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            String sid = o.getSampleId() != null ? o.getSampleId() : "-";
            rows.add(new String[]{
                String.valueOf(i + 1),
                shortId(o.getId()),
                o.getCustomerName() != null ? o.getCustomerName() : "-",
                sid.length() > 14 ? "…" + sid.substring(sid.length() - 13) : sid,
                o.getQuantity() + " ea",
                statusLabel(o.getStatus())
            });
        }
        cv.printTable(RELEASE_COLS, RELEASE_HDR, rows);
        cv.printBlank();
    }

    public void printReleaseResult(Order order) {
        cv.printBlank();
        cv.printSuccess("출고 처리 완료");
        int bw = 44;
        cv.print("  " + Color.DIM + "┌" + "─".repeat(bw) + "┐" + Color.RESET);
        printBoxLine("주문번호",  shortId(order.getId()), bw);
        printBoxLine("출고 수량", order.getQuantity() + " ea", bw);
        printBoxLine("현재 상태", statusLabel(order.getStatus()), bw);
        cv.print("  " + Color.DIM + "└" + "─".repeat(bw) + "┘" + Color.RESET);
        cv.printBlank();
    }

    // ── 공통 헬퍼 ─────────────────────────────────────────────────────────────

    private void printBoxLine(String label, String value, int bw) {
        String labelPart = "  " + Color.DIM + ConsoleView.pr(label, 8) + Color.RESET + "  ";
        int labelWidth = 2 + 8 + 2;
        int valueWidth = bw - labelWidth;
        cv.print("  " + Color.DIM + "│" + Color.RESET
                + labelPart + ConsoleView.pr(value, valueWidth)
                + Color.DIM + "│" + Color.RESET);
    }

    private String shortId(String id) {
        if (id.startsWith("ORD-")) return id;
        return id.length() > 13 ? "…" + id.substring(id.length() - 12) : id;
    }

    private String statusLabel(OrderStatus s) {
        switch (s) {
            case RESERVED:  return Color.BRIGHT_YELLOW + "RESERVED"  + Color.RESET;
            case PRODUCING: return Color.BRIGHT_CYAN   + "PRODUCING" + Color.RESET;
            case CONFIRMED: return Color.BLUE          + "CONFIRMED" + Color.RESET;
            case RELEASED:  return Color.BRIGHT_GREEN  + "RELEASED"  + Color.RESET;
            case REJECTED:  return Color.BRIGHT_RED    + "REJECTED"  + Color.RESET;
            default:        return s.name();
        }
    }
}
