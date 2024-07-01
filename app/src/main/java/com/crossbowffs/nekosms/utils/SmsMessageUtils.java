package com.crossbowffs.nekosms.utils;

import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;

import com.crossbowffs.nekosms.data.SmsMessageData;

import java.text.Normalizer;

public final class SmsMessageUtils {
    private static final String SUBSCRIPTION_KEY = "subscription";

    private SmsMessageUtils() { }

    private static String getMessageBody(SmsMessage[] messageParts) {
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

    private static int getSubId(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int subId = intent.getIntExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX, -1);
            if (subId >= 0) {
                return subId;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            // SmsMessage#getSubId() cannot be relied upon; it was made defunct by
            // https://android-review.googlesource.com/c/platform/frameworks/base/+/1196441
            // Instead, just read from the intent directly.
            int subId = intent.getIntExtra(SUBSCRIPTION_KEY, -1);
            if (subId >= 0) {
                return subId;
            }
        }

        return -1;
    }

    public static SmsMessageData getMessageFromIntent(Intent intent) {
        SmsMessage[] messageParts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        String sender = messageParts[0].getDisplayOriginatingAddress();
        String body = getMessageBody(messageParts);
        long timeSent = messageParts[0].getTimestampMillis();
        long timeReceived = System.currentTimeMillis();
        int subId = getSubId(intent);

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
}
