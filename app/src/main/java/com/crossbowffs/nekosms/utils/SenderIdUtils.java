package com.crossbowffs.nekosms.utils;

import java.util.regex.Pattern;

public final class SenderIdUtils {

    private static final Pattern REGEX_PATTERN_GENERAL_PHONE_NUMBER = Pattern.compile("^[+]*\\d*[(\\s-]{0,2}[0-9]{1,4}[)]{0,1}[-\\s./0-9]*$");
    private static final String REGEX_REMOVE_NON_DIGITS = "[^0-9]";
    private static final int PHONE_NUMBER_DIGITS_MIN = 3;
    private static final int PHONE_NUMBER_DIGITS_MAX = 15;

    private SenderIdUtils() {}

    public static boolean isSenderIdAlphanumeric(String senderId) {
        return !isValidPhoneNumber(senderId);
    }

    public static boolean isSenderIdPhone(String senderId) {
        return isValidPhoneNumber(senderId);
    }

    private static boolean isValidPhoneNumber(String senderId) {
        int numberOfDigits = senderId.replaceAll(REGEX_REMOVE_NON_DIGITS, "").length();
        if (numberOfDigits < PHONE_NUMBER_DIGITS_MIN || numberOfDigits > PHONE_NUMBER_DIGITS_MAX) {
            return false;
        }
        return REGEX_PATTERN_GENERAL_PHONE_NUMBER.matcher(senderId).matches();
    }
}
