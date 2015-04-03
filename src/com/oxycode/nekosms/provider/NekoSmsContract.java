package com.oxycode.nekosms.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class NekoSmsContract {
    public static final String AUTHORITY = "com.oxycode.nekosms.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    public static class Blocked implements BaseColumns {
        public static final String TABLE = "blocked";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(NekoSmsContract.CONTENT_URI, TABLE);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.oxycode.sms";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.oxycode.sms";

        public static final String SENDER             = "sender";
        public static final String BODY               = "body";
        public static final String TIME_SENT          = "time_sent";
        public static final String TIME_RECEIVED      = "time_received";
    }

    public static class Filters implements BaseColumns {
        public static final String TABLE = "filters";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(NekoSmsContract.CONTENT_URI, TABLE);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.oxycode.filter";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.oxycode.filter";

        public static final String FIELD              = "field";
        public static final String MODE               = "mode";
        public static final String PATTERN            = "pattern";
        public static final String CASE_SENSITIVE     = "case_sensitive";
    }
}
