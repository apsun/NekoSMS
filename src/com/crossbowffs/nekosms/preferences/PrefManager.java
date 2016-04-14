package com.crossbowffs.nekosms.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PrefManager {
    private static final String DEFAULT_RINGTONE_URI = "content://settings/system/notification_sound";
    public static final PrefItem<Boolean> PREF_ENABLE = new PrefItem<>(PrefKeys.KEY_ENABLE, true);
    public static final PrefItem<Boolean> PREF_DEBUG_MODE = new PrefItem<>(PrefKeys.KEY_DEBUG_MODE, false);
    public static final PrefItem<Boolean> PREF_NOTIFICATIONS_ENABLE = new PrefItem<>(PrefKeys.KEY_NOTIFICATIONS_ENABLE, false);
    public static final PrefItem<String> PREF_NOTIFICATIONS_RINGTONE = new PrefItem<>(PrefKeys.KEY_NOTIFICATIONS_RINGTONE, DEFAULT_RINGTONE_URI);
    public static final PrefItem<Boolean> PREF_NOTIFICATIONS_VIBRATE = new PrefItem<>(PrefKeys.KEY_NOTIFICATIONS_VIBRATE, true);
    public static final PrefItem<Boolean> PREF_NOTIFICATIONS_LIGHTS = new PrefItem<>(PrefKeys.KEY_NOTIFICATIONS_LIGHTS, true);

    public static class Editor {
        private final SharedPreferences.Editor mEditor;

        private Editor(SharedPreferences.Editor editor) {
            mEditor = editor;
        }

        public Editor put(PrefItem<Boolean> pref, boolean value) {
            mEditor.putBoolean(pref.getKey(), value);
            return this;
        }

        public Editor put(PrefItem<String> pref, String value) {
            mEditor.putString(pref.getKey(), value);
            return this;
        }

        public void apply() {
            mEditor.apply();
        }
    }

    private final SharedPreferences mPreferences;

    public PrefManager(SharedPreferences preferences) {
        mPreferences = preferences;
    }

    public boolean getBoolean(PrefItem<Boolean> pref) {
        return mPreferences.getBoolean(pref.getKey(), pref.getDefaultValue());
    }

    public String getString(PrefItem<String> pref) {
        return mPreferences.getString(pref.getKey(), pref.getDefaultValue());
    }

    public Editor edit() {
        return new Editor(mPreferences.edit());
    }

    public static PrefManager fromContext(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return new PrefManager(preferences);
    }

    public static PrefManager fromContext(Context context, String name) {
        SharedPreferences preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        return new PrefManager(preferences);
    }
}
