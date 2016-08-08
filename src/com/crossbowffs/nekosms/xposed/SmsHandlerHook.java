package com.crossbowffs.nekosms.xposed;

import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.app.BroadcastConsts;
import com.crossbowffs.nekosms.app.PreferenceConsts;
import com.crossbowffs.nekosms.data.SmsFilterAction;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.filters.SmsFilter;
import com.crossbowffs.nekosms.loader.BlockedSmsLoader;
import com.crossbowffs.nekosms.loader.CursorWrapper;
import com.crossbowffs.nekosms.loader.FilterRuleLoader;
import com.crossbowffs.nekosms.provider.DatabaseContract;
import com.crossbowffs.nekosms.utils.AppOpsUtils;
import com.crossbowffs.nekosms.utils.Xlog;
import com.crossbowffs.remotepreferences.RemotePreferenceAccessException;
import com.crossbowffs.remotepreferences.RemotePreferences;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
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

    private ContentObserver mContentObserver;
    private BroadcastReceiver mBroadcastReceiver;
    private RemotePreferences mPreferences;
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
        message.setRead(false);
        message.setSeen(false);
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

    private void resetFilters() {
        mSmsFilters = null;
    }

    private ContentObserver registerContentObserver(Context context) {
        Xlog.i(TAG, "Registering SMS filter content observer");

        ContentObserver contentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                Xlog.i(TAG, "SMS filter database updated, marking cache as dirty");
                resetFilters();
            }
        };

        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.registerContentObserver(DatabaseContract.FilterRules.CONTENT_URI, true, contentObserver);
        return contentObserver;
    }

    private BroadcastReceiver registerBroadcastReceiver(Context context) {
        // It is necessary to listen for these events because uninstalling
        // an app or clearing its data does not notify registered ContentObservers.
        // If the filter cache is not cleared, messages may be unintentionally blocked.
        // A user might be able to get around this by manually modifying the
        // database file itself, but at that point, it's not worth trying to handle.
        // The only other alternative would be to reload the entire filter list every
        // time a SMS is received, which does not scale well to a large number of filters.
        Xlog.i(TAG, "Registering NekoSMS package state receiver");

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (!Intent.ACTION_PACKAGE_REMOVED.equals(action) &&
                    !Intent.ACTION_PACKAGE_DATA_CLEARED.equals(action)) {
                    return;
                }

                Uri data = intent.getData();
                if (data == null) {
                    return;
                }

                String packageName = data.getSchemeSpecificPart();
                if (!NEKOSMS_PACKAGE.equals(packageName)) {
                    return;
                }

                if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                    Xlog.i(TAG, "NekoSMS uninstalled, resetting filters");
                    resetFilters();
                } else if (Intent.ACTION_PACKAGE_DATA_CLEARED.equals(action)) {
                    Xlog.i(TAG, "NekoSMS data cleared, resetting filters");
                    resetFilters();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
        filter.addDataScheme("package");
        context.registerReceiver(receiver, filter);
        return receiver;
    }

    private RemotePreferences createRemotePreferences(Context context) {
        Xlog.i(TAG, "Initializing remote preferences");

        return new RemotePreferences(context,
            PreferenceConsts.REMOTE_PREFS_AUTHORITY,
            PreferenceConsts.FILE_MAIN,
            true);
    }

    private void broadcastBlockedSms(Context context, Uri messageUri) {
        // Permissions are not required here since we are only
        // broadcasting the URI of the message, not the message
        // contents. The provider requires permissions to read
        // the actual message contents.
        Intent intent = new Intent(BroadcastConsts.ACTION_RECEIVE_SMS);
        intent.putExtra(BroadcastConsts.EXTRA_MESSAGE, messageUri);
        context.sendBroadcast(intent);
    }

    private static ArrayList<SmsFilter> loadSmsFilters(Context context) {
        ArrayList<SmsFilter> blacklist = new ArrayList<>();
        ArrayList<SmsFilter> whitelist = new ArrayList<>();
        try (CursorWrapper<SmsFilterData> filterCursor = FilterRuleLoader.get().queryAll(context)) {
            if (filterCursor == null) {
                // Can occur if NekoSMS has been uninstalled
                Xlog.e(TAG, "Failed to load SMS filters (queryAll returned null)");
                return null;
            }
            // Blacklist rules are expected to make up the majority
            // of rules, and we will end up adding all rules to the
            // whitelist list. Reserve the appropriate capacities.
            blacklist.ensureCapacity(filterCursor.getCount());
            whitelist.ensureCapacity(filterCursor.getCount());
            SmsFilterData data = new SmsFilterData();
            while (filterCursor.moveToNext()) {
                data = filterCursor.get(data);
                if (data.getAction() == SmsFilterAction.BLOCK) {
                    blacklist.add(new SmsFilter(data));
                } else {
                    whitelist.add(new SmsFilter(data));
                }
            }
        } catch (Exception e) {
            Xlog.e(TAG, "Failed to load SMS filters", e);
            return null;
        }
        // Combine whitelist and blacklist, with whitelist rules first
        whitelist.addAll(blacklist);
        return whitelist;
    }

    private boolean isMessageFromContact(Context context, String sender) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(sender));
        ContentResolver contentResolver = context.getContentResolver();
        try (Cursor cursor = contentResolver.query(uri, new String[0], null, null, null)) {
            return cursor != null && cursor.getCount() > 0;
        }
    }

    private boolean shouldFilterMessage(Context context, String sender, String body) {
        ArrayList<SmsFilter> filters = mSmsFilters;
        if (filters == null) {
            Xlog.i(TAG, "Cached SMS filters dirty, loading from database");
            mSmsFilters = filters = loadSmsFilters(context);
        }

        if (filters == null) {
            // This might occur if NekoSMS has been uninstalled (removing the DB),
            // but the user has not rebooted their device yet. We should not filter
            // any messages in this state.
            return false;
        }

        for (SmsFilter filter : filters) {
            if (filter.match(sender, body)) {
                return filter.getAction() == SmsFilterAction.BLOCK;
            }
        }
        return false;
    }

    private void grantWriteSmsPermissions(Context context) {
        // We need to grant OP_WRITE_SMS permissions to the
        // NekoSMS package so it can restore messages to the
        // SMS inbox. We can do this from the com.android.phone
        // process since it holds the UPDATE_APP_OPS_STATS
        // permission.
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(NEKOSMS_PACKAGE, 0);
        } catch (PackageManager.NameNotFoundException e) {
            // This might occur if NekoSMS has been uninstalled.
            // In this case, don't do anything - we can't do anything
            // with the permissions anyways.
            Xlog.w(TAG, "NekoSMS package not found", e);
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

    private void deleteFromRawTable19(Object smsHandler, Object smsReceiver) {
        Xlog.d(TAG, "Removing raw SMS data from database for Android v19+");
        XposedHelpers.callMethod(smsHandler, "deleteFromRawTable",
            new Class<?>[] {String.class, String[].class},
            XposedHelpers.getObjectField(smsReceiver, "mDeleteWhere"),
            XposedHelpers.getObjectField(smsReceiver, "mDeleteWhereArgs"));
    }

    private void deleteFromRawTable24(Object smsHandler, Object smsReceiver) {
        Xlog.d(TAG, "Removing raw SMS data from database for Android v24+");
        XposedHelpers.callMethod(smsHandler, "deleteFromRawTable",
            new Class<?>[] {String.class, String[].class, int.class},
            XposedHelpers.getObjectField(smsReceiver, "mDeleteWhere"),
            XposedHelpers.getObjectField(smsReceiver, "mDeleteWhereArgs"),
            2 /* MARK_DELETED */);
    }

    private void sendBroadcastComplete(Object smsHandler) {
        Xlog.d(TAG, "Notifying completion of SMS broadcast");
        XposedHelpers.callMethod(smsHandler, "sendMessage",
            new Class<?>[] {int.class},
            3 /* EVENT_BROADCAST_COMPLETE */);
    }

    private void finishSmsBroadcast(Object smsHandler, Object smsReceiver) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            deleteFromRawTable24(smsHandler, smsReceiver);
            sendBroadcastComplete(smsHandler);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            deleteFromRawTable19(smsHandler, smsReceiver);
            sendBroadcastComplete(smsHandler);
        } else {
            throw new UnsupportedOperationException("NekoSMS is only supported on Android 4.4+");
        }
    }

    private void afterConstructorHandler(XC_MethodHook.MethodHookParam param) {
        Context context = (Context)param.args[1];
        if (mContentObserver == null) {
            mContentObserver = registerContentObserver(context);
        }
        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = registerBroadcastReceiver(context);
        }
        if (mPreferences == null) {
            mPreferences = createRemotePreferences(context);
        }
        grantWriteSmsPermissions(context);
    }

    private void beforeDispatchIntentHandler(XC_MethodHook.MethodHookParam param, int receiverIndex) {
        Intent intent = (Intent)param.args[0];
        String action = intent.getAction();

        if (!Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(action)) {
            return;
        }

        boolean enable = PreferenceConsts.KEY_ENABLE_DEFAULT;
        try {
            enable = mPreferences.getBoolean(PreferenceConsts.KEY_ENABLE, enable);
        } catch (RemotePreferenceAccessException e) {
            Xlog.e(TAG, "Failed to read enable preference");
        }

        if (!enable) {
            Xlog.i(TAG, "SMS blocking disabled, exiting");
            return;
        }

        boolean allowContacts = PreferenceConsts.KEY_WHITELIST_CONTACTS_DEFAULT;
        try {
            allowContacts = mPreferences.getBoolean(PreferenceConsts.KEY_WHITELIST_CONTACTS, allowContacts);
        } catch (RemotePreferenceAccessException e) {
            Xlog.e(TAG, "Failed to read whitelist contacts preference");
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

        if (allowContacts && isMessageFromContact(context, sender)) {
            Xlog.i(TAG, "  Result: Allowed (contact whitelist)");
        } else if (shouldFilterMessage(context, sender, body)) {
            Xlog.i(TAG, "  Result: Blocked");
            Uri messageUri = BlockedSmsLoader.get().insert(context, message);
            broadcastBlockedSms(context, messageUri);
            param.setResult(null);
            finishSmsBroadcast(smsHandler, param.args[receiverIndex]);
        } else {
            Xlog.i(TAG, "  Result: Allowed");
        }
    }

    private void hookConstructor19(XC_LoadPackage.LoadPackageParam lpparam) {
        String className = "com.android.internal.telephony.InboundSmsHandler";
        Class<?> param1Type = String.class;
        Class<?> param2Type = Context.class;
        String param3Type = "com.android.internal.telephony.SmsStorageMonitor";
        String param4Type = "com.android.internal.telephony.PhoneBase";
        String param5Type = "com.android.internal.telephony.CellBroadcastHandler";

        Xlog.i(TAG, "Hooking InboundSmsHandler constructor for Android v19+");

        XposedHelpers.findAndHookConstructor(className, lpparam.classLoader,
            param1Type, param2Type, param3Type, param4Type, param5Type, new ConstructorHook());
    }

    private void hookConstructor24(XC_LoadPackage.LoadPackageParam lpparam) {
        String className = "com.android.internal.telephony.InboundSmsHandler";
        Class<?> param1Type = String.class;
        Class<?> param2Type = Context.class;
        String param3Type = "com.android.internal.telephony.SmsStorageMonitor";
        String param4Type = "com.android.internal.telephony.Phone";
        String param5Type = "com.android.internal.telephony.CellBroadcastHandler";

        Xlog.i(TAG, "Hooking InboundSmsHandler constructor for Android v24+");

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            hookConstructor24(lpparam);
            hookDispatchIntent23(lpparam);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hookConstructor19(lpparam);
            hookDispatchIntent23(lpparam);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            hookConstructor19(lpparam);
            hookDispatchIntent21(lpparam);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            hookConstructor19(lpparam);
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
