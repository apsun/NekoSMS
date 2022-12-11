package com.crossbowffs.nekosms.utils;

import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import java.lang.reflect.Method;

public final class SmsMessageUtils {
    private static final Method sGetSubId;

    static {
        Method getSubId = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                getSubId = ReflectionUtils.getDeclaredMethod(SmsMessage.class, "getSubId");
            } catch (Exception e) {
                Xlog.e("Could not find SmsMessage.getSubId() method");
            }
        }
        sGetSubId = getSubId;
    }

    private SmsMessageUtils() { }

    public static SmsMessage[] fromIntent(Intent intent) {
        return Telephony.Sms.Intents.getMessagesFromIntent(intent);
    }

    public static int getSubId(SmsMessage message) {
        try {
            if (sGetSubId != null) {
                return (Integer)ReflectionUtils.invoke(sGetSubId, message);
            }
        } catch (Exception e) {
            Xlog.e("Failed to get SMS subscription ID", e);
        }
        return 0;
    }

    public static String getMessageBody(SmsMessage[] messageParts) {
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
}
