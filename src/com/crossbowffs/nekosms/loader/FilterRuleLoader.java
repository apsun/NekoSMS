package com.crossbowffs.nekosms.loader;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.data.SmsFilterMode;
import com.crossbowffs.nekosms.data.SmsFilterPatternData;
import com.crossbowffs.nekosms.provider.DatabaseContract;
import com.crossbowffs.nekosms.utils.Xlog;

import java.util.ArrayList;
import java.util.List;

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
    protected void clearData(SmsFilterData data) {
        data.reset();
    }

    @Override
    protected void bindData(Cursor cursor, int column, String columnName, SmsFilterData data) {
        switch (columnName) {
        case FilterRules._ID:
            data.setId(cursor.getLong(column));
            break;
        case FilterRules.SENDER_MODE:
            data.getSenderPattern().setMode(SmsFilterMode.parse(cursor.getString(column)));
            break;
        case FilterRules.SENDER_PATTERN:
            data.getSenderPattern().setPattern(cursor.getString(column));
            break;
        case FilterRules.SENDER_CASE_SENSITIVE:
            if (!cursor.isNull(column))
                data.getSenderPattern().setCaseSensitive(cursor.getInt(column) != 0);
            break;
        case FilterRules.BODY_MODE:
            data.getBodyPattern().setMode(SmsFilterMode.parse(cursor.getString(column)));
            break;
        case FilterRules.BODY_PATTERN:
            data.getBodyPattern().setPattern(cursor.getString(column));
            break;
        case FilterRules.BODY_CASE_SENSITIVE:
            if (!cursor.isNull(column))
                data.getBodyPattern().setCaseSensitive(cursor.getInt(column) != 0);
            break;
        }
    }

    @Override
    protected ContentValues serialize(SmsFilterData data) {
        ContentValues values = new ContentValues(7);
        if (data.getId() >= 0) {
            values.put(FilterRules._ID, data.getId());
        }
        SmsFilterPatternData senderPattern = data.getSenderPattern();
        if (senderPattern.hasData()) {
            values.put(FilterRules.SENDER_MODE, senderPattern.getMode().name());
            values.put(FilterRules.SENDER_PATTERN, senderPattern.getPattern());
            values.put(FilterRules.SENDER_CASE_SENSITIVE, senderPattern.isCaseSensitive() ? 1 : 0);
        } else {
            values.putNull(FilterRules.SENDER_MODE);
            values.putNull(FilterRules.SENDER_PATTERN);
            values.putNull(FilterRules.SENDER_CASE_SENSITIVE);
        }
        SmsFilterPatternData bodyPattern = data.getBodyPattern();
        if (bodyPattern.hasData()) {
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

    public boolean replaceAll(Context context, List<SmsFilterData> filters) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>(filters.size() + 1);
        ops.add(ContentProviderOperation.newDelete(FilterRules.CONTENT_URI).build());
        for (SmsFilterData filter : filters) {
            ContentValues values = serialize(filter);
            ops.add(ContentProviderOperation.newInsert(FilterRules.CONTENT_URI).withValues(values).build());
        }
        try {
            context.getContentResolver().applyBatch(DatabaseContract.AUTHORITY, ops);
            return true;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (OperationApplicationException e) {
            return false;
        }
    }
}
