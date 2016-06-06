package com.crossbowffs.nekosms.backup;

import android.content.Context;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.loader.FilterRuleLoader;
import com.crossbowffs.nekosms.utils.IOUtils;
import com.crossbowffs.nekosms.utils.Xlog;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.List;

/* package */ class BackupImporter implements Closeable {
    private static final String TAG = BackupImporter.class.getSimpleName();
    private final InputStream mJsonStream;

    public BackupImporter(InputStream in) {
        mJsonStream = in;
    }

    public BackupImporter(File file) throws FileNotFoundException {
        this(new FileInputStream(file));
    }

    public void read(Context context) throws IOException, InvalidBackupException {
        // We use JSONObject (DOM) instead of JsonReader (SAX) because
        // we need to read the version number from the JSON object before
        // we can determine how to parse it. With the SAX model, we might
        // have to read the file twice in the worst case (if the version field
        // is at the end of the file), so in this case a DOM approach is faster.
        // If memory consumption becomes a problem, we should probably move back
        // to JsonReader and encode the version in the file extension.
        String jsonString = IOUtils.streamToString(mJsonStream);
        try {
            readImpl(context, jsonString);
        } catch (JSONException e) {
            throw new InvalidBackupException(e);
        }
    }

    private void readImpl(Context context, String jsonString) throws JSONException, InvalidBackupException {
        JSONObject json = new JSONObject(jsonString);
        int version = json.getInt(BackupConsts.KEY_VERSION);
        BackupImporterDelegate delegate;
        if (version == 1) {
            delegate = new BackupImporterDelegate1();
        } else if (version == 2) {
            delegate = new BackupImporterDelegate2();
        } else {
            throw new InvalidBackupException("Unknown backup file version: " + version);
        }
        Xlog.i(TAG, "Importing data from backup (version: %d)", version);
        importFilters(delegate, context, json);
    }

    private void importFilters(BackupImporterDelegate delegate, Context context, JSONObject json) throws JSONException, InvalidBackupException {
        List<SmsFilterData> filters = delegate.readFilters(context, json);
        if (!FilterRuleLoader.get().replaceAll(context, filters)) {
            throw new InvalidBackupException("Unknown error occurred while importing filters into database");
        }
    }

    @Override
    public void close() throws IOException {
        mJsonStream.close();
    }
}
