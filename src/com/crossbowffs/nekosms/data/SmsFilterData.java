package com.crossbowffs.nekosms.data;

public class SmsFilterData {
    private long mId = -1;
    private SmsFilterAction mAction;
    private SmsFilterPatternData mSenderPattern;
    private SmsFilterPatternData mBodyPattern;

    public void setId(long id) {
        mId = id;
    }

    public void setAction(SmsFilterAction action) {
        mAction = action;
    }

    public void setSenderPattern(SmsFilterPatternData pattern) {
        mSenderPattern = pattern;
    }

    public void setBodyPattern(SmsFilterPatternData pattern) {
        mBodyPattern = pattern;
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
