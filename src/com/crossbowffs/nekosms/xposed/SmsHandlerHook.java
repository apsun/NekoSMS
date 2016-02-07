package com.crossbowffs.nekosms.xposed;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.data.BroadcastConsts;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.database.CursorWrapper;
import com.crossbowffs.nekosms.database.SmsFilterDbLoader;
import com.crossbowffs.nekosms.filters.SmsFilter;
import com.crossbowffs.nekosms.preferences.BooleanPreference;
import com.crossbowffs.nekosms.preferences.Preferences;
import com.crossbowffs.nekosms.provider.NekoSmsContract;
import com.crossbowffs.nekosms.utils.AppOpsUtils;
import com.crossbowffs.nekosms.utils.Xlog;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;

public class SmsHandlerHook implements IXposedHookLoadPackage {
    private class ConstructorHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                afterConstructorHandler(param);
            } catch (Throwable e) {
                Xlog.e(TAG, "Error occurred in constructor hook", e);
                throw e;
            }
        }
    }

    private class DispatchIntentHook extends XC_MethodHook {
        private final int mReceiverIndex;

        public DispatchIntentHook(int receiverIndex) {
            mReceiverIndex = receiverIndex;
        }

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                beforeDispatchIntentHandler(param, mReceiverIndex);
            } catch (Throwable e) {
                Xlog.e(TAG, "Error occurred in dispatchIntent() hook", e);
                throw e;
            }
        }
    }

    private static final String TAG = SmsHandlerHook.class.getSimpleName();
    private static final String NEKOSMS_PACKAGE = BuildConfig.APPLICATION_ID;
    private static final int SMS_CHARACTER_LIMIT = 160;

    private final Object mFiltersLock = new Object();
    private ContentObserver mContentObserver;
    private ArrayList<SmsFilter> mSmsFilters;

    private static SmsMessageData createMessageData(SmsMessage[] messageParts) {
        String sender = messageParts[0].getDisplayOriginatingAddress();
        String body = mergeMessageBodies(messageParts);
        long timeSent = messageParts[0].getTimestampMillis();
        long timeReceived = System.currentTimeMillis();

        SmsMessageData message = new SmsMessageData();
        message.setSender(sender);
        message.setBody(body);
        message.setTimeSent(timeSent);
        message.setTimeReceived(timeReceived);
        return message;
    }

    private static String mergeMessageBodies(SmsMessage[] messageParts) {
        if (messageParts.length == 1) {
            return messageParts[0].getDisplayMessageBody();
        } else {
            StringBuilder sb = new StringBuilder(SMS_CHARACTER_LIMIT * messageParts.length);
            for (SmsMessage messagePart : messageParts) {
                sb.append(messagePart.getDisplayMessageBody());
            }
            return sb.toString();
        }
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

        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.registerContentObserver(NekoSmsContract.Filters.CONTENT_URI, true, contentObserver);
        return contentObserver;
    }

    private void broadcastBlockedSms(Context context, SmsMessageData message) {
        Intent intent = new Intent(BroadcastConsts.ACTION_RECEIVE_SMS);
        intent.putExtra(BroadcastConsts.EXTRA_MESSAGE, message);
        context.sendBroadcast(intent, BroadcastConsts.PERMISSION_RECEIVE_SMS);
    }

    private static ArrayList<SmsFilter> loadSmsFilters(Context context) {
        try (CursorWrapper<SmsFilterData> filterCursor = SmsFilterDbLoader.loadAllFilters(context)) {
            if (filterCursor == null) {
                // Can occur if NekoSMS has been uninstalled
                Xlog.e(TAG, "Failed to load SMS filters (loadAllFilters returned null)");
                return null;
            }

            ArrayList<SmsFilter> filters = new ArrayList<>(filterCursor.getCount());
            while (filterCursor.moveToNext()) {
                SmsFilterData filterData = filterCursor.get();
                filters.add(SmsFilter.create(filterData));
            }
            return filters;
        } catch (Exception e) {
            Xlog.e(TAG, "Failed to load SMS filters", e);
            return null;
        }
    }

    private boolean shouldFilterMessage(Context context, String sender, String body) {
        ArrayList<SmsFilter> filters;
        synchronized (mFiltersLock) {
            filters = mSmsFilters;
            if (filters == null) {
                Xlog.d(TAG, "Cached SMS filters dirty, loading from database");
                filters = loadSmsFilters(context);
            }
            mSmsFilters = filters;
        }

        if (filters == null) {
            // This might occur if NekoSMS has been uninstalled (removing the DB),
            // but the user has not rebooted their device yet. We should not filter
            // any messages in this state.
            return false;
        }

        for (SmsFilter filter : filters) {
            if (filter.matches(sender, body)) {
                return true;
            }
        }
        return false;
    }

    private void grantWriteSmsPermissions(Context context) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(NEKOSMS_PACKAGE, 0);
        } catch (PackageManager.NameNotFoundException e) {
            // This might occur if NekoSMS has been uninstalled.
            // In this case, don't do anything - we can't do anything
            // with the permissions anyways.
            Log.w(TAG, "NekoSMS package not found", e);
            return;
        }

        int uid = packageInfo.applicationInfo.uid;

        Xlog.i(TAG, "Checking if we have OP_WRITE_SMS permission");
        if (AppOpsUtils.checkOp(context, AppOpsUtils.OP_WRITE_SMS, uid, NEKOSMS_PACKAGE)) {
            Xlog.i(TAG, "Already have OP_WRITE_SMS permission");
        } else {
            Xlog.i(TAG, "Giving our package OP_WRITE_SMS permission");
            AppOpsUtils.allowOp(context, AppOpsUtils.OP_WRITE_SMS, uid, NEKOSMS_PACKAGE);
        }
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
        grantWriteSmsPermissions(context);
    }

    private void beforeDispatchIntentHandler(XC_MethodHook.MethodHookParam param, int receiverIndex) {
        Intent intent = (Intent)param.args[0];
        String action = intent.getAction();

        if (!Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(action)) {
            return;
        }

        // For some reason, caching the instance and calling reload() doesn't
        // update the values, so we have to create a new instance every time
        XSharedPreferences preferences = new XSharedPreferences(NEKOSMS_PACKAGE);
        BooleanPreference enablePref = Preferences.PREF_ENABLE;
        if (!preferences.getBoolean(enablePref.getKey(), enablePref.getDefaultValue())) {
            Xlog.d(TAG, "SMS blocking disabled in app preferences");
            return;
        }

        Object smsHandler = param.thisObject;
        Context context = (Context)XposedHelpers.getObjectField(smsHandler, "mContext");

        SmsMessage[] messageParts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        SmsMessageData message = createMessageData(messageParts);
        String sender = message.getSender();
        String body = message.getBody();
        Xlog.i(TAG, "Received a new SMS message");
        Xlog.v(TAG, "  Sender: %s", sender);
        Xlog.v(TAG, "  Body: %s", body);

        if (shouldFilterMessage(context, sender, body)) {
            Xlog.i(TAG, "  Result: Blocked");
            broadcastBlockedSms(context, message);
            param.setResult(null);
            finishSmsBroadcast(smsHandler, param.args[receiverIndex]);
        } else {
            Xlog.i(TAG, "  Result: Allowed");
        }
    }

    private void hookConstructor(XC_LoadPackage.LoadPackageParam lpparam) {
        String className = "com.android.internal.telephony.InboundSmsHandler";
        Class<?> param1Type = String.class;
        Class<?> param2Type = Context.class;
        String param3Type = "com.android.internal.telephony.SmsStorageMonitor";
        String param4Type = "com.android.internal.telephony.PhoneBase";
        String param5Type = "com.android.internal.telephony.CellBroadcastHandler";

        Xlog.i(TAG, "Hooking InboundSmsHandler constructor");

        XposedHelpers.findAndHookConstructor(className, lpparam.classLoader,
            param1Type, param2Type, param3Type, param4Type, param5Type, new ConstructorHook());
    }

    private void hookDispatchIntent19(XC_LoadPackage.LoadPackageParam lpparam) {
        String className = "com.android.internal.telephony.InboundSmsHandler";
        String methodName = "dispatchIntent";
        Class<?> param1Type = Intent.class;
        Class<?> param2Type = String.class;
        Class<?> param3Type = int.class;
        Class<?> param4Type = BroadcastReceiver.class;

        Xlog.i(TAG, "Hooking dispatchIntent() for Android v19+");

        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, methodName,
            param1Type, param2Type, param3Type, param4Type, new DispatchIntentHook(3));
    }

    private void hookDispatchIntent21(XC_LoadPackage.LoadPackageParam lpparam) {
        String className = "com.android.internal.telephony.InboundSmsHandler";
        String methodName = "dispatchIntent";
        Class<?> param1Type = Intent.class;
        Class<?> param2Type = String.class;
        Class<?> param3Type = int.class;
        Class<?> param4Type = BroadcastReceiver.class;
        Class<?> param5Type = UserHandle.class;

        Xlog.i(TAG, "Hooking dispatchIntent() for Android v21+");

        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, methodName,
            param1Type, param2Type, param3Type, param4Type, param5Type, new DispatchIntentHook(3));
    }

    private void hookDispatchIntent23(XC_LoadPackage.LoadPackageParam lpparam) {
        String className = "com.android.internal.telephony.InboundSmsHandler";
        String methodName = "dispatchIntent";
        Class<?> param1Type = Intent.class;
        Class<?> param2Type = String.class;
        Class<?> param3Type = int.class;
        Class<?> param4Type = Bundle.class;
        Class<?> param5Type = BroadcastReceiver.class;
        Class<?> param6Type = UserHandle.class;

        Xlog.i(TAG, "Hooking dispatchIntent() for Android v23+");

        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, methodName,
            param1Type, param2Type, param3Type, param4Type, param5Type, param6Type, new DispatchIntentHook(4));
    }

    private void hookSmsHandler(XC_LoadPackage.LoadPackageParam lpparam) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hookConstructor(lpparam);
            hookDispatchIntent23(lpparam);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            hookConstructor(lpparam);
            hookDispatchIntent21(lpparam);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            hookConstructor(lpparam);
            hookDispatchIntent19(lpparam);
        } else {
            throw new UnsupportedOperationException("NekoSMS is only supported on Android 4.4+");
        }
    }

    private static void printDeviceInfo() {
        Xlog.i(TAG, "Phone manufacturer: %s", Build.MANUFACTURER);
        Xlog.i(TAG, "Phone model: %s", Build.MODEL);
        Xlog.i(TAG, "Android version: %s", Build.VERSION.RELEASE);
        Xlog.i(TAG, "Xposed bridge version: %d", XposedBridge.XPOSED_BRIDGE_VERSION);
        Xlog.i(TAG, "NekoSMS version: %s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if ("com.android.phone".equals(lpparam.packageName)) {
            Xlog.i(TAG, "NekoSMS initializing...");
            printDeviceInfo();
            try {
                hookSmsHandler(lpparam);
            } catch (Throwable e) {
                Xlog.e(TAG, "Failed to hook SMS handler", e);
                throw e;
            }
            Xlog.i(TAG, "NekoSMS initialization complete!");
        }
    }
}
