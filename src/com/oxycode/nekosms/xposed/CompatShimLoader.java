package com.oxycode.nekosms.xposed;

import com.oxycode.nekosms.xposed.shims.LenovoSafeCenterShim;
import de.robv.android.xposed.IXposedHookLoadPackage;

import java.util.ArrayList;
import java.util.List;

public final class CompatShimLoader {
    public static List<IXposedHookLoadPackage> getEnabledShims() {
        List<IXposedHookLoadPackage> shims = new ArrayList<IXposedHookLoadPackage>();
        shims.add(new LenovoSafeCenterShim());
        return shims;
    }
}
