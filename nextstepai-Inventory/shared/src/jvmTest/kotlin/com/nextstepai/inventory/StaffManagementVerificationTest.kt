package com.nextstepai.inventory

import com.nextstepai.inventory.auth.PasswordHasher
import com.nextstepai.inventory.data.db.*
import java.io.File
import kotlin.test.*
import kotlin.time.Clock

/**
 * اختبارات التحقق الشاملة لمنظومة إدارة الموظفين والصلاحيات وأكواد التفعيل المدمجة.
 */
class StaffManagementVerificationTest {

    private lateinit var tempDbFile: File

    @BeforeTest
    fun setUp() {
        val runToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
        tempDbFile = File(System.getProperty("java.io.tmpdir"), "test_staff_mgmt_$runToken.db")
        if (tempDbFile.exists()) tempDbFile.delete()
        tempDbFile.deleteOnExit()
        SqliteDatabaseManager.setCustomDatabasePath(tempDbFile.absolutePath)
    }

    @AfterTest
    fun tearDown() {
        SqliteDatabaseManager.closeDatabase()
        if (::tempDbFile.isInitialized && tempDbFile.exists()) {
            tempDbFile.delete()
        }
    }

    @Test
    fun testPasswordHasherHashingAndVerification() {
        val password = "StrongPassword@2026"
        val hashedPassword = PasswordHasher.hashPassword(password)

        assertTrue(hashedPassword.isNotBlank())
        assertTrue(hashedPassword.startsWith("pbkdf2_sha256$"))

        assertTrue(PasswordHasher.verifyPassword(password, hashedPassword))
        assertFalse(PasswordHasher.verifyPassword("WrongPassword", hashedPassword))
        assertFalse(PasswordHasher.verifyPassword("", hashedPassword))
        assertFalse(PasswordHasher.verifyPassword(password, "invalid_hash_string"))
    }

    @Test
    fun testStaffManagementDaoFullLifecycle() {
        val staffDao = StaffManagementDao()
        val appUserDao = AppUserDao()
        val ownerId = "OWNER-TEST"

        // 1. إضافة موظف والبحث عنه
        val userUuid = "usr-test-101"
        val userEntity = AppUserEntity(
            uuid = userUuid,
            ownerId = ownerId,
            name = "أحمد مهندس خط الإنتاج",
            phoneNumber = "+966500000001",
            passwordHash = PasswordHasher.hashPassword("secret123"),
            role = "PRODUCTION_OPERATOR",
            active = true
        )
        appUserDao.insertOrUpdate(userEntity)

        val retrievedByPhone = appUserDao.getUserByPhone("+966500000001")
        assertNotNull(retrievedByPhone)
        assertEquals("أحمد مهندس خط الإنتاج", retrievedByPhone.name)
        assertTrue(PasswordHasher.verifyPassword("secret123", retrievedByPhone.passwordHash))

        val usersList = staffDao.getUsersByOwner(ownerId)
        assertTrue(usersList.any { it.uuid == userUuid })

        // 2. تحديث بيانات الموظف
        staffDao.updateAppUser(userUuid, "أحمد - كبير الفنيين", "LEAD_OPERATOR")
        val updatedUser = appUserDao.getUserByPhone("+966500000001")
        assertNotNull(updatedUser)
        assertEquals("أحمد - كبير الفنيين", updatedUser.name)
        assertEquals("LEAD_OPERATOR", updatedUser.role)

        // 3. تبديل حالة النشاط
        staffDao.setUserActive(userUuid, false)
        val inactiveUser = staffDao.getUsersByOwner(ownerId).firstOrNull { it.uuid == userUuid }
        assertNotNull(inactiveUser)
        assertFalse(inactiveUser.active)

        // 4. اختبار الصلاحيات التفصيلية
        val permissions = listOf(
            UserPermissionEntity(
                uuid = "perm-1",
                ownerId = ownerId,
                userUuid = userUuid,
                module = "WAREHOUSE",
                canCreate = true,
                canRead = true,
                canUpdate = false,
                canDelete = false
            ),
            UserPermissionEntity(
                uuid = "perm-2",
                ownerId = ownerId,
                userUuid = userUuid,
                module = "PRODUCTION",
                canCreate = true,
                canRead = true,
                canUpdate = true,
                canDelete = false
            )
        )
        staffDao.saveUserPermissions(permissions)
        val loadedPerms = staffDao.getUserPermissions(userUuid)
        assertEquals(2, loadedPerms.size)
        val warehousePerm = loadedPerms.find { it.module == "WAREHOUSE" }
        assertNotNull(warehousePerm)
        assertTrue(warehousePerm.canCreate)
        assertTrue(warehousePerm.canRead)
        assertFalse(warehousePerm.canUpdate)

        // 5. إنشاء كود تفعيل
        val codeEntity = ActivationCodeEntity(
            code = "ACT-7788",
            ownerId = ownerId,
            assignedRole = "STOREKEEPER",
            label = "كود أمين مستودع",
            permissionsJson = "{\"WAREHOUSE\":{\"read\":true,\"create\":true}}"
        )
        staffDao.insertActivationCode(codeEntity)

        val activeCodes = staffDao.getActiveInvitationCodes(ownerId)
        assertTrue(activeCodes.any { it.code == "ACT-7788" })

        val fetchedCode = appUserDao.getActivationCode("ACT-7788")
        assertNotNull(fetchedCode)
        assertEquals("كود أمين مستودع", fetchedCode.label)
        assertFalse(fetchedCode.isUsed)

        // 6. استخدام كود التفعيل
        appUserDao.markActivationCodeUsed("ACT-7788", userUuid)
        val codeAfterUse = appUserDao.getActivationCode("ACT-7788")
        assertNotNull(codeAfterUse)
        assertTrue(codeAfterUse.isUsed)
        assertEquals(userUuid, codeAfterUse.usedByUuid)

        // 7. حذف الموظف (Soft Delete)
        staffDao.deleteUser(userUuid)
        val usersAfterDelete = staffDao.getUsersByOwner(ownerId)
        assertFalse(usersAfterDelete.any { it.uuid == userUuid })
    }
}
