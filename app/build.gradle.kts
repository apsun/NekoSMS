plugins {
    id("com.android.application")
}

dependencies {
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.annotation:annotation:1.7.0")
    implementation("androidx.loader:loader:1.1.0")
    implementation("androidx.fragment:fragment:1.6.1")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.google.android.material:material:1.10.0")
    implementation("com.crossbowffs.remotepreferences:remotepreferences:0.8")
    compileOnly("de.robv.android.xposed:api:53")
}

android {
    namespace = "com.crossbowffs.nekosms"
    compileSdk = 34

    defaultConfig {
        versionCode = 42
        versionName = "0.22.0"
        minSdk = 19
        targetSdk = 34
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
