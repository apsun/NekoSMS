package com.oxycode.nekosms.data;

import com.oxycode.nekosms.utils.Xlog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexSmsFilter implements SmsFilter {
    private static final String TAG = RegexSmsFilter.class.getSimpleName();

    private final SmsFilterField mField;
    private final String mPattern;
    private final Matcher mMatcher;

    public RegexSmsFilter(SmsFilterAction action, SmsFilterField field, String pattern) {
        mField = field;
        mPattern = pattern;
        mMatcher = Pattern.compile(pattern).matcher("");
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
