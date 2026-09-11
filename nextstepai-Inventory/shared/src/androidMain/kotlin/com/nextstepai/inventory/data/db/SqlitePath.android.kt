package com.nextstepai.inventory.data.db

import java.io.File

actual fun getDatabasePath(): String {
    val ctx = AppContextHolder.appContext
    if (ctx != null) {
        val dbFile = ctx.getDatabasePath("nextstepai_inventory.db")
        dbFile.parentFile?.mkdirs()
        return dbFile.absolutePath
    }
    val tmpDir = File(System.getProperty("java.io.tmpdir"), "nextstepai_inventory_test")
    if (!tmpDir.exists()) {
        tmpDir.mkdirs()
    }
    return File(tmpDir, "nextstepai_inventory.db").absolutePath
}
