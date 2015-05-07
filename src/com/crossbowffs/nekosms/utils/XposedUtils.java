package com.crossbowffs.nekosms.utils;

public final class XposedUtils {
    // This number should be incremented every time the module
    // is modified
    private static final int VERSION = 1;

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
}
