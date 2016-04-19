package com.crossbowffs.nekosms.filters;

import com.crossbowffs.nekosms.data.SmsFilterField;
import com.crossbowffs.nekosms.data.SmsFilterMode;
import com.crossbowffs.nekosms.data.SmsFilterPatternData;

/* package */ abstract class SmsFilterPattern {
    private SmsFilterField mField;
    private SmsFilterMode mMode;
    private String mPattern;
    private boolean mCaseSensitive;

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

    public abstract boolean match(String sender, String body);
}
