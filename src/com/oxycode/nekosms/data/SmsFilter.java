package com.oxycode.nekosms.data;

public abstract class SmsFilter {
    public static SmsFilter create(SmsFilterData data) {
        switch (data.getMode()) {
        case REGEX:
            return new RegexSmsFilter(data);
        case WILDCARD:
            throw new UnsupportedOperationException("Not yet implemented");
        case CONTAINS:
        case PREFIX:
        case SUFFIX:
        case EQUALS:
            return new StringSmsFilter(data);
        default:
            throw new IllegalArgumentException("Invalid filter mode: " + data.getMode());
        }
    }

    public abstract boolean matches(String sender, String body);
}
