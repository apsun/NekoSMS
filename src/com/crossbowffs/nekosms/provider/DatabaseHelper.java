package com.crossbowffs.nekosms.provider;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.utils.Xlog;

import java.util.List;

import static com.crossbowffs.nekosms.provider.DatabaseContract.*;

/* package */ class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = DatabaseHelper.class.getSimpleName();
    private static final String DATABASE_NAME = "nekosms.db";
    private static final int DATABASE_VERSION = BuildConfig.DATABASE_VERSION;

    private static final String CREATE_BLOCKED_MESSAGES_TABLE =
        "CREATE TABLE " + BlockedMessages.TABLE + "(" +
            BlockedMessages._ID                 + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            BlockedMessages.SENDER              + " TEXT NOT NULL," +
            BlockedMessages.BODY                + " TEXT NOT NULL," +
            BlockedMessages.TIME_SENT           + " INTEGER NOT NULL," +
            BlockedMessages.TIME_RECEIVED       + " INTEGER NOT NULL," +
            BlockedMessages.READ                + " INTEGER NOT NULL," +
            BlockedMessages.SEEN                + " INTEGER NOT NULL" +
        ");";

    private static final String CREATE_FILTER_RULES_TABLE =
        "CREATE TABLE " + FilterRules.TABLE + "(" +
            FilterRules._ID                      + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            FilterRules.ACTION                   + " INTEGER NOT NULL, " +
            FilterRules.SENDER_MODE              + " TEXT, " +
            FilterRules.SENDER_PATTERN           + " TEXT, " +
            FilterRules.SENDER_CASE_SENSITIVE    + " INTEGER, " +
            FilterRules.BODY_MODE                + " TEXT, " +
            FilterRules.BODY_PATTERN             + " TEXT, " +
            FilterRules.BODY_CASE_SENSITIVE      + " INTEGER" +
        ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_BLOCKED_MESSAGES_TABLE);
        db.execSQL(CREATE_FILTER_RULES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Xlog.i(TAG, "Upgrading database from v%d to v%d", oldVersion, newVersion);
        List<SmsFilterData> filters = null;
        List<SmsMessageData> messages = null;
        if (oldVersion < 8) {
            db.execSQL("DROP TABLE IF EXISTS filters");
            db.execSQL("DROP TABLE IF EXISTS blocked");
        } else if (oldVersion == 8) {
            Xlog.i(TAG, "Performing database upgrade from v8");
            Cursor tableCursor = db.query("filters", new String[] {
                "field",
                "mode",
                "pattern",
                "case_sensitive"
            }, null, null, null, null, null);
            Cursor messagesCursor = db.query("blocked", new String[] {
                "sender",
                "body",
                "time_sent",
                "time_received"
            }, null, null, null, null, null);
            // TODO
            db.execSQL("DROP TABLE IF EXISTS filters");
            db.execSQL("DROP TABLE IF EXISTS blocked");
        }
        onCreate(db);
        if (oldVersion == 8) {
            // TODO
        }
    }
}
