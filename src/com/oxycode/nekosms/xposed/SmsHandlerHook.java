package com.oxycode.nekosms.xposed;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Build;
import android.os.UserHandle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import com.oxycode.nekosms.utils.ReflectionHelper;
import com.oxycode.nekosms.utils.Xlog;
import com.oxycode.nekosms.xposed.compat.CompatShim;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;

public class SmsHandlerHook implements IXposedHookLoadPackage {
    private static SmsMessage[] getMessagesFromIntent(Intent intent) {
        Object[] pdus = (Object[])intent.getExtras().get("pdus");
        SmsMessage[] messages = new SmsMessage[pdus.length];
        for (int i = 0; i < pdus.length; ++i) {
            byte[] pdu = (byte[])pdus[i];
            messages[i] = SmsMessage.createFromPdu(pdu);
        }
        return messages;
    }

    private static SmsMessage[] filterMessages(SmsMessage[] messages) {
        ArrayList<SmsMessage> messageList = new ArrayList<SmsMessage>(messages.length);
        for (int i = 0; i < messages.length; i++) {
            SmsMessage message = messages[i];
            Xlog.v("[%d] Sender: %s", i, message.getOriginatingAddress());
            Xlog.v("[%d] Message: %s", i, message.getMessageBody());
            if (shouldFilterMessage(message)) {
                Xlog.v("[%d] Result: Blocked");
            } else {
                Xlog.v("[%d] Result: Allowed");
                messageList.add(message);
            }
        }
        return messageList.toArray(new SmsMessage[messageList.size()]);
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

    private static void cleanUpTempDatabase(Object smsHandler, Object smsReceiver) {
        Xlog.i("Cleaning up temp database");
        ReflectionHelper.invoke(smsHandler, "deleteFromRawTable",
            String.class, ReflectionHelper.getFieldValue(smsReceiver, "mDeleteWhere"),
            String[].class, ReflectionHelper.getFieldValue(smsReceiver, "mDeleteWhereArgs"));
        ReflectionHelper.invoke(smsHandler, "sendMessage",
            int.class, 3);
    }

    private static void hookSmsHandler(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        String className = "com.android.internal.telephony.InboundSmsHandler";
        String methodName = "dispatchIntent";
        Class<?> param1Type = Intent.class;
        Class<?> param2Type = String.class;
        Class<?> param3Type = int.class;
        Class<?> param4Type = BroadcastReceiver.class;
        Class<?> param5Type = UserHandle.class;

        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, methodName,
            param1Type, param2Type, param3Type, param4Type, param5Type, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Intent intent = (Intent)param.args[0];
                    String action = intent.getAction();

                    if (!action.equals(Telephony.Sms.Intents.SMS_DELIVER_ACTION)) {
                        return;
                    }

                    SmsMessage[] messages = getMessagesFromIntent(intent);
                    int messageCount = messages.length;
                    Xlog.i("Got %d new SMS message(s)", messages.length);
                    SmsMessage[] filteredMessages = filterMessages(messages);
                    int remainingCount = filteredMessages.length;
                    int filteredCount = messageCount - remainingCount;
                    Xlog.i("Filtered %d/%d message(s)", filteredCount, messageCount);
                    if (filteredMessages.length == 0) {
                        Xlog.i("All messages filtered, skipping broadcast");
                        param.setResult(null);
                        cleanUpTempDatabase(param.thisObject, param.args[3]);
                    } else {
                        Xlog.i("%d message(s) unfiltered, continuing broadcast", remainingCount);
                        intent.putExtra("pdus", getPdusFromMessages(filteredMessages));
                    }
                }
            }
        );
    }

    private static void loadShims(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        CompatShim.loadShims(lpparam);
    }

    private static void printDeviceInfo() {
        Xlog.i("Phone manufacturer: " + Build.MANUFACTURER);
        Xlog.i("Phone model: " + Build.MODEL);
        Xlog.i("Android version: " + Build.VERSION.RELEASE);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.android.phone")) {
            return;
        }

        Xlog.i("NekoSMS initializing...");
        printDeviceInfo();
        loadShims(lpparam);
        hookSmsHandler(lpparam);
        Xlog.i("NekoSMS initialization complete!");
    }
}
