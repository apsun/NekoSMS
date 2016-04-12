package com.crossbowffs.nekosms.data;

import android.content.ContentValues;
import com.crossbowffs.nekosms.provider.NekoSmsContract;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SmsFilterData {
    private long mId = -1;
    private SmsFilterAction mAction = SmsFilterAction.BLOCK; // For compatibility
    private SmsFilterField mField;
    private SmsFilterMode mMode;
    private String mPattern;
    private boolean mCaseSensitive;

    public ContentValues serialize() {
        ContentValues values = new ContentValues(6);
        if (mId >= 0) {
            values.put(NekoSmsContract.Filters._ID, mId);
        }
        values.put(NekoSmsContract.Filters.ACTION, getAction().name());
        values.put(NekoSmsContract.Filters.FIELD, getField().name());
        values.put(NekoSmsContract.Filters.MODE, getMode().name());
        values.put(NekoSmsContract.Filters.PATTERN, getPattern());
        values.put(NekoSmsContract.Filters.CASE_SENSITIVE, isCaseSensitive() ? 1 : 0);
        return values;
    }

    public void setId(long id) {
        mId = id;
    }

    public void setAction(SmsFilterAction action) {
        mAction = action;
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

    public SmsFilterAction getAction() {
        return mAction;
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

    public void validate() {
        if (mAction == null) throw new InvalidFilterException("Missing filter action");
        if (mField == null) throw new InvalidFilterException("Missing filter field");
        if (mMode == null) throw new InvalidFilterException("Missing filter mode");
        if (mPattern == null || mPattern.isEmpty()) {
            throw new InvalidFilterException("Missing filter pattern");
        }

        if (mMode == SmsFilterMode.REGEX) {
            try {
                Pattern.compile(mPattern);
            } catch (PatternSyntaxException e) {
                throw new InvalidFilterException("Invalid regular expression pattern: " + mPattern, e);
            }
        }
    }
}
