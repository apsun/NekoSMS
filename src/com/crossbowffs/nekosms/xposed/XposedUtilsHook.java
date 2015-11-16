package com.crossbowffs.nekosms.xposed;

import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.utils.Xlog;
import com.crossbowffs.nekosms.utils.XposedUtils;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedUtilsHook implements IXposedHookLoadPackage {
    private static final String TAG = XposedUtilsHook.class.getSimpleName();

    private static final String NEKOSMS_PACKAGE = BuildConfig.APPLICATION_ID;

    private static void hookXposedUtils(XC_LoadPackage.LoadPackageParam lpparam) {
        String className = XposedUtils.class.getName();

        Xlog.i(TAG, "Hooking Xposed module status checker");

        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, "isModuleEnabled",
            new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return true;
                }
            });

        // This is the version as fetched when the *module* is loaded
        // If the app is updated, this value will be changed within the
        // app, but will not be changed here. Thus, we can use this to
        // check whether the app and module versions are out of sync.
        final int moduleVersion = XposedUtils.getModuleVersion();

        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, "getModuleVersion",
            new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return moduleVersion;
                }
            });
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (NEKOSMS_PACKAGE.equals(lpparam.packageName)) {
            try {
                hookXposedUtils(lpparam);
            } catch (Throwable e) {
                Xlog.e(TAG, "Failed to hook Xposed module status checker", e);
                throw e;
            }
        }
    }
}
