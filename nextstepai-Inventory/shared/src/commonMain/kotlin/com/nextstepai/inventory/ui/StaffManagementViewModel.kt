package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import com.nextstepai.inventory.data.db.ActivationCodeEntity
import com.nextstepai.inventory.data.db.AppUserEntity
import com.nextstepai.inventory.data.db.StaffManagementDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random
import kotlin.time.Clock

data class ModulePermissionState(
    val moduleKey: String,
    val moduleNameAr: String,
    val canRead: Boolean = true,
    val canCreate: Boolean = false,
    val canUpdate: Boolean = false,
    val canDelete: Boolean = false
)

data class StaffManagementUiState(
    val users: List<AppUserEntity> = emptyList(),
    val activeInvitationCodes: List<ActivationCodeEntity> = emptyList(),
    val newStaffLabel: String = "",
    val newStaffRole: String = "OPERATOR",
    val editingCodeCode: String? = null,
    val selectedPreset: String = "STOREKEEPER",
    val permissionMatrix: List<ModulePermissionState> = listOf(
        ModulePermissionState("WAREHOUSE", "قسم المستودع والمخزون", canRead = true, canCreate = true, canUpdate = true, canDelete = false),
        ModulePermissionState("PRODUCTION", "قسم الإنتاج والتصنيع", canRead = false, canCreate = false, canUpdate = false, canDelete = false),
        ModulePermissionState("SALES_ORDERS", "قسم الشركاء والمبيعات", canRead = false, canCreate = false, canUpdate = false, canDelete = false),
        ModulePermissionState("SETTINGS", "قسم إعدادات النظام", canRead = false, canCreate = false, canUpdate = false, canDelete = false)
    ),
    val generatedCodeDialog: ActivationCodeEntity? = null,
    val isLoading: Boolean = false,
    val message: String? = null
)

class StaffManagementViewModel(
    private val staffDao: StaffManagementDao = StaffManagementDao(),
    private val ownerId: String = "OWNER-MAIN"
) : ViewModel() {

    private val _uiState = MutableStateFlow(StaffManagementUiState())
    val uiState: StateFlow<StaffManagementUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        _uiState.update { it.copy(isLoading = true) }
        val users = staffDao.getUsersByOwner(ownerId)
        val activeCodes = staffDao.getActiveInvitationCodes(ownerId)
        _uiState.update {
            it.copy(
                users = users,
                activeInvitationCodes = activeCodes,
                isLoading = false
            )
        }
    }

    fun onLabelChange(value: String) {
        _uiState.update { it.copy(newStaffLabel = value) }
    }

    fun onRoleChange(role: String) {
        _uiState.update { it.copy(newStaffRole = role) }
    }

    fun applyPreset(presetKey: String) {
        _uiState.update { state ->
            val updatedMatrix = state.permissionMatrix.map { item ->
                when (presetKey) {
                    "STOREKEEPER" -> {
                        if (item.moduleKey == "WAREHOUSE") {
                            item.copy(canRead = true, canCreate = true, canUpdate = true, canDelete = false)
                        } else {
                            item.copy(canRead = false, canCreate = false, canUpdate = false, canDelete = false)
                        }
                    }
                    "PRODUCTION" -> {
                        when (item.moduleKey) {
                            "WAREHOUSE" -> item.copy(canRead = true, canCreate = true, canUpdate = false, canDelete = false)
                            "PRODUCTION" -> item.copy(canRead = true, canCreate = true, canUpdate = true, canDelete = false)
                            else -> item.copy(canRead = false, canCreate = false, canUpdate = false, canDelete = false)
                        }
                    }
                    "SALES" -> {
                        when (item.moduleKey) {
                            "WAREHOUSE" -> item.copy(canRead = true, canCreate = false, canUpdate = false, canDelete = false)
                            "SALES_ORDERS" -> item.copy(canRead = true, canCreate = true, canUpdate = true, canDelete = false)
                            else -> item.copy(canRead = false, canCreate = false, canUpdate = false, canDelete = false)
                        }
                    }
                    else -> item
                }
            }
            state.copy(
                selectedPreset = presetKey,
                permissionMatrix = updatedMatrix,
                newStaffRole = if (presetKey == "MANAGER") "MANAGER" else "OPERATOR"
            )
        }
    }

    fun toggleAllPermissionsForModule(moduleKey: String) {
        _uiState.update { state ->
            val currentModule = state.permissionMatrix.find { it.moduleKey == moduleKey }
            val allEnabled = currentModule?.let { it.canRead && it.canCreate && it.canUpdate && it.canDelete } ?: false
            val targetValue = !allEnabled

            val updatedMatrix = state.permissionMatrix.map { item ->
                if (item.moduleKey == moduleKey) {
                    item.copy(canRead = targetValue, canCreate = targetValue, canUpdate = targetValue, canDelete = targetValue)
                } else item
            }
            state.copy(permissionMatrix = updatedMatrix, selectedPreset = "CUSTOM")
        }
    }

    fun togglePermission(moduleKey: String, action: String) {
        _uiState.update { state ->
            val updatedMatrix = state.permissionMatrix.map { item ->
                if (item.moduleKey == moduleKey) {
                    when (action) {
                        "READ" -> item.copy(canRead = !item.canRead)
                        "CREATE" -> item.copy(canCreate = !item.canCreate)
                        "UPDATE" -> item.copy(canUpdate = !item.canUpdate)
                        "DELETE" -> item.copy(canDelete = !item.canDelete)
                        else -> item
                    }
                } else item
            }
            state.copy(permissionMatrix = updatedMatrix, selectedPreset = "CUSTOM")
        }
    }

    private fun parsePermissionsJson(json: String, baseMatrix: List<ModulePermissionState>): List<ModulePermissionState> {
        if (json.isBlank() || json.trim() == "{}") return baseMatrix

        return baseMatrix.map { item ->
            val moduleRegex = Regex(""""${item.moduleKey}"\s*:\s*\{([^}]*)\}""", RegexOption.IGNORE_CASE)
            val match = moduleRegex.find(json)
            if (match != null) {
                val block = match.groupValues[1]
                item.copy(
                    canRead = Regex(""""read"\s*:\s*true""", RegexOption.IGNORE_CASE).containsMatchIn(block),
                    canCreate = Regex(""""create"\s*:\s*true""", RegexOption.IGNORE_CASE).containsMatchIn(block),
                    canUpdate = Regex(""""update"\s*:\s*true""", RegexOption.IGNORE_CASE).containsMatchIn(block),
                    canDelete = Regex(""""delete"\s*:\s*true""", RegexOption.IGNORE_CASE).containsMatchIn(block)
                )
            } else {
                item.copy(canRead = false, canCreate = false, canUpdate = false, canDelete = false)
            }
        }
    }

    fun prepareEditCode(codeEntity: ActivationCodeEntity) {
        val defaultState = StaffManagementUiState()
        val loadedMatrix = parsePermissionsJson(codeEntity.permissionsJson, defaultState.permissionMatrix)
        _uiState.update {
            it.copy(
                editingCodeCode = codeEntity.code,
                newStaffLabel = codeEntity.label,
                newStaffRole = codeEntity.assignedRole,
                permissionMatrix = loadedMatrix,
                selectedPreset = "CUSTOM"
            )
        }
    }

    fun prepareAddNewStaff() {
        val defaultState = StaffManagementUiState()
        _uiState.update {
            it.copy(
                editingCodeCode = null,
                newStaffLabel = "",
                newStaffRole = defaultState.newStaffRole,
                selectedPreset = defaultState.selectedPreset,
                permissionMatrix = defaultState.permissionMatrix
            )
        }
    }

    fun generateActivationCode() {
        val state = _uiState.value
        val label = state.newStaffLabel.trim()
        val role = state.newStaffRole
        val now = Clock.System.now().toEpochMilliseconds()

        val jsonBuilder = StringBuilder("{")
        state.permissionMatrix.forEachIndexed { idx, perm ->
            jsonBuilder.append("\"${perm.moduleKey}\":{")
            jsonBuilder.append("\"read\":${perm.canRead},")
            jsonBuilder.append("\"create\":${perm.canCreate},")
            jsonBuilder.append("\"update\":${perm.canUpdate},")
            jsonBuilder.append("\"delete\":${perm.canDelete}}")
            if (idx < state.permissionMatrix.size - 1) jsonBuilder.append(",")
        }
        jsonBuilder.append("}")

        val editingCode = state.editingCodeCode
        if (editingCode != null) {
            staffDao.updateActivationCodeFull(editingCode, label.ifBlank { "كود موظف" }, role, jsonBuilder.toString())
            _uiState.update {
                it.copy(
                    editingCodeCode = null,
                    newStaffLabel = "",
                    message = "تم تحديث بيانات التفعيل والصلاحيات بنجاح"
                )
            }
        } else {
            val maxRetries = 10
            var inserted = false
            var createdEntity: ActivationCodeEntity? = null

            for (attempt in 1..maxRetries) {
                val randomNum = Random.nextInt(1000, 9999)
                val codeStr = "STF-$randomNum"

                val newCodeEntity = ActivationCodeEntity(
                    code = codeStr,
                    ownerId = ownerId,
                    assignedRole = role,
                    permissionsJson = jsonBuilder.toString(),
                    label = label.ifBlank { "كود موظف جديد" },
                    isUsed = false,
                    usedByUuid = "",
                    expiresAt = 0L,
                    version = 1,
                    syncStatus = "PENDING_PUSH",
                    createdAt = now,
                    updatedAt = now
                )

                if (staffDao.insertActivationCode(newCodeEntity)) {
                    inserted = true
                    createdEntity = newCodeEntity
                    break
                }
            }

            if (inserted && createdEntity != null) {
                _uiState.update {
                    it.copy(
                        generatedCodeDialog = createdEntity,
                        newStaffLabel = "",
                        message = "تم توليد كود التفعيل المخصص بنجاح"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        message = "فشل توليد كود التفعيل بسبب تكرار الأكواد، يرجى المحاولة مرة أخرى"
                    )
                }
            }
        }
        loadData()
    }

    fun dismissGeneratedDialog() {
        _uiState.update { it.copy(generatedCodeDialog = null) }
    }

    fun deleteInvitationCode(code: String) {
        staffDao.deleteActivationCode(code)
        loadData()
    }

    fun toggleUserActiveStatus(userUuid: String, currentActive: Boolean) {
        val now = Clock.System.now().toEpochMilliseconds()
        staffDao.setUserActive(userUuid, !currentActive, now)
        loadData()
    }

    fun deleteUser(userUuid: String) {
        staffDao.deleteUser(userUuid)
        loadData()
    }

    fun updateActivationCodeLabel(code: String, newLabel: String) {
        staffDao.updateActivationCodeLabel(code, newLabel)
        loadData()
    }

    fun updateAppUser(uuid: String, name: String, role: String) {
        staffDao.updateAppUser(uuid, name, role)
        loadData()
    }
}
