package com.crossbowffs.nekosms.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.backup.BackupLoader;
import com.crossbowffs.nekosms.consts.PreferenceConsts;
import com.crossbowffs.nekosms.utils.AsyncUtils;
import com.crossbowffs.nekosms.utils.XposedUtils;

public class SettingsFragment extends PreferenceFragmentCompat implements OnNewArgumentsListener {
    private static final int PICK_RINGTONE_REQUEST = 1852;
    private static final int IMPORT_BACKUP_REQUEST = 1853;
    private static final int EXPORT_BACKUP_REQUEST = 1854;
    public static final String ARG_IMPORT_URI = "import_uri";

    private static final String NEKOSMS_PACKAGE = BuildConfig.APPLICATION_ID;
    private static final String VERSION_NAME = BuildConfig.VERSION_NAME;
    private static final int VERSION_CODE = BuildConfig.VERSION_CODE;
    private static final String GITHUB_URL = "https://github.com/apsun/NekoSMS";
    private static final String WIKI_URL = GITHUB_URL + "/wiki";

    @NonNull
    @SuppressWarnings("unchecked")
    private <T extends Preference> T requirePreference(CharSequence key) {
        Preference pref = findPreference(key);
        if (pref == null) {
            throw new IllegalStateException("Could not find preference with key " + key);
        }
        return (T)pref;
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        // General
        addPreferencesFromResource(R.xml.settings_general);
        if (!XposedUtils.isModuleEnabled()) {
            Preference enablePreference = requirePreference(PreferenceConsts.KEY_ENABLE);
            enablePreference.setEnabled(false);
            enablePreference.setSummary(R.string.pref_enable_summary_alt);
        }

        // Notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            addPreferencesFromResource(R.xml.settings_notifications_v26);
            Preference settingsPreference = requirePreference(PreferenceConsts.KEY_NOTIFICATIONS_OPEN_SETTINGS);
            settingsPreference.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, NEKOSMS_PACKAGE);
                startActivity(intent);
                return true;
            });
        } else {
            addPreferencesFromResource(R.xml.settings_notifications);
            RingtonePreference ringtonePreference = requirePreference(PreferenceConsts.KEY_NOTIFICATIONS_RINGTONE);
            ringtonePreference.setOnPreferenceClickListener(preference -> {
                Intent intent = ((RingtonePreference)preference).getRingtonePickerIntent();
                startActivityForResult(intent, PICK_RINGTONE_REQUEST);
                return true;
            });
        }

        // Backup
        addPreferencesFromResource(R.xml.settings_backup);
        requirePreference(PreferenceConsts.KEY_IMPORT_BACKUP).setOnPreferenceClickListener(preference -> {
            startActivityForResult(BackupLoader.getImportFilePickerIntent(), IMPORT_BACKUP_REQUEST);
            return true;
        });
        requirePreference(PreferenceConsts.KEY_EXPORT_BACKUP).setOnPreferenceClickListener(preference -> {
            startActivityForResult(BackupLoader.getExportFilePickerIntent(), EXPORT_BACKUP_REQUEST);
            return true;
        });

        // About
        addPreferencesFromResource(R.xml.settings_about);
        requirePreference(PreferenceConsts.KEY_ABOUT_HELP).setOnPreferenceClickListener(preference -> {
            startBrowserActivity(WIKI_URL);
            return true;
        });
        requirePreference(PreferenceConsts.KEY_ABOUT_GITHUB).setOnPreferenceClickListener(preference -> {
            startBrowserActivity(GITHUB_URL);
            return true;
        });
        String versionSummary = getString(R.string.format_pref_about_version_summary, VERSION_NAME, VERSION_CODE);
        requirePreference(PreferenceConsts.KEY_ABOUT_VERSION).setSummary(versionSummary);

        // Handle import requests as necessary
        onNewArguments(getArguments());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MainActivity activity = (MainActivity)requireActivity();

        activity.setTitle(R.string.settings);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == PICK_RINGTONE_REQUEST) {
            RingtonePreference ringtonePreference = requirePreference(PreferenceConsts.KEY_NOTIFICATIONS_RINGTONE);
            ringtonePreference.onRingtonePickerResult(data);
        } else if (requestCode == IMPORT_BACKUP_REQUEST) {
            importFilterRules(data.getData());
        } else if (requestCode == EXPORT_BACKUP_REQUEST) {
            exportFilterRules(data.getData());
        }
    }

    @Override
    public void onNewArguments(Bundle args) {
        if (args == null) {
            return;
        }

        Uri importUri = args.getParcelable(ARG_IMPORT_URI);
        if (importUri != null) {
            args.remove(ARG_IMPORT_URI);
            showConfirmImportDialog(importUri);
        }
    }

    private void showConfirmImportDialog(final Uri uri) {
        Context context = requireActivity();

        new AlertDialog.Builder(context)
            .setIcon(R.drawable.ic_warning_24dp)
            .setTitle(R.string.import_confirm_title)
            .setMessage(R.string.import_confirm_message)
            .setPositiveButton(R.string.backup_button_import, (dialog, which) -> importFilterRules(uri))
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void importFilterRules(final Uri uri) {
        Context appContext = requireContext().getApplicationContext();

        AsyncUtils.run(
            () -> BackupLoader.importFilterRules(appContext, uri),
            (result) -> {
                int messageId;
                switch (result) {
                case SUCCESS:
                    messageId = R.string.import_success;
                    break;
                case UNKNOWN_VERSION:
                    messageId = R.string.import_unknown_version;
                    break;
                case INVALID_BACKUP:
                    messageId = R.string.import_invalid_backup;
                    break;
                case READ_FAILED:
                    messageId = R.string.import_read_failed;
                    break;
                default:
                    throw new AssertionError("Unknown backup import result code: " + result);
                }

                MainActivity activity = (MainActivity)getActivity();
                if (activity != null) {
                    activity.makeSnackbar(messageId).show();
                }
            });
    }

    private void exportFilterRules(final Uri uri) {
        Context appContext = requireContext().getApplicationContext();

        AsyncUtils.run(
            () -> BackupLoader.exportFilterRules(appContext, uri),
            (result) -> {
                int messageId;
                switch (result) {
                case SUCCESS:
                    messageId = R.string.export_success;
                    break;
                case WRITE_FAILED:
                    messageId = R.string.export_write_failed;
                    break;
                default:
                    throw new AssertionError("Unknown backup export result code: " + result);
                }

                MainActivity activity = (MainActivity)getActivity();
                if (activity != null) {
                    activity.makeSnackbar(messageId).show();
                }
            });
    }

    private void startBrowserActivity(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}
