package com.crossbowffs.nekosms.backup;

import android.content.Context;
import android.util.JsonReader;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.data.SmsFilterField;
import com.crossbowffs.nekosms.data.SmsFilterMode;
import com.crossbowffs.nekosms.database.SmsFilterDbLoader;
import com.crossbowffs.nekosms.preferences.Preferences;
import com.crossbowffs.nekosms.utils.Xlog;

import java.io.*;
import java.util.ArrayList;

/* package */ class BackupImporter implements JsonConstants, Closeable {
    private static final String TAG = BackupImporter.class.getSimpleName();
    private final JsonReader mJsonReader;

    public BackupImporter(InputStream in) {
        InputStreamReader streamReader;
        try {
            streamReader = new InputStreamReader(in, JSON_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
        mJsonReader = new JsonReader(streamReader);
    }

    public BackupImporter(File file) throws FileNotFoundException {
        this(new FileInputStream(file));
    }

    public void read(Context context) throws IOException {
        mJsonReader.beginObject();
        while (mJsonReader.hasNext()) {
            String name = mJsonReader.nextName();
            switch (name) {
            case KEY_VERSION:
                readVersion();
                break;
            case KEY_SETTINGS:
                readSettings(context);
                break;
            case KEY_FILTERS:
                readFilters(context);
                break;
            }
        }
        mJsonReader.endObject();
    }

    private void readVersion() throws IOException {
        int version = mJsonReader.nextInt();
        Xlog.i(TAG, "Importing settings from backup v%d", version);
    }

    private void readSettings(Context context) throws IOException {
        // The editor already holds these preferences in memory for us, so the
        // transaction is automatically atomic. No temporary object is required.
        Preferences preferences = Preferences.fromContext(context);
        Preferences.Editor editor = preferences.edit();
        mJsonReader.beginObject();
        while (mJsonReader.hasNext()) {
            String name = mJsonReader.nextName();
            switch (name) {
            case Preferences.KEY_ENABLE:
                editor.put(Preferences.PREF_ENABLE, mJsonReader.nextBoolean());
                break;
            case Preferences.KEY_NOTIFICATIONS_ENABLE:
                editor.put(Preferences.PREF_NOTIFICATIONS_ENABLE, mJsonReader.nextBoolean());
                break;
            case Preferences.KEY_NOTIFICATIONS_SOUND:
                editor.put(Preferences.PREF_NOTIFICATIONS_SOUND, mJsonReader.nextBoolean());
                break;
            case Preferences.KEY_NOTIFICATIONS_VIBRATE:
                editor.put(Preferences.PREF_NOTIFICATIONS_LIGHTS, mJsonReader.nextBoolean());
                break;
            default:
                Xlog.w(TAG, "Unknown settings JSON key: " + name);
                mJsonReader.skipValue();
                break;
            }
        }
        mJsonReader.endObject();
        editor.apply();
    }

    private void readFilters(Context context) throws IOException {
        ArrayList<SmsFilterData> filters = new ArrayList<>();
        mJsonReader.beginArray();
        while (mJsonReader.hasNext()) {
            SmsFilterData filterData = readFilter();
            filters.add(filterData);
        }
        mJsonReader.endArray();
        SmsFilterDbLoader.deleteAllFilters(context);
        if (!SmsFilterDbLoader.writeFilters(context, filters)) {
            Xlog.e(TAG, "Could not write all filters to database");
        }
    }

    private SmsFilterData readFilter() throws IOException {
        SmsFilterData filterData = new SmsFilterData();
        String fieldString = null;
        String modeString = null;
        String pattern = null;
        boolean caseSensitive = true;

        mJsonReader.beginObject();
        while (mJsonReader.hasNext()) {
            String name = mJsonReader.nextName();
            switch (name) {
            case KEY_FILTER_FIELD:
                fieldString = mJsonReader.nextString();
                break;
            case KEY_FILTER_MODE:
                modeString = mJsonReader.nextString();
                break;
            case KEY_FILTER_PATTERN:
                pattern = mJsonReader.nextString();
                break;
            case KEY_FILTER_CASE_SENSITIVE:
                caseSensitive = mJsonReader.nextBoolean();
                break;
            default:
                Xlog.w(TAG, "Unknown filter JSON key: " + name);
                mJsonReader.skipValue();
                break;
            }
        }
        mJsonReader.endObject();

        SmsFilterField field = SmsFilterField.parse(fieldString);
        SmsFilterMode mode = SmsFilterMode.parse(modeString);
        filterData.setField(field);
        filterData.setMode(mode);
        filterData.setPattern(pattern);
        filterData.setCaseSensitive(caseSensitive);
        filterData.validate();
        return filterData;
    }

    @Override
    public void close() throws IOException {
        mJsonReader.close();
    }
}
