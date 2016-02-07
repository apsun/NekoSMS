package com.crossbowffs.nekosms.backup;

import android.content.Context;
import android.os.Environment;
import com.crossbowffs.nekosms.data.InvalidFilterException;
import com.crossbowffs.nekosms.utils.Xlog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public final class BackupLoader {
    private static final String TAG = BackupLoader.class.getSimpleName();
    private static final String BACKUP_DIRECTORY = "NekoSMS";
    private static final String BACKUP_FILE_NAME = "backup.json";

    public static final int IMPORT_SUCCESS = 100;
    public static final int IMPORT_NO_BACKUP = 101;
    public static final int IMPORT_INVALID_BACKUP = 102;
    public static final int IMPORT_READ_FAILED = 103;

    public static final int EXPORT_SUCCESS = 200;
    public static final int EXPORT_WRITE_FAILED = 201;

    public static final int OPTION_INCLUDE_FILTERS = 1 << 0;
    public static final int OPTION_INCLUDE_SETTINGS = 1 << 1;

    private BackupLoader() { }

    public static int importFromStorage(Context context, int options) {
        File sdCard = Environment.getExternalStorageDirectory();
        File file = new File(new File(sdCard, BACKUP_DIRECTORY), BACKUP_FILE_NAME);
        try (BackupImporter importer = new BackupImporter(file)) {
            importer.read(context, options);
        } catch (FileNotFoundException e) {
            Xlog.e(TAG, "Import failed: no backup file found", e);
            return IMPORT_NO_BACKUP;
        } catch (InvalidFilterException | IllegalStateException | NumberFormatException e) {
            // IllegalStateException and NumberFormatException are thrown by
            // the JSON parser if it encounters an unexpected token or invalid
            // numeric value (including unicode escape values)
            Xlog.e(TAG, "Import failed: invalid backup file", e);
            return IMPORT_INVALID_BACKUP;
        } catch (IOException e) {
            Xlog.e(TAG, "Import failed: could not read backup file", e);
            return IMPORT_READ_FAILED;
        }
        Xlog.i(TAG, "Import succeeded");
        return IMPORT_SUCCESS;
    }

    public static int exportToStorage(Context context, int options) {
        File sdCard = Environment.getExternalStorageDirectory();
        File exportDir = new File(sdCard, BACKUP_DIRECTORY);
        // The return value is useless, since it combines the "directory
        // already exists" and error cases into one result. Instead, if
        // the directory creation fails, we rely on the FileOutputStream
        // initialization failing in the try block below, which will
        // throw an IOException.
        exportDir.mkdirs();
        File file = new File(exportDir, BACKUP_FILE_NAME);
        try (BackupExporter exporter = new BackupExporter(file)) {
            exporter.write(context, options);
        } catch (IOException e) {
            Xlog.e(TAG, "Export failed: could not write backup file", e);
            return EXPORT_WRITE_FAILED;
        }
        Xlog.i(TAG, "Export succeeded");
        return EXPORT_SUCCESS;
    }
}
