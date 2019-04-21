package com.crossbowffs.nekosms.widget;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;

public abstract class AutoContentProvider extends ContentProvider {
    protected static class ProviderTable {
        private final String mTableName;
        private final String mItemType;
        private final String mDirType;

        public ProviderTable(String tableName, String itemType, String dirType) {
            mTableName = tableName;
            mItemType = itemType;
            mDirType = dirType;
        }
    }

    private final ProviderTable[] mTables;
    private final UriMatcher mUriMatcher;
    private SQLiteOpenHelper mDatabaseHelper;

    public AutoContentProvider(String authority, ProviderTable[] tables) {
        mTables = tables;
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        for (int i = 0; i < tables.length; ++i) {
            String table = tables[i].mTableName;
            mUriMatcher.addURI(authority, table, i * 2);
            mUriMatcher.addURI(authority, table + "/#", i * 2 + 1);
        }
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = createDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        int matchCode = mUriMatcher.match(uri);
        if (matchCode < 0) {
            throw new IllegalArgumentException("Invalid query URI: " + uri);
        }
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        if (isItemUri(matchCode)) {
            queryBuilder.appendWhere(BaseColumns._ID + "=" + uri.getLastPathSegment());
        }
        queryBuilder.setTables(getTableName(matchCode));
        SQLiteDatabase db = getDatabase(false);
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        int matchCode = mUriMatcher.match(uri);
        if (matchCode < 0) {
            throw new IllegalArgumentException("Invalid URI: " + uri);
        }
        return getType(matchCode);
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        int matchCode = mUriMatcher.match(uri);
        if (matchCode < 0 || isItemUri(matchCode)) {
            throw new IllegalArgumentException("Invalid insert URI: " + uri);
        }
        SQLiteDatabase db = getDatabase(true);
        long row = db.insert(getTableName(matchCode), null, values);
        Uri newUri = ContentUris.withAppendedId(uri, row);
        if (row >= 0) {
            getContext().getContentResolver().notifyChange(newUri, null);
        }
        return newUri;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] bulkValues) {
        int matchCode = mUriMatcher.match(uri);
        if (matchCode < 0 || isItemUri(matchCode)) {
            throw new IllegalArgumentException("Invalid insert URI: " + uri);
        }

        String tableName = getTableName(matchCode);
        int successCount = 0;
        ContentResolver contentResolver = getContext().getContentResolver();
        SQLiteDatabase db = getDatabase(true);
        for (ContentValues values : bulkValues) {
            long row = db.insert(tableName, null, values);
            if (row >= 0) {
                Uri newUri = ContentUris.withAppendedId(uri, row);
                contentResolver.notifyChange(newUri, null);
                successCount++;
            }
        }

        return successCount;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int matchCode = mUriMatcher.match(uri);
        if (matchCode < 0) {
            throw new IllegalArgumentException("Invalid delete URI: " + uri);
        }
        if (isItemUri(matchCode)) {
            selection = getCombinedSelectionString(BaseColumns._ID, uri, selection);
        }
        SQLiteDatabase db = getDatabase(true);
        int deletedRows = db.delete(getTableName(matchCode), selection, selectionArgs);
        if (selection == null || deletedRows > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return deletedRows;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int matchCode = mUriMatcher.match(uri);
        if (matchCode < 0) {
            throw new IllegalArgumentException("Invalid update URI: " + uri);
        }
        if (isItemUri(matchCode)) {
            selection = getCombinedSelectionString(BaseColumns._ID, uri, selection);
        }
        SQLiteDatabase db = getDatabase(true);
        int updatedRows = db.update(getTableName(matchCode), values, selection, selectionArgs);
        if (updatedRows > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return updatedRows;
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        // Since we opened the database as writable, each call to the
        // content provider operations will reuse the same database instance.
        // This is technically taking advantage of an implementation detail -
        // it may be cleaner to create an overloaded version of the operation
        // methods that take the database as a parameter.
        SQLiteDatabase db = getDatabase(true);
        ContentProviderResult[] results = new ContentProviderResult[operations.size()];
        db.beginTransaction();
        try {
            for (int i = 0; i < results.length; ++i) {
                results[i] = operations.get(i).apply(this, results, i);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return results;
    }

    private boolean isItemUri(int matchCode) {
        return matchCode % 2 == 1;
    }

    private String getTableName(int matchCode) {
        return mTables[matchCode / 2].mTableName;
    }

    private String getType(int matchCode) {
        ProviderTable table = mTables[matchCode / 2];
        if (isItemUri(matchCode)) {
            return table.mItemType;
        } else {
            return table.mDirType;
        }
    }

    protected abstract SQLiteOpenHelper createDatabaseHelper(Context context);

    protected SQLiteDatabase getDatabase(boolean write) {
        if (write) {
            return mDatabaseHelper.getWritableDatabase();
        } else {
            return mDatabaseHelper.getReadableDatabase();
        }
    }

    private static String getCombinedSelectionString(String idColumnName, Uri uri, String selection) {
        String profileWhere = idColumnName + "=" + uri.getLastPathSegment();
        if (TextUtils.isEmpty(selection)) {
            return profileWhere;
        } else {
            return profileWhere + " AND " + selection;
        }
    }
}
