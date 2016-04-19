package com.crossbowffs.nekosms.provider;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;

public class NekoSmsProvider extends AutoContentProvider {
    public NekoSmsProvider() {
        super(NekoSmsContract.class);
    }

    @Override
    protected SQLiteOpenHelper getDatabase(Context context) {
        return new NekoSmsDbHelper(context);
    }
}
