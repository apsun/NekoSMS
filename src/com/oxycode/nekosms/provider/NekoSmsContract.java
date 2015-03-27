package com.oxycode.nekosms.provider;

import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.Telephony;

public final class NekoSmsContract {
    public static final String AUTHORITY = "com.oxycode.nekosms.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    public static class Blocked implements BaseColumns {
        public static final String TABLE = "blocked";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(NekoSmsContract.CONTENT_URI, TABLE);

        public static final String ADDRESS            = Telephony.Sms.ADDRESS;
        public static final String BODY               = Telephony.Sms.BODY;
        public static final String DATE_SENT          = Telephony.Sms.DATE_SENT;
        public static final String DATE               = Telephony.Sms.DATE;
        public static final String PROTOCOL           = Telephony.Sms.PROTOCOL;
        public static final String SEEN               = Telephony.Sms.SEEN;
        public static final String READ               = Telephony.Sms.READ;
        public static final String SUBJECT            = Telephony.Sms.SUBJECT;
        public static final String REPLY_PATH_PRESENT = Telephony.Sms.REPLY_PATH_PRESENT;
        public static final String SERVICE_CENTER     = Telephony.Sms.SERVICE_CENTER;
    }

    public static class Filters implements BaseColumns {
        public static final String TABLE = "filters";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(NekoSmsContract.CONTENT_URI, TABLE);

        public static final String FIELD              = "field";
        public static final String MODE               = "mode";
        public static final String PATTERN            = "pattern";
        public static final String FLAGS              = "flags";
    }
}
