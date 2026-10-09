package com.nextstepai.inventory.data.db

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppRoomDatabase>

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        // v1 -> v2: Add part_allocations table safely if not already present
        runCatching {
            connection.prepare("""
                CREATE TABLE IF NOT EXISTS part_allocations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL DEFAULT '',
                    partId INTEGER NOT NULL DEFAULT 0,
                    partUuid TEXT NOT NULL DEFAULT '',
                    allocatedQuantity REAL NOT NULL DEFAULT 0.0,
                    allocationType TEXT NOT NULL DEFAULT 'HARD',
                    referenceType TEXT NOT NULL DEFAULT 'BUILD_ORDER',
                    referenceId TEXT NOT NULL DEFAULT '',
                    referenceUuid TEXT NOT NULL DEFAULT '',
                    referenceTitle TEXT NOT NULL DEFAULT '',
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    createdAt INTEGER NOT NULL DEFAULT 0,
                    createdByUserId TEXT NOT NULL DEFAULT '1',
                    createdByUserUuid TEXT NOT NULL DEFAULT '',
                    notes TEXT,
                    version INTEGER NOT NULL DEFAULT 1,
                    syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                    isDeleted INTEGER NOT NULL DEFAULT 0,
                    updatedAt INTEGER NOT NULL DEFAULT 0,
                    lastModifiedByDeviceUuid TEXT
                )
            """.trimIndent()).use { it.step() }
        }
    }
}

private var instance: AppRoomDatabase? = null

fun getRoomDatabase(): AppRoomDatabase {
    return instance ?: synchronized(AppRoomDatabase::class) {
        instance ?: getDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_1_2)
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
            .also { instance = it }
    }
}
