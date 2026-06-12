package com.ssemi.sampleorder.view;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.service.ProductionProgressInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ProductionView — 시간 기반 생산 화면")
class ProductionViewTest {

    private ByteArrayOutputStream out;
    private ProductionView view;

    @BeforeEach
    void setUp() {
        out = new ByteArrayOutputStream();
        ConsoleView cv = new ConsoleView(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        view = new ProductionView(cv);
    }

    private String output() {
        return out.toString();
    }

    private ProductionProgressInfo buildProgressInfo(int progressPercent,
                                                      LocalDateTime estimatedCompletion) {
        Order order = new Order("ORD-0038", "S004", "카이스트 전력연구소", 80);
        order.transitionTo(OrderStatus.PRODUCING);
        order.startProduction(estimatedCompletion.minusMinutes(60), 60);

        return new ProductionProgressInfo(
                order,
                "SiC 파워기판-6인치",
                30,    // currentStock
                50,    // shortage
                61,    // actualProduction
                0.92,  // yield
                49,    // avgProductionTime
                43L,   // elapsedMinutes
                60,    // totalMinutes
                progressPercent,
                estimatedCompletion
        );
    }

    // ── Cycle 2: 현재 생산 중 화면 ───────────────────────────────────────────

    @Nested
    @DisplayName("printCurrentlyProducing — 현재 처리 중 섹션")
    class PrintCurrentlyProducing {

        @Test
        @DisplayName("'현재 처리 중' 헤더가 출력에 포함됨")
        void containsCurrentlyProducingHeader() {
            LocalDateTime eta = LocalDateTime.of(2026, 6, 12, 9, 49);
            view.printCurrentlyProducing(buildProgressInfo(72, eta));
            assertTrue(output().contains("현재 처리 중"),
                    "출력에 '현재 처리 중' 포함 필요. 실제: " + output());
        }

        @Test
        @DisplayName("진행률 %(72%)가 출력에 포함됨")
        void containsProgressPercent() {
            LocalDateTime eta = LocalDateTime.of(2026, 6, 12, 9, 49);
            view.printCurrentlyProducing(buildProgressInfo(72, eta));
            assertTrue(output().contains("72%"),
                    "출력에 '72%' 포함 필요. 실제: " + output());
        }

        @Test
        @DisplayName("예정 완료 시각(09:49)이 출력에 포함됨")
        void containsEstimatedCompletionTime() {
            LocalDateTime eta = LocalDateTime.of(2026, 6, 12, 9, 49);
            view.printCurrentlyProducing(buildProgressInfo(72, eta));
            assertTrue(output().contains("09:49"),
                    "출력에 '09:49' 포함 필요. 실제: " + output());
        }

        @Test
        @DisplayName("프로그레스 바(█ 또는 ░)가 출력에 포함됨")
        void containsProgressBar() {
            LocalDateTime eta = LocalDateTime.of(2026, 6, 12, 9, 49);
            view.printCurrentlyProducing(buildProgressInfo(72, eta));
            String o = output();
            assertTrue(o.contains("█") || o.contains("░"),
                    "출력에 프로그레스 바(█ 또는 ░) 포함 필요. 실제: " + o);
        }

        @Test
        @DisplayName("주문번호가 출력에 포함됨")
        void containsOrderId() {
            LocalDateTime eta = LocalDateTime.of(2026, 6, 12, 9, 49);
            view.printCurrentlyProducing(buildProgressInfo(72, eta));
            assertTrue(output().contains("ORD-0038"),
                    "출력에 주문번호 포함 필요. 실제: " + output());
        }

        @Test
        @DisplayName("시료명이 출력에 포함됨")
        void containsSampleName() {
            LocalDateTime eta = LocalDateTime.of(2026, 6, 12, 9, 49);
            view.printCurrentlyProducing(buildProgressInfo(72, eta));
            assertTrue(output().contains("SiC 파워기판-6인치"),
                    "출력에 시료명 포함 필요. 실제: " + output());
        }
    }

    // ── Cycle 2: 대기 큐 + 예상 완료 시각 ──────────────────────────────────

    @Nested
    @DisplayName("printQueueWithEta — 대기 큐 + 예상 완료 시각")
    class PrintQueueWithEta {

        @Test
        @DisplayName("빈 큐이고 PRODUCING도 없으면 '대기 중인 주문이 없습니다' 출력")
        void emptyQueueShowsEmptyMessage() {
            view.printQueueWithEta(List.of());
            assertTrue(output().contains("없"),
                    "빈 큐 메시지에 '없' 포함 필요. 실제: " + output());
        }

        @Test
        @DisplayName("'예상 완료' 컬럼 헤더가 출력에 포함됨")
        void containsEtaColumnHeader() {
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"1", "ORD-0040", "GaAs 웨이퍼", "150 ea", "150 ea", "190 ea", "11:43"});
            view.printQueueWithEta(rows);
            assertTrue(output().contains("예상 완료"),
                    "출력에 '예상 완료' 컬럼 포함 필요. 실제: " + output());
        }

        @Test
        @DisplayName("대기 주문의 예상 완료 시각이 출력에 포함됨")
        void containsEtaTime() {
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"1", "ORD-0040", "GaAs 웨이퍼", "150 ea", "150 ea", "190 ea", "11:43"});
            view.printQueueWithEta(rows);
            assertTrue(output().contains("11:43"),
                    "출력에 ETA '11:43' 포함 필요. 실제: " + output());
        }
    }
}
