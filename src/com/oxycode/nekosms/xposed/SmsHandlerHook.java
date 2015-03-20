package com.oxycode.nekosms.xposed;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.UserHandle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import com.oxycode.nekosms.data.RegexSmsFilter;
import com.oxycode.nekosms.data.SmsFilter;
import com.oxycode.nekosms.data.SmsFilterField;
import com.oxycode.nekosms.data.SmsFilterMode;
import com.oxycode.nekosms.provider.NekoSmsContract;
import com.oxycode.nekosms.utils.Xlog;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.List;

public class SmsHandlerHook implements IXposedHookLoadPackage {
    private static final String TAG = SmsHandlerHook.class.getSimpleName();

    private List<SmsFilter> mSmsFilters;

    private static String getMessageBody(SmsMessage[] messageParts) {
        if (messageParts.length == 1) {
            return messageParts[0].getMessageBody();
        } else {
            StringBuilder sb = new StringBuilder();
            for (SmsMessage messagePart : messageParts) {
                sb.append(messagePart.getMessageBody());
            }
            return sb.toString();
        }
    }

    private static ContentValues serializeSmsMessage(SmsMessage[] messageParts, String messageBody) {
        SmsMessage messagePart = messageParts[0];
        ContentValues values = new ContentValues();
        values.put(NekoSmsContract.Blocked.ADDRESS, messagePart.getOriginatingAddress());
        values.put(NekoSmsContract.Blocked.BODY, messageBody);
        values.put(NekoSmsContract.Blocked.DATE_SENT, messagePart.getTimestampMillis());
        values.put(NekoSmsContract.Blocked.DATE, System.currentTimeMillis());
        values.put(NekoSmsContract.Blocked.PROTOCOL, messagePart.getProtocolIdentifier());
        values.put(NekoSmsContract.Blocked.SEEN, 0);
        values.put(NekoSmsContract.Blocked.READ, 0);
        values.put(NekoSmsContract.Blocked.SUBJECT, messagePart.getPseudoSubject());
        values.put(NekoSmsContract.Blocked.REPLY_PATH_PRESENT, messagePart.isReplyPathPresent() ? 1 : 0);
        values.put(NekoSmsContract.Blocked.SERVICE_CENTER, messagePart.getServiceCenterAddress());
        return values;
    }

    private static SmsFilter createSmsFilter(SmsFilterField field, SmsFilterMode mode, String pattern) {
        switch (mode) {
            case REGEX:
                return new RegexSmsFilter(field, pattern);
            default:
                throw new UnsupportedOperationException("TBA");
        }
    }

    private List<SmsFilter> getSmsFilters(Context context) {
        Uri filtersUri = NekoSmsContract.Filters.CONTENT_URI;
        String[] projection = {
            NekoSmsContract.Filters.FIELD,
            NekoSmsContract.Filters.MODE,
            NekoSmsContract.Filters.PATTERN
        };
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(filtersUri, projection, null, null, null);
        List<SmsFilter> filters = new ArrayList<SmsFilter>(cursor.getCount());
        while (cursor.moveToNext()) {
            SmsFilterField field = SmsFilterField.valueOf(cursor.getString(0));
            SmsFilterMode mode = SmsFilterMode.valueOf(cursor.getString(1));
            String pattern = cursor.getString(2);
            SmsFilter filter = createSmsFilter(field, mode, pattern);
            filters.add(filter);
        }
        return filters;
    }

    private Uri writeBlockedSms(Context context, SmsMessage[] messageParts, String messageBody) {
        Uri messagesUri = NekoSmsContract.Blocked.CONTENT_URI;
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues values = serializeSmsMessage(messageParts, messageBody);
        return contentResolver.insert(messagesUri, values);
    }

    private boolean shouldFilterMessage(Context context, String sender, String body) {
        // TODO: Reload filters on database modification
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

    private void finishSmsBroadcast(Object smsHandler, Object smsReceiver) {
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
