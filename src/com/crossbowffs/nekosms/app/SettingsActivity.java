package com.crossbowffs.nekosms.app;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.PreferenceConsts;
import com.crossbowffs.nekosms.utils.XposedUtils;

public class SettingsActivity extends AppCompatActivity {
    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Xposed needs to be able to read the main toggle preference,
            // so we need to make the preferences world-readable.
            getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.settings);
            if (!XposedUtils.isModuleEnabled()) {
                Preference enablePreference = findPreference(PreferenceConsts.PREF_ENABLE);
                enablePreference.setEnabled(false);
                enablePreference.setSummary(R.string.pref_enable_summary_alt);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getFragmentManager()
            .beginTransaction()
            .replace(R.id.content_frame, new SettingsFragment())
            .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
