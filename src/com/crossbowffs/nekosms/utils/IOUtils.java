package com.crossbowffs.nekosms.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class IOUtils {
    private IOUtils() { }

    public static String streamToString(InputStream inputStream, String encoding, int bufferSize) throws IOException {
        char[] buffer = new char[bufferSize];
        StringBuilder sb = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(inputStream, encoding)) {
            while (true) {
                int count = reader.read(buffer, 0, bufferSize);
                if (count < 0) break;
                sb.append(buffer, 0, count);
            }
        }
        return sb.toString();
    }

    public static String streamToString(InputStream inputStream) throws IOException {
        return streamToString(inputStream, "UTF-8", 2048);
    }
}
