package com.crossbowffs.nekosms.app;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.preferences.PrefConsts;
import com.crossbowffs.nekosms.utils.XposedUtils;

public class SettingsFragment extends PreferenceFragment {
    private static final String TAG = SettingsFragment.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        if (!XposedUtils.isModuleEnabled()) {
            Preference enablePreference = findPreference(PrefConsts.KEY_ENABLE);
            enablePreference.setEnabled(false);
            enablePreference.setSummary(R.string.pref_enable_summary_alt);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MainActivity activity = ((MainActivity)getActivity());
        activity.setFabVisible(false);
        activity.setFabCallback(null);
        activity.setTitle(R.string.settings);
    }
}
