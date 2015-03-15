package com.oxycode.nekosms.xposed.shims;

import android.content.res.Resources;
import com.oxycode.nekosms.utils.Xlog;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class LenovoSafeCenterShim implements IXposedHookLoadPackage {
    private static final String TAG = LenovoSafeCenterShim.class.getSimpleName();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        String className = "android.content.res.Resources";
        String methodName = "getBoolean";
        Class<?> param1Type = int.class;

        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, methodName,
            param1Type, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Resources res = (Resources)param.thisObject;
                    int resId = (Integer)param.args[0];
                    String resName = res.getResourceEntryName(resId);
                    if ("config_lenovo_safe_center_enabled".equals(resName)) {
                        Xlog.i(TAG, "Bypassing Lenovo SafeCenter");
                        param.setResult(false);
                    }
                }
            }
        );
    }
}
