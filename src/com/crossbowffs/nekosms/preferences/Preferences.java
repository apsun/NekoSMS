package com.crossbowffs.nekosms.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {
    public static final String KEY_ENABLE = "pref_enable";
    public static final String KEY_DEBUG_MODE = "pref_debug_mode";
    public static final String KEY_NOTIFICATIONS_ENABLE = "pref_notifications_enable";
    public static final String KEY_NOTIFICATIONS_SOUND = "pref_notifications_sound";
    public static final String KEY_NOTIFICATIONS_VIBRATE = "pref_notifications_vibrate";
    public static final String KEY_NOTIFICATIONS_LIGHTS = "pref_notifications_lights";

    public static final BooleanPreference PREF_ENABLE = new BooleanPreference(KEY_ENABLE, true);
    public static final BooleanPreference PREF_DEBUG_MODE = new BooleanPreference(KEY_DEBUG_MODE, false);
    public static final BooleanPreference PREF_NOTIFICATIONS_ENABLE = new BooleanPreference(KEY_NOTIFICATIONS_ENABLE, false);
    public static final BooleanPreference PREF_NOTIFICATIONS_SOUND = new BooleanPreference(KEY_NOTIFICATIONS_SOUND, true);
    public static final BooleanPreference PREF_NOTIFICATIONS_VIBRATE = new BooleanPreference(KEY_NOTIFICATIONS_VIBRATE, true);
    public static final BooleanPreference PREF_NOTIFICATIONS_LIGHTS = new BooleanPreference(KEY_NOTIFICATIONS_LIGHTS, true);

    public static class Editor {
        private final SharedPreferences.Editor mEditor;

        private Editor(SharedPreferences.Editor editor) {
            mEditor = editor;
        }

        public Editor put(BooleanPreference pref, boolean value) {
            mEditor.putBoolean(pref.getKey(), value);
            return this;
        }

        public void apply() {
            mEditor.apply();
        }
    }

    private final SharedPreferences mPreferences;

    public Preferences(SharedPreferences preferences) {
        mPreferences = preferences;
    }

    public boolean get(BooleanPreference pref) {
        return mPreferences.getBoolean(pref.getKey(), pref.getDefaultValue());
    }

    public Editor edit() {
        return new Editor(mPreferences.edit());
    }

    public static Preferences fromContext(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return new Preferences(preferences);
    }
}
