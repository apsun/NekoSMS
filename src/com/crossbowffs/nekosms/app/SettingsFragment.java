package com.crossbowffs.nekosms.app;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.preferences.PrefManager;
import com.crossbowffs.nekosms.utils.Xlog;
import com.crossbowffs.nekosms.utils.XposedUtils;

public class SettingsFragment extends PreferenceFragment {
    private static final String TAG = SettingsFragment.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Xposed needs to be able to read the main toggle preference,
        // so we need to make the preferences world-readable.
        // TODO: Replace with ContentProvider
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.settings);
        if (!XposedUtils.isModuleEnabled()) {
            Preference enablePreference = findPreference(PrefManager.KEY_ENABLE);
            enablePreference.setEnabled(false);
            enablePreference.setSummary(R.string.pref_enable_summary_alt);
        }

        bindRingtoneChangeListener();

        MainActivity activity = ((MainActivity)getActivity());
        activity.setFabVisible(false);
        activity.setFabCallback(null);
        activity.setTitle(R.string.settings);
    }

    private void bindRingtoneChangeListener() {
        Preference pref = findPreference(PrefManager.KEY_NOTIFICATIONS_RINGTONE);
        Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                String stringValue = value.toString();
                if (TextUtils.isEmpty(stringValue)) {
                    preference.setSummary(R.string.pref_notifications_ringtone_none);
                } else {
                    Uri uri = Uri.parse(stringValue);
                    Ringtone ringtone = RingtoneManager.getRingtone(preference.getContext(), uri);
                    if (ringtone == null) {
                        Xlog.e(TAG, "Failed to load ringtone: %s", uri);
                        preference.setSummary(R.string.pref_notifications_ringtone_none);
                    } else {
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }
                return true;
            }
        };
        pref.setOnPreferenceChangeListener(listener);

        // Force the initial update
        PrefManager preferences = PrefManager.fromContext(pref.getContext());
        String ringtoneString = preferences.getString(PrefManager.PREF_NOTIFICATIONS_RINGTONE);
        listener.onPreferenceChange(pref, ringtoneString);
    }
}
