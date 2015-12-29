package com.crossbowffs.nekosms.database;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.Telephony;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.utils.Xlog;

public final class InboxSmsDbLoader {
    private static final String TAG = InboxSmsDbLoader.class.getSimpleName();

    private InboxSmsDbLoader() { }

    public static Uri writeMessage(Context context, SmsMessageData messageData) {
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues values = new ContentValues(6);
        values.put(Telephony.Sms.ADDRESS, messageData.getSender());
        values.put(Telephony.Sms.BODY, messageData.getBody());
        values.put(Telephony.Sms.DATE, messageData.getTimeReceived());
        values.put(Telephony.Sms.DATE_SENT, messageData.getTimeSent());
        values.put(Telephony.Sms.READ, 1);
        values.put(Telephony.Sms.SEEN, 1);

        Uri uri = contentResolver.insert(Telephony.Sms.CONTENT_URI, values);
        long id = ContentUris.parseId(uri);

        // An ID of 0 when writing to the SMS inbox means we don't have the
        // OP_WRITE_SMS permission. see ContentProvider#rejectInsert(Uri, ContentValues)
        if (id == 0) {
            Xlog.e(TAG, "Writing to SMS inbox failed (don't have OP_WRITE_SMS permission)");
            throw new DatabaseException("Failed to write message to SMS inbox (no OP_WRITE_SMS permission)");
        } else if (id < 0) {
            Xlog.e(TAG, "Writing to SMS inbox failed (unknown reason)");
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
            Xlog.e(TAG, "URI does not match any message in SMS inbox");
        }
    }
}
