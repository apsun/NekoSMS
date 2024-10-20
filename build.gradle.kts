buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.7.1")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()

        // TODO: Needed for Xposed API libs
        jcenter()
    }
}
