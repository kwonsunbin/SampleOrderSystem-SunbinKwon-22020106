package com.ssemi.sampleorder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("Main 클래스 구조 검증")
class MainTest {

    @Test
    @DisplayName("main 메서드가 존재한다")
    void mainMethodExists() {
        assertDoesNotThrow(() -> {
            Class<?> mainClass = Class.forName("com.ssemi.sampleorder.Main");
            mainClass.getMethod("main", String[].class);
        });
    }
}
