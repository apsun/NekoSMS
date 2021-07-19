package com.crossbowffs.nekosms.xposed;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;

import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.consts.BroadcastConsts;
import com.crossbowffs.nekosms.consts.PreferenceConsts;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.filters.SmsFilterLoader;
import com.crossbowffs.nekosms.loader.BlockedSmsLoader;
import com.crossbowffs.nekosms.utils.*;
import com.crossbowffs.remotepreferences.RemotePreferenceAccessException;
import com.crossbowffs.remotepreferences.RemotePreferences;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.lang.reflect.Method;

public class SmsHandlerHook implements IXposedHookLoadPackage {
    private class ConstructorHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                afterConstructorHandler(param);
            } catch (Throwable e) {
                Xlog.e("Error occurred in constructor hook", e);
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
                Xlog.e("Error occurred in dispatchIntent() hook", e);
                throw e;
            }
        }
    }

    private static final String NEKOSMS_PACKAGE = BuildConfig.APPLICATION_ID;
    private static final String TELEPHONY_PACKAGE = "com.android.internal.telephony";
    private static final String SMS_HANDLER_CLASS = TELEPHONY_PACKAGE + ".InboundSmsHandler";
    private static final int MARK_DELETED = 2;
    private static final int EVENT_BROADCAST_COMPLETE = 3;

    private Context mContext;
    private SmsFilterLoader mFilterLoader;
    private RemotePreferences mPreferences;

    private static Object callDeclaredMethod(String clsName, Object obj, String methodName, Object... args) {
        // Unlike Xposed's built-in callMethod, this one searches
        // for private methods as well, in the specified class
        // (which must be assignable from the object).
        Class<?> cls = XposedHelpers.findClass(clsName, obj.getClass().getClassLoader());
        Method method = XposedHelpers.findMethodBestMatch(cls, methodName, args);
        return ReflectionUtils.invoke(method, obj, args);
    }

    private void grantWriteSmsPermissions(Context context) {
        // We need to grant OP_WRITE_SMS permissions to the app
        // (the non-Xposed part) so it can restore messages to the
        // SMS inbox. We can do this from the com.android.phone
        // process since it holds the UPDATE_APP_OPS_STATS
        // permission.
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(NEKOSMS_PACKAGE, 0);
        } catch (PackageManager.NameNotFoundException e) {
            // This might occur if the app has been uninstalled.
            // In this case, don't do anything - we can't do anything
            // with the permissions anyways.
            Xlog.e("App package not found, ignoring", e);
            return;
        }

        int uid = packageInfo.applicationInfo.uid;
        try {
            Xlog.i("Checking if we have OP_WRITE_SMS permission");
            if (AppOpsUtils.checkOp(context, AppOpsUtils.OP_WRITE_SMS, uid, NEKOSMS_PACKAGE)) {
                Xlog.i("Already have OP_WRITE_SMS permission");
            } else {
                Xlog.i("Giving our package OP_WRITE_SMS permission");
                AppOpsUtils.allowOp(context, AppOpsUtils.OP_WRITE_SMS, uid, NEKOSMS_PACKAGE);
            }
        } catch (Exception e) {
            // This isn't a fatal error - the user just won't
            // be able to restore messages to the inbox.
            Xlog.e("Failed to grant OP_WRITE_SMS permission", e);
        }
    }

    private void deleteFromRawTable19(Object smsHandler, Object smsReceiver) {
        Xlog.i("Removing raw SMS data from database for Android v19+");
        callDeclaredMethod(SMS_HANDLER_CLASS, smsHandler, "deleteFromRawTable",
            /*     deleteWhere */ XposedHelpers.getObjectField(smsReceiver, "mDeleteWhere"),
            /* deleteWhereArgs */ XposedHelpers.getObjectField(smsReceiver, "mDeleteWhereArgs"));
    }

    private void deleteFromRawTable24(Object smsHandler, Object smsReceiver) {
        Xlog.i("Removing raw SMS data from database for Android v24+");
        callDeclaredMethod(SMS_HANDLER_CLASS, smsHandler, "deleteFromRawTable",
            /*     deleteWhere */ XposedHelpers.getObjectField(smsReceiver, "mDeleteWhere"),
            /* deleteWhereArgs */ XposedHelpers.getObjectField(smsReceiver, "mDeleteWhereArgs"),
            /*      deleteType */ MARK_DELETED);
    }

    private void deleteFromRawTable(Object smsHandler, Object smsReceiver) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            deleteFromRawTable24(smsHandler, smsReceiver);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            deleteFromRawTable19(smsHandler, smsReceiver);
        }
    }

    private void sendBroadcastComplete(Object smsHandler) {
        Xlog.i("Notifying completion of SMS broadcast");
        XposedHelpers.callMethod(smsHandler, "sendMessage",
            /* what */ EVENT_BROADCAST_COMPLETE);
    }

    private void finishSmsBroadcast(Object smsHandler, Object smsReceiver) {
        // Need to clear calling identity since dispatchIntent() might be
        // called from CarrierSmsFilterCallback.onFilterComplete(), which is
        // executing an IPC. This is required to write to the SMS database.
        long token = Binder.clearCallingIdentity();
        try {
            deleteFromRawTable(smsHandler, smsReceiver);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
        sendBroadcastComplete(smsHandler);
    }

    private void broadcastBlockedSms(Uri messageUri) {
        // Permissions are not required here since we are only
        // broadcasting the URI of the message, not the message
        // contents. The provider requires permissions to read
        // the actual message contents.
        Intent intent = new Intent(BroadcastConsts.ACTION_RECEIVE_SMS);
        intent.setComponent(new ComponentName(NEKOSMS_PACKAGE, BroadcastConsts.RECEIVER_NAME));
        intent.putExtra(BroadcastConsts.EXTRA_MESSAGE, messageUri);
        mContext.sendBroadcast(intent);
    }

    private boolean getBooleanPref(String key, boolean defValue) {
        try {
            return mPreferences.getBoolean(key, defValue);
        } catch (RemotePreferenceAccessException e) {
            Xlog.e("Failed to read preference: %s", key, e);
            return defValue;
        }
    }

    private void afterConstructorHandler(XC_MethodHook.MethodHookParam param) {
        Context context = (Context)param.args[1];
        if (mContext == null) {
            mContext = context;
            mFilterLoader = new SmsFilterLoader(context);
            mPreferences = new RemotePreferences(context,
                PreferenceConsts.REMOTE_PREFS_AUTHORITY,
                PreferenceConsts.FILE_MAIN,
                true);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                grantWriteSmsPermissions(context);
            }
        }
    }

    private void putPhoneIdAndSubIdExtra(Object inboundSmsHandler, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                Object phone = XposedHelpers.getObjectField(inboundSmsHandler, "mPhone");
                int phoneId = (Integer)XposedHelpers.callMethod(phone, "getPhoneId");
                XposedHelpers.callStaticMethod(SubscriptionManager.class, "putPhoneIdAndSubIdExtra", intent, phoneId);
            } catch (Exception e) {
                Xlog.e("Could not update intent with subscription id", e);
            }
        }
    }

    private void beforeDispatchIntentHandler(XC_MethodHook.MethodHookParam param, int receiverIndex) {
        Intent intent = (Intent)param.args[0];
        String action = intent.getAction();

        // We only care about the initial SMS_DELIVER intent,
        // the rest are irrelevant
        if (!Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(action)) {
            return;
        }

        // Skip everything if the global killswitch is toggled
        if (!getBooleanPref(PreferenceConsts.KEY_ENABLE, PreferenceConsts.KEY_ENABLE_DEFAULT)) {
            Xlog.i("SMS blocking disabled, exiting");
            return;
        }

        // dispatchIntent is where the subscription id gets attached
        // to the intent, so we have to emulate that behavior ourselves
        // if we want to use the field.
        putPhoneIdAndSubIdExtra(param.thisObject, intent);

        SmsMessageData message = SmsMessageData.fromIntent(intent);
        String sender = message.getSender();
        String body = message.getBody();
        Xlog.i("Received a new SMS message");
        if (getBooleanPref(PreferenceConsts.KEY_VERBOSE_LOGGING, PreferenceConsts.KEY_VERBOSE_LOGGING_DEFAULT)) {
            Xlog.i("Sender: %s", StringUtils.escape(sender));
            Xlog.i("Body: %s", StringUtils.escape(body));
        } else {
            Xlog.v("Sender: %s", StringUtils.escape(sender));
            Xlog.v("Body: %s", StringUtils.escape(body));
        }

        // Skip if "whitelist contacts" is enabled and the message
        // is from a contact (this is done in the module so we don't
        // need contact permissions on the app itself).
        boolean allowContacts = getBooleanPref(
            PreferenceConsts.KEY_WHITELIST_CONTACTS,
            PreferenceConsts.KEY_WHITELIST_CONTACTS_DEFAULT);
        if (allowContacts && ContactUtils.isContact(mContext, sender)) {
            Xlog.i("Allowing message (contact whitelist)");
            return;
        }

        if (!mFilterLoader.shouldBlockMessage(sender, body)) {
            return;
        }

        // Order is important here! First, save a copy of the message to
        // the blocked message list and notify everyone about it. THEN,
        // we can delete the original. If it were the other way around,
        // any bug in our code would cause the message to disappear. This
        // way, the worst that can happen is that the user gets two copies.
        Uri messageUri = BlockedSmsLoader.get().insert(mContext, message);
        broadcastBlockedSms(messageUri);
        finishSmsBroadcast(param.thisObject, param.args[receiverIndex]);
        param.setResult(null);
    }

    private void hookConstructor19(XC_LoadPackage.LoadPackageParam lpparam) {
        Xlog.i("Hooking InboundSmsHandler constructor for Android v19+");
        XposedHelpers.findAndHookConstructor(SMS_HANDLER_CLASS, lpparam.classLoader,
            /*                 name */ String.class,
            /*              context */ Context.class,
            /*       storageMonitor */ TELEPHONY_PACKAGE + ".SmsStorageMonitor",
            /*                phone */ TELEPHONY_PACKAGE + ".PhoneBase",
            /* cellBroadcastHandler */ TELEPHONY_PACKAGE + ".CellBroadcastHandler",
            new ConstructorHook());
    }

    private void hookConstructor24(XC_LoadPackage.LoadPackageParam lpparam) {
        Xlog.i("Hooking InboundSmsHandler constructor for Android v24+");
        XposedHelpers.findAndHookConstructor(SMS_HANDLER_CLASS, lpparam.classLoader,
            /*                 name */ String.class,
            /*              context */ Context.class,
            /*       storageMonitor */ TELEPHONY_PACKAGE + ".SmsStorageMonitor",
            /*                phone */ TELEPHONY_PACKAGE + ".Phone",
            /* cellBroadcastHandler */ TELEPHONY_PACKAGE + ".CellBroadcastHandler",
            new ConstructorHook());
    }

    private void hookConstructor30(XC_LoadPackage.LoadPackageParam lpparam) {
        Xlog.i("Hooking InboundSmsHandler constructor for Android v30+");
        XposedHelpers.findAndHookConstructor(SMS_HANDLER_CLASS, lpparam.classLoader,
            /*                 name */ String.class,
            /*              context */ Context.class,
            /*       storageMonitor */ TELEPHONY_PACKAGE + ".SmsStorageMonitor",
            /*                phone */ TELEPHONY_PACKAGE + ".Phone",
            new ConstructorHook());
    }

    private void hookDispatchIntent19(XC_LoadPackage.LoadPackageParam lpparam) {
        Xlog.i("Hooking dispatchIntent() for Android v19+");
        XposedHelpers.findAndHookMethod(SMS_HANDLER_CLASS, lpparam.classLoader, "dispatchIntent",
            /*         intent */ Intent.class,
            /*     permission */ String.class,
            /*          appOp */ int.class,
            /* resultReceiver */ BroadcastReceiver.class,
            new DispatchIntentHook(3));
    }

    private void hookDispatchIntent21(XC_LoadPackage.LoadPackageParam lpparam) {
        Xlog.i("Hooking dispatchIntent() for Android v21+");
        XposedHelpers.findAndHookMethod(SMS_HANDLER_CLASS, lpparam.classLoader, "dispatchIntent",
            /*         intent */ Intent.class,
            /*     permission */ String.class,
            /*          appOp */ int.class,
            /* resultReceiver */ BroadcastReceiver.class,
            /*           user */ UserHandle.class,
            new DispatchIntentHook(3));
    }

    private void hookDispatchIntent23(XC_LoadPackage.LoadPackageParam lpparam) {
        Xlog.i("Hooking dispatchIntent() for Android v23+");
        XposedHelpers.findAndHookMethod(SMS_HANDLER_CLASS, lpparam.classLoader, "dispatchIntent",
            /*         intent */ Intent.class,
            /*     permission */ String.class,
            /*          appOp */ int.class,
            /*           opts */ Bundle.class,
            /* resultReceiver */ BroadcastReceiver.class,
            /*           user */ UserHandle.class,
            new DispatchIntentHook(4));
    }
    
    private void hookDispatchIntent29(XC_LoadPackage.LoadPackageParam lpparam) {
        Xlog.i("Hooking dispatchIntent() for Android v29+");
        XposedHelpers.findAndHookMethod(SMS_HANDLER_CLASS, lpparam.classLoader, "dispatchIntent",
            /*         intent */ Intent.class,
            /*     permission */ String.class,
            /*          appOp */ int.class,
            /*           opts */ Bundle.class,
            /* resultReceiver */ BroadcastReceiver.class,
            /*           user */ UserHandle.class,
            /*          subId */ int.class,
            new DispatchIntentHook(4));
    }

    private void hookDispatchIntent30(XC_LoadPackage.LoadPackageParam lpparam) {
        Xlog.i("Hooking dispatchIntent() for Android v30+");
        XposedHelpers.findAndHookMethod(SMS_HANDLER_CLASS, lpparam.classLoader, "dispatchIntent",
            /*         intent */ Intent.class,
            /*     permission */ String.class,
            /*          appOp */ String.class,
            /*           opts */ Bundle.class,
            /* resultReceiver */ BroadcastReceiver.class,
            /*           user */ UserHandle.class,
            /*          subId */ int.class,
            new DispatchIntentHook(4));
    }

    private void hookConstructor(XC_LoadPackage.LoadPackageParam lpparam) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hookConstructor30(lpparam);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            hookConstructor24(lpparam);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            hookConstructor19(lpparam);
        }
    }

    private void hookDispatchIntent(XC_LoadPackage.LoadPackageParam lpparam) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hookDispatchIntent30(lpparam);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                hookDispatchIntent29(lpparam);
            } catch (NoSuchMethodError e) {
                // Just in case. I have reason to suspect that the function signature
                // changed in a minor patch, so fall back to the old version if possible.
                // See commit 6939834098aaf8126da4832453ebf64a85a898a6.
                hookDispatchIntent23(lpparam);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hookDispatchIntent23(lpparam);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            hookDispatchIntent21(lpparam);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            hookDispatchIntent19(lpparam);
        }
    }

    private void hookSmsHandler(XC_LoadPackage.LoadPackageParam lpparam) {
        hookConstructor(lpparam);
        hookDispatchIntent(lpparam);
    }

    private static void printDeviceInfo() {
        Xlog.i("Phone manufacturer: %s", Build.MANUFACTURER);
        Xlog.i("Phone model: %s", Build.MODEL);
        Xlog.i("Android version: %s", Build.VERSION.RELEASE);
        Xlog.i("Xposed bridge version: %d", XposedBridge.XPOSED_BRIDGE_VERSION);
        Xlog.i("NekoSMS version: %s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if ("com.android.phone".equals(lpparam.packageName)) {
            Xlog.i("NekoSMS initializing...");
            printDeviceInfo();
            try {
                hookSmsHandler(lpparam);
            } catch (Throwable e) {
                Xlog.e("Failed to hook SMS handler", e);
                throw e;
            }
            Xlog.i("NekoSMS initialization complete!");
        }
    }
}
