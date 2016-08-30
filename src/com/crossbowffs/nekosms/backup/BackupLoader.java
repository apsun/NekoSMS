package com.crossbowffs.nekosms.backup;

import android.content.Context;
import android.os.Environment;
import com.crossbowffs.nekosms.utils.Xlog;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public final class BackupLoader {
    private static final String BACKUP_DIRECTORY = "NekoSMS";
    private static final String BACKUP_FILE_EXTENSION = ".json";
    private static final String BACKUP_FILE_NAME_FORMAT = "backup-%s";

    private BackupLoader() { }

    private static File getBackupDirectory() {
        File sdCard = Environment.getExternalStorageDirectory();
        return new File(sdCard, BACKUP_DIRECTORY);
    }

    public static String getBackupFileExtension() {
        return BACKUP_FILE_EXTENSION;
    }

    public static String getDefaultBackupFileName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = dateFormat.format(new Date());
        File backupDir = getBackupDirectory();
        String baseName = String.format(BACKUP_FILE_NAME_FORMAT, dateStr);
        String testName = baseName + BACKUP_FILE_EXTENSION;
        int i = 0;
        while (new File(backupDir, testName).exists()) {
            testName = baseName + "-" + (++i) + BACKUP_FILE_EXTENSION;
        }
        return testName;
    }

    public static String[] enumerateBackupFileNames() {
        File backupDir = getBackupDirectory();
        String[] fileList = backupDir.list();
        if (fileList != null) {
            // Currently file names are sorted purely lexicographically,
            // maybe we should use an alphanumeric sorting algorithm instead?
            Arrays.sort(fileList);
        }
        return fileList;
    }

    public static ImportResult importFromStorage(Context context, String fileName) {
        switch (Environment.getExternalStorageState()) {
        case Environment.MEDIA_MOUNTED:
        case Environment.MEDIA_MOUNTED_READ_ONLY:
            break;
        default:
            Xlog.e("Import failed: cannot read from external storage");
            return ImportResult.CANNOT_READ_STORAGE;
        }

        File file = new File(getBackupDirectory(), fileName);
        try (BackupImporter importer = new BackupImporter(file)) {
            importer.read(context);
        } catch (IOException e) {
            Xlog.e("Import failed: could not read backup file", e);
            return ImportResult.READ_FAILED;
        } catch (BackupVersionException e) {
            Xlog.e("Import failed: unknown backup version", e);
            return ImportResult.UNKNOWN_VERSION;
        } catch (InvalidBackupException e) {
            Xlog.e("Import failed: invalid backup file", e);
            return ImportResult.INVALID_BACKUP;
        }
        Xlog.i("Import succeeded");
        return ImportResult.SUCCESS;
    }

    public static ExportResult exportToStorage(Context context, String fileName) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Xlog.e("Export failed: cannot write to external storage");
            return ExportResult.CANNOT_WRITE_STORAGE;
        }

        File backupDir = getBackupDirectory();
        // The return value is useless, since it combines the "directory
        // already exists" and error cases into one result. Instead, if
        // the directory creation fails, we rely on the FileOutputStream
        // initialization failing in the try block below, which will
        // throw an IOException.
        backupDir.mkdirs();
        File file = new File(backupDir, fileName);
        try (BackupExporter exporter = new BackupExporter(file)) {
            exporter.write(context);
        } catch (IOException e) {
            Xlog.e("Export failed: could not write backup file", e);
            return ExportResult.WRITE_FAILED;
        }
        Xlog.i("Export succeeded");
        return ExportResult.SUCCESS;
    }
}
