package com.crossbowffs.nekosms.filters;

import com.crossbowffs.nekosms.data.SmsFilterAction;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.data.SmsFilterField;
import com.crossbowffs.nekosms.data.SmsFilterMode;
import com.crossbowffs.nekosms.utils.Xlog;

public abstract class SmsFilter {
    private static final String TAG = SmsFilter.class.getSimpleName();

    private final SmsFilterAction mAction;
    private final SmsFilterField mField;
    private final SmsFilterMode mMode;
    private final String mPattern;
    private final boolean mCaseSensitive;

    public SmsFilter(SmsFilterData data) {
        mAction = data.getAction();
        mField = data.getField();
        mMode = data.getMode();
        mPattern = data.getPattern();
        mCaseSensitive = data.isCaseSensitive();
    }

    public SmsFilterAction getAction() {
        return mAction;
    }

    public SmsFilterField getField() {
        return mField;
    }

    public SmsFilterMode getMode() {
        return mMode;
    }

    public String getPattern() {
        return mPattern;
    }

    public boolean isCaseSensitive() {
        return mCaseSensitive;
    }

    public SmsFilterAction match(String sender, String body) {
        Xlog.v(TAG, "Checking SMS filter");
        Xlog.v(TAG, "  Field: %s", getField().name().toLowerCase());
        Xlog.v(TAG, "  Mode: %s", getMode().name().toLowerCase());
        Xlog.v(TAG, "  Pattern: %s", getPattern());
        Xlog.v(TAG, "  Case sensitive: %s", isCaseSensitive());
        boolean matches = matchImpl(sender, body);
        Xlog.v(TAG, "  Matches: %s", matches);
        if (matches) {
            return getAction();
        } else {
            return SmsFilterAction.PASS;
        }
    }

    public abstract boolean matchImpl(String sender, String body);

    public static SmsFilter create(SmsFilterData data) {
        switch (data.getMode()) {
        case REGEX:
        case WILDCARD:
            return new RegexSmsFilter(data);
        case CONTAINS:
        case PREFIX:
        case SUFFIX:
        case EQUALS:
            return new StringSmsFilter(data);
        default:
            throw new IllegalArgumentException("Invalid filter mode: " + data.getMode());
        }
    }
}
