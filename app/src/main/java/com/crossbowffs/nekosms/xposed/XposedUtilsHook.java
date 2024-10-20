package com.crossbowffs.nekosms.xposed;

import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.utils.Xlog;
import com.crossbowffs.nekosms.utils.XposedUtils;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedUtilsHook implements IXposedHookLoadPackage {
    private static final String NEKOSMS_PACKAGE = BuildConfig.APPLICATION_ID;
    private static final int MODULE_VERSION = BuildConfig.MODULE_VERSION;

    private static void hookXposedUtils(XC_LoadPackage.LoadPackageParam lpparam) {
        String className = XposedUtils.class.getName();

        Xlog.i("Hooking Xposed module status checker");

        // This is the version as fetched when the *module* is loaded
        // If the app is updated, this value will be changed within the
        // app, but will not be changed here. Thus, we can use this to
        // check whether the app and module versions are out of sync.
        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, "getModuleVersion",
            XC_MethodReplacement.returnConstant(MODULE_VERSION));
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (NEKOSMS_PACKAGE.equals(lpparam.packageName)) {
            try {
                hookXposedUtils(lpparam);
            } catch (Throwable e) {
                Xlog.e("Failed to hook Xposed module status checker", e);
                throw e;
            }
        }
    }
}
