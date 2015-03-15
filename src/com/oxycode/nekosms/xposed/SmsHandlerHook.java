package com.oxycode.nekosms.xposed;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Build;
import android.os.UserHandle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import com.oxycode.nekosms.utils.Xlog;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;

public class SmsHandlerHook implements IXposedHookLoadPackage {
    private static final String TAG = SmsHandlerHook.class.getSimpleName();

    private static SmsMessage[] getMessagesFromPdus(byte[][] pdus) {
        SmsMessage[] messages = new SmsMessage[pdus.length];
        for (int i = 0; i < pdus.length; ++i) {
            messages[i] = SmsMessage.createFromPdu(pdus[i]);
        }
        return messages;
    }

    private static byte[][] getPdusFromMessages(SmsMessage[] messages) {
        byte[][] pdus = new byte[messages.length][];
        for (int i = 0; i < messages.length; ++i) {
            pdus[i] = messages[i].getPdu();
        }
        return pdus;
    }

    private static boolean shouldFilterMessage(SmsMessage message) {
        // TODO: Replace with database check
        return message.getMessageBody().startsWith("123");
    }

    private static SmsMessage[] filterMessages(SmsMessage[] messages) {
        ArrayList<SmsMessage> messageList = new ArrayList<SmsMessage>(messages.length);
        for (int i = 0; i < messages.length; i++) {
            SmsMessage message = messages[i];
            Xlog.v(TAG, "[%d] Sender: %s", i, message.getOriginatingAddress());
            Xlog.v(TAG, "[%d] Message: %s", i, message.getMessageBody());
            if (shouldFilterMessage(message)) {
                Xlog.v(TAG, "[%d] Result: Blocked", i);
            } else {
                Xlog.v(TAG, "[%d] Result: Allowed", i);
                messageList.add(message);
            }
        }

        if (messages.length == messageList.size()) {
            return messages;
        } else {
            return messageList.toArray(new SmsMessage[messageList.size()]);
        }
    }

    private static void finishSmsBroadcast(Object smsHandler, Object smsReceiver) {
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

    private static void beforeSmsHandler(XC_MethodHook.MethodHookParam param) {
        Intent intent = (Intent)param.args[0];
        String action = intent.getAction();

        if (!Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(action)) {
            return;
        }

        byte[][] pdus = (byte[][])intent.getSerializableExtra("pdus");
        SmsMessage[] messages = getMessagesFromPdus(pdus);
        int messageCount = messages.length;
        Xlog.i(TAG, "Got %d new SMS message(s)", messages.length);

        SmsMessage[] filteredMessages = filterMessages(messages);
        int remainingCount = filteredMessages.length;
        int filteredCount = messageCount - remainingCount;
        Xlog.i(TAG, "Filtered %d/%d message(s)", filteredCount, messageCount);

        if (remainingCount == 0) {
            Xlog.d(TAG, "All messages filtered, skipping broadcast");
            param.setResult(null);
            finishSmsBroadcast(param.thisObject, param.args[3]);
        } else if (filteredCount != 0) {
            Xlog.d(TAG, "%d message(s) unfiltered, continuing broadcast", remainingCount);
            intent.putExtra("pdus", getPdusFromMessages(filteredMessages));
        } else {
            Xlog.d(TAG, "No messages filtered, continuing broadcast");
        }
    }

    private static void hookSmsHandler19(XC_LoadPackage.LoadPackageParam lpparam, XC_MethodHook hook) {
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

    private static void hookSmsHandler21(XC_LoadPackage.LoadPackageParam lpparam, XC_MethodHook hook) {
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

    private static void hookSmsHandler(XC_LoadPackage.LoadPackageParam lpparam) {
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
