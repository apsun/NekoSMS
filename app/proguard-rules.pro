-dontobfuscate

# Keep so Xposed module can hook version info
-keep class com.crossbowffs.nekosms.utils.XposedUtils {
    int getModuleVersion();
}

# Keep so explicit intents can use class name
-keep class com.crossbowffs.nekosms.app.BlockedSmsReceiver

# Prevent ProGuard from deleting our Xposed hook classes
-keep class * implements de.robv.android.xposed.IXposedHookLoadPackage
-keep class * implements de.robv.android.xposed.IXposedHookZygoteInit
-keep class * implements de.robv.android.xposed.IXposedHookInitPackageResources
