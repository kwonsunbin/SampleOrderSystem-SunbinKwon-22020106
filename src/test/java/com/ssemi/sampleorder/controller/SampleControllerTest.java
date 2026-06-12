package com.ssemi.sampleorder.controller;

import com.ssemi.sampleorder.model.Sample;
import com.ssemi.sampleorder.service.SampleService;
import com.ssemi.sampleorder.view.ConsoleView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SampleController 테스트")
@ExtendWith(MockitoExtension.class)
class SampleControllerTest {

    @Mock
    private SampleService sampleService;

    // ── Regression: 시료 등록 ─────────────────────────────────────────────────

    @Nested
    @DisplayName("시료 등록")
    class Register {

        @Test
        @DisplayName("입력값으로 sampleService.register()가 정확히 1회 호출됨")
        void registerCallsServiceWithCorrectArgs() {
            String input = "GaAs 웨이퍼\n120\n0.85\n50\n";
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            Sample fakeSample = new Sample("id-1", "GaAs 웨이퍼", 120, 0.85, 50);
            when(sampleService.register("GaAs 웨이퍼", 120, 0.85, 50)).thenReturn(fakeSample);

            SampleController controller = new SampleController(sampleService, consoleView);
            controller.handleRegister();

            verify(sampleService, times(1)).register("GaAs 웨이퍼", 120, 0.85, 50);
        }

        @Test
        @DisplayName("등록 성공 시 출력에 시료명 또는 '등록' 포함")
        void registerPrintsSuccessMessage() {
            String input = "GaAs 웨이퍼\n120\n0.85\n50\n";
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            Sample fakeSample = new Sample("id-1", "GaAs 웨이퍼", 120, 0.85, 50);
            when(sampleService.register("GaAs 웨이퍼", 120, 0.85, 50)).thenReturn(fakeSample);

            SampleController controller = new SampleController(sampleService, consoleView);
            controller.handleRegister();

            String output = outputStream.toString(StandardCharsets.UTF_8);
            assertTrue(
                output.contains("GaAs 웨이퍼") || output.contains("등록"),
                "출력에 'GaAs 웨이퍼' 또는 '등록'이 포함되어야 합니다. 실제 출력: " + output
            );
        }
    }

    // ── Regression: 시료 목록 ─────────────────────────────────────────────────

    @Nested
    @DisplayName("시료 목록")
    class List_ {

        @Test
        @DisplayName("sampleService.listSamples()가 정확히 1회 호출됨")
        void listCallsServiceListSamples() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            when(sampleService.listSamples()).thenReturn(Collections.emptyList());

            SampleController controller = new SampleController(sampleService, consoleView);
            controller.handleList();

            verify(sampleService, times(1)).listSamples();
        }

        @Test
        @DisplayName("시료 목록이 비어 있을 때 출력에 '없' 포함")
        void listShowsEmptyMessageWhenNoSamples() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            when(sampleService.listSamples()).thenReturn(Collections.emptyList());

            SampleController controller = new SampleController(sampleService, consoleView);
            controller.handleList();

            String output = outputStream.toString(StandardCharsets.UTF_8);
            assertTrue(
                output.contains("없"),
                "빈 목록일 때 출력에 '없'이 포함되어야 합니다. 실제 출력: " + output
            );
        }
    }

    // ── Regression: 시료 검색 ─────────────────────────────────────────────────

    @Nested
    @DisplayName("시료 검색")
    class Search {

        @Test
        @DisplayName("입력 키워드로 sampleService.searchSample()이 정확히 1회 호출됨")
        void searchCallsServiceWithKeyword() {
            String input = "GaAs\n";
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            when(sampleService.searchSample("GaAs")).thenReturn(Collections.emptyList());

            SampleController controller = new SampleController(sampleService, consoleView);
            controller.handleSearch();

            verify(sampleService, times(1)).searchSample("GaAs");
        }

        @Test
        @DisplayName("검색 결과 없을 때 출력에 '없' 포함")
        void searchShowsEmptyMessageWhenNoResult() {
            String input = "없는키워드\n";
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            when(sampleService.searchSample("없는키워드")).thenReturn(Collections.emptyList());

            SampleController controller = new SampleController(sampleService, consoleView);
            controller.handleSearch();

            String output = outputStream.toString(StandardCharsets.UTF_8);
            assertTrue(
                output.contains("없"),
                "검색 결과 없을 때 출력에 '없'이 포함되어야 합니다. 실제 출력: " + output
            );
        }
    }

    // ── Safety ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("안전성")
    class Safety {

        @Test
        @DisplayName("서비스가 예외를 던져도 오류 메시지를 출력하고 예외가 외부로 전파되지 않음")
        void registerShowsErrorWhenServiceThrows() {
            String input = "GaAs 웨이퍼\n120\n0.85\n50\n";
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            when(sampleService.register(anyString(), anyInt(), anyDouble(), anyInt()))
                .thenThrow(new IllegalArgumentException("유효하지 않은 입력입니다."));

            SampleController controller = new SampleController(sampleService, consoleView);

            assertDoesNotThrow(controller::handleRegister,
                "서비스에서 예외가 발생해도 handleRegister()는 예외를 외부로 전파하지 않아야 합니다.");

            String output = outputStream.toString(StandardCharsets.UTF_8);
            assertFalse(output.isBlank(),
                "오류 발생 시 출력이 비어 있으면 안 됩니다.");
        }
    }
}
