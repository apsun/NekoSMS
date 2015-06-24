package com.crossbowffs.nekosms.data;

import android.util.JsonReader;
import android.util.JsonWriter;
import com.crossbowffs.nekosms.utils.Xlog;

import java.io.*;
import java.util.List;

public final class SmsFilterJsonLoader {
    public interface FilterLoadCallback {
        void onSuccess(SmsFilterData filter);
        void onError(InvalidFilterException e);
    }

    private static final String TAG = SmsFilterJsonLoader.class.getSimpleName();
    private static final String KEY_FIELD = "field";
    private static final String KEY_MODE = "mode";
    private static final String KEY_PATTERN = "pattern";
    private static final String KEY_CASE_SENSITIVE = "case_sensitive";

    private SmsFilterJsonLoader() { }

    public static void toJson(OutputStream out, List<SmsFilterData> filters) throws IOException {
        OutputStreamWriter streamWriter = new OutputStreamWriter(out, "UTF-8");
        JsonWriter jsonWriter = new JsonWriter(streamWriter);
        jsonWriter.setIndent("\t");
        try {
            writeFiltersArray(jsonWriter, filters);
        } finally {
            jsonWriter.close();
        }
    }

    private static void writeFiltersArray(JsonWriter jsonWriter, List<SmsFilterData> filters) throws IOException {
        jsonWriter.beginArray();
        for (SmsFilterData filter : filters) {
            writeFilter(jsonWriter, filter);
        }
        jsonWriter.endArray();
    }

    private static void writeFilter(JsonWriter jsonWriter, SmsFilterData filter) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name(KEY_FIELD).value(filter.getField().name());
        jsonWriter.name(KEY_MODE).value(filter.getMode().name());
        jsonWriter.name(KEY_PATTERN).value(filter.getPattern());
        jsonWriter.name(KEY_CASE_SENSITIVE).value(filter.isCaseSensitive());
        jsonWriter.endObject();
    }

    public static void fromJson(InputStream in, FilterLoadCallback callback) throws IOException {
        InputStreamReader streamReader = new InputStreamReader(in, "UTF-8");
        JsonReader jsonReader = new JsonReader(streamReader);
        try {
            readFiltersArray(jsonReader, callback);
        } finally {
            jsonReader.close();
        }
    }

    private static void readFiltersArray(JsonReader jsonReader, FilterLoadCallback callback) throws IOException {
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            SmsFilterData filter = new SmsFilterData();
            try {
                readFilter(jsonReader, filter);
            } catch (InvalidFilterException e) {
                Xlog.e(TAG, "Failed to create SMS filter", e);
                callback.onError(e);
                continue;
            }
            callback.onSuccess(filter);
        }
        jsonReader.endArray();
    }

    private static void readFilter(JsonReader jsonReader, SmsFilterData filter) throws IOException {
        String fieldString = null;
        String modeString = null;
        String pattern = null;
        boolean caseSensitive = true;

        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            if (name.equals(KEY_FIELD)) {
                fieldString = jsonReader.nextString();
            } else if (name.equals(KEY_MODE)) {
                modeString = jsonReader.nextString();
            } else if (name.equals(KEY_PATTERN)) {
                pattern = jsonReader.nextString();
            } else if (name.equals(KEY_CASE_SENSITIVE)) {
                caseSensitive = jsonReader.nextBoolean();
            } else {
                jsonReader.skipValue();
            }
        }
        jsonReader.endObject();

        SmsFilterField field = SmsFilterField.parse(fieldString);
        SmsFilterMode mode = SmsFilterMode.parse(modeString);

        filter.setField(field);
        filter.setMode(mode);
        filter.setPattern(pattern);
        filter.setCaseSensitive(caseSensitive);
        filter.validate();
    }
}
