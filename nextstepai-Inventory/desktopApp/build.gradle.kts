import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.File

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.nextstepai.inventory.MainKt"

        val currentJavaHome = File(System.getProperty("java.home"))
        val hasJpackage = File(currentJavaHome, "bin/jpackage.exe").exists() || File(currentJavaHome, "bin/jpackage").exists()
        if (!hasJpackage) {
            val jdksDir = File(System.getProperty("user.home"), ".gradle/jdks")
            val jpackageExe = jdksDir.walkTopDown().firstOrNull { 
                it.name == "jpackage.exe" || it.name == "jpackage" 
            }
            jpackageExe?.parentFile?.parentFile?.let { jdkDir ->
                if (jdkDir.exists()) {
                    javaHome = jdkDir.absolutePath
                }
            }
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "NextStep"
            packageVersion = "1.0.0"

            description = "NextStep AI Inventory System"
            copyright = "© 2025 NextStep AI. All rights reserved."
            vendor = "NextStep AI"

            macOS {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
                shortcut = true
                menu = true
                menuGroup = "NextStep AI"
                dirChooser = true
                perUserInstall = true
            }
        }
    }
}