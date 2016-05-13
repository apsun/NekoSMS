package com.crossbowffs.nekosms.loader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.crossbowffs.nekosms.data.*;
import com.crossbowffs.nekosms.utils.Xlog;

import static com.crossbowffs.nekosms.provider.DatabaseContract.FilterRules;

public class FilterRuleLoader extends AutoContentLoader<SmsFilterData> {
    private static final String TAG = FilterRuleLoader.class.getSimpleName();
    private static FilterRuleLoader sInstance;

    public static FilterRuleLoader get() {
        if (sInstance == null) {
            sInstance = new FilterRuleLoader();
        }
        return sInstance;
    }

    private FilterRuleLoader() {
        super(FilterRules.CONTENT_URI, FilterRules.ALL);
    }

    @Override
    protected SmsFilterData newData() {
        return new SmsFilterData();
    }

    @Override
    protected void bindData(Cursor cursor, int column, String columnName, SmsFilterData data) {
        switch (columnName) {
        case FilterRules._ID:
            data.setId(cursor.getLong(column));
            break;
        case FilterRules.ACTION:
            data.setAction(cursor.getInt(column) == 0 ? SmsFilterAction.ALLOW : SmsFilterAction.BLOCK);
            break;
        case FilterRules.SENDER_MODE:
            if (!cursor.isNull(column))
                ensureSenderPattern(data).setMode(SmsFilterMode.parse(cursor.getString(column)));
            break;
        case FilterRules.SENDER_PATTERN:
            if (!cursor.isNull(column))
                ensureSenderPattern(data).setPattern(cursor.getString(column));
            break;
        case FilterRules.SENDER_CASE_SENSITIVE:
            if (!cursor.isNull(column))
                ensureSenderPattern(data).setCaseSensitive(cursor.getInt(column) != 0);
            break;
        case FilterRules.BODY_MODE:
            if (!cursor.isNull(column))
                ensureBodyPattern(data).setMode(SmsFilterMode.parse(cursor.getString(column)));
            break;
        case FilterRules.BODY_PATTERN:
            if (!cursor.isNull(column))
                ensureBodyPattern(data).setPattern(cursor.getString(column));
            break;
        case FilterRules.BODY_CASE_SENSITIVE:
            if (!cursor.isNull(column))
                ensureBodyPattern(data).setCaseSensitive(cursor.getInt(column) != 0);
            break;
        }
    }

    @Override
    protected ContentValues serialize(SmsFilterData data) {
        ContentValues values = new ContentValues(8);
        if (data.getId() >= 0) {
            values.put(FilterRules._ID, data.getId());
        }
        values.put(FilterRules.ACTION, data.getAction() == SmsFilterAction.BLOCK ? 1 : 0);
        SmsFilterPatternData senderPattern = data.getSenderPattern();
        if (senderPattern != null) {
            values.put(FilterRules.SENDER_MODE, senderPattern.getMode().name());
            values.put(FilterRules.SENDER_PATTERN, senderPattern.getPattern());
            values.put(FilterRules.SENDER_CASE_SENSITIVE, senderPattern.isCaseSensitive() ? 1 : 0);
        } else {
            values.putNull(FilterRules.SENDER_MODE);
            values.putNull(FilterRules.SENDER_PATTERN);
            values.putNull(FilterRules.SENDER_CASE_SENSITIVE);
        }
        SmsFilterPatternData bodyPattern = data.getBodyPattern();
        if (bodyPattern != null) {
            values.put(FilterRules.BODY_MODE, bodyPattern.getMode().name());
            values.put(FilterRules.BODY_PATTERN, bodyPattern.getPattern());
            values.put(FilterRules.BODY_CASE_SENSITIVE, bodyPattern.isCaseSensitive() ? 1 : 0);
        } else {
            values.putNull(FilterRules.BODY_MODE);
            values.putNull(FilterRules.BODY_PATTERN);
            values.putNull(FilterRules.BODY_CASE_SENSITIVE);
        }
        return values;
    }

    public CursorWrapper<SmsFilterData> queryAllWhitelistFirst(Context context) {
        return queryAll(context, null, null, FilterRules.ACTION + " ASC");
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
