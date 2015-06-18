package com.crossbowffs.nekosms.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.crossbowffs.nekosms.provider.NekoSmsContract;
import com.crossbowffs.nekosms.utils.Xlog;

import java.util.ArrayList;
import java.util.List;

public final class SmsFilterLoader {
    public static final String TAG = SmsFilterLoader.class.getSimpleName();
    private static final String[] DEFAULT_PROJECTION = {
        NekoSmsContract.Filters._ID,
        NekoSmsContract.Filters.FIELD,
        NekoSmsContract.Filters.MODE,
        NekoSmsContract.Filters.PATTERN,
        NekoSmsContract.Filters.CASE_SENSITIVE,
    };
    private static final int COL_ID = 0;
    private static final int COL_FIELD = 1;
    private static final int COL_MODE = 2;
    private static final int COL_PATTERN = 3;
    private static final int COL_CASE_SENSITIVE = 4;

    private static int[] sDefaultColumns;

    private SmsFilterLoader() { }

    public static int[] getColumns(Cursor cursor) {
        int[] columns = new int[5];
        columns[COL_ID] = cursor.getColumnIndexOrThrow(NekoSmsContract.Filters._ID);
        columns[COL_FIELD] = cursor.getColumnIndexOrThrow(NekoSmsContract.Filters.FIELD);
        columns[COL_MODE] = cursor.getColumnIndexOrThrow(NekoSmsContract.Filters.MODE);
        columns[COL_PATTERN] = cursor.getColumnIndexOrThrow(NekoSmsContract.Filters.PATTERN);
        columns[COL_CASE_SENSITIVE] = cursor.getColumnIndexOrThrow(NekoSmsContract.Filters.CASE_SENSITIVE);
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
        String fieldStr = cursor.getString(columns[COL_FIELD]);
        String modeStr = cursor.getString(columns[COL_MODE]);
        String pattern = cursor.getString(columns[COL_PATTERN]);
        boolean caseSensitive = cursor.getInt(columns[COL_CASE_SENSITIVE]) != 0;

        SmsFilterField field = SmsFilterField.valueOf(fieldStr);
        SmsFilterMode mode = SmsFilterMode.valueOf(modeStr);

        if (data == null) {
            data = new SmsFilterData();
        }
        data.setId(id);
        data.setField(field);
        data.setMode(mode);
        data.setPattern(pattern);
        data.setCaseSensitive(caseSensitive);
        return data;
    }

    public static List<SmsFilterData> loadAllFilters(Context context, boolean ignoreErrors) {
        Uri filtersUri = NekoSmsContract.Filters.CONTENT_URI;
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(filtersUri, DEFAULT_PROJECTION, null, null, null);
        int[] columns = getDefaultColumns(cursor);
        List<SmsFilterData> filters = new ArrayList<SmsFilterData>(cursor.getCount());
        while (cursor.moveToNext()) {
            SmsFilterData filter;
            try {
                filter = getFilterData(cursor, columns, null);
            } catch (IllegalArgumentException e) {
                if (ignoreErrors) {
                    Xlog.e(TAG, "Failed to create SMS filter", e);
                    continue;
                } else {
                    throw e;
                }
            }
            filters.add(filter);
        }
        cursor.close();
        return filters;
    }

    public static SmsFilterData loadFilter(Context context, Uri filterUri) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(filterUri, DEFAULT_PROJECTION, null, null, null);

        if (!cursor.moveToFirst()) {
            throw new IllegalArgumentException("URI does not match any filter");
        }

        if (cursor.getCount() > 1) {
            throw new IllegalArgumentException("URI matched more than one filter");
        }

        SmsFilterData filter = getFilterData(cursor, getDefaultColumns(cursor), null);
        cursor.close();
        return filter;
    }
}
