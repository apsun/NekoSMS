package com.crossbowffs.nekosms.loader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import com.crossbowffs.nekosms.data.*;

import static com.crossbowffs.nekosms.provider.NekoSmsContract.FilterListRules;

public class FilterListRuleLoader extends SmsFilterLoader {
    private static FilterListRuleLoader sInstance;

    public static FilterListRuleLoader get() {
        if (sInstance == null) {
            sInstance = new FilterListRuleLoader();
        }
        return sInstance;
    }

    private FilterListRuleLoader() {
        super(FilterListRules.class);
    }

    @Override
    protected SmsFilterData newData() {
        return new SmsFilterData();
    }

    @Override
    protected void bindData(Cursor cursor, int column, String columnName, SmsFilterData data) {
        switch (columnName) {
        case FilterListRules._ID:
            data.setId(cursor.getLong(column));
            break;
        case FilterListRules.LIST_ID:
            data.setFilterListId(cursor.getLong(column));
            break;
        case FilterListRules.ACTION:
            data.setAction(cursor.getInt(column) == 0 ? SmsFilterAction.ALLOW : SmsFilterAction.BLOCK);
            break;
        case FilterListRules.SENDER_PATTERN:
            if (!cursor.isNull(column))
                ensureSenderPattern(data).setMode(SmsFilterMode.parse(cursor.getString(column)));
            break;
        case FilterListRules.BODY_PATTERN:
            if (!cursor.isNull(column))
                ensureBodyPattern(data).setPattern(cursor.getString(column));
            break;
        }
    }

    @Override
    protected ContentValues serialize(SmsFilterData data) {
        ContentValues values = new ContentValues(5);
        if (data.getId() >= 0) {
            values.put(FilterListRules._ID, data.getId());
        }
        values.put(FilterListRules.LIST_ID, data.getFilterListId());
        values.put(FilterListRules.ACTION, data.getAction() == SmsFilterAction.BLOCK ? 1 : 0);
        SmsFilterPatternData senderPattern = data.getSenderPattern();
        if (senderPattern != null) {
            values.put(FilterListRules.SENDER_PATTERN, senderPattern.getPattern());
        }
        SmsFilterPatternData bodyPattern = data.getBodyPattern();
        if (bodyPattern != null) {
            values.put(FilterListRules.BODY_PATTERN, bodyPattern.getPattern());
        }
        return values;
    }

    @Override
    public CursorWrapper<SmsFilterData> queryAllWhitelistFirst(Context context) {
        return queryAll(context, null, null, FilterListRules.ACTION + " ASC");
    }

    private SmsFilterPatternData ensureSenderPattern(SmsFilterData data) {
        SmsFilterPatternData pattern = data.getSenderPattern();
        if (pattern == null) {
            pattern = new SmsFilterPatternData();
            pattern.setField(SmsFilterField.SENDER);
            pattern.setMode(SmsFilterMode.REGEX);
            pattern.setCaseSensitive(true);
            data.setSenderPattern(pattern);
        }
        return pattern;
    }

    private SmsFilterPatternData ensureBodyPattern(SmsFilterData data) {
        SmsFilterPatternData pattern = data.getBodyPattern();
        if (pattern == null) {
            pattern = new SmsFilterPatternData();
            pattern.setField(SmsFilterField.BODY);
            pattern.setMode(SmsFilterMode.REGEX);
            pattern.setCaseSensitive(true);
            data.setBodyPattern(pattern);
        }
        return pattern;
    }
}
