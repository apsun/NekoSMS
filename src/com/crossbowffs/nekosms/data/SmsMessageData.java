package com.crossbowffs.nekosms.data;

public class SmsMessageData {
    private long mId = -1;
    private String mSender;
    private String mBody;
    private long mTimeSent;
    private long mTimeReceived;
    private boolean mRead;
    private boolean mSeen;

    public void setId(long id) {
        mId = id;
    }

    public void setSender(String sender) {
        mSender = sender;
    }

    public void setBody(String body) {
        mBody = body;
    }

    public void setTimeSent(long timeSent) {
        mTimeSent = timeSent;
    }

    public void setTimeReceived(long timeReceived) {
        mTimeReceived = timeReceived;
    }

    public void setRead(boolean read) {
        mRead = read;
    }

    public void setSeen(boolean seen) {
        mSeen = seen;
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
