package com.crossbowffs.nekosms.provider;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;

import static com.crossbowffs.nekosms.provider.DatabaseContract.*;

public class DatabaseProvider extends AutoContentProvider {
    public DatabaseProvider() {
        super(DatabaseContract.AUTHORITY, new ProviderTable[] {
            new ProviderTable(BlockedMessages.TABLE, BlockedMessages.CONTENT_ITEM_TYPE, BlockedMessages.CONTENT_TYPE),
            new ProviderTable(FilterRules.TABLE, FilterRules.CONTENT_ITEM_TYPE, FilterRules.CONTENT_TYPE)
        });
    }

    @Override
    protected SQLiteOpenHelper getDatabase(Context context) {
        return new DatabaseHelper(context);
    }
}
