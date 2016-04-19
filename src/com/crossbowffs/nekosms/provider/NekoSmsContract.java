package com.crossbowffs.nekosms.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;
import com.crossbowffs.nekosms.BuildConfig;

public final class NekoSmsContract {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    public static class BlockedMessages implements BaseColumns {
        public static final String TABLE = "blocked_messages";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(NekoSmsContract.CONTENT_URI, TABLE);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.crossbowffs.blocked-message";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.crossbowffs.blocked-message";

        public static final String SENDER = "sender";
        public static final String BODY = "body";
        public static final String TIME_SENT = "time_sent";
        public static final String TIME_RECEIVED = "time_received";
        public static final String READ = "read";
        public static final String SEEN = "seen";
        public static final String[] ALL = {
            _ID,
            SENDER,
            BODY,
            TIME_SENT,
            TIME_RECEIVED,
            READ,
            SEEN
        };
    }

    public static class UserRules implements BaseColumns {
        public static final String TABLE = "user_rules";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(NekoSmsContract.CONTENT_URI, TABLE);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.crossbowffs.user-rule";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.crossbowffs.user-rule";

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
            BODY_CASE_SENSITIVE
        };
    }

    public static class FilterLists implements BaseColumns {
        public static final String TABLE = "filter_lists";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(NekoSmsContract.CONTENT_URI, TABLE);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.crossbowffs.filter-list";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.crossbowffs.filter-list";

        public static final String NAMESPACE = "namespace";
        public static final String NAME = "name";
        public static final String VERSION = "version";
        public static final String AUTHOR = "author";
        public static final String URL = "url";
        public static final String UPDATED = "updated";
        public static final String[] ALL = {
            _ID,
            NAMESPACE,
            NAME,
            VERSION,
            AUTHOR,
            URL,
            UPDATED
        };
    }

    public static class FilterListRules implements BaseColumns {
        public static final String TABLE = "filter_list_rules";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(NekoSmsContract.CONTENT_URI, TABLE);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.crossbowffs.filter-list-rule";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.crossbowffs.filter-list-rule";

        public static final String LIST_ID = "list_id";
        public static final String ACTION = "action";
        public static final String SENDER_PATTERN = "sender_pattern";
        public static final String BODY_PATTERN = "body_pattern";
        public static final String[] ALL = {
            _ID,
            LIST_ID,
            ACTION,
            SENDER_PATTERN,
            BODY_PATTERN
        };
    }
}
