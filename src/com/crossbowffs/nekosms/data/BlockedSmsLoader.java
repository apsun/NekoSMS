package com.crossbowffs.nekosms.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.crossbowffs.nekosms.provider.NekoSmsContract;

public final class BlockedSmsLoader {
    private static final String[] DEFAULT_PROJECTION = {
        NekoSmsContract.Blocked._ID,
        NekoSmsContract.Blocked.SENDER,
        NekoSmsContract.Blocked.BODY,
        NekoSmsContract.Blocked.TIME_SENT,
        NekoSmsContract.Blocked.TIME_RECEIVED,
    };
    private static final int COL_ID = 0;
    private static final int COL_SENDER = 1;
    private static final int COL_BODY = 2;
    private static final int COL_TIME_SENT = 3;
    private static final int COL_TIME_RECEIVED = 4;

    private static int[] sDefaultColumns;

    private BlockedSmsLoader() { }

    public static int[] getColumns(Cursor cursor) {
        int[] columns = new int[5];
        columns[COL_ID] = cursor.getColumnIndexOrThrow(NekoSmsContract.Blocked._ID);
        columns[COL_SENDER] = cursor.getColumnIndexOrThrow(NekoSmsContract.Blocked.SENDER);
        columns[COL_BODY] = cursor.getColumnIndexOrThrow(NekoSmsContract.Blocked.BODY);
        columns[COL_TIME_SENT] = cursor.getColumnIndexOrThrow(NekoSmsContract.Blocked.TIME_SENT);
        columns[COL_TIME_RECEIVED] = cursor.getColumnIndexOrThrow(NekoSmsContract.Blocked.TIME_RECEIVED);
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

    public static SmsMessageData loadMessage(Context context, Uri messageUri) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(messageUri, DEFAULT_PROJECTION, null, null, null);

        if (!cursor.moveToFirst()) {
            throw new IllegalArgumentException("URI does not match any message");
        }

        if (cursor.getCount() > 1) {
            throw new IllegalArgumentException("URI matched more than one message");
        }

        SmsMessageData message = getMessageData(cursor, getDefaultColumns(cursor), null);
        cursor.close();
        return message;
    }
}
