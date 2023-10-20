package com.crossbowffs.nekosms.data;

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;

import com.crossbowffs.nekosms.provider.DatabaseContract;
import com.crossbowffs.nekosms.utils.SmsMessageUtils;

import java.text.Normalizer;

public class SmsMessageData {
    private long mId = -1;
    private String mSender;
    private String mBody;
    private long mTimeSent;
    private long mTimeReceived;
    private boolean mRead;
    private boolean mSeen;
    private int mSubId;

    public static SmsMessageData fromIntent(Intent intent) {
        SmsMessage[] messageParts = SmsMessageUtils.fromIntent(intent);
        String sender = messageParts[0].getDisplayOriginatingAddress();
        String body = SmsMessageUtils.getMessageBody(messageParts);
        long timeSent = messageParts[0].getTimestampMillis();
        long timeReceived = System.currentTimeMillis();
        int subId = SmsMessageUtils.getSubId(messageParts[0]);//Android 11 测试始终为0

        // 高版本android，换种获取sub_id的途径
        // 参考了 Telephony.Sms.Intents.getMessagesFromIntent(intent)的方法体中获取subId的方法
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            subId = intent.getIntExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,0);
        }

        SmsMessageData message = new SmsMessageData();
        message.setSender(Normalizer.normalize(sender, Normalizer.Form.NFC));
        message.setBody(Normalizer.normalize(body, Normalizer.Form.NFC));
        message.setTimeSent(timeSent);
        message.setTimeReceived(timeReceived);
        message.setRead(false);
        message.setSeen(false);
        message.setSubId(subId);
        return message;
    }

    public void reset() {
        mId = -1;
        mSender = null;
        mBody = null;
        mTimeSent = 0;
        mTimeReceived = 0;
        mRead = false;
        mSeen = false;
        mSubId = 0;
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

    public SmsMessageData setSubId(int subId) {
        mSubId = subId;
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

    public int getSubId() {
        return mSubId;
    }

    public Uri getUri() {
        long id = getId();
        if (id < 0) {
            return null;
        }
        return ContentUris.withAppendedId(DatabaseContract.BlockedMessages.CONTENT_URI, id);
    }
}
