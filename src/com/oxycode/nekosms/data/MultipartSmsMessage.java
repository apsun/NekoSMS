package com.oxycode.nekosms.data;

import android.content.ContentValues;
import android.telephony.SmsMessage;
import com.oxycode.nekosms.provider.NekoSmsContract;

public class MultipartSmsMessage {
    private final String mSender;
    private final String mMessageBody;
    private final long mTimeSent;
    private final long mTimeReceived;

    public MultipartSmsMessage(SmsMessage[] messageParts) {
        if (messageParts.length == 0) {
            throw new IllegalArgumentException("No message parts provided");
        }
        mSender = messageParts[0].getDisplayOriginatingAddress();
        mMessageBody = mergeMessageParts(messageParts);
        mTimeSent = messageParts[0].getTimestampMillis();
        mTimeReceived = System.currentTimeMillis();
    }

    private static String mergeMessageParts(SmsMessage[] messageParts) {
        if (messageParts.length == 1) {
            return messageParts[0].getDisplayMessageBody();
        } else {
            StringBuilder sb = new StringBuilder();
            for (SmsMessage messagePart : messageParts) {
                sb.append(messagePart.getDisplayMessageBody());
            }
            return sb.toString();
        }
    }

    public ContentValues serialize() {
        ContentValues values = new ContentValues(4);
        values.put(NekoSmsContract.Blocked.SENDER, getSender());
        values.put(NekoSmsContract.Blocked.BODY, getBody());
        values.put(NekoSmsContract.Blocked.TIME_SENT, mTimeSent);
        values.put(NekoSmsContract.Blocked.TIME_RECEIVED, mTimeReceived);
        return values;
    }

    public String getSender() {
        return mSender;
    }

    public String getBody() {
        return mMessageBody;
    }
}
