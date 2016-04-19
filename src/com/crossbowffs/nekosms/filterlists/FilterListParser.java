package com.crossbowffs.nekosms.filterlists;

import com.crossbowffs.nekosms.utils.Xlog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterListParser {
    private static final String TAG = FilterListParser.class.getSimpleName();

    private BufferedReader mReader;
    private int mCurrentLine;

    public FilterListParser(InputStream input) throws IOException {
        mReader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
    }

    private String readLine() throws IOException {
        String line;
        do {
            line = mReader.readLine();
            if (line != null) {
                line = line.trim();
            }
            ++mCurrentLine;
        } while (line != null && line.isEmpty());
        return line;
    }

    private void throwException(String message, Throwable inner) {
        throw new InvalidFilterListException(mCurrentLine, message, inner);
    }

    private void throwException(String message) {
        throwException(message, null);
    }

    private int parseVersion(String str) {
        int value = -1;
        try {
            value = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            // Will be checked in the block below
        }
        if (value < 0) {
            throwException("Invalid version value: " + str);
        }
        return value;
    }

    private long parseUpdateDate(String str) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;
        try {
            date = df.parse(str);
        } catch (ParseException e) {
            throwException("Invalid updated date format: " + str);
        }
        return date.getTime() / 1000;
    }

    private void fillHeaderField(FilterListInfo header, String key, String value) {
        key = key.toLowerCase();
        if ("id".equals(key)) {
            header.mId = value;
        } else if ("name".equals(key)) {
            header.mName = value;
        } else if ("version".equals(key)) {
            header.mVersion = parseVersion(value);
        } else if ("author".equals(key)) {
            header.mAuthor = value;
        } else if ("url".equals(key)) {
            header.mUrl = value;
        } else if ("updated".equals(key)) {
            header.mUpdated = parseUpdateDate(value);
        } else {
            throwException("Unknown header key: " + key);
        }
    }

    private FilterListInfo parseHeader() throws IOException {
        if (!"######## NekoSMS filter list v1 ########".equals(readLine())) {
            throwException("Did not find header");
        }
        FilterListInfo header = new FilterListInfo();
        Pattern pattern = Pattern.compile("#\\s*@(\\w)\\s*(.*)");
        Matcher matcher = pattern.matcher("");
        while (true) {
            String line = readLine();
            if (line == null) {
                throwException("Reached end of file inside header");
            }
            if ("#########################################".equals(line)) {
                Xlog.v(TAG, "Reached end of header");
                return header;
            }
            matcher.reset(line);
            if (!matcher.matches()) {
                throwException("Invalid header line: " + line);
            }
            String key = matcher.group(1);
            String value = matcher.group(2);
            Xlog.v(TAG, "Read header field: " + key + " -> " + value);
            fillHeaderField(header, key, value);
        }
    }

    private List<FilterListRule> parseRules() throws IOException {
        List<FilterListRule> rules = new ArrayList<>();
        while (true) {
            String line = readLine();
            if (line == null) {
                Xlog.v(TAG, "Reached end of file");
                return rules;
            }
            if (line.charAt(0) == '#') {
                Xlog.v(TAG, "Read comment: " + line);
                continue;
            }
            Xlog.v(TAG, "Read rule: " + line);
            FilterListRule rule = parseRule(line);
            rules.add(rule);
        }
    }

    private FilterListRule parseRule(String line) {
        boolean whitelist = false;
        int offset = 0;
        if (line.charAt(0) == '~') {
            whitelist = true;
            offset = 1;
        }
        String senderPattern = null;
        String bodyPattern;
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < line.length(); ++i) {
            char c = line.charAt(i);
            if (c == ':') {
                if (senderPattern != null) {
                    throwException("Invalid rule: " + line);
                } else {
                    senderPattern = sb.toString();
                    sb.setLength(0);
                }
            } else if (c == '\\' && i < line.length() - 1) {
                char next = line.charAt(++i);
                if (next == ':' || next == '~' || next == '#') {
                    sb.append(next);
                } else {
                    sb.append(c);
                    sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }
        if (senderPattern == null) {
            throwException("Invalid rule: " + line);
        }
        bodyPattern = sb.toString();
        FilterListRule rule = new FilterListRule();
        rule.mIsWhitelist = whitelist;
        rule.mSenderPattern = senderPattern;
        rule.mBodyPattern = bodyPattern;
        return rule;
    }

    public FilterList parse() throws IOException {
        FilterListInfo header = parseHeader();
        List<FilterListRule> rules = parseRules();
        FilterList list = new FilterList();
        list.mInfo = header;
        list.mRules = rules;
        return list;
    }
}
