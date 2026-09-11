package com.nextstepai.inventory.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppRoomDatabase>

private var instance: AppRoomDatabase? = null

fun getRoomDatabase(): AppRoomDatabase {
    return instance ?: synchronized(AppRoomDatabase::class) {
        instance ?: getDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
            .also { instance = it }
    }
}
