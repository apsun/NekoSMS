-dontobfuscate

# This is technically not required since we don't obfuscate,
# but keep it anyways in case we want to do so in the future
-keep class com.crossbowffs.nekosms.utils.XposedUtils {
    int getModuleVersion();
}

# Prevent ProGuard from deleting our Xposed hook classes
-keep class * implements de.robv.android.xposed.IXposedMod
