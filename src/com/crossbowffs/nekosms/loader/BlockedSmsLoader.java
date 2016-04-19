package com.crossbowffs.nekosms.loader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.provider.NekoSmsContract;

import static com.crossbowffs.nekosms.provider.NekoSmsContract.BlockedMessages;

public class BlockedSmsLoader extends AutoContentLoader<SmsMessageData> {
    private static BlockedSmsLoader sInstance;

    public static BlockedSmsLoader get() {
        if (sInstance == null) {
            sInstance = new BlockedSmsLoader();
        }
        return sInstance;
    }

    public BlockedSmsLoader() {
        super(BlockedMessages.class);
    }

    @Override
    protected SmsMessageData newData() {
        return new SmsMessageData();
    }

    @Override
    protected void bindData(Cursor cursor, int column, String columnName, SmsMessageData data) {
        switch (columnName) {
        case BlockedMessages._ID:
            data.setId(cursor.getLong(column));
            break;
        case BlockedMessages.SENDER:
            data.setSender(cursor.getString(column));
            break;
        case BlockedMessages.BODY:
            data.setBody(cursor.getString(column));
            break;
        case BlockedMessages.TIME_SENT:
            data.setTimeSent(cursor.getLong(column));
            break;
        case BlockedMessages.TIME_RECEIVED:
            data.setTimeReceived(cursor.getLong(column));
            break;
        case BlockedMessages.READ:
            data.setRead(cursor.getInt(column) != 0);
            break;
        case BlockedMessages.SEEN:
            data.setSeen(cursor.getInt(column) != 0);
            break;
        }
    }

    @Override
    protected ContentValues serialize(SmsMessageData data) {
        ContentValues values = new ContentValues(7);
        if (data.getId() >= 0) {
            values.put(NekoSmsContract.BlockedMessages._ID, data.getId());
        }
        values.put(NekoSmsContract.BlockedMessages.SENDER, data.getSender());
        values.put(NekoSmsContract.BlockedMessages.BODY, data.getBody());
        values.put(NekoSmsContract.BlockedMessages.TIME_SENT, data.getTimeSent());
        values.put(NekoSmsContract.BlockedMessages.TIME_RECEIVED, data.getTimeReceived());
        values.put(NekoSmsContract.BlockedMessages.READ, data.isRead());
        values.put(NekoSmsContract.BlockedMessages.SEEN, data.isSeen());
        return values;
    }

    public CursorWrapper<SmsMessageData> queryUnseen(Context context) {
        return queryAll(context, BlockedMessages.SEEN + "=?", new String[] {"0"}, BlockedMessages.TIME_SENT + " DESC");
    }

    public SmsMessageData queryAndDelete(Context context, long messageId) {
        return queryAndDelete(context, convertIdToUri(messageId));
    }

    public SmsMessageData queryAndDelete(Context context, Uri messageUri) {
        SmsMessageData messageData = query(context, messageUri);
        if (messageData != null) {
            delete(context, messageUri);
        }
        return messageData;
    }

    public boolean setReadStatus(Context context, long messageId, boolean read) {
        return setReadStatus(context, convertIdToUri(messageId), read);
    }

    public boolean setReadStatus(Context context, Uri messageUri, boolean read) {
        ContentValues values = new ContentValues(2);
        values.put(BlockedMessages.READ, read ? 1 : 0);
        if (read) {
            values.put(BlockedMessages.SEEN, 1);
        }
        return update(context, messageUri, values);
    }

    public boolean setSeenStatus(Context context, long messageId, boolean seen) {
        return setSeenStatus(context, convertIdToUri(messageId), seen);
    }

    public boolean setSeenStatus(Context context, Uri messageUri, boolean seen) {
        ContentValues values = new ContentValues(1);
        values.put(BlockedMessages.SEEN, seen ? 1 : 0);
        return update(context, messageUri, values);
    }

    public void markAllSeen(Context context) {
        ContentValues values = new ContentValues(1);
        values.put(BlockedMessages.SEEN, 1);
        updateAll(context, values, BlockedMessages.SEEN + "=?", new String[] {"0"});
    }
}
