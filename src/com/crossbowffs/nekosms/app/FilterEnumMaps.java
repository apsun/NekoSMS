package com.crossbowffs.nekosms.app;

import android.content.Context;
import android.content.res.Resources;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsFilterField;
import com.crossbowffs.nekosms.data.SmsFilterMode;

import java.util.HashMap;
import java.util.Map;

public final class FilterEnumMaps {
    private FilterEnumMaps() { }

    public static Map<SmsFilterField, String> getFieldMap(Context context) {
        Resources resources = context.getResources();

        Map<SmsFilterField, String> fieldMap = new HashMap<SmsFilterField, String>(2);
        fieldMap.put(SmsFilterField.SENDER, resources.getString(R.string.filter_field_sender));
        fieldMap.put(SmsFilterField.BODY, resources.getString(R.string.filter_field_body));

        return fieldMap;
    }

    public static Map<SmsFilterMode, String> getModeMap(Context context) {
        Resources resources = context.getResources();

        Map<SmsFilterMode, String> modeMap = new HashMap<SmsFilterMode, String>(6);
        modeMap.put(SmsFilterMode.REGEX, resources.getString(R.string.filter_mode_regex));
        // modeMap.put(SmsFilterMode.WILDCARD, resources.getString(R.string.filter_mode_wildcard));
        modeMap.put(SmsFilterMode.CONTAINS, resources.getString(R.string.filter_mode_contains));
        modeMap.put(SmsFilterMode.PREFIX, resources.getString(R.string.filter_mode_prefix));
        modeMap.put(SmsFilterMode.SUFFIX, resources.getString(R.string.filter_mode_suffix));
        modeMap.put(SmsFilterMode.EQUALS, resources.getString(R.string.filter_mode_equals));

        return modeMap;
    }
}
