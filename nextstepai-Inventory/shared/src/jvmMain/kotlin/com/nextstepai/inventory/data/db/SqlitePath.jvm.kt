package com.nextstepai.inventory.data.db

import java.io.File

actual fun getDatabasePath(): String {
    val userHome = System.getProperty("user.home") ?: "."
    val appDir = File(userHome, ".nextstepai_inventory")
    if (!appDir.exists()) {
        appDir.mkdirs()
    }
    return File(appDir, "nextstepai_inventory.db").absolutePath
}
