package com.oxycode.nekosms.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.oxycode.nekosms.utils.Xlog;

public class NekoSmsDbHelper extends SQLiteOpenHelper {
    private static final String TAG = NekoSmsDbHelper.class.getSimpleName();

    private static final String DATABASE_NAME = "nekosms.db";
    private static final int DATABASE_VERSION = 4;

    private static final String CREATE_FILTERS_TABLE =
        "CREATE TABLE " + NekoSmsContract.Filters.TABLE + "(" +
            NekoSmsContract.Filters._ID                + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            NekoSmsContract.Filters.FIELD              + " TEXT NOT NULL," +
            NekoSmsContract.Filters.MODE               + " TEXT NOT NULL," +
            NekoSmsContract.Filters.PATTERN            + " TEXT NOT NULL" +
        ");";

    private static final String CREATE_BLOCKED_TABLE =
        "CREATE TABLE " + NekoSmsContract.Blocked.TABLE + "(" +
            NekoSmsContract.Blocked._ID                + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            NekoSmsContract.Blocked.ADDRESS            + " TEXT," +
            NekoSmsContract.Blocked.BODY               + " TEXT," +
            NekoSmsContract.Blocked.DATE_SENT          + " INTEGER," +
            NekoSmsContract.Blocked.DATE               + " INTEGER," +
            NekoSmsContract.Blocked.PROTOCOL           + " TEXT," +
            NekoSmsContract.Blocked.SEEN               + " INTEGER," +
            NekoSmsContract.Blocked.READ               + " INTEGER," +
            NekoSmsContract.Blocked.SUBJECT            + " TEXT," +
            NekoSmsContract.Blocked.REPLY_PATH_PRESENT + " INTEGER," +
            NekoSmsContract.Blocked.SERVICE_CENTER     + " TEXT" +
        ");";

    public NekoSmsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_FILTERS_TABLE);
        db.execSQL(CREATE_BLOCKED_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Xlog.i(TAG, "Upgrading database from v%d to v%d", oldVersion, newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + NekoSmsContract.Filters.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + NekoSmsContract.Blocked.TABLE);
        onCreate(db);
    }
}
