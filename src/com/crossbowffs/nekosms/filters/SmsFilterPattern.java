package com.crossbowffs.nekosms.filters;

import com.crossbowffs.nekosms.data.SmsFilterField;
import com.crossbowffs.nekosms.data.SmsFilterMode;
import com.crossbowffs.nekosms.data.SmsFilterPatternData;
import com.crossbowffs.nekosms.utils.Xlog;

/* package */ abstract class SmsFilterPattern {
    private final SmsFilterField mField;
    private final SmsFilterMode mMode;
    private final String mPattern;
    private final boolean mCaseSensitive;

    public SmsFilterPattern(SmsFilterPatternData data) {
        mField = data.getField();
        mMode = data.getMode();
        mPattern = data.getPattern();
        mCaseSensitive = data.isCaseSensitive();
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

    public void printToLog() {
        Xlog.v("Field: %s", getField().name());
        Xlog.v("  Mode: %s", getMode().name());
        Xlog.v("  Pattern: %s", getPattern());
        Xlog.v("  Case sensitive: %s", isCaseSensitive());
    }

    public abstract boolean match(String sender, String body);
}
