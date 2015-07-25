package com.crossbowffs.nekosms.database;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.crossbowffs.nekosms.data.*;
import com.crossbowffs.nekosms.utils.Xlog;

import static com.crossbowffs.nekosms.provider.NekoSmsContract.Filters;

public final class SmsFilterDbLoader {
    public static final String TAG = SmsFilterDbLoader.class.getSimpleName();
    private static final String[] DEFAULT_PROJECTION = {
        Filters._ID,
        Filters.FIELD,
        Filters.MODE,
        Filters.PATTERN,
        Filters.CASE_SENSITIVE,
    };
    private static final int COL_ID = 0;
    private static final int COL_FIELD = 1;
    private static final int COL_MODE = 2;
    private static final int COL_PATTERN = 3;
    private static final int COL_CASE_SENSITIVE = 4;

    private static int[] sDefaultColumns;

    private SmsFilterDbLoader() { }

    public static int[] getColumns(Cursor cursor) {
        int[] columns = new int[5];
        columns[COL_ID] = cursor.getColumnIndexOrThrow(Filters._ID);
        columns[COL_FIELD] = cursor.getColumnIndexOrThrow(Filters.FIELD);
        columns[COL_MODE] = cursor.getColumnIndexOrThrow(Filters.MODE);
        columns[COL_PATTERN] = cursor.getColumnIndexOrThrow(Filters.PATTERN);
        columns[COL_CASE_SENSITIVE] = cursor.getColumnIndexOrThrow(Filters.CASE_SENSITIVE);
        return columns;
    }

    private static int[] getDefaultColumns(Cursor cursor) {
        if (sDefaultColumns != null) {
            return sDefaultColumns;
        }

        sDefaultColumns = getColumns(cursor);
        return sDefaultColumns;
    }

    public static SmsFilterData getFilterData(Cursor cursor, int[] columns, SmsFilterData data) {
        long id = cursor.getLong(columns[COL_ID]);
        String fieldString = cursor.getString(columns[COL_FIELD]);
        String modeString = cursor.getString(columns[COL_MODE]);
        String pattern = cursor.getString(columns[COL_PATTERN]);
        boolean caseSensitive = cursor.getInt(columns[COL_CASE_SENSITIVE]) != 0;

        SmsFilterField field = SmsFilterField.parse(fieldString);
        SmsFilterMode mode = SmsFilterMode.parse(modeString);

        if (data == null) {
            data = new SmsFilterData();
        }
        data.setId(id);
        data.setField(field);
        data.setMode(mode);
        data.setPattern(pattern);
        data.setCaseSensitive(caseSensitive);
        data.validate();
        return data;
    }

    public static void loadAllFilters(Context context, SmsFilterLoadCallback callback) {
        Uri filtersUri = Filters.CONTENT_URI;
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(filtersUri, DEFAULT_PROJECTION, null, null, null);
        int[] columns = getDefaultColumns(cursor);
        SmsFilterData data = new SmsFilterData();
        callback.onBegin(cursor.getCount());
        while (cursor.moveToNext()) {
            try {
                data = getFilterData(cursor, columns, data);
            } catch (InvalidFilterException e) {
                callback.onError(e);
            }
            callback.onSuccess(data);
        }
        cursor.close();
    }

    private static Uri convertIdToUri(long messageId) {
        return ContentUris.withAppendedId(Filters.CONTENT_URI, messageId);
    }

    public static SmsFilterData loadFilter(Context context, long filterId) {
        return loadFilter(context, convertIdToUri(filterId));
    }

    public static SmsFilterData loadFilter(Context context, Uri filterUri) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(filterUri, DEFAULT_PROJECTION, null, null, null);

        if (!cursor.moveToFirst()) {
            Xlog.e(TAG, "URI does not match any filter: %s", filterUri);
            return null;
        } else if (cursor.getCount() > 1) {
            Xlog.w(TAG, "URI matched more than one filter: %s", filterUri);
        }

        SmsFilterData data = getFilterData(cursor, getDefaultColumns(cursor), null);
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
