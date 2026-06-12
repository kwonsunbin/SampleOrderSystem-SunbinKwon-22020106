package com.ssemi.sampleorder.controller;

import com.ssemi.sampleorder.view.ConsoleView;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("MainController 테스트")
@ExtendWith(MockitoExtension.class)
class MainControllerTest {

    @Mock
    private SampleController sampleController;

    @Mock
    private OrderController orderController;

    @Mock
    private ProductionController productionController;

    @Mock
    private MonitorController monitorController;

    @Mock
    private ReleaseController releaseController;

    // ── Regression: 메뉴 분기 ───────────────────────────────────────────────────

    @Nested
    @DisplayName("메뉴 분기")
    class MenuBranch {

        @Test
        @DisplayName("입력 '0'이면 루프가 정상 종료됨")
        void inputZeroExitsLoop() {
            String input = "0\n";
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            MainController mainController = new MainController(
                sampleController, orderController, productionController,
                monitorController, releaseController, consoleView
            );

            assertDoesNotThrow(mainController::run,
                "입력 '0'에서 run()은 예외 없이 정상 종료되어야 합니다.");
        }

        @Test
        @DisplayName("입력 '1' 후 '0'이면 sampleController.showMenu()가 1회 호출됨")
        void inputOneEntersSampleController() {
            String input = "1\n0\n";
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            MainController mainController = new MainController(
                sampleController, orderController, productionController,
                monitorController, releaseController, consoleView
            );

            mainController.run();

            verify(sampleController, times(1)).showMenu();
        }

        @Test
        @DisplayName("입력 '2' 후 '0'이면 orderController.showMenu()가 1회 호출됨")
        void inputTwoEntersOrderController() {
            String input = "2\n0\n";
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            MainController mainController = new MainController(
                sampleController, orderController, productionController,
                monitorController, releaseController, consoleView
            );

            mainController.run();

            verify(orderController, times(1)).showMenu();
        }
    }

    // ── Safety: 잘못된 입력 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("잘못된 입력 처리")
    class InvalidInput {

        @Test
        @DisplayName("범위 밖 숫자 '9' 입력 시 오류 메시지 출력 후 '0'에서 정상 종료")
        void invalidNumberShowsErrorAndContinues() {
            String input = "9\n0\n";
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            MainController mainController = new MainController(
                sampleController, orderController, productionController,
                monitorController, releaseController, consoleView
            );

            assertDoesNotThrow(mainController::run,
                "범위 밖 숫자 입력 시 run()은 예외 없이 정상 종료되어야 합니다.");

            String output = outputStream.toString(StandardCharsets.UTF_8);
            assertFalse(output.isBlank(),
                "잘못된 입력 시 오류/안내 메시지가 출력되어야 합니다.");
        }

        @Test
        @DisplayName("문자열 'abc' 입력 시 오류 메시지 출력 후 '0'에서 정상 종료")
        void nonNumericInputShowsErrorAndContinues() {
            String input = "abc\n0\n";
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
            ConsoleView consoleView = new ConsoleView(inputStream, new PrintStream(outputStream));

            MainController mainController = new MainController(
                sampleController, orderController, productionController,
                monitorController, releaseController, consoleView
            );

            assertDoesNotThrow(mainController::run,
                "숫자가 아닌 입력 시 run()은 예외 없이 정상 종료되어야 합니다.");

            String output = outputStream.toString(StandardCharsets.UTF_8);
            assertFalse(output.isBlank(),
                "잘못된 입력 시 오류/안내 메시지가 출력되어야 합니다.");
        }
    }
}
