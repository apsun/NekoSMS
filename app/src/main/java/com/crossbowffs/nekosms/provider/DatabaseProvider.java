package com.crossbowffs.nekosms.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.crossbowffs.nekosms.widget.AutoContentProvider;

import static com.crossbowffs.nekosms.provider.DatabaseContract.BlockedMessages;
import static com.crossbowffs.nekosms.provider.DatabaseContract.FilterRules;

public class DatabaseProvider extends AutoContentProvider {
    public DatabaseProvider() {
        super(DatabaseContract.AUTHORITY, new ProviderTable[] {
            new ProviderTable(BlockedMessages.TABLE, BlockedMessages.CONTENT_ITEM_TYPE, BlockedMessages.CONTENT_TYPE),
            new ProviderTable(FilterRules.TABLE, FilterRules.CONTENT_ITEM_TYPE, FilterRules.CONTENT_TYPE)
        });
    }

    @Override
    protected SQLiteOpenHelper createDatabaseHelper(Context context) {
        return new DatabaseHelper(context);
    }

    /*
     * Below is an ugly workaround for Android 8.0+. Since the
     * com.android.phone package no longer has SMS permissions,
     * we can't use android:{read,write}Permissions in AndroidManifest.xml.
     * Instead, we just check the calling package via code.
     *
     * TODO: Migrate to new architecture and delete this hack
     */

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        checkAccess();
        return super.query(uri, projection, selection, selectionArgs, sortOrder);
    }

    @Override
    public String getType(@NonNull Uri uri) {
        checkAccess();
        return super.getType(uri);
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        checkAccess();
        return super.insert(uri, values);
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] bulkValues) {
        checkAccess();
        return super.bulkInsert(uri, bulkValues);
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        checkAccess();
        return super.delete(uri, selection, selectionArgs);
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        checkAccess();
        return super.update(uri, values, selection, selectionArgs);
    }

    private void checkAccess() {
        String caller = getCallingPackage();
        if (caller != null && !"com.android.phone".equals(caller) && !"com.crossbowffs.nekosms".equals(caller)) {
            throw new SecurityException("Cannot access this database, go away!");
        }
    }
}
