package com.crossbowffs.nekosms.data;

public class SmsFilterData {
    private long mId = -1;
    private final SmsFilterPatternData mSenderPattern = new SmsFilterPatternData(SmsFilterField.SENDER);
    private final SmsFilterPatternData mBodyPattern = new SmsFilterPatternData(SmsFilterField.BODY);

    public void reset() {
        mId = -1;
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

    public SmsFilterPatternData getSenderPattern() {
        return mSenderPattern;
    }

    public SmsFilterPatternData getBodyPattern() {
        return mBodyPattern;
    }
}
