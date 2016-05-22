package com.crossbowffs.nekosms.data;

public class SmsFilterData {
    private long mId = -1;
    private SmsFilterAction mAction;
    private SmsFilterPatternData mSenderPattern;
    private SmsFilterPatternData mBodyPattern;

    public SmsFilterData setId(long id) {
        mId = id;
        return this;
    }

    public SmsFilterData setAction(SmsFilterAction action) {
        mAction = action;
        return this;
    }

    public SmsFilterData setSenderPattern(SmsFilterPatternData pattern) {
        if (pattern != null && pattern.getField() != SmsFilterField.SENDER) {
            throw new IllegalArgumentException("Pattern must have field set to SENDER");
        }
        mSenderPattern = pattern;
        return this;
    }

    public SmsFilterData setBodyPattern(SmsFilterPatternData pattern) {
        if (pattern != null && pattern.getField() != SmsFilterField.BODY) {
            throw new IllegalArgumentException("Pattern must have field set to BODY");
        }
        mBodyPattern = pattern;
        return this;
    }

    public long getId() {
        return mId;
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
}
