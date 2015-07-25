package com.crossbowffs.nekosms.backup;

import android.util.JsonReader;
import com.crossbowffs.nekosms.data.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

/* package */ class SmsFilterJsonDeserializer implements JsonConstants {
    private final JsonReader mJsonReader;

    public SmsFilterJsonDeserializer(InputStream in) {
        InputStreamReader streamReader;
        try {
            streamReader = new InputStreamReader(in, JSON_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }

        mJsonReader = new JsonReader(streamReader);
    }

    public void read(SmsFilterLoadCallback callback) throws IOException {
        SmsFilterData filterData = new SmsFilterData();
        mJsonReader.beginArray();
        while (mJsonReader.hasNext()) {
            try {
                readSingle(filterData);
            } catch (InvalidFilterException e) {
                callback.onError(e);
                continue;
            }
            callback.onSuccess(filterData);
        }
        mJsonReader.endArray();
    }

    private void readSingle(SmsFilterData filterData) throws IOException {
        String fieldString = null;
        String modeString = null;
        String pattern = null;
        boolean caseSensitive = true;

        mJsonReader.beginObject();
        while (mJsonReader.hasNext()) {
            String name = mJsonReader.nextName();
            switch (name) {
            case KEY_FIELD:
                fieldString = mJsonReader.nextString();
                break;
            case KEY_MODE:
                modeString = mJsonReader.nextString();
                break;
            case KEY_PATTERN:
                pattern = mJsonReader.nextString();
                break;
            case KEY_CASE_SENSITIVE:
                caseSensitive = mJsonReader.nextBoolean();
                break;
            default:
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
    }

    public void close() throws IOException {
        mJsonReader.close();
    }
}
