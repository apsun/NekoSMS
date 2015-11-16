package com.crossbowffs.nekosms.xposed;

import android.app.AppOpsManager;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.data.InvalidFilterException;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.data.SmsFilterLoadCallback;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.database.SmsFilterDbLoader;
import com.crossbowffs.nekosms.filters.SmsFilter;
import com.crossbowffs.nekosms.provider.NekoSmsContract;
import com.crossbowffs.nekosms.utils.Xlog;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;

public class SmsHandlerHook implements IXposedHookLoadPackage {
    private static final String TAG = SmsHandlerHook.class.getSimpleName();

    private static final String NEKOSMS_PACKAGE = BuildConfig.APPLICATION_ID;

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
            StringBuilder sb = new StringBuilder();
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

    private Uri writeBlockedSms(Context context, SmsMessageData message) {
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues values = message.serialize();
        return contentResolver.insert(NekoSmsContract.Blocked.CONTENT_URI, values);
    }

    private static ArrayList<SmsFilter> loadSmsFilters(Context context) {
        final ArrayList<SmsFilter> filters = new ArrayList<>(0);
        SmsFilterDbLoader.loadAllFilters(context, new SmsFilterLoadCallback() {
            @Override
            public void onBegin(int count) {
                filters.ensureCapacity(count);
            }

            @Override
            public void onSuccess(SmsFilterData filterData) {
                SmsFilter filter = SmsFilter.create(filterData);
                filters.add(filter);
            }

            @Override
            public void onError(InvalidFilterException e) {
                Xlog.e(TAG, "Failed to load SMS filter", e);
            }
        });

        return filters;
    }

    private boolean shouldFilterMessage(Context context, String sender, String body) {
        ArrayList<SmsFilter> filters;

        synchronized (mFiltersLock) {
            if (mSmsFilters == null) {
                Xlog.d(TAG, "Cached SMS filters dirty, loading from database");
                mSmsFilters = loadSmsFilters(context);
            }

            filters = mSmsFilters;
        }

        for (SmsFilter filter : filters) {
            if (filter.matches(sender, body)) {
                return true;
            }
        }

        return false;
    }

    private void grantWriteSmsPermissions(Context context) {
        AppOpsManager appOps = (AppOpsManager)context.getSystemService(Context.APP_OPS_SERVICE);
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(NEKOSMS_PACKAGE, 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new AssertionError(e);
        }

        int uid = packageInfo.applicationInfo.uid;

        Xlog.i(TAG, "Checking if we have OP_WRITE_SMS permission");
        int mode = (Integer)XposedHelpers.callMethod(appOps, "checkOp",
            new Class<?>[] {int.class, int.class, String.class},
            15, uid, NEKOSMS_PACKAGE);

        if (mode == AppOpsManager.MODE_ALLOWED) {
            Xlog.i(TAG, "Already have OP_WRITE_SMS permission");
            return;
        }

        Xlog.i(TAG, "Giving our package OP_WRITE_SMS permission");
        XposedHelpers.callMethod(appOps, "setMode",
            new Class<?>[] {int.class, int.class, String.class, int.class},
            15, uid, NEKOSMS_PACKAGE, AppOpsManager.MODE_ALLOWED);
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

    private void beforeDispatchIntentHandler(XC_MethodHook.MethodHookParam param) {
        Intent intent = (Intent)param.args[0];
        String action = intent.getAction();

        if (!Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(action)) {
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
            writeBlockedSms(context, message);
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

        Xlog.i(TAG, "Hooking dispatchIntent() for Android v19+");

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

        Xlog.i(TAG, "Hooking dispatchIntent() for Android v21+");

        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, methodName,
            param1Type, param2Type, param3Type, param4Type, param5Type, hook);
    }

    private void hookDispatchIntent23(XC_LoadPackage.LoadPackageParam lpparam, XC_MethodHook hook) {
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
            param1Type, param2Type, param3Type, param4Type, param5Type, param6Type, hook);
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hookConstructor(lpparam, constructorHook);
            hookDispatchIntent23(lpparam, dispatchIntentHook);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            hookConstructor(lpparam, constructorHook);
            hookDispatchIntent21(lpparam, dispatchIntentHook);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            hookConstructor(lpparam, constructorHook);
            hookDispatchIntent19(lpparam, dispatchIntentHook);
        } else {
            throw new UnsupportedOperationException("NekoSMS is only supported on Android 4.4+");
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
