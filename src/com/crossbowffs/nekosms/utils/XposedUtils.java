package com.crossbowffs.nekosms.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import com.crossbowffs.nekosms.BuildConfig;

import java.util.ArrayList;
import java.util.List;

public final class XposedUtils {
    public enum Section {
        INSTALL("install", 0),
        MODULES("modules", 1);

        private String mSection;
        private int mFragment;

        Section(String section, int fragment) {
            mSection = section;
            mFragment = fragment;
        }
    }

    private static final int MODULE_VERSION = BuildConfig.MODULE_VERSION;
    private static final String XPOSED_PACKAGE = "de.robv.android.xposed.installer";

    // Old Xposed installer
    private static final String XPOSED_OPEN_SECTION_ACTION = XPOSED_PACKAGE + ".OPEN_SECTION";
    private static final String XPOSED_EXTRA_SECTION = "section";

    // New Xposed installer
    private static final String XPOSED_ACTIVITY = XPOSED_PACKAGE + ".WelcomeActivity";
    private static final String XPOSED_EXTRA_FRAGMENT = "fragment";

    private static final String[] TASK_KILLER_PACKAGES = {
        "me.piebridge.forcestopgb",
        "com.oasisfeng.greenify",
    };

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

    public static boolean isXposedInstalled(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            packageManager.getPackageInfo(XPOSED_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private static boolean startOldXposedActivity(Context context, String section) {
        Intent intent = new Intent(XPOSED_OPEN_SECTION_ACTION);
        intent.putExtra(XPOSED_EXTRA_SECTION, section);
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    private static boolean startNewXposedActivity(Context context, int fragment) {
        Intent intent = new Intent();
        intent.setClassName(XPOSED_PACKAGE, XPOSED_ACTIVITY);
        intent.putExtra(XPOSED_EXTRA_FRAGMENT, fragment);
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    public static boolean startXposedActivity(Context context, Section section) {
        return startOldXposedActivity(context, section.mSection) ||
               startNewXposedActivity(context, section.mFragment);
    }

    public static List<PackageInfo> getInstalledTaskKillers(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ArrayList<PackageInfo> apps = new ArrayList<>();
        for (String pkgName : TASK_KILLER_PACKAGES) {
            PackageInfo pkgInfo;
            try {
                pkgInfo = packageManager.getPackageInfo(pkgName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }
            apps.add(pkgInfo);
        }
        return apps;
    }

    public static String getAppDisplayName(Context context, PackageInfo pkgInfo) {
        PackageManager packageManager = context.getPackageManager();
        CharSequence name = packageManager.getApplicationLabel(pkgInfo.applicationInfo);
        if (name != null) {
            return name.toString();
        } else {
            return pkgInfo.packageName;
        }
    }
}
