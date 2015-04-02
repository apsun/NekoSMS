package com.oxycode.nekosms.data;

public class StringSmsFilter extends SmsFilter {
    private final SmsFilterField mField;
    private final SmsFilterMode mMode;
    private final String mPattern;
    private final boolean mCaseSensitive;

    public StringSmsFilter(SmsFilterData data) {
        mField = data.getField();
        mMode = data.getMode();
        mCaseSensitive = data.isCaseSensitive();
        if (mCaseSensitive) {
            mPattern = data.getPattern().toLowerCase();
        } else {
            mPattern = data.getPattern();
        }
    }

    @Override
    public boolean matches(String sender, String body) {
        String testString = null;
        switch (mField) {
        case SENDER:
            testString = sender;
            break;
        case BODY:
            testString = body;
            break;
        }

        if (mCaseSensitive) {
            testString = testString.toLowerCase();
        }

        boolean matches = false;
        switch (mMode) {
        case CONTAINS:
            matches = testString.contains(mPattern);
            break;
        case PREFIX:
            matches = testString.startsWith(mPattern);
            break;
        case SUFFIX:
            matches = testString.endsWith(mPattern);
            break;
        case EQUALS:
            matches = testString.equals(mPattern);
            break;
        }

        return matches;
    }
}
