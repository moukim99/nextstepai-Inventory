import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(17)
    jvm()
    
    android {
       namespace = "com.nextstepai.inventory.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation("com.google.crypto.tink:tink-android:1.23.0")
            implementation("androidx.datastore:datastore-preferences:1.1.3")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(libs.compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation("org.xerial:sqlite-jdbc:3.49.1.0")
            implementation(libs.kotlinx.datetime)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.jetbrains.adaptive)
            implementation(libs.jetbrains.adaptive.layout)
            implementation(libs.jetbrains.adaptive.navigation)
            implementation(libs.jetbrains.adaptive.navigation.suite)
            implementation(libs.camerak)
            implementation("com.github.javakeyring:java-keyring:1.0.4")
        }
        getByName("jvmMain") {
            dependencies {
                implementation("com.github.javakeyring:java-keyring:1.0.4")
                implementation("net.java.dev.jna:jna:5.16.0")
                implementation("net.java.dev.jna:jna-platform:5.16.0")
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}