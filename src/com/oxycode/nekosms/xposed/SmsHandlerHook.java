package com.oxycode.nekosms.xposed;

import android.content.*;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import com.oxycode.nekosms.data.*;
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

    private final Object mFiltersLock = new Object();
    private ContentObserver mContentObserver;
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
            SmsFilter filter;
            try {
                SmsFilterField field = SmsFilterField.valueOf(cursor.getString(0));
                SmsFilterMode mode = SmsFilterMode.valueOf(cursor.getString(1));
                String pattern = cursor.getString(2);
                filter = createSmsFilter(field, mode, pattern);
            } catch (Exception e) {
                Xlog.e(TAG, "Failed to create SMS filter", e);
                continue;
            }
            filters.add(filter);
        }
        cursor.close();
        return filters;
    }

    private ContentObserver registerContentObserver(Context context) {
        Xlog.i(TAG, "Registering SMS filter content observer");

        ContentObserver contentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                Xlog.d(TAG, "SMS filter database updated, marking cache as dirty");
                synchronized (mFiltersLock) {
                    mSmsFilters = null;
                }
            }
        };

        Uri filtersUri = NekoSmsContract.Filters.CONTENT_URI;
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.registerContentObserver(filtersUri, true, contentObserver);
        return contentObserver;
    }

    private Uri writeBlockedSms(Context context, SmsMessage[] messageParts, String messageBody) {
        Uri messagesUri = NekoSmsContract.Blocked.CONTENT_URI;
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues values = serializeSmsMessage(messageParts, messageBody);
        return contentResolver.insert(messagesUri, values);
    }

    private boolean shouldFilterMessage(Context context, String sender, String body) {
        List<SmsFilter> smsFilters;
        synchronized (mFiltersLock) {
            smsFilters = mSmsFilters;
            if (smsFilters == null) {
                Xlog.d(TAG, "Cached SMS filters dirty, loading from database");
                smsFilters = mSmsFilters = getSmsFilters(context);
            }
        }

        for (SmsFilter filter : smsFilters) {
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

    private void afterConstructorHandler(XC_MethodHook.MethodHookParam param) {
        Context context = (Context)param.args[1];
        if (mContentObserver == null) {
            mContentObserver = registerContentObserver(context);
        }
    }

    private void beforeDispatchIntentHandler(XC_MethodHook.MethodHookParam param) {
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

    private void hookConstructor(XC_LoadPackage.LoadPackageParam lpparam, XC_MethodHook hook) {
        String className = "com.android.internal.telephony.InboundSmsHandler";
        Class<?> param1Type = String.class;
        Class<?> param2Type = Context.class;
        String param3Type = "com.android.internal.telephony.SmsStorageMonitor";
        String param4Type = "com.android.internal.telephony.PhoneBase";
        String param5Type = "com.android.internal.telephony.CellBroadcastHandler";

        Xlog.i(TAG, "Hooking InboundSmsHandler constructor");

        XposedHelpers.findAndHookConstructor(className, lpparam.classLoader,
            param1Type, param2Type, param3Type, param4Type, param5Type, hook);
    }

    private void hookDispatchIntent19(XC_LoadPackage.LoadPackageParam lpparam, XC_MethodHook hook) {
        String className = "com.android.internal.telephony.InboundSmsHandler";
        String methodName = "dispatchIntent";
        Class<?> param1Type = Intent.class;
        Class<?> param2Type = String.class;
        Class<?> param3Type = int.class;
        Class<?> param4Type = BroadcastReceiver.class;

        Xlog.i(TAG, "Hooking InboundSmsHandler#dispatchIntent() for Android v19+");

        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, methodName,
            param1Type, param2Type, param3Type, param4Type, hook);
    }

    private void hookDispatchIntent21(XC_LoadPackage.LoadPackageParam lpparam, XC_MethodHook hook) {
        String className = "com.android.internal.telephony.InboundSmsHandler";
        String methodName = "dispatchIntent";
        Class<?> param1Type = Intent.class;
        Class<?> param2Type = String.class;
        Class<?> param3Type = int.class;
        Class<?> param4Type = BroadcastReceiver.class;
        Class<?> param5Type = UserHandle.class;

        Xlog.i(TAG, "Hooking InboundSmsHandler#dispatchIntent() for Android v21+");

        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, methodName,
            param1Type, param2Type, param3Type, param4Type, param5Type, hook);
    }

    private void hookSmsHandler(XC_LoadPackage.LoadPackageParam lpparam) {
        XC_MethodHook constructorHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    afterConstructorHandler(param);
                } catch (Throwable e) {
                    Xlog.e(TAG, "Error occurred in constructor hook", e);
                    throw e;
                }
            }
        };

        XC_MethodHook dispatchIntentHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    beforeDispatchIntentHandler(param);
                } catch (Throwable e) {
                    Xlog.e(TAG, "Error occurred in dispatchIntent() hook", e);
                    throw e;
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            hookConstructor(lpparam, constructorHook);
            hookDispatchIntent21(lpparam, dispatchIntentHook);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            hookConstructor(lpparam, constructorHook);
            hookDispatchIntent19(lpparam, dispatchIntentHook);
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
