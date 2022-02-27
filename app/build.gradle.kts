plugins {
    id("com.android.application")
}

dependencies {
    implementation("androidx.core:core:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.annotation:annotation:1.3.0")

    // Material design library is a complete PoS that has reliably fixed and introduced a
    // visible bug in every version, so lock ourselves to this alpha version that is new
    // enough to have the disappearing icon bug fixed but old enough to not have the
    // broken snackbar/FAB animation that was added in 1.1.0-alpha10.
    // See: https://github.com/material-components/material-components-android/issues/1036
    implementation("com.google.android.material:material:1.1.0-alpha09")

    implementation("com.crossbowffs.remotepreferences:remotepreferences:0.8")
    compileOnly("de.robv.android.xposed:api:53")
}

android {
    compileSdk = 31

    defaultConfig {
        versionCode = 39
        versionName = "0.20.0"
        minSdk = 19
        targetSdk = 31
        resourceConfigurations.addAll(listOf("en", "zh-rCN", "ru"))
        buildConfigField("int", "MODULE_VERSION", "18")
        buildConfigField("int", "DATABASE_VERSION", "11")
        buildConfigField("int", "BACKUP_VERSION", "3")
        buildConfigField("String", "LOG_TAG", "\"NekoSMS\"")
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("int", "LOG_LEVEL", "2")
            buildConfigField("boolean", "LOG_TO_XPOSED", "false")
        }

        getByName("release") {
            postprocessing {
                isRemoveUnusedCode = true
                isRemoveUnusedResources = true
                isObfuscate = false
                isOptimizeCode = true
                proguardFile("proguard-rules.pro")
            }
            buildConfigField("int", "LOG_LEVEL", "4")
            buildConfigField("boolean", "LOG_TO_XPOSED", "true")
        }
    }
}
