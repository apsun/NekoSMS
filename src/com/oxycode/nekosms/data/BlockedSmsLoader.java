package com.oxycode.nekosms.data;

import android.database.Cursor;
import com.oxycode.nekosms.provider.NekoSmsContract;

public final class BlockedSmsLoader {
    private static final String[] PROJECTION = {
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
    private static final int[] COLUMNS = {
        COL_ID,
        COL_SENDER,
        COL_BODY,
        COL_TIME_SENT,
        COL_TIME_RECEIVED,
    };

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
}
