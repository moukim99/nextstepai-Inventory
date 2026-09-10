// Remove conflicting ANDROID_PREFS_ROOT environment variable if ANDROID_USER_HOME is present
try {
    val pe = Class.forName("java.lang.ProcessEnvironment")
    val envField = pe.getDeclaredField("theEnvironment").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val env = envField.get(null) as? MutableMap<String, String>
    env?.remove("ANDROID_PREFS_ROOT")

    val cienvField = pe.getDeclaredField("theCaseInsensitiveEnvironment").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val cienv = cienvField.get(null) as? MutableMap<String, String>
    cienv?.remove("ANDROID_PREFS_ROOT")
} catch (_: Throwable) {}

rootProject.name = "nextstepai-Inventory"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":androidApp")
include(":desktopApp")
include(":shared")