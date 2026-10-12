package com.nextstepai.inventory

import com.nextstepai.inventory.auth.PasswordHasher
import com.nextstepai.inventory.data.db.*
import com.nextstepai.inventory.ui.StaffManagementViewModel
import com.nextstepai.inventory.util.StaffLabelPdfGenerator
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
        assertFalse(PasswordHasher.verifyPassword(password, "pbkdf2_sha256\$0\$c2FsdA==\$aGFzaA=="))
        assertFalse(PasswordHasher.verifyPassword(password, "pbkdf2_sha256\$-100\$c2FsdA==\$aGFzaA=="))
        assertFalse(PasswordHasher.verifyPassword(password, "pbkdf2_sha256\$5000000\$c2FsdA==\$aGFzaA=="))
        assertFalse(PasswordHasher.verifyPassword(password, "pbkdf2_sha256\$120000\$\$aGFzaA=="))
        assertFalse(PasswordHasher.verifyPassword(password, "pbkdf2_sha256\$120000\$c2FsdA==\$"))
        assertFalse(PasswordHasher.verifyPassword(password, "pbkdf2_sha256\$120000\$\$"))
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

        // تحديث صلاحية بمعرف جديد مع نفس (userUuid, module) يجب أن يستبدلها دون تكرار
        staffDao.saveUserPermissions(
            listOf(
                UserPermissionEntity(
                    uuid = "perm-1-updated-uuid",
                    ownerId = ownerId,
                    userUuid = userUuid,
                    module = "WAREHOUSE",
                    canCreate = true,
                    canRead = true,
                    canUpdate = true,
                    canDelete = true
                )
            )
        )
        val permsAfterReplacement = staffDao.getUserPermissions(userUuid)
        assertEquals(2, permsAfterReplacement.size)
        val updatedWarehouse = permsAfterReplacement.find { it.module == "WAREHOUSE" }
        assertNotNull(updatedWarehouse)
        assertTrue(updatedWarehouse.canUpdate)
        assertTrue(updatedWarehouse.canDelete)

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
        val markResult = appUserDao.markActivationCodeUsed("ACT-7788", userUuid)
        assertTrue(markResult)
        val codeAfterUse = appUserDao.getActivationCode("ACT-7788")
        assertNotNull(codeAfterUse)
        assertTrue(codeAfterUse.isUsed)
        assertEquals(userUuid, codeAfterUse.usedByUuid)

        // محاولة استخدام كود مستخدم مسبقاً يجب أن تفشل
        val duplicateUseResult = appUserDao.markActivationCodeUsed("ACT-7788", userUuid)
        assertFalse(duplicateUseResult)

        // 7. حذف كود التفعيل والتحقق من تحديث updatedAt و isDeleted
        val codeBeforeDelete = appUserDao.getActivationCode("ACT-7788")
        assertNotNull(codeBeforeDelete)
        staffDao.deleteActivationCode("ACT-7788")
        val codeAfterDelete = appUserDao.getActivationCode("ACT-7788")
        assertNotNull(codeAfterDelete)
        assertTrue(codeAfterDelete.isDeleted)
        assertTrue(codeAfterDelete.updatedAt >= codeBeforeDelete.updatedAt)

        // 8. حذف الموظف (Soft Delete)
        staffDao.deleteUser(userUuid)
        val usersAfterDelete = staffDao.getUsersByOwner(ownerId)
        assertFalse(usersAfterDelete.any { it.uuid == userUuid })
    }

    @Test
    fun testStaffManagementViewModelPrepareEditAndAddNewStaff() {
        val staffDao = StaffManagementDao()
        val ownerId = "OWNER-TEST"
        val viewModel = StaffManagementViewModel(staffDao, ownerId)

        assertEquals("STOREKEEPER", viewModel.uiState.value.selectedPreset)

        val customCode = ActivationCodeEntity(
            code = "ACT-CUSTOM-99",
            ownerId = ownerId,
            assignedRole = "SUPERVISOR",
            label = "مشرف خط إنتاج",
            permissionsJson = "{\"PRODUCTION\":{\"read\":true,\"create\":true,\"update\":true,\"delete\":true}}"
        )

        // 1. prepareEditCode يجب أن يحمل الصلاحيات ويحدد النمط إلى CUSTOM
        viewModel.prepareEditCode(customCode)
        val stateAfterEdit = viewModel.uiState.value
        assertEquals("ACT-CUSTOM-99", stateAfterEdit.editingCodeCode)
        assertEquals("مشرف خط إنتاج", stateAfterEdit.newStaffLabel)
        assertEquals("SUPERVISOR", stateAfterEdit.newStaffRole)
        assertEquals("CUSTOM", stateAfterEdit.selectedPreset)

        val prodPerm = stateAfterEdit.permissionMatrix.find { it.moduleKey == "PRODUCTION" }
        assertNotNull(prodPerm)
        assertTrue(prodPerm.canRead)
        assertTrue(prodPerm.canCreate)
        assertTrue(prodPerm.canUpdate)
        assertTrue(prodPerm.canDelete)

        val warehousePerm = stateAfterEdit.permissionMatrix.find { it.moduleKey == "WAREHOUSE" }
        assertNotNull(warehousePerm)
        assertFalse(warehousePerm.canRead)

        // 2. prepareAddNewStaff يجب أن يعيد تعيين المصفوفة والنمط إلى الافتراضي
        viewModel.prepareAddNewStaff()
        val stateAfterReset = viewModel.uiState.value
        assertNull(stateAfterReset.editingCodeCode)
        assertEquals("", stateAfterReset.newStaffLabel)
        assertEquals("OPERATOR", stateAfterReset.newStaffRole)
        assertEquals("STOREKEEPER", stateAfterReset.selectedPreset)

        val defaultWarehouse = stateAfterReset.permissionMatrix.find { it.moduleKey == "WAREHOUSE" }
        assertNotNull(defaultWarehouse)
        assertTrue(defaultWarehouse.canRead)
        assertTrue(defaultWarehouse.canCreate)
        assertTrue(defaultWarehouse.canUpdate)
        assertFalse(defaultWarehouse.canDelete)
    }

    @Test
    fun testInsertActivationCodeRejectsDuplicateAndViewModelGeneratesUniqueCode() {
        val staffDao = StaffManagementDao()
        val appUserDao = AppUserDao()
        val ownerId = "OWNER-COLLISION-TEST"
        val codeEntity = ActivationCodeEntity(
            code = "ACT-UNIQUE-1",
            ownerId = ownerId,
            assignedRole = "OPERATOR",
            label = "كود تجريبي أولي"
        )

        // الإدراج الأول يجب أن ينجح
        val firstInsert = staffDao.insertActivationCode(codeEntity)
        assertTrue(firstInsert)

        // محاولة إدراج نفس الكود مرة أخرى يجب أن تفشل بدلاً من استبداله
        val duplicateInsert = staffDao.insertActivationCode(codeEntity.copy(label = "محاولة استبدال"))
        assertFalse(duplicateInsert)

        // التأكد من أن البيانات الأصلية لم يتم استبدالها
        val existing = appUserDao.getActivationCode("ACT-UNIQUE-1")
        assertNotNull(existing)
        assertEquals("كود تجريبي أولي", existing.label)

        // اختبار توليد كود جديد عبر ViewModel
        val viewModel = StaffManagementViewModel(staffDao, ownerId)
        viewModel.generateActivationCode()
        val generatedDialog = viewModel.uiState.value.generatedCodeDialog
        assertNotNull(generatedDialog)
        assertTrue(generatedDialog.code.startsWith("STF-"))
        val storedGenerated = appUserDao.getActivationCode(generatedDialog.code)
        assertNotNull(storedGenerated)
    }

    @Test
    fun testStaffLabelPdfGeneratorJvmSupportStatus() {
        assertFalse(StaffLabelPdfGenerator.isPdfExportSupported)
        assertFailsWith<UnsupportedOperationException> {
            StaffLabelPdfGenerator.generatePdf("STF-1234", "مشرف", "SUPERVISOR", "2026-10-12")
        }
    }
}

