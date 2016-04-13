package com.crossbowffs.nekosms.database;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.crossbowffs.nekosms.data.SmsFilterAction;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.data.SmsFilterField;
import com.crossbowffs.nekosms.data.SmsFilterMode;
import com.crossbowffs.nekosms.utils.Xlog;

import java.util.List;

import static com.crossbowffs.nekosms.provider.NekoSmsContract.Filters;

public final class SmsFilterDbLoader {
    private static final String TAG = SmsFilterDbLoader.class.getSimpleName();
    private static final int COL_ID = 0;
    private static final int COL_ACTION = 1;
    private static final int COL_FIELD = 2;
    private static final int COL_MODE = 3;
    private static final int COL_PATTERN = 4;
    private static final int COL_CASE_SENSITIVE = 5;

    private SmsFilterDbLoader() { }

    public static int[] getColumns(Cursor cursor) {
        int[] columns = new int[6];
        columns[COL_ID] = cursor.getColumnIndex(Filters._ID);
        columns[COL_ACTION] = cursor.getColumnIndex(Filters.ACTION);
        columns[COL_FIELD] = cursor.getColumnIndex(Filters.FIELD);
        columns[COL_MODE] = cursor.getColumnIndex(Filters.MODE);
        columns[COL_PATTERN] = cursor.getColumnIndex(Filters.PATTERN);
        columns[COL_CASE_SENSITIVE] = cursor.getColumnIndex(Filters.CASE_SENSITIVE);
        return columns;
    }

    public static SmsFilterData getFilterData(Cursor cursor, int[] columns, SmsFilterData data) {
        if (data == null)
            data = new SmsFilterData();
        if (columns[COL_ID] >= 0)
            data.setId(cursor.getLong(columns[COL_ID]));
        if (columns[COL_ACTION] >= 0)
            data.setAction(SmsFilterAction.parse(cursor.getString(columns[COL_ACTION])));
        if (columns[COL_FIELD] >= 0)
            data.setField(SmsFilterField.parse(cursor.getString(columns[COL_FIELD])));
        if (columns[COL_MODE] >= 0)
            data.setMode(SmsFilterMode.parse(cursor.getString(columns[COL_MODE])));
        if (columns[COL_PATTERN] >= 0)
            data.setPattern(cursor.getString(columns[COL_PATTERN]));
        if (columns[COL_CASE_SENSITIVE] >= 0)
            data.setCaseSensitive(cursor.getInt(columns[COL_CASE_SENSITIVE]) != 0);
        return data;
    }

    public static CursorWrapper<SmsFilterData> loadAllFilters(Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        return new CursorWrapper<SmsFilterData>(cursor, getColumns(cursor)) {
            @Override
            protected SmsFilterData bindData(Cursor cursor, int[] columns, SmsFilterData data) {
                return getFilterData(cursor, columns, data);
            }
        };
    }

    public static CursorWrapper<SmsFilterData> loadAllFilters(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(Filters.CONTENT_URI, Filters.ALL, null, null, null);
        return loadAllFilters(cursor);
    }

    public static void deleteAllFilters(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.delete(Filters.CONTENT_URI, null, null);
    }

    private static Uri convertIdToUri(long messageId) {
        return ContentUris.withAppendedId(Filters.CONTENT_URI, messageId);
    }

    public static SmsFilterData loadFilter(Context context, long filterId) {
        return loadFilter(context, convertIdToUri(filterId));
    }

    public static SmsFilterData loadFilter(Context context, Uri filterUri) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(filterUri, Filters.ALL, null, null, null);

        if (!cursor.moveToFirst()) {
            Xlog.e(TAG, "URI does not match any filter: %s", filterUri);
            cursor.close();
            return null;
        } else if (cursor.getCount() > 1) {
            Xlog.w(TAG, "URI matched more than one filter: %s", filterUri);
        }

        SmsFilterData data = getFilterData(cursor, getColumns(cursor), null);
        cursor.close();
        return data;
    }

    private static Uri writeFilter(ContentResolver contentResolver, ContentValues values) {
        Uri uri = contentResolver.insert(Filters.CONTENT_URI, values);
        long id = ContentUris.parseId(uri);
        if (id < 0) {
            Xlog.w(TAG, "Failed to write filter, possibly already exists");
            return null;
        } else {
            return uri;
        }
    }

    public static Uri writeFilter(Context context, SmsFilterData filterData) {
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues values = filterData.serialize();
        return writeFilter(contentResolver, values);
    }

    public static boolean writeFilters(Context context, List<SmsFilterData> filters) {
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues[] contentValues = new ContentValues[filters.size()];
        for (int i = 0; i < contentValues.length; i++) {
            contentValues[i] = filters.get(i).serialize();
        }
        int insertCount = contentResolver.bulkInsert(Filters.CONTENT_URI, contentValues);
        return insertCount == contentValues.length;
    }

    public static Uri updateFilter(Context context, Uri filterUri, SmsFilterData filterData, boolean insertIfError) {
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues values = filterData.serialize();

        if (filterUri == null && insertIfError) {
            return writeFilter(contentResolver, values);
        } else if (filterUri == null) {
            throw new IllegalArgumentException("No filter URI provided, failed to write new filter");
        }

        int updatedRows = contentResolver.update(filterUri, values, null, null);
        if (updatedRows == 0 && insertIfError) {
            return writeFilter(contentResolver, values);
        } else if (updatedRows == 0) {
            Xlog.w(TAG, "Failed to update filter, possibly already exists");
            return null;
        } else {
            return filterUri;
        }
    }

    public static void deleteFilter(Context context, long filterId) {
        deleteFilter(context, convertIdToUri(filterId));
    }

    public static void deleteFilter(Context context, Uri filterUri) {
        ContentResolver contentResolver = context.getContentResolver();
        int deletedRows = contentResolver.delete(filterUri, null, null);
        if (deletedRows == 0) {
            Xlog.w(TAG, "URI does not match any filter: %s", filterUri);
        }
    }

    public static SmsFilterData loadAndDeleteFilter(Context context, long messageId) {
        Uri filterUri = convertIdToUri(messageId);
        SmsFilterData filterData = loadFilter(context, filterUri);
        deleteFilter(context, filterUri);
        return filterData;
    }
}
