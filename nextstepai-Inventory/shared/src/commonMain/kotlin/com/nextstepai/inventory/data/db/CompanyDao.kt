package com.nextstepai.inventory.data.db

import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات الشركات (CompanyDao) للفلترة والتصفح بطلب مجزأ صريح (LIMIT & OFFSET).
 */
class CompanyDao {
    private val companyEntities = mutableListOf<CompanyEntity>()

    /**
     * استعلام مجزأ صريح مع التقييد بالصفحات (Explicit LIMIT / OFFSET Query).
     */
    fun getCompaniesPaged(
        supplierOnly: Boolean = false,
        manufacturerOnly: Boolean = false,
        customerOnly: Boolean = false,
        limit: Int = 20,
        offset: Int = 0
    ): List<CompanyEntity> {
        return companyEntities
            .filter { entity ->
                !entity.isDeleted &&
                        (!supplierOnly || entity.isSupplier) &&
                        (!manufacturerOnly || entity.isManufacturer) &&
                        (!customerOnly || entity.isCustomer)
            }
            .sortedByDescending { it.updatedAt }
            .drop(offset)
            .take(limit)
    }

    /**
     * جلب السجلات المعلّقة للمزامنة الدفعية.
     */
    fun getPendingSyncCompanies(limit: Int = 50, offset: Int = 0): List<CompanyEntity> {
        return companyEntities
            .filter { it.syncStatus == SyncStatus.PENDING_PUSH && !it.isDeleted }
            .sortedBy { it.updatedAt }
            .drop(offset)
            .take(limit)
    }

    /**
     * إدراج أو تحديث سجل شركة.
     */
    fun insertOrUpdate(entity: CompanyEntity) {
        val index = companyEntities.indexOfFirst { it.uuid == entity.uuid }
        if (index != -1) {
            companyEntities[index] = entity
        } else {
            companyEntities.add(entity)
        }
    }

    /**
     * تحديث حالة المزامنة لمعرفات المقبولة في الباتش.
     */
    fun updateSyncStatusForUuids(uuids: List<String>, newStatus: SyncStatus) {
        for (i in companyEntities.indices) {
            if (companyEntities[i].uuid in uuids) {
                companyEntities[i] = companyEntities[i].copy(syncStatus = newStatus)
            }
        }
    }
}
