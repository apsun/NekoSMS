package com.crossbowffs.nekosms.data;

import android.content.ContentValues;
import com.crossbowffs.nekosms.provider.NekoSmsContract;

public class SmsMessageData {
    private long mId = -1;
    private String mSender;
    private String mBody;
    private long mTimeSent;
    private long mTimeReceived;
    private boolean mRead;
    private boolean mSeen;

    public ContentValues serialize() {
        ContentValues values = new ContentValues(7);
        if (mId >= 0) {
            values.put(NekoSmsContract.Blocked._ID, mId);
        }
        values.put(NekoSmsContract.Blocked.SENDER, getSender());
        values.put(NekoSmsContract.Blocked.BODY, getBody());
        values.put(NekoSmsContract.Blocked.TIME_SENT, getTimeSent());
        values.put(NekoSmsContract.Blocked.TIME_RECEIVED, getTimeReceived());
        values.put(NekoSmsContract.Blocked.READ, isRead());
        values.put(NekoSmsContract.Blocked.SEEN, isSeen());
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
