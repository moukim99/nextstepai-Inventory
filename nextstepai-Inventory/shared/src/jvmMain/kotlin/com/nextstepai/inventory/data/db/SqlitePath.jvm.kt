package com.nextstepai.inventory.data.db

import java.io.File

private var testDbPath: String? = null

actual fun getDatabasePath(): String {
    val isTestEnv = System.getProperty("org.gradle.test.worker") != null ||
            System.getProperty("inventory.test.db") != null
    if (isTestEnv) {
        val customTestDb = System.getProperty("inventory.test.db")
        if (!customTestDb.isNullOrBlank()) {
            return customTestDb
        }
        val existing = testDbPath
        if (existing != null) {
            return existing
        }
        val testDir = File(System.getProperty("java.io.tmpdir"), "nextstepai_inventory_test")
        if (!testDir.exists()) {
            testDir.mkdirs()
        }
        val workerId = System.getProperty("org.gradle.test.worker") ?: "default"
        val pid = ProcessHandle.current().pid()
        val dbFile = File(testDir, "test_inventory_${workerId}_$pid.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }
        dbFile.deleteOnExit()
        testDbPath = dbFile.absolutePath
        return dbFile.absolutePath
    }

    val userHome = System.getProperty("user.home") ?: "."
    val appDir = File(userHome, ".nextstepai_inventory")
    if (!appDir.exists()) {
        appDir.mkdirs()
    }
    return File(appDir, "nextstepai_inventory.db").absolutePath
}
