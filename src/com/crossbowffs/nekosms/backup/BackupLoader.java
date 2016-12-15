package com.crossbowffs.nekosms.backup;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.utils.Xlog;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

public final class BackupLoader {
    private static final String BACKUP_DIRECTORY = "NekoSMS";
    private static final String BACKUP_FILE_EXTENSION = ".nsbak";
    private static final String BACKUP_FILE_NAME_FORMAT = "backup-%s";
    private static final String BACKUP_MIME_TYPE = "application/json";
    private static final String FILE_AUTHORITY = BuildConfig.APPLICATION_ID + ".files";

    private BackupLoader() { }

    public static File getBackupDirectory() {
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

    public static File[] enumerateBackupFiles() {
        File backupDir = getBackupDirectory();

        // In the future we will only accept backup files with the
        // correct extension.
        File[] fileList = backupDir.listFiles(/* new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(BACKUP_FILE_EXTENSION);
            }
        } */);

        if (fileList != null) {
            // Currently file names are sorted purely lexicographically,
            // maybe we should use an alphanumeric sorting algorithm instead?
            Arrays.sort(fileList, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    return f1.getName().compareTo(f2.getName());
                }
            });
        }
        return fileList;
    }

    public static ImportResult importFilterRules(Context context, Uri uri) {
        try (BackupImporter importer = new BackupImporter(context.getContentResolver().openInputStream(uri))) {
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

    public static ExportResult exportFilterRules(Context context, File file) {
        File parentDir = file.getParentFile();
        if (parentDir != null) {
            // The return value is useless, since it combines the "directory
            // already exists" and error cases into one result. Instead, if
            // the directory creation fails, we rely on the FileOutputStream
            // initialization failing in the try block below, which will
            // throw an IOException.
            parentDir.mkdirs();
        }

        try (BackupExporter exporter = new BackupExporter(file)) {
            exporter.write(context);
        } catch (IOException e) {
            Xlog.e("Export failed: could not write backup file", e);
            return ExportResult.WRITE_FAILED;
        }
        Xlog.i("Export succeeded");
        return ExportResult.SUCCESS;
    }

    public static void shareBackupFile(Context context, File file) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        Uri contentUri = FileProvider.getUriForFile(context, FILE_AUTHORITY, file);
        intent.putExtra(Intent.EXTRA_STREAM, contentUri);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType(BACKUP_MIME_TYPE);
        context.startActivity(Intent.createChooser(intent, null));
    }
}
