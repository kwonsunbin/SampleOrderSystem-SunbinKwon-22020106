package com.ssemi.sampleorder.repository.util;

import org.json.JSONArray;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonFileUtil {

    public static JSONArray readArray(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) return new JSONArray();
        try {
            String content = Files.readString(path);
            return new JSONArray(content);
        } catch (IOException e) {
            throw new UncheckedIOException("JSON 파일 읽기 실패: " + filePath, e);
        }
    }

    public static void writeArray(String filePath, JSONArray array) {
        Path path = Paths.get(filePath);
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            Files.writeString(path, array.toString(2));
        } catch (IOException e) {
            throw new UncheckedIOException("JSON 파일 쓰기 실패: " + filePath, e);
        }
    }
}
