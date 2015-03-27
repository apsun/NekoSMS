package com.oxycode.nekosms.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.oxycode.nekosms.provider.NekoSmsContract;
import com.oxycode.nekosms.utils.Xlog;

import java.util.ArrayList;
import java.util.List;

public final class SmsFilterLoader {
    public static final String TAG = SmsFilterLoader.class.getSimpleName();
    private static final String[] PROJECTION = {
        NekoSmsContract.Filters.FIELD,
        NekoSmsContract.Filters.MODE,
        NekoSmsContract.Filters.PATTERN,
        NekoSmsContract.Filters.FLAGS,
    };
    private static final int[] COLUMNS = {
        0, // field
        1, // mode
        2, // pattern
        3, // flags
    };

    private SmsFilterLoader() { }

    public static int[] getColumns(Cursor cursor) {
        int[] columns = new int[4];
        columns[0] = cursor.getColumnIndexOrThrow(NekoSmsContract.Filters.FIELD);
        columns[1] = cursor.getColumnIndexOrThrow(NekoSmsContract.Filters.MODE);
        columns[2] = cursor.getColumnIndexOrThrow(NekoSmsContract.Filters.PATTERN);
        columns[3] = cursor.getColumnIndexOrThrow(NekoSmsContract.Filters.FLAGS);
        return columns;
    }

    public static SmsFilterData getFilterData(Cursor cursor, int[] columns) {
        String fieldStr = cursor.getString(columns[0]);
        String modeStr = cursor.getString(columns[1]);
        String pattern = cursor.getString(columns[2]);
        int flags = cursor.getInt(columns[3]);

        SmsFilterField field = SmsFilterField.valueOf(fieldStr);
        SmsFilterMode mode = SmsFilterMode.valueOf(modeStr);

        SmsFilterData data = new SmsFilterData();
        data.setField(field);
        data.setMode(mode);
        data.setPattern(pattern);
        data.setFlags(flags);
        return data;
    }

    public static List<SmsFilterData> loadAllFilters(Context context, boolean ignoreErrors) {
        Uri filtersUri = NekoSmsContract.Filters.CONTENT_URI;
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(filtersUri, PROJECTION, null, null, null);
        List<SmsFilterData> filters = new ArrayList<SmsFilterData>(cursor.getCount());
        while (cursor.moveToNext()) {
            SmsFilterData filter;
            try {
                filter = getFilterData(cursor, COLUMNS);
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
        Cursor cursor = contentResolver.query(filterUri, PROJECTION, null, null, null);

        if (!cursor.moveToFirst()) {
            throw new IllegalArgumentException("URI does not match any filter");
        }

        if (cursor.getCount() > 1) {
            throw new IllegalArgumentException("URI matched more than one filter");
        }

        SmsFilterData filter = getFilterData(cursor, COLUMNS);
        cursor.close();
        return filter;
    }
}
