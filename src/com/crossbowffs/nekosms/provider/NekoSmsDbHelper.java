package com.crossbowffs.nekosms.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.crossbowffs.nekosms.utils.Xlog;

import static com.crossbowffs.nekosms.provider.NekoSmsContract.Filters;
import static com.crossbowffs.nekosms.provider.NekoSmsContract.Blocked;

public class NekoSmsDbHelper extends SQLiteOpenHelper {
    private static final String TAG = NekoSmsDbHelper.class.getSimpleName();

    private static final String DATABASE_NAME = "nekosms.db";
    private static final int DATABASE_VERSION = 8;

    private static final String CREATE_FILTERS_TABLE =
        "CREATE TABLE " + Filters.TABLE + "(" +
            Filters._ID                + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            Filters.FIELD              + " TEXT NOT NULL," +
            Filters.MODE               + " TEXT NOT NULL," +
            Filters.PATTERN            + " TEXT NOT NULL," +
            Filters.CASE_SENSITIVE     + " INTEGER NOT NULL," +
            "UNIQUE(" +
                Filters.FIELD + "," + Filters.MODE + "," +
                Filters.PATTERN + "," + Filters.CASE_SENSITIVE +
            ") ON CONFLICT IGNORE" +
        ");";

    private static final String CREATE_BLOCKED_TABLE =
        "CREATE TABLE " + Blocked.TABLE + "(" +
            Blocked._ID                + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            Blocked.SENDER             + " TEXT," +
            Blocked.BODY               + " TEXT," +
            Blocked.TIME_SENT          + " INTEGER," +
            Blocked.TIME_RECEIVED      + " INTEGER" +
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
        db.execSQL("DROP TABLE IF EXISTS " + Filters.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + Blocked.TABLE);
        onCreate(db);
    }
}
