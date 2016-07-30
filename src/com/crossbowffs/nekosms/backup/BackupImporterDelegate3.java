package com.crossbowffs.nekosms.backup;

import android.content.Context;
import com.crossbowffs.nekosms.data.InvalidFilterException;
import com.crossbowffs.nekosms.data.SmsFilterAction;
import com.crossbowffs.nekosms.data.SmsFilterData;
import org.json.JSONException;
import org.json.JSONObject;

public class BackupImporterDelegate3 extends BackupImporterDelegate2 {
    public BackupImporterDelegate3(Context context) {
        super(context);
    }

    @Override
    protected SmsFilterData readFilterData(JSONObject filterJson) throws JSONException, InvalidBackupException {
        SmsFilterData data = super.readFilterData(filterJson);
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
        return data;
    }
}
