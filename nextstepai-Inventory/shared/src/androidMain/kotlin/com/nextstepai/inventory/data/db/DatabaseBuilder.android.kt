package com.nextstepai.inventory.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

object AppContextHolder {
    var appContext: Context? = null
}

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppRoomDatabase> {
    val ctx = AppContextHolder.appContext
        ?: throw IllegalStateException("AppContextHolder.appContext must be set before accessing Android Room Database")
    val dbFile = ctx.getDatabasePath("nextstepai_inventory.db")
    return Room.databaseBuilder<AppRoomDatabase>(
        context = ctx,
        name = dbFile.absolutePath
    )
}
