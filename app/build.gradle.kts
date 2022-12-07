plugins {
    id("com.android.application")
}

dependencies {
    implementation("androidx.core:core:1.9.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.annotation:annotation:1.5.0")
    implementation("androidx.loader:loader:1.1.0")
    implementation("androidx.fragment:fragment:1.5.4")
    implementation("androidx.preference:preference:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.drawerlayout:drawerlayout:1.1.1")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    // HACK: Workaround for https://issuetracker.google.com/issues/242384116
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation("com.google.android.material:material:1.7.0")
    implementation("com.crossbowffs.remotepreferences:remotepreferences:0.8")
    compileOnly("de.robv.android.xposed:api:53")
}

android {
    compileSdk = 33

    defaultConfig {
        versionCode = 39
        versionName = "0.20.0"
        minSdk = 19
        targetSdk = 33
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
