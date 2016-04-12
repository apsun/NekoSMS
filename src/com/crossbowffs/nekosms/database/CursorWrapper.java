package com.crossbowffs.nekosms.database;

import android.database.Cursor;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

public abstract class CursorWrapper<T> implements Closeable {
    private final Cursor mCursor;
    private final int[] mColumns;
    private final T mData;

    public CursorWrapper(Cursor cursor, int[] columns, T data) {
        mCursor = cursor;
        mColumns = columns;
        mData = data;
    }

    public int getCount() {
        return mCursor.getCount();
    }

    public boolean moveToNext() {
        return mCursor.moveToNext();
    }

    public T get() {
        bindData(mCursor, mColumns, mData);
        return mData;
    }

    @Override
    public void close() {
        mCursor.close();
    }

    public List<T> toList() {
        ArrayList<T> list = new ArrayList<>(getCount());
        while (moveToNext()) {
            T value = get();
            list.add(value);
        }
        return list;
    }

    protected abstract void bindData(Cursor cursor, int[] columns, T data);
}
