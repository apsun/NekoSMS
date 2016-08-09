package com.crossbowffs.nekosms.filters;

import com.crossbowffs.nekosms.data.SmsFilterPatternData;

import java.text.Normalizer;

/* package */ class StringFilterPattern extends SmsFilterPattern {
    private final String mNormalizedPattern;

    public StringFilterPattern(SmsFilterPatternData data) {
        super(data);
        String pattern = getPattern();
        if (!isCaseSensitive()) {
            pattern = pattern.toLowerCase();
        }
        // Make sure the pattern is normalized, since Java does not
        // perform Unicode normalization when comparing strings.
        // The sender and body values are normalized once in the
        // Xposed module to improve performance.
        mNormalizedPattern = Normalizer.normalize(pattern, Normalizer.Form.NFC);
    }

    @Override
    public boolean match(String sender, String body) {
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
