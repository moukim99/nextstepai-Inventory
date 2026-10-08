package com.nextstepai.inventory.data.db

import java.io.File

actual fun getDatabasePath(): String {
    val isTestEnv = System.getProperty("org.gradle.test.worker") != null ||
            System.getProperty("inventory.test.db") != null
    if (isTestEnv) {
        val testDir = File(System.getProperty("java.io.tmpdir"), "nextstepai_inventory_test")
        if (!testDir.exists()) {
            testDir.mkdirs()
        }
        return File(testDir, "test_inventory.db").absolutePath
    }

    val userHome = System.getProperty("user.home") ?: "."
    val appDir = File(userHome, ".nextstepai_inventory")
    if (!appDir.exists()) {
        appDir.mkdirs()
    }
    return File(appDir, "nextstepai_inventory.db").absolutePath
}
