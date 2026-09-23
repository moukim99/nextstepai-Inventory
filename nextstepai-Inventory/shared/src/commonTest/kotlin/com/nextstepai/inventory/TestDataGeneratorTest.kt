package com.nextstepai.inventory

import com.nextstepai.inventory.data.TestDataGenerator
import com.nextstepai.inventory.data.db.SqliteDatabaseManager
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestDataGeneratorTest {

    @Test
    fun testGenerateAndClearAllTestData() = runBlocking {
        // 1. توليد البيانات الاختبارية
        val generatedTables = TestDataGenerator.generateAllTestData()
        assertEquals(37, generatedTables)

        val conn = SqliteDatabaseManager.getConnection()

        // 2. التحقق من وجود البيانات في مختلف الجداول
        var partsCount = 0L
        conn.prepare("SELECT COUNT(*) FROM parts").use { stmt ->
            if (stmt.step()) partsCount = stmt.getLong(0)
        }
        assertTrue(partsCount >= 5L, "يجب أن تحتوي طاولة القطع على 5 قطع على الأقل")

        var categoriesCount = 0L
        conn.prepare("SELECT COUNT(*) FROM part_categories").use { stmt ->
            if (stmt.step()) categoriesCount = stmt.getLong(0)
        }
        assertTrue(categoriesCount >= 5L, "يجب أن تحتوي طاولة تصنيفات القطع على 5 تصنيفات")

        var stockCount = 0L
        conn.prepare("SELECT COUNT(*) FROM stock_items").use { stmt ->
            if (stmt.step()) stockCount = stmt.getLong(0)
        }
        assertTrue(stockCount >= 3L, "يجب أن تحتوي طاولة المخزون على 3 عناصر")

        var companiesCount = 0L
        conn.prepare("SELECT COUNT(*) FROM companies").use { stmt ->
            if (stmt.step()) companiesCount = stmt.getLong(0)
        }
        assertTrue(companiesCount >= 3L, "يجب أن تحتوي طاولة الشركات على 3 شركات")

        var buildOrdersCount = 0L
        conn.prepare("SELECT COUNT(*) FROM build_orders").use { stmt ->
            if (stmt.step()) buildOrdersCount = stmt.getLong(0)
        }
        assertTrue(buildOrdersCount >= 1L, "يجب أن تحتوي طاولة أوامر البناء على أمر واحد على الأقل")

        // 3. مسح البيانات الاختبارية
        TestDataGenerator.clearAllTestData()

        var partsAfterClear = 0L
        conn.prepare("SELECT COUNT(*) FROM parts").use { stmt ->
            if (stmt.step()) partsAfterClear = stmt.getLong(0)
        }
        assertEquals(0L, partsAfterClear, "يجب مسح كافة القطع بعد تفريغ البيانات")

        var stockAfterClear = 0L
        conn.prepare("SELECT COUNT(*) FROM stock_items").use { stmt ->
            if (stmt.step()) stockAfterClear = stmt.getLong(0)
        }
        assertEquals(0L, stockAfterClear, "يجب مسح كافة عناصر المخزون بعد تفريغ البيانات")

        var settingsCount = 0L
        conn.prepare("SELECT COUNT(*) FROM app_settings").use { stmt ->
            if (stmt.step()) settingsCount = stmt.getLong(0)
        }
        assertTrue(settingsCount >= 1L, "يجب أن تظل إعدادات التطبيق app_settings محفوظة دون مسح")
    }
}
