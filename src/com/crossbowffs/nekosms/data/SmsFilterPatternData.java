package com.crossbowffs.nekosms.data;

public class SmsFilterPatternData {
    private SmsFilterField mField;
    private SmsFilterMode mMode;
    private String mPattern;
    private boolean mCaseSensitive;

    public void setField(SmsFilterField field) {
        mField = field;
    }

    public void setMode(SmsFilterMode mode) {
        mMode = mode;
    }

    public void setPattern(String pattern) {
        mPattern = pattern;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        mCaseSensitive = caseSensitive;
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
