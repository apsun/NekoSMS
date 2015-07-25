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

        // An ID of zero when writing to the SMS inbox denotes failure
        if (id <= 0) {
            Xlog.e(TAG, "Writing to SMS inbox failed, does app have OP_WRITE_SMS permission?");
            throw new IllegalArgumentException("Failed to write message to SMS inbox");
        } else {
            return uri;
        }
    }

    public static void deleteMessage(Context context, Uri messageUri) {
        ContentResolver contentResolver = context.getContentResolver();
        int deletedRows = contentResolver.delete(messageUri, null, null);
        if (deletedRows == 0) {
            Xlog.e(TAG, "Writing to SMS inbox failed, does app have OP_WRITE_SMS permission?");
            throw new IllegalArgumentException("URI does not match any message in SMS inbox");
        }
    }
}
