package com.crossbowffs.nekosms.data;

import android.content.ContentUris;
import android.net.Uri;
import com.crossbowffs.nekosms.provider.DatabaseContract;

public class SmsFilterData {
    private long mId = -1;
    private SmsFilterAction mAction;
    private final SmsFilterPatternData mSenderPattern = new SmsFilterPatternData(SmsFilterField.SENDER);
    private final SmsFilterPatternData mBodyPattern = new SmsFilterPatternData(SmsFilterField.BODY);

    public void reset() {
        mId = -1;
        mAction = null;
        mSenderPattern.reset();
        mBodyPattern.reset();
    }

    public SmsFilterData setId(long id) {
        mId = id;
        return this;
    }

    public long getId() {
        return mId;
    }

    public SmsFilterData setAction(SmsFilterAction action) {
        mAction = action;
        return this;
    }

    public SmsFilterAction getAction() {
        return mAction;
    }

    public SmsFilterPatternData getSenderPattern() {
        return mSenderPattern;
    }

    public SmsFilterPatternData getBodyPattern() {
        return mBodyPattern;
    }

    public SmsFilterPatternData getPatternForField(SmsFilterField field) {
        switch (field) {
        case SENDER:
            return getSenderPattern();
        case BODY:
            return getBodyPattern();
        default:
            throw new AssertionError("Invalid filter field: " + field);
        }
    }

    public Uri getUri() {
        long id = getId();
        if (id < 0) {
            return null;
        }
        return ContentUris.withAppendedId(DatabaseContract.FilterRules.CONTENT_URI, id);
    }

    @Override
    public String toString() {
        return "SmsFilterData{" +
            "id=" + mId +
            ", action=" + mAction +
            ", senderPattern=" + mSenderPattern +
            ", bodyPattern=" + mBodyPattern +
            "}";
    }
}
