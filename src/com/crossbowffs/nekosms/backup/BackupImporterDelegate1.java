package com.crossbowffs.nekosms.backup;

import android.content.Context;
import com.crossbowffs.nekosms.data.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/* package */ class BackupImporterDelegate1 implements BackupImporterDelegate {
    @Override
    public List<SmsFilterData> readFilters(Context context, JSONObject json) throws JSONException, InvalidBackupException {
        // In version 1 it was possible to export a backup w/o a filters field.
        // In that case, we just treat the backup file as invalid, since
        // there's no usable data in it anyways.
        JSONArray filterListJson = json.getJSONArray("filters");
        ArrayList<SmsFilterData> filters = new ArrayList<>(filterListJson.length());
        for (int i = 0; i < filterListJson.length(); ++i) {
            filters.add(readFilterData(filterListJson.getJSONObject(i)));
        }
        return filters;
    }

    private SmsFilterData readFilterData(JSONObject filterJson) throws JSONException, InvalidBackupException {
        String fieldString = filterJson.getString("field");
        String modeString = filterJson.getString("mode");
        String patternString = filterJson.getString("pattern");
        boolean caseSensitive = filterJson.getBoolean("case_sensitive");
        SmsFilterField field;
        SmsFilterMode mode;
        try {
            field = SmsFilterField.parse(fieldString);
            mode = SmsFilterMode.parse(modeString);
        } catch (InvalidFilterException e) {
            throw new InvalidBackupException(e);
        }
        SmsFilterData data = new SmsFilterData();
        SmsFilterPatternData patternData = data.getPatternForField(field);
        patternData.setMode(mode);
        patternData.setPattern(patternString);
        patternData.setCaseSensitive(caseSensitive);
        return data;
    }
}
