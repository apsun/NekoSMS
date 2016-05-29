package com.crossbowffs.nekosms.filters;

import com.crossbowffs.nekosms.data.*;
import com.crossbowffs.nekosms.utils.Xlog;

public class SmsFilter {
    private static final String TAG = SmsFilter.class.getSimpleName();

    private final SmsFilterPattern mSenderPattern;
    private final SmsFilterPattern mBodyPattern;

    public SmsFilter(SmsFilterData data) {
        mSenderPattern = createPattern(data.getSenderPattern());
        mBodyPattern = createPattern(data.getBodyPattern());
    }

    public boolean match(String sender, String body) {
        Xlog.v(TAG, "Checking SMS filter");
        if (mSenderPattern == null && mBodyPattern == null) {
            Xlog.w(TAG, "  No sender or body pattern, ignoring filter");
            return false;
        }
        boolean matches = true;
        if (mSenderPattern != null) {
            matches &= mSenderPattern.match(sender, body);
        }
        if (mBodyPattern != null) {
            matches &= mBodyPattern.match(sender, body);
        }
        Xlog.v(TAG, "  Matches: %s", matches);
        return matches;
    }

    private static SmsFilterPattern createPattern(SmsFilterPatternData data) {
        if (!data.hasData()) {
            return null;
        }
        switch (data.getMode()) {
        case REGEX:
        case WILDCARD:
            return new RegexFilterPattern(data);
        case CONTAINS:
        case PREFIX:
        case SUFFIX:
        case EQUALS:
            return new StringFilterPattern(data);
        default:
            throw new IllegalArgumentException("Invalid filter mode: " + data.getMode());
        }
    }
}
