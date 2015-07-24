package com.crossbowffs.nekosms.backup;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import com.crossbowffs.nekosms.data.InvalidFilterException;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.data.SmsFilterLoadCallback;
import com.crossbowffs.nekosms.data.SmsFilterLoader;
import com.crossbowffs.nekosms.utils.Xlog;

import java.io.*;

public class SmsFilterStorageLoader {
    // This is required in order to pass checked exceptions
    // across closure boundaries. Wrap the checked exception
    // using this, then catch the exception outside the
    // closure and rethrow the wrapped exception.
    private static class UncheckedIOException extends RuntimeException {
        public UncheckedIOException(IOException exception) {
            super(exception);
        }

        @Override
        public IOException getCause() {
            return (IOException)super.getCause();
        }
    }

    public static class FilterImportResult {
        public int mSuccessCount;
        public int mDuplicateCount;
        public int mErrorCount;
    }

    public static class FilterExportResult {
        public int mSuccessCount;
        public int mErrorCount;
    }

    private static final String TAG = SmsFilterStorageLoader.class.getSimpleName();
    private static final String EXPORT_FILE_PATH = "nekosms.json";

    public static FilterImportResult importFromStorage(final Context context) throws IOException {
        File sdCard = Environment.getExternalStorageDirectory();
        File file = new File(sdCard, EXPORT_FILE_PATH);
        FileInputStream in = new FileInputStream(file);
        SmsFilterJsonDeserializer jsonDeserializer = new SmsFilterJsonDeserializer(in);
        final FilterImportResult result = new FilterImportResult();
        try {
            jsonDeserializer.read(new SmsFilterLoadCallback() {
                @Override
                public void onSuccess(SmsFilterData filterData) {
                    Uri filterUri = SmsFilterLoader.writeFilter(context, filterData);
                    if (filterUri != null) {
                        result.mSuccessCount++;
                    } else {
                        result.mDuplicateCount++;
                    }
                }

                @Override
                public void onError(InvalidFilterException e) {
                    result.mErrorCount++;
                    Xlog.e(TAG, "Failed to load SMS filter", e);
                }
            });
        } finally {
            jsonDeserializer.close();
        }

        return result;
    }

    public static FilterExportResult exportToStorage(Context context) throws IOException {
        File sdCard = Environment.getExternalStorageDirectory();
        File file = new File(sdCard, EXPORT_FILE_PATH);
        FileOutputStream out = new FileOutputStream(file);
        final SmsFilterJsonSerializer jsonSerializer = new SmsFilterJsonSerializer(out);
        final FilterExportResult result = new FilterExportResult();
        try {
            jsonSerializer.begin();
            SmsFilterLoader.loadAllFilters(context, new SmsFilterLoadCallback() {
                @Override
                public void onSuccess(SmsFilterData filterData) {
                    try {
                        jsonSerializer.writeFilter(filterData);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }

                    result.mSuccessCount++;
                }

                @Override
                public void onError(InvalidFilterException e) {
                    result.mErrorCount++;
                    Xlog.e(TAG, "Failed to load SMS filter", e);
                }
            });
            jsonSerializer.end();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } finally {
            jsonSerializer.close();
        }

        return result;
    }
}
