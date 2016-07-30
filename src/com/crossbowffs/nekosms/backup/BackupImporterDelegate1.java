package com.crossbowffs.nekosms.backup;

import android.content.Context;
import com.crossbowffs.nekosms.data.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/* package */ class BackupImporterDelegate1 extends BackupImporterDelegate {
    public BackupImporterDelegate1(Context context) {
        super(context);
    }

    @Override
    public void performImport(JSONObject json) throws JSONException, InvalidBackupException {
        List<SmsFilterData> filters = readFilters(json);
        if (filters != null) {
            writeFiltersToDatabase(filters);
        }
    }

    private List<SmsFilterData> readFilters(JSONObject json) throws JSONException, InvalidBackupException {
        // In version 1 it was possible to export a backup w/o a filters field.
        // In that case, we just ignore the data and return success.
        JSONArray filterListJson = json.optJSONArray("filters");
        if (filterListJson == null) {
            return null;
        }
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
        data.setAction(SmsFilterAction.BLOCK);
        SmsFilterPatternData patternData = data.getPatternForField(field);
        patternData.setMode(mode);
        patternData.setPattern(patternString);
        patternData.setCaseSensitive(caseSensitive);
        return data;
    }
}
