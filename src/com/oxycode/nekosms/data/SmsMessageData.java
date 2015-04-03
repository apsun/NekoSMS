package com.oxycode.nekosms.data;

import android.content.ContentValues;
import com.oxycode.nekosms.provider.NekoSmsContract;

public class SmsMessageData {
    private long mId;
    private String mSender;
    private String mBody;
    private long mTimeSent;
    private long mTimeReceived;

    public ContentValues serialize() {
        ContentValues values = new ContentValues(4);
        values.put(NekoSmsContract.Blocked.SENDER, getSender());
        values.put(NekoSmsContract.Blocked.BODY, getBody());
        values.put(NekoSmsContract.Blocked.TIME_SENT, getTimeSent());
        values.put(NekoSmsContract.Blocked.TIME_RECEIVED, getTimeReceived());
        return values;
    }

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
}
