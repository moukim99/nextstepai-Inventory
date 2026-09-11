package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.Company
import com.nextstepai.inventory.data.CompanyTable
import com.nextstepai.inventory.data.db.CompanyDao
import com.nextstepai.inventory.data.db.CompanyEntity
import com.nextstepai.inventory.media.ImageProcessor
import com.nextstepai.inventory.media.ProcessedImage
import com.nextstepai.inventory.sync.BatchSyncService
import com.nextstepai.inventory.sync.SyncPayload
import com.nextstepai.inventory.sync.SyncStatus

/**
 * المستودع (Repository) المسؤول عن إدارة الشركات والعلاقات التجارية (الموردين، المصنعين، العملاء)،
 * والدعم للمزامنة الدفعية وتصفح القوائم بالصفحات وضغط الشعارات.
 */
class CompanyRepository(
    private val companyTable: CompanyTable = CompanyTable(),
    private val companyDao: CompanyDao = CompanyDao(),
    private val batchSyncService: BatchSyncService = BatchSyncService(),
    private val imageProcessor: ImageProcessor = ImageProcessor(maxDimension = 1024, compressionQuality = 85)
) {
    /**
     * جلب سجلات الشركات مجزأة صفحات (LIMIT & OFFSET) لمنع التحميل الكامل في الذاكرة.
     */
    fun getCompaniesPaged(
        supplierOnly: Boolean = false,
        manufacturerOnly: Boolean = false,
        customerOnly: Boolean = false,
        limit: Int = 20,
        offset: Int = 0
    ): List<CompanyEntity> {
        return companyDao.getCompaniesPaged(
            supplierOnly = supplierOnly,
            manufacturerOnly = manufacturerOnly,
            customerOnly = customerOnly,
            limit = limit,
            offset = offset
        )
    }

    /**
     * البحث والفلترة في الشركات.
     */
    fun searchCompanies(
        query: String = "",
        supplierOnly: Boolean = false,
        manufacturerOnly: Boolean = false,
        customerOnly: Boolean = false,
        activeOnly: Boolean = true
    ): List<Company> {
        return companyTable.searchCompanies(
            query = query,
            supplierOnly = supplierOnly,
            manufacturerOnly = manufacturerOnly,
            customerOnly = customerOnly,
            activeOnly = activeOnly
        )
    }

    /**
     * إضافة شركة جديدة مع تحديث الكيان المحلي القابل للمزامنة.
     */
    fun addCompany(company: Company): Company {
        val inserted = companyTable.insertCompany(company)
        companyDao.insertOrUpdate(
            CompanyEntity(
                uuid = "company-${inserted.id}",
                name = inserted.name,
                description = inserted.description,
                phone = inserted.phone,
                email = inserted.email,
                isSupplier = inserted.isSupplier,
                isManufacturer = inserted.isManufacturer,
                isCustomer = inserted.isCustomer,
                currency = inserted.currency,
                syncStatus = SyncStatus.PENDING_PUSH
            )
        )
        return inserted
    }

    /**
     * معالجة وضغط شعار الشركة قبل الحفظ المحلي لمنع استهلاك مساحة التخزين.
     */
    fun processCompanyLogo(rawBytes: ByteArray, width: Int, height: Int): ProcessedImage {
        return imageProcessor.processAndCompressProductImage(rawBytes, width, height)
    }

    /**
     * تنفيذ المزامنة الدفعية (Batch Sync) لبيانات الشركات المعلّقة في طلب شبكي واحد.
     */
    suspend fun syncPendingCompanyChanges(): Int {
        val pending = companyDao.getPendingSyncCompanies(limit = 50, offset = 0)
        if (pending.isEmpty()) return 0

        val payloads = pending.map { entity ->
            SyncPayload(
                uuid = entity.uuid,
                entityType = "Company",
                payloadJson = "{\"name\":\"${entity.name}\",\"email\":\"${entity.email}\",\"currency\":\"${entity.currency}\"}",
                isDeleted = entity.isDeleted,
                updatedAt = entity.updatedAt
            )
        }

        val response = batchSyncService.performBatchSync(payloads, System.currentTimeMillis() - 86400000)
        companyDao.updateSyncStatusForUuids(response.acceptedUuids, SyncStatus.SYNCED)
        return response.acceptedUuids.size
    }

    /**
     * جلب كافة الشركات.
     */
    fun getCompanies(): List<Company> = companyTable.getAllCompanies()
}
