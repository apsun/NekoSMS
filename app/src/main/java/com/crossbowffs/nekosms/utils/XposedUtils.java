package com.crossbowffs.nekosms.utils;

import com.crossbowffs.nekosms.BuildConfig;

public final class XposedUtils {
    private static final int MODULE_VERSION = BuildConfig.MODULE_VERSION;

    private XposedUtils() { }

    public static boolean isModuleEnabled() {
        return getModuleVersion() >= 0;
    }

    public static boolean isModuleUpdated() {
        return MODULE_VERSION != getModuleVersion();
    }

    private static int getModuleVersion() {
        // This method is hooked by the module to return the
        // value of BuildConfig.MODULE_VERSION, as seen from the
        // module side.
        return -1;
    }
}
