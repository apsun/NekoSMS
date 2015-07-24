package com.crossbowffs.nekosms.utils;

import android.content.Context;
import android.content.Intent;

public final class XposedUtils {
    // This number should be incremented every time the module is modified
    private static final int VERSION = 1;

    private static final String XPOSED_ACTION = "de.robv.android.xposed.installer.OPEN_SECTION";
    private static final String XPOSED_EXTRA_SECTION = "section";
    public static final String XPOSED_SECTION_MODULES = "modules";
    public static final String XPOSED_SECTION_INSTALL = "install";

    private XposedUtils() { }

    public static boolean isModuleEnabled() {
        // This method is hooked by the module to return true.
        return false;
    }

    public static int getAppVersion() {
        // This method is *NOT* hooked by the module; it must
        // be compared to the value of getModuleVersion()
        // to check whether the app and module are out of sync.
        return VERSION;
    }

    public static int getModuleVersion() {
        // This method is hooked by the module to return the
        // value of XposedUtils.VERSION, as seen from the
        // module side.
        return VERSION;
    }

    public static void startXposedActivity(Context context, String section) {
        Intent intent = new Intent(XPOSED_ACTION);
        intent.putExtra(XPOSED_EXTRA_SECTION, section);
        context.startActivity(intent);
    }
}
