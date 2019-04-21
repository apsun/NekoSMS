package com.crossbowffs.nekosms.backup;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.crossbowffs.nekosms.utils.Xlog;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class BackupLoader {
    private static final String BACKUP_FILE_NAME_FORMAT = "backup-%s.nsbak";
    private static final String BACKUP_MIME_TYPE = "application/json";

    private BackupLoader() { }

    private static String getDefaultBackupFileName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String dateStr = dateFormat.format(new Date());
        return String.format(BACKUP_FILE_NAME_FORMAT, dateStr);
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

    public static ExportResult exportFilterRules(Context context, Uri uri) {
        try (BackupExporter exporter = new BackupExporter(context.getContentResolver().openOutputStream(uri))) {
            exporter.write(context);
        } catch (IOException e) {
            Xlog.e("Export failed: could not write backup file", e);
            return ExportResult.WRITE_FAILED;
        }
        Xlog.i("Export succeeded");
        return ExportResult.SUCCESS;
    }

    public static Intent getImportFilePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        return intent;
    }

    public static Intent getExportFilePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(BACKUP_MIME_TYPE);
        intent.putExtra(Intent.EXTRA_TITLE, getDefaultBackupFileName());
        return intent;
    }
}
