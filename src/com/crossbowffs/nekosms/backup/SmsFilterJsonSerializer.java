package com.crossbowffs.nekosms.backup;

import android.util.JsonWriter;
import com.crossbowffs.nekosms.data.SmsFilterData;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

/* package */ class SmsFilterJsonSerializer implements JsonConstants {
    private final JsonWriter mJsonWriter;

    public SmsFilterJsonSerializer(OutputStream out) {
        OutputStreamWriter streamWriter;
        try {
            streamWriter = new OutputStreamWriter(out, JSON_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
        mJsonWriter = new JsonWriter(streamWriter);
        mJsonWriter.setIndent("\t");
    }

    public void begin() throws IOException {
        mJsonWriter.beginArray();
    }

    public void writeFilter(SmsFilterData filterData) throws IOException {
        mJsonWriter.beginObject();
        mJsonWriter.name(KEY_FIELD).value(filterData.getField().name());
        mJsonWriter.name(KEY_MODE).value(filterData.getMode().name());
        mJsonWriter.name(KEY_PATTERN).value(filterData.getPattern());
        mJsonWriter.name(KEY_CASE_SENSITIVE).value(filterData.isCaseSensitive());
        mJsonWriter.endObject();
    }

    public void end() throws IOException {
        mJsonWriter.endArray();
    }

    public void close() throws IOException {
        mJsonWriter.close();
    }
}
