package com.crossbowffs.nekosms.provider;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.database.BlockedSmsDbLoader;
import com.crossbowffs.nekosms.database.SmsFilterDbLoader;
import com.crossbowffs.nekosms.utils.Xlog;

import java.util.List;

import static com.crossbowffs.nekosms.provider.NekoSmsContract.Blocked;
import static com.crossbowffs.nekosms.provider.NekoSmsContract.Filters;

/* package */ class NekoSmsDbHelper extends SQLiteOpenHelper {
    private static final String TAG = NekoSmsDbHelper.class.getSimpleName();
    private static final String DATABASE_NAME = "nekosms.db";
    private static final int DATABASE_VERSION = BuildConfig.DATABASE_VERSION;

    private static final String CREATE_FILTERS_TABLE =
        "CREATE TABLE " + Filters.TABLE + "(" +
            Filters._ID                + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            Filters.ACTION             + " TEXT NOT NULL," +
            Filters.FIELD              + " TEXT NOT NULL," +
            Filters.MODE               + " TEXT NOT NULL," +
            Filters.PATTERN            + " TEXT NOT NULL," +
            Filters.CASE_SENSITIVE     + " INTEGER NOT NULL" +
        ");";

    private static final String CREATE_BLOCKED_TABLE =
        "CREATE TABLE " + Blocked.TABLE + "(" +
            Blocked._ID                + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            Blocked.SENDER             + " TEXT NOT NULL," +
            Blocked.BODY               + " TEXT NOT NULL," +
            Blocked.TIME_SENT          + " INTEGER NOT NULL," +
            Blocked.TIME_RECEIVED      + " INTEGER NOT NULL," +
            Blocked.READ               + " INTEGER NOT NULL" +
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
        List<SmsFilterData> filters = null;
        List<SmsMessageData> messages = null;
        if (oldVersion == 8) {
            Cursor tableCursor = db.query(Filters.TABLE, new String[] {
                Filters.FIELD,
                Filters.MODE,
                Filters.PATTERN,
                Filters.CASE_SENSITIVE
            }, null, null, null, null, null);
            filters = SmsFilterDbLoader.loadAllFilters(tableCursor).toList();
            Cursor messagesCursor = db.query(Blocked.TABLE, new String[] {
                Blocked.SENDER,
                Blocked.BODY,
                Blocked.TIME_SENT,
                Blocked.TIME_RECEIVED
            }, null, null, null, null, null);
            messages = BlockedSmsDbLoader.loadAllMessages(messagesCursor).toList();
        }
        db.execSQL("DROP TABLE IF EXISTS " + Filters.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + Blocked.TABLE);
        onCreate(db);
        if (oldVersion == 8) {
            for (SmsFilterData filter : filters) {
                db.insert(Filters.TABLE, null, filter.serialize());
            }
            for (SmsMessageData message : messages) {
                db.insert(Blocked.TABLE, null, message.serialize());
            }
        }
    }
}
