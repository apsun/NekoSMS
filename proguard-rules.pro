-dontobfuscate

# This is technically not required since we don't obfuscate,
# but keep it anyways in case we want to do so in the future
-keep class com.crossbowffs.nekosms.utils.XposedUtils {
    boolean isModuleEnabled();
    int getModuleVersion();
}

-keep class * implements de.robv.android.xposed.IXposedMod
