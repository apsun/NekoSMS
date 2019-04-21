package com.crossbowffs.nekosms.backup;

import android.content.Context;
import android.util.JsonWriter;
import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.data.SmsFilterPatternData;
import com.crossbowffs.nekosms.widget.CursorWrapper;
import com.crossbowffs.nekosms.loader.FilterRuleLoader;

import java.io.*;

/* package */ class BackupExporter implements Closeable {
    private static final int BACKUP_VERSION = BuildConfig.BACKUP_VERSION;
    private final JsonWriter mJsonWriter;

    public BackupExporter(OutputStream out) {
        OutputStreamWriter streamWriter;
        try {
            streamWriter = new OutputStreamWriter(out, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
        mJsonWriter = new JsonWriter(streamWriter);
        mJsonWriter.setIndent("\t");
    }

    public void write(Context context) throws IOException {
        begin();
        writeFilters(context);
        end();
    }

    private void begin() throws IOException {
        mJsonWriter.beginObject();
        mJsonWriter.name(BackupConsts.KEY_VERSION).value(BACKUP_VERSION);
    }

    private void writeFilterPattern(SmsFilterPatternData patternData) throws IOException {
        mJsonWriter
            .beginObject()
            .name(BackupConsts.KEY_FILTER_MODE).value(patternData.getMode().name().toLowerCase())
            .name(BackupConsts.KEY_FILTER_PATTERN).value(patternData.getPattern())
            .name(BackupConsts.KEY_FILTER_CASE_SENSITIVE).value(patternData.isCaseSensitive())
            .endObject();
    }

    private void writeFilter(SmsFilterData filterData) throws IOException {
        mJsonWriter.beginObject();
        mJsonWriter.name(BackupConsts.KEY_FILTER_ACTION).value(filterData.getAction().name().toLowerCase());
        SmsFilterPatternData senderPattern = filterData.getSenderPattern();
        if (senderPattern.hasData()) {
            mJsonWriter.name(BackupConsts.KEY_FILTER_SENDER);
            writeFilterPattern(senderPattern);
        }
        SmsFilterPatternData bodyPattern = filterData.getBodyPattern();
        if (bodyPattern.hasData()) {
            mJsonWriter.name(BackupConsts.KEY_FILTER_BODY);
            writeFilterPattern(bodyPattern);
        }
        mJsonWriter.endObject();
    }

    private void writeFilters(Context context) throws IOException {
        try (CursorWrapper<SmsFilterData> filterCursor = FilterRuleLoader.get().queryAll(context)) {
            if (filterCursor == null) {
                throw new AssertionError("Failed to load SMS filters (queryAll returned null)");
            }
            mJsonWriter.name(BackupConsts.KEY_FILTERS).beginArray();
            SmsFilterData data = new SmsFilterData();
            while (filterCursor.moveToNext()) {
                writeFilter(filterCursor.get(data));
            }
            mJsonWriter.endArray();
        }
    }

    private void end() throws IOException {
        mJsonWriter.endObject();
    }

    @Override
    public void close() throws IOException {
        mJsonWriter.close();
    }
}
