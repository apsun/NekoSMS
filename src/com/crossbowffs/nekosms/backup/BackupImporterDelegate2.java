package com.crossbowffs.nekosms.backup;

import android.content.Context;
import com.crossbowffs.nekosms.data.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/* package */ class BackupImporterDelegate2 extends BackupImporterDelegate {
    public BackupImporterDelegate2(Context context) {
        super(context);
    }

    @Override
    public void performImport(JSONObject json) throws JSONException, InvalidBackupException {
        List<SmsFilterData> filters = readFilters(json);
        writeFiltersToDatabase(filters);
    }

    private List<SmsFilterData> readFilters(JSONObject json) throws JSONException, InvalidBackupException {
        JSONArray filterListJson = json.getJSONArray(BackupConsts.KEY_FILTERS);
        ArrayList<SmsFilterData> filters = new ArrayList<>(filterListJson.length());
        for (int i = 0; i < filterListJson.length(); ++i) {
            filters.add(readFilterData(filterListJson.getJSONObject(i)));
        }
        return filters;
    }

    private SmsFilterData readFilterData(JSONObject filterJson) throws JSONException, InvalidBackupException {
        SmsFilterData data = new SmsFilterData();
        String actionString = filterJson.optString(BackupConsts.KEY_FILTER_ACTION, null);
        SmsFilterAction action;
        try {
            action = SmsFilterAction.parse(actionString);
        } catch (InvalidFilterException e) {
            throw new InvalidBackupException(e);
        }
        if (action == null) {
            action = SmsFilterAction.BLOCK;
        }
        data.setAction(action);
        JSONObject senderPatternJson = filterJson.optJSONObject(BackupConsts.KEY_FILTER_SENDER);
        if (senderPatternJson != null) {
            readFilterPatternData(data.getSenderPattern(), senderPatternJson);
        }
        JSONObject bodyPatternJson = filterJson.optJSONObject(BackupConsts.KEY_FILTER_BODY);
        if (bodyPatternJson != null) {
            readFilterPatternData(data.getBodyPattern(), bodyPatternJson);
        }
        return data;
    }

    private void readFilterPatternData(SmsFilterPatternData pattern, JSONObject patternJson) throws JSONException, InvalidBackupException {
        String modeString = patternJson.getString(BackupConsts.KEY_FILTER_MODE);
        String patternString = patternJson.getString(BackupConsts.KEY_FILTER_PATTERN);
        boolean caseSensitive = patternJson.getBoolean(BackupConsts.KEY_FILTER_CASE_SENSITIVE);
        SmsFilterMode mode;
        try {
            mode = SmsFilterMode.parse(modeString);
        } catch (InvalidFilterException e) {
            throw new InvalidBackupException(e);
        }
        pattern.setMode(mode);
        pattern.setPattern(patternString);
        pattern.setCaseSensitive(caseSensitive);
    }
}
