package com.nextstepai.inventory.data.db

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppRoomDatabase> {
    val userHome = System.getProperty("user.home") ?: "."
    val appDir = File(userHome, ".nextstepai_inventory")
    if (!appDir.exists()) {
        appDir.mkdirs()
    }
    val dbFile = File(appDir, "nextstepai_inventory.db")
    return Room.databaseBuilder<AppRoomDatabase>(
        name = dbFile.absolutePath
    )
}
