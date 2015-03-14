package com.oxycode.nekosms.xposed.compat;

import android.os.Build;
import com.oxycode.nekosms.utils.Xlog;
import com.oxycode.nekosms.xposed.compat.devices.CompatShimXT1085;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CompatShim implements IXposedHookLoadPackage {
    private static final class AggregateShim extends CompatShim {
        private final List<? extends CompatShim> _shims;

        public AggregateShim(List<CompatShim> shims) {
            _shims = shims;
        }

        @Override
        public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
            for (CompatShim shim : _shims) {
                shim.handleLoadPackage(lpparam);
            }
        }
    }

    private static final class EmptyShim extends CompatShim {
        @Override
        public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable { }
    }

    private static final List<Class<? extends CompatShim>> SHIM_CLASSES;
    private static transient CompatShim SHIM;

    static {
        SHIM_CLASSES = new ArrayList<Class<? extends CompatShim>>();
        SHIM_CLASSES.add(CompatShimXT1085.class);
    }

    private static boolean shimTargetsDevice(Class<? extends CompatShim> shimClass,
                                             String packageName) {
        CompatShimTarget shimTarget = shimClass.getAnnotation(CompatShimTarget.class);
        String shimTargetPackage = shimTarget.packageName();
        String shimTargetManufacturer = shimTarget.manufacturer();
        String[] shimTargetModels = shimTarget.models();

        if (!shimTargetPackage.equals("") && !shimTargetPackage.equals(packageName)) {
            return false;
        }

        if (!shimTargetManufacturer.equals("") && !shimTargetManufacturer.equals(Build.MANUFACTURER)) {
            return false;
        }

        if (shimTargetModels.length > 0 && Arrays.asList(shimTargetModels).indexOf(Build.MODEL) < 0) {
            return false;
        }

        return true;
    }

    private static CompatShim instantiateShim(Class<? extends CompatShim> shimClass) {
        try {
            return shimClass.newInstance();
        } catch (InstantiationException e) {
            throw new AssertionError(e);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static CompatShim createShim(XC_LoadPackage.LoadPackageParam lpparam) {
        CompatShim shim = null;
        ArrayList<CompatShim> shims = null;

        for (Class<? extends CompatShim> shimClass : SHIM_CLASSES) {
            if (shimTargetsDevice(shimClass, lpparam.packageName)) {
                Xlog.i("Loading compatibility shim: %s", shimClass.getSimpleName());
                if (shims != null) {
                    shims.add(instantiateShim(shimClass));
                } else if (shim != null) {
                    shims = new ArrayList<CompatShim>();
                    shims.add(shim);
                    shims.add(instantiateShim(shimClass));
                } else {
                    shim = instantiateShim(shimClass);
                }
            }
        }

        if (shims != null) {
            return new AggregateShim(shims);
        } else if (shim != null) {
            return shim;
        } else {
            return new EmptyShim();
        }
    }

    public static void loadShims(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (SHIM == null) {
            SHIM = createShim(lpparam);
        }

        SHIM.handleLoadPackage(lpparam);
    }
}
