package com.crossbowffs.nekosms.data;

public enum SmsFilterField {
    SENDER,
    BODY;

    public static SmsFilterField parse(String fieldString) {
        if (fieldString == null) {
            return null;
        }

        try {
            return SmsFilterField.valueOf(fieldString);
        } catch (IllegalArgumentException e) {
            throw new InvalidFilterException("Invalid filter field value: " + fieldString, e);
        }
    }
}
