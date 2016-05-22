package com.crossbowffs.nekosms.data;

public class SmsFilterPatternData {
    private SmsFilterField mField;
    private SmsFilterMode mMode;
    private String mPattern;
    private boolean mCaseSensitive;

    public SmsFilterPatternData setField(SmsFilterField field) {
        mField = field;
        return this;
    }

    public SmsFilterPatternData setMode(SmsFilterMode mode) {
        mMode = mode;
        return this;
    }

    public SmsFilterPatternData setPattern(String pattern) {
        mPattern = pattern;
        return this;
    }

    public SmsFilterPatternData setCaseSensitive(boolean caseSensitive) {
        mCaseSensitive = caseSensitive;
        return this;
    }

    public SmsFilterField getField() {
        return mField;
    }

    public SmsFilterMode getMode() {
        return mMode;
    }

    public String getPattern() {
        return mPattern;
    }

    public boolean isCaseSensitive() {
        return mCaseSensitive;
    }
}
