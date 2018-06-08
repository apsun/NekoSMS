package com.crossbowffs.nekosms.filters;

import com.crossbowffs.nekosms.data.*;
import com.crossbowffs.nekosms.utils.Xlog;

public class SmsFilter {
    private final SmsFilterAction mAction;
    private final SmsFilterPattern mSenderPattern;
    private final SmsFilterPattern mBodyPattern;

    public SmsFilter(SmsFilterData data) {
        mAction = data.getAction();
        mSenderPattern = createPattern(data.getSenderPattern());
        mBodyPattern = createPattern(data.getBodyPattern());
    }

    public SmsFilterAction getAction() {
        return mAction;
    }

    public boolean match(String sender, String body) {
        if (mSenderPattern == null && mBodyPattern == null) {
            Xlog.w("No sender or body pattern, ignoring");
            return false;
        }
        Xlog.v("Action: %s", getAction().name());
        boolean matches = true;
        if (mSenderPattern != null) {
            mSenderPattern.printToLog();
            matches = mSenderPattern.match(sender, body);
        }
        if (mBodyPattern != null) {
            mBodyPattern.printToLog();
            matches = matches && mBodyPattern.match(sender, body);
        }
        Xlog.v("Matches: %s", matches);
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
