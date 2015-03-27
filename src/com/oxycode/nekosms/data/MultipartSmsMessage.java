package com.oxycode.nekosms.data;

import android.content.ContentValues;
import android.telephony.SmsMessage;
import com.oxycode.nekosms.provider.NekoSmsContract;

public class MultipartSmsMessage {
    private final SmsMessage[] mMessageParts;
    private final String mMessageBody;

    public MultipartSmsMessage(SmsMessage[] messageParts) {
        if (messageParts.length == 0) {
            throw new IllegalArgumentException("No message parts provided");
        }
        mMessageParts = messageParts;
        mMessageBody = mergeMessageParts(messageParts);
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
        ContentValues values = new ContentValues();
        values.put(NekoSmsContract.Blocked.ADDRESS, getDisplayOriginatingAddress());
        values.put(NekoSmsContract.Blocked.BODY, getDisplayMessageBody());
        values.put(NekoSmsContract.Blocked.DATE_SENT, getTimestampMillis());
        values.put(NekoSmsContract.Blocked.DATE, System.currentTimeMillis());
        values.put(NekoSmsContract.Blocked.PROTOCOL, getProtocolIdentifier());
        values.put(NekoSmsContract.Blocked.SEEN, 0);
        values.put(NekoSmsContract.Blocked.READ, 0);
        values.put(NekoSmsContract.Blocked.SUBJECT, getPseudoSubject());
        values.put(NekoSmsContract.Blocked.REPLY_PATH_PRESENT, isReplyPathPresent() ? 1 : 0);
        values.put(NekoSmsContract.Blocked.SERVICE_CENTER, getServiceCenterAddress());
        return values;
    }

    public String getServiceCenterAddress() {
        return mMessageParts[0].getServiceCenterAddress();
    }

    public String getDisplayOriginatingAddress() {
        return mMessageParts[0].getDisplayOriginatingAddress();
    }

    public String getDisplayMessageBody() {
        return mMessageBody;
    }

    public String getPseudoSubject() {
        return mMessageParts[0].getPseudoSubject();
    }

    public long getTimestampMillis() {
        return mMessageParts[0].getTimestampMillis();
    }

    public int getProtocolIdentifier() {
        return mMessageParts[0].getProtocolIdentifier();
    }

    public boolean isReplyPathPresent() {
        return mMessageParts[0].isReplyPathPresent();
    }
}
