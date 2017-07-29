package com.crossbowffs.nekosms.widget;

import android.database.Cursor;

import java.io.Closeable;

public abstract class CursorWrapper<T> implements Closeable {
    private final Cursor mCursor;
    private final int[] mColumns;

    public CursorWrapper(Cursor cursor, int[] columns) {
        mCursor = cursor;
        mColumns = columns;
    }

    public int getCount() {
        return mCursor.getCount();
    }

    public boolean moveToNext() {
        return mCursor.moveToNext();
    }

    public T get(T data) {
        return bindData(mCursor, mColumns, data);
    }

    public T get() {
        return get(null);
    }

    @Override
    public void close() {
        mCursor.close();
    }

    protected abstract T bindData(Cursor cursor, int[] columns, T data);
}
