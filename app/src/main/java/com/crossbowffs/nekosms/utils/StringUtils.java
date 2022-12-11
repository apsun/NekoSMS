package com.crossbowffs.nekosms.utils;

public final class StringUtils {
    private StringUtils() { }

    public static boolean equals(String a, String b, boolean ignoreCase) {
        if (ignoreCase) {
            return a.equalsIgnoreCase(b);
        } else {
            return a.equals(b);
        }
    }

    public static boolean startsWith(String haystack, String needle, boolean ignoreCase) {
        if (!ignoreCase) {
            return haystack.startsWith(needle);
        }
        return haystack.regionMatches(true, 0, needle, 0, needle.length());
    }

    public static boolean endsWith(String haystack, String needle, boolean ignoreCase) {
        if (!ignoreCase) {
            return haystack.endsWith(needle);
        }
        return haystack.regionMatches(true, haystack.length() - needle.length(), needle, 0, needle.length());
    }

    public static boolean contains(String haystack, String needle, boolean ignoreCase) {
        if (!ignoreCase) {
            return haystack.contains(needle);
        }

        if (needle.length() == 0) {
            return true;
        }

        char needleFirstLower = Character.toLowerCase(needle.charAt(0));
        char needleFirstUpper = Character.toUpperCase(needle.charAt(0));
        int end = haystack.length() - needle.length();

        for (int i = 0; i <= end; ++i) {
            char c = haystack.charAt(i);
            if (c == needleFirstLower || c == needleFirstUpper) {
                if (haystack.regionMatches(true, i, needle, 0, needle.length())) {
                    return true;
                }
            }
        }

        return false;
    }

    public static String escape(String str) {
        if (str == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(str.length() + 2);
        sb.append('"');
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            switch (c) {
            case '\t': sb.append("\\t"); break;
            case '\b': sb.append("\\b"); break;
            case '\n': sb.append("\\n"); break;
            case '\r': sb.append("\\r"); break;
            case '\f': sb.append("\\f"); break;
            case '\\': sb.append("\\\\"); break;
            case '\'': sb.append("\\'"); break;
            case '\"': sb.append("\\\""); break;
            default:
                if (c < 32 || c >= 127) {
                    sb.append(String.format("\\u%04x", (int)c));
                } else {
                    sb.append(c);
                }
                break;
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
