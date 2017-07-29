package com.crossbowffs.nekosms.app;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.consts.PreferenceConsts;
import com.crossbowffs.nekosms.utils.XposedUtils;

public class SettingsFragment extends PreferenceFragment {
    private static final String NEKOSMS_PACKAGE = BuildConfig.APPLICATION_ID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // General settings
        addPreferencesFromResource(R.xml.settings);
        if (!XposedUtils.isModuleEnabled()) {
            Preference enablePreference = findPreference(PreferenceConsts.KEY_ENABLE);
            enablePreference.setEnabled(false);
            enablePreference.setSummary(R.string.pref_enable_summary_alt);
        }

        // Notification settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            addPreferencesFromResource(R.xml.settings_notifications_v26);
            Preference settingsPreference = findPreference(PreferenceConsts.KEY_NOTIFICATIONS_OPEN_SETTINGS);
            settingsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, NEKOSMS_PACKAGE);
                    startActivity(intent);
                    return true;
                }
            });
        } else {
            addPreferencesFromResource(R.xml.settings_notifications);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MainActivity activity = ((MainActivity)getActivity());
        activity.disableFab();
        activity.setTitle(R.string.settings);
    }
}
