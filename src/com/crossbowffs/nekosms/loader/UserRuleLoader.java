package com.crossbowffs.nekosms.loader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.crossbowffs.nekosms.data.*;
import com.crossbowffs.nekosms.utils.Xlog;

import static com.crossbowffs.nekosms.provider.NekoSmsContract.UserRules;

public class UserRuleLoader extends SmsFilterLoader {
    private static final String TAG = UserRuleLoader.class.getSimpleName();
    private static UserRuleLoader sInstance;

    public static UserRuleLoader get() {
        if (sInstance == null) {
            sInstance = new UserRuleLoader();
        }
        return sInstance;
    }

    public UserRuleLoader() {
        super(UserRules.class);
    }

    @Override
    protected SmsFilterData newData() {
        return new SmsFilterData();
    }

    @Override
    protected void bindData(Cursor cursor, int column, String columnName, SmsFilterData data) {
        switch (columnName) {
        case UserRules._ID:
            data.setId(cursor.getLong(column));
            break;
        case UserRules.ACTION:
            data.setAction(cursor.getInt(column) == 0 ? SmsFilterAction.ALLOW : SmsFilterAction.BLOCK);
            break;
        case UserRules.SENDER_MODE:
            if (!cursor.isNull(column))
                ensureSenderPattern(data).setMode(SmsFilterMode.parse(cursor.getString(column)));
            break;
        case UserRules.SENDER_PATTERN:
            if (!cursor.isNull(column))
                ensureSenderPattern(data).setPattern(cursor.getString(column));
            break;
        case UserRules.SENDER_CASE_SENSITIVE:
            if (!cursor.isNull(column))
                ensureSenderPattern(data).setCaseSensitive(cursor.getInt(column) != 0);
            break;
        case UserRules.BODY_MODE:
            if (!cursor.isNull(column))
                ensureBodyPattern(data).setMode(SmsFilterMode.parse(cursor.getString(column)));
            break;
        case UserRules.BODY_PATTERN:
            if (!cursor.isNull(column))
                ensureBodyPattern(data).setPattern(cursor.getString(column));
            break;
        case UserRules.BODY_CASE_SENSITIVE:
            if (!cursor.isNull(column))
                ensureBodyPattern(data).setCaseSensitive(cursor.getInt(column) != 0);
            break;
        }
    }

    @Override
    protected ContentValues serialize(SmsFilterData data) {
        ContentValues values = new ContentValues(8);
        if (data.getId() >= 0) {
            values.put(UserRules._ID, data.getId());
        }
        values.put(UserRules.ACTION, data.getAction() == SmsFilterAction.BLOCK ? 1 : 0);
        SmsFilterPatternData senderPattern = data.getSenderPattern();
        if (senderPattern != null) {
            values.put(UserRules.SENDER_MODE, senderPattern.getMode().name());
            values.put(UserRules.SENDER_PATTERN, senderPattern.getPattern());
            values.put(UserRules.SENDER_CASE_SENSITIVE, senderPattern.isCaseSensitive() ? 1 : 0);
        }
        SmsFilterPatternData bodyPattern = data.getBodyPattern();
        if (bodyPattern != null) {
            values.put(UserRules.BODY_MODE, bodyPattern.getMode().name());
            values.put(UserRules.BODY_PATTERN, bodyPattern.getPattern());
            values.put(UserRules.BODY_CASE_SENSITIVE, bodyPattern.isCaseSensitive() ? 1 : 0);
        }
        return values;
    }

    @Override
    public CursorWrapper<SmsFilterData> queryAllWhitelistFirst(Context context) {
        return queryAll(context, null, null, UserRules.ACTION + " ASC");
    }

    public Uri update(Context context, Uri filterUri, SmsFilterData filterData, boolean insertIfError) {
        if (filterUri == null && insertIfError) {
            return insert(context, filterData);
        } else if (filterUri == null) {
            throw new IllegalArgumentException("No filter URI provided, failed to write new filter");
        }

        boolean updated = update(context, filterUri, serialize(filterData));
        if (!updated && insertIfError) {
            return insert(context, filterData);
        } else if (!updated) {
            Xlog.w(TAG, "Failed to update filter, possibly already exists");
            return null;
        } else {
            return filterUri;
        }
    }

    public SmsFilterData queryAndDelete(Context context, long messageId) {
        Uri filterUri = convertIdToUri(messageId);
        SmsFilterData filterData = query(context, filterUri);
        if (filterData != null) {
            delete(context, filterUri);
        }
        return filterData;
    }

    private SmsFilterPatternData ensureSenderPattern(SmsFilterData data) {
        SmsFilterPatternData pattern = data.getSenderPattern();
        if (pattern == null) {
            pattern = new SmsFilterPatternData();
            pattern.setField(SmsFilterField.SENDER);
            data.setSenderPattern(pattern);
        }
        return pattern;
    }

    private SmsFilterPatternData ensureBodyPattern(SmsFilterData data) {
        SmsFilterPatternData pattern = data.getBodyPattern();
        if (pattern == null) {
            pattern = new SmsFilterPatternData();
            pattern.setField(SmsFilterField.BODY);
            data.setBodyPattern(pattern);
        }
        return pattern;
    }
}
