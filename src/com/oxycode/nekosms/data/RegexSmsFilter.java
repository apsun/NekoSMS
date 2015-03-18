package com.oxycode.nekosms.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexSmsFilter implements SmsFilter {
    private SmsFilterField mField;
    private Matcher mMatcher;

    public RegexSmsFilter(SmsFilterField field, String pattern) {
        mField = field;
        mMatcher = Pattern.compile(pattern).matcher("");
    }

    @Override
    public boolean matches(String sender, String body) {
        switch (mField) {
            case SENDER:
                mMatcher.reset(sender);
                break;
            case BODY:
                mMatcher.reset(body);
                break;
        }
        boolean matches = mMatcher.matches();
        mMatcher.reset("");
        return matches;
    }
}
