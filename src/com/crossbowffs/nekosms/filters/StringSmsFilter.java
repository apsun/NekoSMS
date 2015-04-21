package com.crossbowffs.nekosms.filters;

import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.data.SmsFilterField;
import com.crossbowffs.nekosms.data.SmsFilterMode;
import com.crossbowffs.nekosms.utils.Xlog;

public class StringSmsFilter extends SmsFilter {
    private static final String TAG = StringSmsFilter.class.getSimpleName();

    private final SmsFilterField mField;
    private final SmsFilterMode mMode;
    private final String mPattern;
    private final boolean mCaseSensitive;

    public StringSmsFilter(SmsFilterData data) {
        mField = data.getField();
        mMode = data.getMode();
        mCaseSensitive = data.isCaseSensitive();
        if (!mCaseSensitive) {
            mPattern = data.getPattern().toLowerCase();
        } else {
            mPattern = data.getPattern();
        }
    }

    @Override
    public boolean matches(String sender, String body) {
        Xlog.v(TAG, "Checking string filter");
        Xlog.v(TAG, "  Field: %s", mField.name().toLowerCase());
        Xlog.v(TAG, "  Mode: %s", mMode.name().toLowerCase());
        Xlog.v(TAG, "  Pattern: %s", mPattern);
        Xlog.v(TAG, "  Case sensitive: %s", mCaseSensitive);

        String testString = null;
        switch (mField) {
        case SENDER:
            testString = sender;
            break;
        case BODY:
            testString = body;
            break;
        }

        if (!mCaseSensitive) {
            testString = testString.toLowerCase();
        }

        boolean matches = false;
        switch (mMode) {
        case CONTAINS:
            matches = testString.contains(mPattern);
            break;
        case PREFIX:
            matches = testString.startsWith(mPattern);
            break;
        case SUFFIX:
            matches = testString.endsWith(mPattern);
            break;
        case EQUALS:
            matches = testString.equals(mPattern);
            break;
        }

        Xlog.v(TAG, "  Matches: %s", matches);
        return matches;
    }
}
