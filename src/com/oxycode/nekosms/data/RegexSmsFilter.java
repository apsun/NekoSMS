package com.oxycode.nekosms.data;

import com.oxycode.nekosms.utils.Xlog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexSmsFilter extends SmsFilter {
    private static final String TAG = RegexSmsFilter.class.getSimpleName();

    private final SmsFilterField mField;
    private final String mPattern;
    private final Matcher mMatcher;

    public RegexSmsFilter(SmsFilterData data) {
        mField = data.getField();
        mPattern = data.getPattern();
        int regexFlags = Pattern.UNICODE_CASE;
        if ((data.getFlags() & SmsFilterFlags.IGNORE_CASE) != 0) {
            regexFlags |= Pattern.CASE_INSENSITIVE;
        }
        mMatcher = Pattern.compile(mPattern, regexFlags).matcher("");
    }

    @Override
    public boolean matches(String sender, String body) {
        Xlog.v(TAG, "Checking regex filter");
        Xlog.v(TAG, "  Field: %s", mField);
        Xlog.v(TAG, "  Pattern: %s", mPattern);
        switch (mField) {
        case SENDER:
            mMatcher.reset(sender);
            break;
        case BODY:
            mMatcher.reset(body);
            break;
        }

        boolean matches = mMatcher.find();
        Xlog.v(TAG, "  Matches: %s", matches);
        mMatcher.reset("");
        return matches;
    }
}
