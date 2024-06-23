package com.crossbowffs.nekosms.loader;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.utils.Xlog;

public final class InboxSmsLoader {
    private InboxSmsLoader() { }

    private static ContentValues serializeMessage(SmsMessageData messageData) {
        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.ADDRESS, messageData.getSender());
        values.put(Telephony.Sms.BODY, messageData.getBody());
        values.put(Telephony.Sms.DATE, messageData.getTimeReceived());
        values.put(Telephony.Sms.DATE_SENT, messageData.getTimeSent());
        values.put(Telephony.Sms.READ, messageData.isRead() ? 1 : 0);
        values.put(Telephony.Sms.SEEN, 1); // Always mark messages as seen

        // Also write subscription ID (aka SIM card number) on Android 5.1+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            int subId = messageData.getSubId();
            if (subId != 0){
                values.put(Telephony.Sms.SUBSCRIPTION_ID, messageData.getSubId());
            }
        }

        return values;
    }

    public static Uri writeMessage(Context context, SmsMessageData messageData) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = contentResolver.insert(Telephony.Sms.CONTENT_URI, serializeMessage(messageData));
        long id = -1;
        if (uri != null) {
            id = ContentUris.parseId(uri);
        }

        // An ID of 0 when writing to the SMS inbox means we don't have the
        // OP_WRITE_SMS permission. See ContentProvider#rejectInsert(Uri, ContentValues).
        if (id <= 0) {
            Xlog.e("Writing to SMS inbox failed");
            throw new DatabaseException("Failed to write message to SMS inbox");
        } else {
            return uri;
        }
    }

    public static void deleteMessage(Context context, Uri messageUri) {
        ContentResolver contentResolver = context.getContentResolver();
        int deletedRows = contentResolver.delete(messageUri, null, null);
        if (deletedRows == 0) {
            // This can occur under two situations:
            // 1. The URI does not match any message in the SMS inbox (tried to delete a non-existent message)
            // 2. The app does not have permission to write to the SMS inbox
            // Note that this method is only called after writeMessage(), which means that
            // if we get here, we can rule out situation (2), and ignore the error.
            Xlog.e("URI does not match any message in SMS inbox");
        }
    }
}
