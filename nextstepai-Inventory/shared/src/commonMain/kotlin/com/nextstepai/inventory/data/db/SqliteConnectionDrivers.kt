package com.nextstepai.inventory.data.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

/**
 * محوّل (Adapter) لتنفيذ أوامر SQLite عبر بروتوكول JDBC لبيئات JVM وDesktop.
 */
internal class JdbcSqliteConnection(private val conn: java.sql.Connection) : SQLiteConnection {
    override fun prepare(sql: String): SQLiteStatement {
        return JdbcSqliteStatement(conn.prepareStatement(sql))
    }

    override fun close() {
        conn.close()
    }
}

internal class JdbcSqliteStatement(private val stmt: PreparedStatement) : SQLiteStatement {
    private var resultSet: ResultSet? = null
    private var isQuery: Boolean? = null

    override fun step(): Boolean {
        if (isQuery == null) {
            val hasResultSet = try {
                stmt.execute()
            } catch (e: SQLException) {
                false
            }
            if (hasResultSet) {
                resultSet = stmt.resultSet
                isQuery = true
            } else {
                isQuery = false
            }
        }

        val rs = resultSet
        return if (rs != null) {
            rs.next()
        } else {
            false
        }
    }

    override fun bindText(index: Int, value: String) {
        stmt.setString(index, value)
    }

    override fun bindLong(index: Int, value: Long) {
        stmt.setLong(index, value)
    }

    override fun bindDouble(index: Int, value: Double) {
        stmt.setDouble(index, value)
    }

    override fun bindNull(index: Int) {
        stmt.setNull(index, Types.NULL)
    }

    override fun bindBlob(index: Int, value: ByteArray) {
        stmt.setBytes(index, value)
    }

    override fun getText(index: Int): String {
        return resultSet?.getString(index + 1) ?: ""
    }

    override fun getLong(index: Int): Long {
        return resultSet?.getLong(index + 1) ?: 0L
    }

    override fun getDouble(index: Int): Double {
        return resultSet?.getDouble(index + 1) ?: 0.0
    }

    override fun getBlob(index: Int): ByteArray {
        return resultSet?.getBytes(index + 1) ?: ByteArray(0)
    }

    override fun isNull(index: Int): Boolean {
        val rs = resultSet ?: return true
        val obj = rs.getObject(index + 1)
        return obj == null || rs.wasNull()
    }

    override fun getColumnCount(): Int {
        return resultSet?.metaData?.columnCount ?: 0
    }

    override fun getColumnName(index: Int): String {
        return resultSet?.metaData?.getColumnName(index + 1) ?: ""
    }

    override fun getColumnType(index: Int): Int {
        return resultSet?.metaData?.getColumnType(index + 1) ?: 0
    }

    override fun clearBindings() {
        stmt.clearParameters()
    }

    override fun reset() {
        resultSet?.close()
        resultSet = null
        isQuery = null
    }

    override fun close() {
        resultSet?.close()
        stmt.close()
    }
}

/**
 * غلاف اتصال آمن متعدد الخيوط (Thread-Safe Wrapper) يضمن قفل العمليات على مؤشر قاعدة البيانات
 * لمنع حدوث أعطال التزامن غير المتزامن على مستوى C / JNI.
 */
internal class ThreadSafeSQLiteConnection(
    private val delegate: SQLiteConnection
) : SQLiteConnection {
    private val lock = Any()

    override fun prepare(sql: String): SQLiteStatement {
        synchronized(lock) {
            val stmt = delegate.prepare(sql)
            return ThreadSafeSQLiteStatement(stmt, lock)
        }
    }

    override fun close() {
        synchronized(lock) {
            delegate.close()
        }
    }
}

internal class ThreadSafeSQLiteStatement(
    private val delegate: SQLiteStatement,
    private val lock: Any
) : SQLiteStatement {

    override fun step(): Boolean = synchronized(lock) { delegate.step() }
    override fun bindText(index: Int, value: String) = synchronized(lock) { delegate.bindText(index, value) }
    override fun bindLong(index: Int, value: Long) = synchronized(lock) { delegate.bindLong(index, value) }
    override fun bindDouble(index: Int, value: Double) = synchronized(lock) { delegate.bindDouble(index, value) }
    override fun bindNull(index: Int) = synchronized(lock) { delegate.bindNull(index) }
    override fun bindBlob(index: Int, value: ByteArray) = synchronized(lock) { delegate.bindBlob(index, value) }
    override fun clearBindings() = synchronized(lock) { delegate.clearBindings() }
    override fun getText(index: Int): String = synchronized(lock) { delegate.getText(index) }
    override fun getLong(index: Int): Long = synchronized(lock) { delegate.getLong(index) }
    override fun getDouble(index: Int): Double = synchronized(lock) { delegate.getDouble(index) }
    override fun getBlob(index: Int): ByteArray = synchronized(lock) { delegate.getBlob(index) }
    override fun isNull(index: Int): Boolean = synchronized(lock) { delegate.isNull(index) }
    override fun getColumnCount(): Int = synchronized(lock) { delegate.getColumnCount() }
    override fun getColumnName(index: Int): String = synchronized(lock) { delegate.getColumnName(index) }
    override fun getColumnType(index: Int): Int = synchronized(lock) { delegate.getColumnType(index) }
    override fun reset() = synchronized(lock) { delegate.reset() }
    override fun close() = synchronized(lock) { delegate.close() }
}
