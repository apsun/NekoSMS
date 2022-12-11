package com.crossbowffs.nekosms.loader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.widget.AutoContentLoader;
import com.crossbowffs.nekosms.widget.CursorWrapper;

import static com.crossbowffs.nekosms.provider.DatabaseContract.BlockedMessages;

public class BlockedSmsLoader extends AutoContentLoader<SmsMessageData> {
    private static BlockedSmsLoader sInstance;

    public static BlockedSmsLoader get() {
        if (sInstance == null) {
            sInstance = new BlockedSmsLoader();
        }
        return sInstance;
    }

    private BlockedSmsLoader() {
        super(BlockedMessages.CONTENT_URI, BlockedMessages.ALL);
    }

    @Override
    protected SmsMessageData newData() {
        return new SmsMessageData();
    }

    @Override
    protected void clearData(SmsMessageData data) {
        data.reset();
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
        case BlockedMessages.SUB_ID:
            data.setSubId(cursor.getInt(column));
            break;
        }
    }

    @Override
    protected ContentValues serialize(SmsMessageData data) {
        ContentValues values = new ContentValues();
        if (data.getId() >= 0) {
            values.put(BlockedMessages._ID, data.getId());
        }
        values.put(BlockedMessages.SENDER, data.getSender());
        values.put(BlockedMessages.BODY, data.getBody());
        values.put(BlockedMessages.TIME_SENT, data.getTimeSent());
        values.put(BlockedMessages.TIME_RECEIVED, data.getTimeReceived());
        values.put(BlockedMessages.READ, data.isRead() ? 1 : 0);
        values.put(BlockedMessages.SEEN, data.isSeen() ? 1 : 0);
        values.put(BlockedMessages.SUB_ID, data.getSubId());
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
        ContentValues values = new ContentValues();
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
        ContentValues values = new ContentValues();
        values.put(BlockedMessages.SEEN, seen ? 1 : 0);
        return update(context, messageUri, values);
    }

    public void markAllSeen(Context context) {
        ContentValues values = new ContentValues();
        values.put(BlockedMessages.SEEN, 1);
        updateAll(context, values, BlockedMessages.SEEN + "=?", new String[] {"0"});
    }
}
