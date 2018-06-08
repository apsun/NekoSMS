package com.crossbowffs.nekosms.backup;

import android.content.Context;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.loader.FilterRuleLoader;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/* package */ abstract class BackupImporterDelegate {
    protected final Context mContext;

    public BackupImporterDelegate(Context context) {
        mContext = context;
    }

    public abstract void performImport(JSONObject json) throws JSONException, InvalidBackupException;

    protected void writeFiltersToDatabase(List<SmsFilterData> filters) throws InvalidBackupException {
        if (!FilterRuleLoader.get().replaceAll(mContext, filters)) {
            throw new InvalidBackupException("Unknown error occurred while importing filters into database");
        }
    }
}
