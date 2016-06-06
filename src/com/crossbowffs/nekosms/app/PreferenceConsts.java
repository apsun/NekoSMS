package com.crossbowffs.nekosms.app;

import com.crossbowffs.nekosms.BuildConfig;

public final class PreferenceConsts {
    private static final String NEKOSMS_PACKAGE = BuildConfig.APPLICATION_ID;
    public static final String FILE_MAIN = NEKOSMS_PACKAGE + "_preferences";
    public static final String REMOTE_PREFS_AUTHORITY = NEKOSMS_PACKAGE + ".preferences";

    public static final String KEY_ENABLE = "pref_enable";
    public static final boolean KEY_ENABLE_DEFAULT = true;
    public static final String KEY_NOTIFICATIONS_ENABLE = "pref_notifications_enable";
    public static final boolean KEY_NOTIFICATIONS_ENABLE_DEFAULT = false;
    public static final String KEY_NOTIFICATIONS_RINGTONE = "pref_notifications_ringtone";
    public static final String KEY_NOTIFICATIONS_RINGTONE_DEFAULT = "content://settings/system/notification_sound";
    public static final String KEY_NOTIFICATIONS_VIBRATE = "pref_notifications_vibrate";
    public static final boolean KEY_NOTIFICATIONS_VIBRATE_DEFAULT = true;
    public static final String KEY_NOTIFICATIONS_LIGHTS = "pref_notifications_lights";
    public static final boolean KEY_NOTIFICATIONS_LIGHTS_DEFAULT = true;

    private PreferenceConsts() { }
}
