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

    public static String join(String delimiter, Object[] parts) {
        if (parts.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object part : parts) {
            sb.append(part);
            sb.append(delimiter);
        }
        sb.setLength(sb.length() - delimiter.length());
        return sb.toString();
    }
}
