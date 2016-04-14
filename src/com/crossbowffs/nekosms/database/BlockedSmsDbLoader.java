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
    private static final int COL_ID = 0;
    private static final int COL_SENDER = 1;
    private static final int COL_BODY = 2;
    private static final int COL_TIME_SENT = 3;
    private static final int COL_TIME_RECEIVED = 4;
    private static final int COL_READ = 5;

    private BlockedSmsDbLoader() { }

    public static int[] getColumns(Cursor cursor) {
        int[] columns = new int[6];
        columns[COL_ID] = cursor.getColumnIndex(Blocked._ID);
        columns[COL_SENDER] = cursor.getColumnIndex(Blocked.SENDER);
        columns[COL_BODY] = cursor.getColumnIndex(Blocked.BODY);
        columns[COL_TIME_SENT] = cursor.getColumnIndex(Blocked.TIME_SENT);
        columns[COL_TIME_RECEIVED] = cursor.getColumnIndex(Blocked.TIME_RECEIVED);
        columns[COL_READ] = cursor.getColumnIndex(Blocked.READ);
        return columns;
    }

    public static SmsMessageData getMessageData(Cursor cursor, int[] columns, SmsMessageData data) {
        if (data == null)
            data = new SmsMessageData();
        if (columns[COL_ID] >= 0)
            data.setId(cursor.getLong(columns[COL_ID]));
        if (columns[COL_SENDER] >= 0)
            data.setSender(cursor.getString(columns[COL_SENDER]));
        if (columns[COL_BODY] >= 0)
            data.setBody(cursor.getString(columns[COL_BODY]));
        if (columns[COL_TIME_SENT] >= 0)
            data.setTimeSent(cursor.getLong(columns[COL_TIME_SENT]));
        if (columns[COL_TIME_RECEIVED] >= 0)
            data.setTimeReceived(cursor.getLong(columns[COL_TIME_RECEIVED]));
        if (columns[COL_READ] >= 0)
            data.setRead(cursor.getInt(columns[COL_READ]) != 0);
        return data;
    }

    public static CursorWrapper<SmsMessageData> loadAllMessages(Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        return new CursorWrapper<SmsMessageData>(cursor, getColumns(cursor)) {
            @Override
            protected SmsMessageData bindData(Cursor cursor, int[] columns, SmsMessageData data) {
                return getMessageData(cursor, columns, data);
            }
        };
    }

    public static CursorWrapper<SmsMessageData> loadAllMessages(Context context, boolean unreadOnly) {
        ContentResolver contentResolver = context.getContentResolver();
        String selection = null;
        String[] selectionArgs = null;
        if (unreadOnly) {
            selection = Blocked.READ + "=?";
            selectionArgs = new String[] {"0"};
        }
        Cursor cursor = contentResolver.query(
            Blocked.CONTENT_URI,
            Blocked.ALL,
            selection, selectionArgs,
            Blocked.TIME_SENT + " DESC");
        return loadAllMessages(cursor);
    }

    private static Uri convertIdToUri(long messageId) {
        return ContentUris.withAppendedId(Blocked.CONTENT_URI, messageId);
    }

    public static SmsMessageData loadMessage(Context context, long messageId) {
        return loadMessage(context, convertIdToUri(messageId));
    }

    public static SmsMessageData loadMessage(Context context, Uri messageUri) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(messageUri, Blocked.ALL, null, null, null);

        if (!cursor.moveToFirst()) {
            Xlog.e(TAG, "URI does not match any message: %s", messageUri);
            cursor.close();
            return null;
        }

        if (cursor.getCount() > 1) {
            Xlog.w(TAG, "URI matched more than one message: %s", messageUri);
        }

        SmsMessageData data = getMessageData(cursor, getColumns(cursor), null);
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

    public static void deleteAllMessages(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.delete(Blocked.CONTENT_URI, null, null);
    }

    public static boolean deleteMessage(Context context, long messageId) {
        return deleteMessage(context, convertIdToUri(messageId));
    }

    public static boolean deleteMessage(Context context, Uri messageUri) {
        ContentResolver contentResolver = context.getContentResolver();
        int deletedRows = contentResolver.delete(messageUri, null, null);
        if (deletedRows == 0) {
            Xlog.w(TAG, "URI does not match any message: %s", messageUri);
            return false;
        } else {
            return true;
        }
    }

    public static SmsMessageData loadAndDeleteMessage(Context context, long messageId) {
        return loadAndDeleteMessage(context, convertIdToUri(messageId));
    }

    public static SmsMessageData loadAndDeleteMessage(Context context, Uri messageUri) {
        SmsMessageData messageData = loadMessage(context, messageUri);
        deleteMessage(context, messageUri);
        return messageData;
    }

    public static boolean setReadStatus(Context context, long messageId, boolean read) {
        return setReadStatus(context, convertIdToUri(messageId), read);
    }

    public static boolean setReadStatus(Context context, Uri messageUri, boolean read) {
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues values = new ContentValues(1);
        values.put(Blocked.READ, read ? 1 : 0);
        return contentResolver.update(messageUri, values, null, null) >= 0;
    }
}
