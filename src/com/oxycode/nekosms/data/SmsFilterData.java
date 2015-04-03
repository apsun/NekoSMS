package com.oxycode.nekosms.data;

import android.content.ContentValues;
import com.oxycode.nekosms.provider.NekoSmsContract;

public class SmsFilterData {
    private long mId;
    private SmsFilterField mField;
    private SmsFilterMode mMode;
    private String mPattern;
    private boolean mCaseSensitive;

    public ContentValues serialize() {
        ContentValues values = new ContentValues(4);
        values.put(NekoSmsContract.Filters.FIELD, getField().name());
        values.put(NekoSmsContract.Filters.MODE, getMode().name());
        values.put(NekoSmsContract.Filters.PATTERN, getPattern());
        values.put(NekoSmsContract.Filters.CASE_SENSITIVE, isCaseSensitive() ? 1 : 0);
        return values;
    }

    public void setId(long id) {
        mId = id;
    }

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

    public long getId() {
        return mId;
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
