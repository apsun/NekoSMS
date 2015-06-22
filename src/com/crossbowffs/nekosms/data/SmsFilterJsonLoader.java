package com.crossbowffs.nekosms.data;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public final class SmsFilterJsonLoader {
    private static final String KEY_FIELD = "field";
    private static final String KEY_MODE = "mode";
    private static final String KEY_PATTERN = "pattern";
    private static final String KEY_CASE_SENSITIVE = "case_sensitive";

    private SmsFilterJsonLoader() { }

    public static void toJson(OutputStream out, List<SmsFilterData> filters) throws IOException {
        OutputStreamWriter streamWriter = new OutputStreamWriter(out, "UTF-8");
        JsonWriter jsonWriter = new JsonWriter(streamWriter);
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

    public static List<SmsFilterData> fromJson(InputStream in, boolean ignoreErrors) throws IOException {
        InputStreamReader streamReader = new InputStreamReader(in, "UTF-8");
        JsonReader jsonReader = new JsonReader(streamReader);
        try {
            return readFiltersArray(jsonReader, ignoreErrors);
        } finally {
            jsonReader.close();
        }
    }

    private static List<SmsFilterData> readFiltersArray(JsonReader jsonReader, boolean ignoreErrors) throws IOException {
        List<SmsFilterData> filters = new ArrayList<>();
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            SmsFilterData filter;
            try {
                filter = readFilter(jsonReader);
            } catch (IllegalArgumentException e) {
                if (ignoreErrors) {
                    continue;
                } else {
                    throw new IOException(e);
                }
            }
            filters.add(filter);
        }
        jsonReader.endArray();
        return filters;
    }

    private static SmsFilterData readFilter(JsonReader jsonReader) throws IOException {
        SmsFilterData data = new SmsFilterData();
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            if (name.equals(KEY_FIELD)) {
                data.setField(SmsFilterField.valueOf(jsonReader.nextString()));
            } else if (name.equals(KEY_MODE)) {
                data.setMode(SmsFilterMode.valueOf(jsonReader.nextString()));
            } else if (name.equals(KEY_PATTERN)) {
                data.setPattern(jsonReader.nextString());
            } else if (name.equals(KEY_CASE_SENSITIVE)) {
                data.setCaseSensitive(jsonReader.nextBoolean());
            } else {
                jsonReader.skipValue();
            }
        }
        jsonReader.endObject();
        if (data.getField() == null || data.getMode() == null || data.getPattern() == null) {
            throw new IllegalArgumentException("Incomplete filter data");
        }
        return data;
    }
}
