package com.oxycode.nekosms.data;

import android.content.ContentValues;
import com.oxycode.nekosms.provider.NekoSmsContract;

public class SmsFilterData {
    private SmsFilterField mField;
    private SmsFilterMode mMode;
    private String mPattern;
    private int mFlags;

    public ContentValues serialize() {
        ContentValues values = new ContentValues(4);
        values.put(NekoSmsContract.Filters.FIELD, getField().name());
        values.put(NekoSmsContract.Filters.MODE, getMode().name());
        values.put(NekoSmsContract.Filters.PATTERN, getPattern());
        values.put(NekoSmsContract.Filters.FLAGS, getFlags());
        return values;
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

    public void setFlags(int flags) {
        mFlags = flags;
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

    public int getFlags() {
        return mFlags;
    }
}
