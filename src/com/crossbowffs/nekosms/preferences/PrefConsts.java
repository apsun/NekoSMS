package com.crossbowffs.nekosms.preferences;

import com.crossbowffs.nekosms.BuildConfig;

public class PrefConsts {
    private static final String NEKOSMS_PACKAGE = BuildConfig.APPLICATION_ID;
    public static final String FILE_MAIN = NEKOSMS_PACKAGE + "_preferences";
    public static final String REMOTE_PREFS_AUTHORITY = NEKOSMS_PACKAGE + ".preferences";

    public static final String KEY_ENABLE = "pref_enable";
    public static final String KEY_DEBUG_MODE = "pref_debug_mode";
    public static final String KEY_NOTIFICATIONS_ENABLE = "pref_notifications_enable";
    public static final String KEY_NOTIFICATIONS_RINGTONE = "pref_notifications_ringtone";
    public static final String KEY_NOTIFICATIONS_VIBRATE = "pref_notifications_vibrate";
    public static final String KEY_NOTIFICATIONS_LIGHTS = "pref_notifications_lights";
}
