package com.crossbowffs.nekosms.consts;

import com.crossbowffs.nekosms.BuildConfig;

public final class PreferenceConsts {
    private static final String NEKOSMS_PACKAGE = BuildConfig.APPLICATION_ID;
    public static final String FILE_MAIN = NEKOSMS_PACKAGE + "_preferences";
    public static final String FILE_INTERNAL = "internal_preferences";
    public static final String REMOTE_PREFS_AUTHORITY = NEKOSMS_PACKAGE + ".preferences";

    public static final String KEY_ENABLE = "pref_enable";
    public static final boolean KEY_ENABLE_DEFAULT = true;
    public static final String KEY_WHITELIST_CONTACTS = "pref_whitelist_contacts";
    public static final boolean KEY_WHITELIST_CONTACTS_DEFAULT = false;
    public static final String KEY_NOTIFICATIONS_ENABLE = "pref_notifications_enable";
    public static final boolean KEY_NOTIFICATIONS_ENABLE_DEFAULT = false;
    public static final String KEY_NOTIFICATIONS_RINGTONE = "pref_notifications_ringtone";
    public static final String KEY_NOTIFICATIONS_RINGTONE_DEFAULT = "content://settings/system/notification_sound";
    public static final String KEY_NOTIFICATIONS_VIBRATE = "pref_notifications_vibrate";
    public static final boolean KEY_NOTIFICATIONS_VIBRATE_DEFAULT = true;
    public static final String KEY_NOTIFICATIONS_LIGHTS = "pref_notifications_lights";
    public static final boolean KEY_NOTIFICATIONS_LIGHTS_DEFAULT = true;
    public static final String KEY_NOTIFICATIONS_PRIORITY = "pref_notifications_priority";
    public static final String KEY_NOTIFICATIONS_PRIORITY_DEFAULT = "0";

    public static final String KEY_APP_VERSION = "pref_app_version";
    public static final String KEY_SELECTED_SECTION = "pref_selected_section";
    public static final String KEY_KNOWN_TASK_KILLERS = "pref_known_task_killers";

    private PreferenceConsts() { }
}
