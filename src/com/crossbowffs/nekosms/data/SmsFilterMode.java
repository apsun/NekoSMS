package com.crossbowffs.nekosms.data;

public enum SmsFilterMode {
    REGEX,
    // WILDCARD,
    CONTAINS,
    PREFIX,
    SUFFIX,
    EQUALS;

    public static SmsFilterMode parse(String modeString) {
        if (modeString == null) {
            return null;
        }

        try {
            return SmsFilterMode.valueOf(modeString);
        } catch (IllegalArgumentException e) {
            throw new InvalidFilterException("Invalid filter mode value: " + modeString, e);
        }
    }
}
