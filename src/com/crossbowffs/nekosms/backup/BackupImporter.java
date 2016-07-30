package com.crossbowffs.nekosms.backup;

import android.content.Context;
import com.crossbowffs.nekosms.utils.IOUtils;
import com.crossbowffs.nekosms.utils.Xlog;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

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
            delegate = new BackupImporterDelegate1(context);
        } else if (version == 2) {
            delegate = new BackupImporterDelegate2(context);
        } else if (version == 3) {
            delegate = new BackupImporterDelegate3(context);
        } else {
            throw new BackupVersionException("Unknown backup file version: " + version);
        }
        Xlog.i(TAG, "Importing data from backup (version: %d)", version);
        delegate.performImport(json);
    }
    @Override
    public void close() throws IOException {
        mJsonStream.close();
    }
}
