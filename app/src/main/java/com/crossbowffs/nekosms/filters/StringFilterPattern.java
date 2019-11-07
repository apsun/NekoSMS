package com.crossbowffs.nekosms.filters;

import com.crossbowffs.nekosms.data.SmsFilterPatternData;
import com.crossbowffs.nekosms.utils.StringUtils;

import java.text.Normalizer;

/* package */public class StringFilterPattern extends SmsFilterPattern {
    private final String mNormalizedPattern;

    public StringFilterPattern(SmsFilterPatternData data) {
        super(data);

        // Make sure the pattern is normalized, since Java does not
        // perform Unicode normalization when comparing strings.
        // The sender and body values are normalized once in the
        // Xposed module to improve performance.
        mNormalizedPattern = Normalizer.normalize(getPattern(), Normalizer.Form.NFC);
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

        boolean ignoreCase = !isCaseSensitive();
        switch (getMode()) {
        case CONTAINS:
            return StringUtils.contains(testString, mNormalizedPattern, ignoreCase);
        case PREFIX:
            return StringUtils.startsWith(testString, mNormalizedPattern, ignoreCase);
        case SUFFIX:
            return StringUtils.endsWith(testString, mNormalizedPattern, ignoreCase);
        case EQUALS:
            return StringUtils.equals(testString, mNormalizedPattern, ignoreCase);
        default:
            throw new AssertionError("Invalid mode: " + getMode());
        }
    }
}
