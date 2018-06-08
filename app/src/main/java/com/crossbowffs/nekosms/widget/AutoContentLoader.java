package com.crossbowffs.nekosms.widget;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public abstract class AutoContentLoader<T> {
    private final Uri mContentUri;
    private final String[] mColumnNames;

    public AutoContentLoader(Uri contentUri, String[] columnNames) {
        mContentUri = contentUri;
        mColumnNames = columnNames;
    }

    protected Uri getContentUri() {
        return mContentUri;
    }

    public int[] getColumns(Cursor cursor) {
        int[] columns = new int[mColumnNames.length];
        for (int i = 0; i < columns.length; ++i) {
            columns[i] = cursor.getColumnIndex(mColumnNames[i]);
        }
        return columns;
    }

    public T getData(Cursor cursor, int[] columns, T data) {
        if (data == null) {
            data = newData();
        } else {
            clearData(data);
        }
        for (int i = 0; i < columns.length; ++i) {
            if (columns[i] >= 0) {
                bindData(cursor, columns[i], mColumnNames[i], data);
            }
        }
        return data;
    }

    public CursorWrapper<T> wrapCursor(Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        return new CursorWrapper<T>(cursor, getColumns(cursor)) {
            @Override
            protected T bindData(Cursor cursor, int[] columns, T data) {
                return getData(cursor, columns, data);
            }
        };
    }

    public CursorWrapper<T> queryAll(Context context) {
        return queryAll(context, null, null, null);
    }

    protected CursorWrapper<T> queryAll(Context context, String where, String[] whereArgs, String orderBy) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(getContentUri(), mColumnNames, where, whereArgs, orderBy);
        return wrapCursor(cursor);
    }

    public T query(Context context, long id) {
        return query(context, convertIdToUri(id));
    }

    public T query(Context context, Uri uri) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(uri, mColumnNames, null, null, null);
        if (cursor == null) {
            return null;
        }
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return getData(cursor, getColumns(cursor), null);
        } finally {
            cursor.close();
        }
    }

    public Uri insert(Context context, T data) {
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues values = serialize(data);
        Uri uri = contentResolver.insert(getContentUri(), values);
        long id = -1;
        if (uri != null) {
            id = ContentUris.parseId(uri);
        }
        if (id < 0) {
            return null;
        } else {
            return uri;
        }
    }

    public void deleteAll(Context context) {
        deleteAll(context, null, null);
    }

    protected void deleteAll(Context context, String where, String[] whereArgs) {
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.delete(getContentUri(), where, whereArgs);
    }

    public boolean delete(Context context, long id) {
        return delete(context, convertIdToUri(id));
    }

    public boolean delete(Context context, Uri uri) {
        ContentResolver contentResolver = context.getContentResolver();
        int deletedRows = contentResolver.delete(uri, null, null);
        return deletedRows != 0;
    }

    protected void updateAll(Context context, ContentValues values) {
        updateAll(context, values, null, null);
    }

    protected void updateAll(Context context, ContentValues values, String where, String[] whereArgs) {
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.update(getContentUri(), values, where, whereArgs);
    }

    protected boolean update(Context context, long id, ContentValues values) {
        return update(context, convertIdToUri(id), values);
    }

    protected boolean update(Context context, Uri uri, ContentValues values) {
        ContentResolver contentResolver = context.getContentResolver();
        int updatedRows = contentResolver.update(uri, values, null, null);
        return updatedRows != 0;
    }

    protected Uri convertIdToUri(long id) {
        return ContentUris.withAppendedId(getContentUri(), id);
    }

    protected abstract T newData();

    protected abstract void clearData(T data);

    protected abstract void bindData(Cursor cursor, int column, String columnName, T data);

    protected abstract ContentValues serialize(T data);
}
