package com.crossbowffs.nekosms.backup;

import android.content.Context;
import com.crossbowffs.nekosms.data.SmsFilterData;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/* package */ interface BackupImporterDelegate {
    List<SmsFilterData> readFilters(Context context, JSONObject json) throws JSONException, InvalidBackupException;
}
