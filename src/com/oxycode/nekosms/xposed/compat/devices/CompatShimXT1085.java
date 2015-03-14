package com.oxycode.nekosms.xposed.compat.devices;

import android.content.res.Resources;
import com.oxycode.nekosms.utils.Xlog;
import com.oxycode.nekosms.xposed.compat.CompatShim;
import com.oxycode.nekosms.xposed.compat.CompatShimTarget;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

@CompatShimTarget(packageName = "com.android.phone", manufacturer = "motorola", models = "XT1085")
public class CompatShimXT1085 extends CompatShim {
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        String className = "android.content.res.Resources";
        String methodName = "getBoolean";
        Class<?> param1Type = int.class;

        findAndHookMethod(className, lpparam.classLoader, methodName, param1Type, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Resources res = (Resources)param.thisObject;
                int resId = (Integer)param.args[0];
                if ("config_lenovo_safe_center_enabled".equals(res.getResourceEntryName(resId))) {
                    Xlog.i("XT1085: Bypassing Lenovo SafeCenter");
                    param.setResult(false);
                }
            }
        });
    }
}
