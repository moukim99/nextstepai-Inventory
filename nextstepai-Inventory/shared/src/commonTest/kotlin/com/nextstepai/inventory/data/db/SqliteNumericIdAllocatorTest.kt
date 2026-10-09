package com.nextstepai.inventory.data.db

import java.io.File
import kotlin.test.*
import kotlin.time.Clock

class SqliteNumericIdAllocatorTest {

    private lateinit var tempDbFile: File

    @BeforeTest
    fun setUp() {
        val runToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
        tempDbFile = File(System.getProperty("java.io.tmpdir"), "test_allocator_$runToken.db")
        if (tempDbFile.exists()) tempDbFile.delete()
        tempDbFile.deleteOnExit()
        SqliteDatabaseManager.setCustomDatabasePath(tempDbFile.absolutePath)
        // Ensure schema initialization
        SqliteDatabaseManager.getConnection()
    }

    @AfterTest
    fun tearDown() {
        SqliteDatabaseManager.closeDatabase()
        SqliteDatabaseManager.setCustomDatabasePath(null)
        runCatching { tempDbFile.delete() }
    }

    @Test
    fun testAllocationFromEmptyTableReturnsOne() {
        // Clear all parts just in case seeder populated any
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("DELETE FROM parts").use { it.step() }

        val nextId = SqliteNumericIdAllocator.nextId("parts", "part-")
        assertEquals(1L, nextId, "Allocation from an empty table must start at 1")
    }

    @Test
    fun testAllocationWithExistingSequentialRecords() {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("DELETE FROM parts").use { it.step() }

        // Insert part-1 to part-5
        for (i in 1..5) {
            conn.prepare("INSERT INTO parts (uuid, name, ipn) VALUES (?, ?, ?)").use { stmt ->
                stmt.bindText(1, "part-$i")
                stmt.bindText(2, "Part $i")
                stmt.bindText(3, "IPN-$i")
                stmt.step()
            }
        }

        val nextId = SqliteNumericIdAllocator.nextId("parts", "part-")
        assertEquals(6L, nextId, "Allocation with records 1..5 must return 6")
    }

    @Test
    fun testAllocationWithSparseIds() {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("DELETE FROM parts").use { it.step() }

        conn.prepare("INSERT INTO parts (uuid, name, ipn) VALUES (?, ?, ?)").use { stmt ->
            stmt.bindText(1, "part-10")
            stmt.bindText(2, "Part 10")
            stmt.bindText(3, "IPN-10")
            stmt.step()
        }

        val nextId = SqliteNumericIdAllocator.nextId("parts", "part-")
        assertEquals(11L, nextId, "Allocation with highest ID part-10 must return 11")
    }

    @Test
    fun testNonNumericUuidsAreIgnored() {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("DELETE FROM parts").use { it.step() }

        // Insert a non-numeric UUID
        conn.prepare("INSERT INTO parts (uuid, name, ipn) VALUES (?, ?, ?)").use { stmt ->
            stmt.bindText(1, "part-abc-xyz")
            stmt.bindText(2, "Non-numeric Part")
            stmt.bindText(3, "IPN-NON-NUMERIC")
            stmt.step()
        }

        val nextId = SqliteNumericIdAllocator.nextId("parts", "part-")
        assertEquals(1L, nextId, "Non-numeric suffixes should be ignored by the allocator")
    }

    @Test
    fun testUnsupportedTargetThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            SqliteNumericIdAllocator.nextId("unknown_table", "unknown-")
        }
    }

    @Test
    fun testAllocationAcrossMultipleSubEntities() {
        val conn = SqliteDatabaseManager.getConnection()

        // Companies
        conn.prepare("DELETE FROM companies").use { it.step() }
        assertEquals(1L, SqliteNumericIdAllocator.nextId("companies", "company-"))

        conn.prepare("INSERT INTO companies (uuid, name) VALUES ('company-42', 'Comp 42')").use { it.step() }
        assertEquals(43L, SqliteNumericIdAllocator.nextId("companies", "company-"))

        // Contacts
        conn.prepare("DELETE FROM contacts").use { it.step() }
        assertEquals(1L, SqliteNumericIdAllocator.nextId("contacts", "contact-"))

        conn.prepare("INSERT INTO contacts (uuid, companyUuid, name) VALUES ('contact-7', 'company-42', 'John')").use { it.step() }
        assertEquals(8L, SqliteNumericIdAllocator.nextId("contacts", "contact-"))

        // Addresses
        conn.prepare("DELETE FROM addresses").use { it.step() }
        assertEquals(1L, SqliteNumericIdAllocator.nextId("addresses", "address-"))

        conn.prepare("INSERT INTO addresses (uuid, companyUuid, line1) VALUES ('address-15', 'company-42', 'Road')").use { it.step() }
        assertEquals(16L, SqliteNumericIdAllocator.nextId("addresses", "address-"))

        // Supplier Price Breaks
        conn.prepare("DELETE FROM supplier_price_breaks").use { it.step() }
        assertEquals(1L, SqliteNumericIdAllocator.nextId("supplier_price_breaks", "price-break-"))

        conn.prepare("INSERT INTO supplier_price_breaks (uuid, supplierPartUuid, quantity, price) VALUES ('price-break-99', 'sup-1', 1.0, 5.0)").use { it.step() }
        assertEquals(100L, SqliteNumericIdAllocator.nextId("supplier_price_breaks", "price-break-"))
    }
}
