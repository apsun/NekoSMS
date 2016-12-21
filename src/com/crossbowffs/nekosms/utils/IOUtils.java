package com.crossbowffs.nekosms.utils;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
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

    public static boolean isValidFileName(CharSequence name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        // Convoluted way of comparing to "." and ".." so we don't have
        // to convert the CharSequence into a String, which prevents
        // unnecessary memory allocations.
        if (name.length() == 1 && name.charAt(0) == '.') {
            return false;
        }
        if (name.length() == 2 && name.charAt(0) == '.' && name.charAt(1) == '.') {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!isValidFileNameChar(c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidFileNameChar(char c) {
        // Check control characters
        if (c <= 0x1f || c == 0x7f) {
            return false;
        }

        // Check special characters
        switch (c) {
        case '"':
        case '*':
        case '/':
        case ':':
        case '<':
        case '>':
        case '?':
        case '\\':
        case '|':
            return false;
        default:
            return true;
        }
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

    public static boolean isExternalStorageFileUri(Uri uri) {
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            File extBase = Environment.getExternalStorageDirectory();
            File file = new File(uri.getPath());
            while (file != null) {
                if (file.equals(extBase)) {
                    return true;
                }
                file = file.getParentFile();
            }
        }
        return false;
    }
}
