package com.crossbowffs.nekosms.filters;

import com.crossbowffs.nekosms.data.SmsFilterMode;
import com.crossbowffs.nekosms.data.SmsFilterPatternData;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* package */ class RegexFilterPattern extends SmsFilterPattern {
    private final Matcher mMatcher;

    public RegexFilterPattern(SmsFilterPatternData data) {
        super(data);
        String regexPattern = getPattern();
        if (getMode() == SmsFilterMode.WILDCARD) {
            regexPattern = wildcardToRegex(regexPattern);
        }
        int regexFlags = Pattern.UNICODE_CASE;
        if (!isCaseSensitive()) {
            regexFlags |= Pattern.CASE_INSENSITIVE;
        }
        mMatcher = Pattern.compile(regexPattern, regexFlags).matcher("");
    }

    @Override
    protected boolean matchInternal(String sender, String body) {
        switch (getField()) {
        case SENDER:
            mMatcher.reset(sender);
            break;
        case BODY:
            mMatcher.reset(body);
            break;
        }

        boolean matches = mMatcher.find();
        mMatcher.reset("");
        return matches;
    }

    private static String wildcardToRegex(String wildcardString) {
        StringBuilder sb = new StringBuilder(wildcardString.length() + 16);
        sb.append('^');
        for (int i = 0; i < wildcardString.length(); ++i) {
            char c = wildcardString.charAt(i);
            if (c == '*') {
                sb.append(".*");
            } else if (c == '?') {
                sb.append('.');
            } else if ("\\.[]{}()+-^$|".indexOf(c) >= 0) {
                sb.append('\\');
                sb.append(c);
            } else {
                sb.append(c);
            }
        }
        sb.append('$');
        return sb.toString();
    }
}
