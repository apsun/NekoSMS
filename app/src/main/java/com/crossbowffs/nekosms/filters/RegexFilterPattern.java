package com.crossbowffs.nekosms.filters;

import com.crossbowffs.nekosms.data.SmsFilterMode;
import com.crossbowffs.nekosms.data.SmsFilterPatternData;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* package */public class RegexFilterPattern extends SmsFilterPattern {
    private final Matcher mMatcher;

    public RegexFilterPattern(SmsFilterPatternData data) {
        super(data);

        // We need to normalize the pattern ourselves since Android
        // doesn't support the CANON_EQ regex flag. Note that this
        // only has an effect if the pattern contains the actual
        // character (e.g. \u3060), NOT the escape sequence (e.g. \\u3060)
        String regexPattern = Normalizer.normalize(getPattern(), Normalizer.Form.NFC);

        // If this is a wildcard pattern, convert it to regex syntax
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
    public boolean match(String sender, String body) {
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
