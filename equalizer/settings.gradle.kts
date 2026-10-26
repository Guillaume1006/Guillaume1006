pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // Versions live here; a plugin is only downloaded by the module that applies it.
    plugins {
        kotlin("jvm") version "2.0.21"
        kotlin("android") version "2.0.21"
        kotlin("plugin.compose") version "2.0.21"
        id("com.android.application") version "8.7.3"
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "equalizer"

include(":core")

// The Android module needs the Android SDK; skip it where there is none
// (for example on a build server that only runs the core tests).
if (System.getenv("ANDROID_HOME") != null || file("local.properties").exists()) {
    include(":app")
}
