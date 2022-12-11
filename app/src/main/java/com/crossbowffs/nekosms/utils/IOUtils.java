package com.crossbowffs.nekosms.utils;

import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

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

    public static boolean isParentUri(Uri parent, Uri child) {
        // Schemes must be equal
        String parentScheme = parent.getScheme();
        String childScheme = child.getScheme();
        if (parentScheme == null || childScheme == null) {
            return false;
        }
        if (!parentScheme.equals(childScheme)) {
            return false;
        }

        // Authorities must be equal
        String parentAuthority = parent.getAuthority();
        String childAuthority = child.getAuthority();
        if (parentAuthority == null || childAuthority == null) {
            return false;
        }
        if (!parentAuthority.equals(childAuthority)) {
            return false;
        }

        // Compare paths
        List<String> parentPathSegments = parent.getPathSegments();
        List<String> childPathSegments = child.getPathSegments();
        if (parentPathSegments.size() >= childPathSegments.size()) {
            return false;
        }
        for (int i = 0; i < parentPathSegments.size(); ++i) {
            if (!parentPathSegments.get(i).equals(childPathSegments.get(i))) {
                return false;
            }
        }

        return true;
    }
}
