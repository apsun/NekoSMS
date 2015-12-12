package com.crossbowffs.nekosms.database;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.utils.Xlog;

import static com.crossbowffs.nekosms.provider.NekoSmsContract.Blocked;

public final class BlockedSmsDbLoader {
    private static final String TAG = BlockedSmsDbLoader.class.getSimpleName();
    private static final String[] DEFAULT_PROJECTION = {
        Blocked._ID,
        Blocked.SENDER,
        Blocked.BODY,
        Blocked.TIME_SENT,
        Blocked.TIME_RECEIVED,
    };
    private static final int COL_ID = 0;
    private static final int COL_SENDER = 1;
    private static final int COL_BODY = 2;
    private static final int COL_TIME_SENT = 3;
    private static final int COL_TIME_RECEIVED = 4;

    private static int[] sDefaultColumns;

    private BlockedSmsDbLoader() { }

    public static int[] getColumns(Cursor cursor) {
        int[] columns = new int[5];
        columns[COL_ID] = cursor.getColumnIndexOrThrow(Blocked._ID);
        columns[COL_SENDER] = cursor.getColumnIndexOrThrow(Blocked.SENDER);
        columns[COL_BODY] = cursor.getColumnIndexOrThrow(Blocked.BODY);
        columns[COL_TIME_SENT] = cursor.getColumnIndexOrThrow(Blocked.TIME_SENT);
        columns[COL_TIME_RECEIVED] = cursor.getColumnIndexOrThrow(Blocked.TIME_RECEIVED);
        return columns;
    }

    private static int[] getDefaultColumns(Cursor cursor) {
        if (sDefaultColumns != null) {
            return sDefaultColumns;
        }

        sDefaultColumns = getColumns(cursor);
        return sDefaultColumns;
    }

    public static SmsMessageData getMessageData(Cursor cursor, int[] columns, SmsMessageData data) {
        long id = cursor.getLong(columns[COL_ID]);
        String sender = cursor.getString(columns[COL_SENDER]);
        String body = cursor.getString(columns[COL_BODY]);
        long timeSent = cursor.getLong(columns[COL_TIME_SENT]);
        long timeReceived = cursor.getLong(columns[COL_TIME_RECEIVED]);

        if (data == null) {
            data = new SmsMessageData();
        }
        data.setId(id);
        data.setSender(sender);
        data.setBody(body);
        data.setTimeSent(timeSent);
        data.setTimeReceived(timeReceived);
        return data;
    }

    private static Uri convertIdToUri(long messageId) {
        return ContentUris.withAppendedId(Blocked.CONTENT_URI, messageId);
    }

    public static SmsMessageData loadMessage(Context context, long messageId) {
        return loadMessage(context, convertIdToUri(messageId));
    }

    public static SmsMessageData loadMessage(Context context, Uri messageUri) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(messageUri, DEFAULT_PROJECTION, null, null, null);

        if (!cursor.moveToFirst()) {
            Xlog.e(TAG, "URI does not match any message: %s", messageUri);
            return null;
        }

        if (cursor.getCount() > 1) {
            Xlog.w(TAG, "URI matched more than one message: %s", messageUri);
        }

        SmsMessageData data = getMessageData(cursor, getDefaultColumns(cursor), null);
        cursor.close();
        return data;
    }

    public static Uri writeMessage(Context context, SmsMessageData messageData) {
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues values = messageData.serialize();
        Uri uri = contentResolver.insert(Blocked.CONTENT_URI, values);
        long id = ContentUris.parseId(uri);
        if (id < 0) {
            throw new DatabaseException("Failed to write message to blocked SMS database");
        } else {
            return uri;
        }
    }

    public static void deleteMessage(Context context, long messageId) {
        deleteMessage(context, convertIdToUri(messageId));
    }

    public static void deleteMessage(Context context, Uri messageUri) {
        ContentResolver contentResolver = context.getContentResolver();
        int deletedRows = contentResolver.delete(messageUri, null, null);
        if (deletedRows == 0) {
            Xlog.w(TAG, "URI does not match any message: %s", messageUri);
        }
    }

    public static SmsMessageData loadAndDeleteMessage(Context context, long messageId) {
        Uri messageUri = convertIdToUri(messageId);
        SmsMessageData messageData = loadMessage(context, messageUri);
        deleteMessage(context, messageUri);
        return messageData;
    }
}
