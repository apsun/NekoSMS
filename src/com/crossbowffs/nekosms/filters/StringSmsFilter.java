package com.crossbowffs.nekosms.filters;

import com.crossbowffs.nekosms.data.SmsFilterData;

/* package */ class StringSmsFilter extends SmsFilter {
    private final String mNormalizedPattern;

    public StringSmsFilter(SmsFilterData data) {
        super(data);
        if (!isCaseSensitive()) {
            mNormalizedPattern = getPattern().toLowerCase();
        } else {
            mNormalizedPattern = getPattern();
        }
    }

    @Override
    public boolean matchImpl(String sender, String body) {
        String testString;
        switch (getField()) {
        case SENDER:
            testString = sender;
            break;
        case BODY:
            testString = body;
            break;
        default:
            throw new AssertionError("Invalid field: " + getField());
        }

        if (!isCaseSensitive()) {
            testString = testString.toLowerCase();
        }

        switch (getMode()) {
        case CONTAINS:
            return testString.contains(mNormalizedPattern);
        case PREFIX:
            return testString.startsWith(mNormalizedPattern);
        case SUFFIX:
            return testString.endsWith(mNormalizedPattern);
        case EQUALS:
            return testString.equals(mNormalizedPattern);
        default:
            throw new AssertionError("Invalid mode: " + getMode());
        }
    }
}
