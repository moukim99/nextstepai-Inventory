package com.nextstepai.inventory.data.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import java.sql.DriverManager

/**
 * مدير دورة حياة واتصال قاعدة بيانات SQLite المحلية النظيفة (Database Connection & Lifecycle Manager).
 * يدير الفتح، الإغلاق، مسار الملف المخصص، وتنسيق التهيئة الآمنة عبر الوحدات الفرعية المعمارية.
 */
object SqliteDatabaseManager {
    private var customDatabasePath: String? = null
    private var connection: SQLiteConnection? = null

    fun setCustomDatabasePath(path: String?) {
        synchronized(this) {
            closeDatabase()
            customDatabasePath = path
        }
    }

    fun getActiveDatabasePath(): String {
        return customDatabasePath ?: getDatabasePath()
    }

    fun getConnection(): SQLiteConnection {
        return connection ?: synchronized(this) {
            connection ?: openDatabase().also { connection = it }
        }
    }

    fun isDatabaseOpen(): Boolean {
        synchronized(this) {
            return connection != null
        }
    }

    fun closeDatabase() {
        synchronized(this) {
            runCatching { connection?.close() }
            connection = null
        }
    }

    private fun openDatabase(): SQLiteConnection {
        val dbPath = getActiveDatabasePath()
        val dbFile = File(dbPath)

        val rawConn = try {
            val driver = BundledSQLiteDriver()
            driver.open(dbPath)
        } catch (e: Throwable) {
            runCatching {
                val jdbcConn = DriverManager.getConnection("jdbc:sqlite:$dbPath")
                JdbcSqliteConnection(jdbcConn)
            }.getOrElse {
                throw IllegalStateException(
                    "Unable to open the inventory database without risking data loss.",
                    e
                )
            }
        }

        return try {
            runCatching {
                rawConn.prepare("PRAGMA journal_mode = DELETE;").use { it.step() }
                rawConn.prepare("PRAGMA busy_timeout = 10000;").use { it.step() }
            }

            // Execute schema creation, migration, and default seeding as a single atomic unit.
            rawConn.prepare("BEGIN IMMEDIATE;").use { it.step() }
            try {
                // 1. إنشاء الجداول الأساسية إن لم تكن موجودة مسبقاً
                SqliteDatabaseSchema.createTables(rawConn)

                // 2. الترحيل التلقائي: تجنب الفشل في حال كان العمود أو الجدول موجوداً مسبقاً
                SqliteDatabaseMigrations.applyMigrations(rawConn)

                // 3. إدراج الإعدادات الافتراضية والبيانات الأولية بأمان
                SqliteDatabaseSeeder.seedDefaultsAndInitialData(rawConn)

                rawConn.prepare("COMMIT;").use { it.step() }
            } catch (initErr: Throwable) {
                runCatching { rawConn.prepare("ROLLBACK;").use { it.step() } }
                throw initErr
            }

            ThreadSafeSQLiteConnection(rawConn)
        } catch (e: Throwable) {
            try { rawConn.close() } catch (_: Throwable) {}
            throw IllegalStateException(
                "Inventory database initialization or migration failed; existing data was preserved.",
                e
            )
        }
    }
}

expect fun getDatabasePath(): String
