package com.oxycode.nekosms.xposed;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.UserHandle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import com.oxycode.nekosms.data.*;
import com.oxycode.nekosms.utils.Xlog;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Arrays;
import java.util.List;

public class SmsHandlerHook implements IXposedHookLoadPackage {
    private static final String TAG = SmsHandlerHook.class.getSimpleName();

    private List<SmsFilter> mSmsFilters;

    private static String getMessageBody(SmsMessage[] messages) {
        if (messages.length == 1) {
            return messages[0].getMessageBody();
        } else {
            StringBuilder sb = new StringBuilder();
            for (SmsMessage message : messages) {
                sb.append(message.getMessageBody());
            }
            return sb.toString();
        }
    }

    private static ContentValues serializeSmsMessage(SmsMessage[] messageParts, String messageBody) {
        SmsMessage message = messageParts[0];
        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.Inbox.ADDRESS, message.getOriginatingAddress());
        values.put(Telephony.Sms.Inbox.BODY, messageBody);
        values.put(Telephony.Sms.Inbox.DATE_SENT, message.getTimestampMillis());
        values.put(Telephony.Sms.Inbox.DATE, System.currentTimeMillis());
        values.put(Telephony.Sms.Inbox.PROTOCOL, message.getProtocolIdentifier());
        values.put(Telephony.Sms.Inbox.SEEN, 0);
        values.put(Telephony.Sms.Inbox.READ, 0);
        String subject = message.getPseudoSubject();
        if (!TextUtils.isEmpty(subject)) {
            values.put(Telephony.Sms.Inbox.SUBJECT, subject);
        }
        values.put(Telephony.Sms.Inbox.REPLY_PATH_PRESENT, message.isReplyPathPresent() ? 1 : 0);
        values.put(Telephony.Sms.Inbox.SERVICE_CENTER, message.getServiceCenterAddress());
        return values;
    }

    private boolean shouldFilterMessage(Context context, String sender, String body) {
        if (mSmsFilters == null) {
            mSmsFilters = getSmsFilters(context);
        }

        for (SmsFilter filter : mSmsFilters) {
            if (filter.matches(sender, body)) {
                return true;
            }
        }
        return false;
    }

    private static SmsFilter createSmsFilter(String fieldStr, String modeStr, String pattern) {
        SmsFilterField field = SmsFilterField.valueOf(fieldStr);
        SmsFilterMode mode = SmsFilterMode.valueOf(modeStr);

        switch (mode) {
            case REGEX:
                return new RegexSmsFilter(field, pattern);
            default:
                throw new UnsupportedOperationException("TBA");
        }
    }

    private List<SmsFilter> getSmsFilters(Context context) {
        return Arrays.asList(new SmsFilter[] {new SmsFilter() {
            @Override
            public boolean matches(String sender, String body) {
                return body.startsWith("123");
            }
        }});

        /* TODO
        Uri filtersUri = Uri.parse("com.oxycode.nekosms.provider/filters");
        String[] projection = {"field", "mode", "pattern"};
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(filtersUri, projection, null, null, null);
        List<SmsFilter> filters = new ArrayList<SmsFilter>(cursor.getCount());
        while (cursor.moveToNext()) {
            SmsFilterField field = SmsFilterField.valueOf(cursor.getString(0));
            SmsFilterMode mode = SmsFilterMode.valueOf(cursor.getString(1));
            String pattern = cursor.getString(2);
            SmsFilter filter = SmsFilter.create(field, mode, pattern);
            filters.add(filter);
        }
        return filters;
        */
    }

    private void writeBlockedSms(Context context, SmsMessage[] messageParts, String messageBody) {
        /* TODO
        Uri messagesUri = Uri.parse("com.oxycode.nekosms.provider/messages");
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues values = serializeSmsMessage(messageParts, messageBody);
        Uri messageUri = contentResolver.insert(messagesUri, values);
        */
    }

    private void finishSmsBroadcast(Object smsHandler, Object smsReceiver) {
        // This code is equivalent to the following 2 lines:
        // deleteFromRawTable(mDeleteWhere, mDeleteWhereArgs);
        // sendMessage(EVENT_BROADCAST_COMPLETE);

        Xlog.d(TAG, "Removing raw SMS data from database");
        XposedHelpers.callMethod(smsHandler, "deleteFromRawTable",
            new Class<?>[] {String.class, String[].class},
            XposedHelpers.getObjectField(smsReceiver, "mDeleteWhere"),
            XposedHelpers.getObjectField(smsReceiver, "mDeleteWhereArgs"));

        Xlog.d(TAG, "Notifying completion of SMS broadcast");
        XposedHelpers.callMethod(smsHandler, "sendMessage",
            new Class<?>[] {int.class}, 3);
    }

    private void beforeSmsHandler(XC_MethodHook.MethodHookParam param) {
        Intent intent = (Intent)param.args[0];
        String action = intent.getAction();

        if (!Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(action)) {
            return;
        }

        Object smsHandler = param.thisObject;
        Context context = (Context)XposedHelpers.getObjectField(smsHandler, "mContext");

        SmsMessage[] messageParts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        String sender = messageParts[0].getOriginatingAddress();
        String body = getMessageBody(messageParts);
        Xlog.i(TAG, "Received a new SMS message");
        Xlog.v(TAG, "  Sender: %s", sender);
        Xlog.v(TAG, "  Body: %s", body);

        if (shouldFilterMessage(context, sender, body)) {
            Xlog.i(TAG, "  Result: Blocked");
            writeBlockedSms(context, messageParts, body);
            param.setResult(null);
            finishSmsBroadcast(smsHandler, param.args[3]);
        } else {
            Xlog.i(TAG, "  Result: Allowed");
        }
    }

    private void hookSmsHandler19(XC_LoadPackage.LoadPackageParam lpparam, XC_MethodHook hook) {
        String className = "com.android.internal.telephony.InboundSmsHandler";
        String methodName = "dispatchIntent";
        Class<?> param1Type = Intent.class;
        Class<?> param2Type = String.class;
        Class<?> param3Type = int.class;
        Class<?> param4Type = BroadcastReceiver.class;

        Xlog.i(TAG, "Hooking SMS handler for Android v19+");

        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, methodName,
            param1Type, param2Type, param3Type, param4Type, hook);
    }

    private void hookSmsHandler21(XC_LoadPackage.LoadPackageParam lpparam, XC_MethodHook hook) {
        String className = "com.android.internal.telephony.InboundSmsHandler";
        String methodName = "dispatchIntent";
        Class<?> param1Type = Intent.class;
        Class<?> param2Type = String.class;
        Class<?> param3Type = int.class;
        Class<?> param4Type = BroadcastReceiver.class;
        Class<?> param5Type = UserHandle.class;

        Xlog.i(TAG, "Hooking SMS handler for Android v21+");

        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, methodName,
            param1Type, param2Type, param3Type, param4Type, param5Type, hook);
    }

    private void hookSmsHandler(XC_LoadPackage.LoadPackageParam lpparam) {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    beforeSmsHandler(param);
                } catch (Throwable e) {
                    Xlog.e(TAG, "Error occurred in SMS handler hook", e);
                    throw e;
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            hookSmsHandler21(lpparam, hook);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            hookSmsHandler19(lpparam, hook);
        } else {
            throw new UnsupportedOperationException("NekoSMS is only supported on Android 4.4+");
        }
    }

    private static void loadShims(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        for (IXposedHookLoadPackage shim : CompatShimLoader.getEnabledShims()) {
            String shimName = shim.getClass().getSimpleName();
            try {
                Xlog.i(TAG, "Loading compatibility shim: %s", shimName);
                shim.handleLoadPackage(lpparam);
            } catch (Throwable e) {
                Xlog.e(TAG, "Error occurred while loading shim: %s", shimName, e);
                throw e;
            }
        }
    }

    private static void printDeviceInfo() {
        Xlog.i(TAG, "Phone manufacturer: %s", Build.MANUFACTURER);
        Xlog.i(TAG, "Phone model: %s", Build.MODEL);
        Xlog.i(TAG, "Android version: %s", Build.VERSION.RELEASE);
        Xlog.i(TAG, "Xposed bridge version: %d", XposedBridge.XPOSED_BRIDGE_VERSION);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!"com.android.phone".equals(lpparam.packageName)) {
            return;
        }

        Xlog.i(TAG, "NekoSMS initializing...");
        printDeviceInfo();
        loadShims(lpparam);
        try {
            hookSmsHandler(lpparam);
        } catch (Throwable e) {
            Xlog.e(TAG, "Failed to hook SMS handler", e);
            throw e;
        }
        Xlog.i(TAG, "NekoSMS initialization complete!");
    }
}
