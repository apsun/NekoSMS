package com.crossbowffs.nekosms.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;
import com.crossbowffs.nekosms.BuildConfig;

public final class DatabaseContract {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".database";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    public static class BlockedMessages implements BaseColumns {
        public static final String TABLE = "blocked_messages";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(DatabaseContract.CONTENT_URI, TABLE);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.crossbowffs.sms";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.crossbowffs.sms";

        public static final String SENDER = "sender";
        public static final String BODY = "body";
        public static final String TIME_SENT = "time_sent";
        public static final String TIME_RECEIVED = "time_received";
        public static final String READ = "read";
        public static final String SEEN = "seen";
        public static final String SUB_ID = "sub_id";
        public static final String[] ALL = {
            _ID,
            SENDER,
            BODY,
            TIME_SENT,
            TIME_RECEIVED,
            READ,
            SEEN,
            SUB_ID,
        };
    }

    public static class FilterRules implements BaseColumns {
        public static final String TABLE = "filter_rules";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(DatabaseContract.CONTENT_URI, TABLE);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.crossbowffs.filter";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.crossbowffs.filter";

        public static final String ACTION = "action";
        public static final String SENDER_MODE = "sender_mode";
        public static final String SENDER_PATTERN = "sender_pattern";
        public static final String SENDER_CASE_SENSITIVE = "sender_case_sensitive";
        public static final String BODY_MODE = "body_mode";
        public static final String BODY_PATTERN = "body_pattern";
        public static final String BODY_CASE_SENSITIVE = "body_case_sensitive";
        public static final String[] ALL = {
            _ID,
            ACTION,
            SENDER_MODE,
            SENDER_PATTERN,
            SENDER_CASE_SENSITIVE,
            BODY_MODE,
            BODY_PATTERN,
            BODY_CASE_SENSITIVE,
        };
    }
}
