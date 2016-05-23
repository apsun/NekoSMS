package com.crossbowffs.nekosms.data;

public class SmsMessageData {
    private long mId;
    private String mSender;
    private String mBody;
    private long mTimeSent;
    private long mTimeReceived;
    private boolean mRead;
    private boolean mSeen;

    public SmsMessageData() {
        reset();
    }

    public void reset() {
        mId = -1;
        mSender = null;
        mBody = null;
        mTimeSent = 0;
        mTimeReceived = 0;
        mRead = false;
        mSeen = false;
    }

    public SmsMessageData setId(long id) {
        mId = id;
        return this;
    }

    public SmsMessageData setSender(String sender) {
        mSender = sender;
        return this;
    }

    public SmsMessageData setBody(String body) {
        mBody = body;
        return this;
    }

    public SmsMessageData setTimeSent(long timeSent) {
        mTimeSent = timeSent;
        return this;
    }

    public SmsMessageData setTimeReceived(long timeReceived) {
        mTimeReceived = timeReceived;
        return this;
    }

    public SmsMessageData setRead(boolean read) {
        mRead = read;
        return this;
    }

    public SmsMessageData setSeen(boolean seen) {
        mSeen = seen;
        return this;
    }

    public long getId() {
        return mId;
    }

    public String getSender() {
        return mSender;
    }

    public String getBody() {
        return mBody;
    }

    public long getTimeSent() {
        return mTimeSent;
    }

    public long getTimeReceived() {
        return mTimeReceived;
    }

    public boolean isRead() {
        return mRead;
    }

    public boolean isSeen() {
        return mSeen;
    }
}
