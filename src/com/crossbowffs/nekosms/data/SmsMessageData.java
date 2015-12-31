package com.crossbowffs.nekosms.data;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;
import com.crossbowffs.nekosms.provider.NekoSmsContract;

public class SmsMessageData implements Parcelable {
    public static final Creator<SmsMessageData> CREATOR = new Creator<SmsMessageData>() {
        @Override
        public SmsMessageData createFromParcel(Parcel in) {
            return new SmsMessageData(in);
        }

        @Override
        public SmsMessageData[] newArray(int size) {
            return new SmsMessageData[size];
        }
    };

    private long mId = -1;
    private String mSender;
    private String mBody;
    private long mTimeSent;
    private long mTimeReceived;

    public SmsMessageData() {

    }

    private SmsMessageData(Parcel in) {
        mId = in.readLong();
        mSender = in.readString();
        mBody = in.readString();
        mTimeSent = in.readLong();
        mTimeReceived = in.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeString(mSender);
        dest.writeString(mBody);
        dest.writeLong(mTimeSent);
        dest.writeLong(mTimeReceived);
    }

    public ContentValues serialize() {
        ContentValues values = new ContentValues(5);
        if (mId >= 0) {
            values.put(NekoSmsContract.Blocked._ID, mId);
        }
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
