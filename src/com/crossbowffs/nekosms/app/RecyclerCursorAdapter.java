package com.crossbowffs.nekosms.app;

import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.v7.widget.RecyclerView;

/* package */ abstract class RecyclerCursorAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    private Cursor mCursor;
    private int[] mColumns;
    private int mIdColumn;

    public RecyclerCursorAdapter() {
        setHasStableIds(true);
    }

    @Override
    public int getItemCount() {
        if (mCursor == null) {
            return 0;
        } else {
            return mCursor.getCount();
        }
    }

    @Override
    public long getItemId(int position) {
        if (mCursor == null) {
            return -1;
        } else {
            mCursor.moveToPosition(position);
            return mCursor.getLong(mIdColumn);
        }
    }

    @Override
    public void onBindViewHolder(VH vh, int i) {
        mCursor.moveToPosition(i);
        if (mColumns == null) {
            mColumns = onBindColumns(mCursor);
        }
        onBindViewHolder(vh, mCursor);
    }

    protected int[] getColumns() {
        return mColumns;
    }

    protected abstract int[] onBindColumns(Cursor cursor);

    protected abstract void onBindViewHolder(VH vh, Cursor cursor);

    public void changeCursor(Cursor cursor) {
        Cursor oldCursor = swapCursor(cursor);
        if (oldCursor != null) {
            oldCursor.close();
        }
    }

    public Cursor swapCursor(Cursor cursor) {
        if (cursor == mCursor) {
            return null;
        }

        Cursor oldCursor = mCursor;
        mCursor = cursor;

        if (cursor != null) {
            mIdColumn = cursor.getColumnIndexOrThrow(BaseColumns._ID);
        } else {
            mIdColumn = -1;
        }

        notifyDataSetChanged();
        return oldCursor;
    }
}
