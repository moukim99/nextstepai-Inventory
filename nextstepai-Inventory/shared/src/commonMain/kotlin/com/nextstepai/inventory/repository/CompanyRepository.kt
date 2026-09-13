package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.*
import com.nextstepai.inventory.data.db.*
import com.nextstepai.inventory.media.ImageProcessor
import com.nextstepai.inventory.media.ProcessedImage
import com.nextstepai.inventory.sync.BatchSyncService
import com.nextstepai.inventory.sync.SyncPayload
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock

/**
 * المستودع (Repository) المسؤول عن إدارة الشركات والعلاقات التجارية (الموردين، المصنعين، العملاء)،
 * والكيانات الملحقة بها (جهات الاتصال، العناوين، المرفقات، قطع المصنع، المعاملات التقنية، قطع الموردين، وشرائح الأسعار).
 */
class CompanyRepository(
    private val companyTable: CompanyTable = CompanyTable(),
    private val companyAttachmentTable: CompanyAttachmentTable = CompanyAttachmentTable(),
    private val contactTable: ContactTable = ContactTable(),
    private val addressTable: AddressTable = AddressTable(),
    private val manufacturerPartTable: ManufacturerPartTable = ManufacturerPartTable(),
    private val manufacturerPartParameterTable: ManufacturerPartParameterTable = ManufacturerPartParameterTable(),
    private val manufacturerPartAttachmentTable: ManufacturerPartAttachmentTable = ManufacturerPartAttachmentTable(),
    private val supplierPartTable: SupplierPartTable = SupplierPartTable(),
    private val supplierPriceBreakTable: SupplierPriceBreakTable = SupplierPriceBreakTable(),
    private val companyDao: CompanyDao = CompanyDao(),
    private val companyAttachmentDao: CompanyAttachmentDao = CompanyAttachmentDao(),
    private val contactDao: ContactDao = ContactDao(),
    private val addressDao: AddressDao = AddressDao(),
    private val manufacturerPartDao: ManufacturerPartDao = ManufacturerPartDao(),
    private val manufacturerPartParameterDao: ManufacturerPartParameterDao = ManufacturerPartParameterDao(),
    private val manufacturerPartAttachmentDao: ManufacturerPartAttachmentDao = ManufacturerPartAttachmentDao(),
    private val supplierPartDao: SupplierPartDao = SupplierPartDao(),
    private val supplierPriceBreakDao: SupplierPriceBreakDao = SupplierPriceBreakDao(),
    private val batchSyncService: BatchSyncService = BatchSyncService(),
    private val imageProcessor: ImageProcessor = ImageProcessor(maxDimension = 1024, compressionQuality = 85)
) {
    /**
     * جلب سجلات الشركات مجزأة صفحات (LIMIT & OFFSET).
     */
    suspend fun getCompaniesPaged(
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
     * جلب شركة حسب المعرف.
     */
    fun getCompanyById(id: Long): Company? = companyTable.getCompanyById(id)

    /**
     * إضافة شركة جديدة مع تحديث الكيان المحلي القابل للمزامنة.
     */
    fun addCompany(company: Company): Company {
        val inserted = companyTable.insertCompany(company)
        runBlocking {
            companyDao.insertOrUpdate(
                CompanyEntity(
                    uuid = "company-${inserted.id}",
                    name = inserted.name,
                    description = inserted.description,
                    website = inserted.website,
                    phone = inserted.phone,
                    email = inserted.email,
                    isSupplier = inserted.isSupplier,
                    isManufacturer = inserted.isManufacturer,
                    isCustomer = inserted.isCustomer,
                    active = inserted.active,
                    currency = inserted.currency,
                    notes = inserted.notes,
                    metadata = inserted.metadata,
                    parentUuid = inserted.parentId?.let { "company-$it" },
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        return inserted
    }

    /**
     * تحديث شركة حالية.
     */
    fun updateCompany(company: Company): Company {
        val updated = companyTable.updateCompany(company)
        runBlocking {
            companyDao.insertOrUpdate(
                CompanyEntity(
                    uuid = "company-${updated.id}",
                    name = updated.name,
                    description = updated.description,
                    website = updated.website,
                    phone = updated.phone,
                    email = updated.email,
                    isSupplier = updated.isSupplier,
                    isManufacturer = updated.isManufacturer,
                    isCustomer = updated.isCustomer,
                    active = updated.active,
                    currency = updated.currency,
                    notes = updated.notes,
                    metadata = updated.metadata,
                    parentUuid = updated.parentId?.let { "company-$it" },
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        return updated
    }

    /**
     * التحقق من صلاحية اختيار الشركة لمسار أمر الشراء (Purchase Order).
     */
    fun validateSupplierForPurchaseOrder(companyId: Long): Boolean {
        val company = getCompanyById(companyId)
        require(company != null) { "الشركة المطلوبة لأمر الشراء غير موجودة" }
        require(company.isSupplier) { "الشركة '${company.name}' ليست مصنفة كمورد (isSupplier = false) ولا يمكن إدراجها في أمر الشراء." }
        return true
    }

    // --- مرفقات الشركات العامة (Company Attachments) ---

    fun getAttachmentsForCompany(companyId: Long): List<CompanyAttachment> =
        companyAttachmentTable.getAttachmentsForCompany(companyId)

    fun addCompanyAttachment(attachment: CompanyAttachment): CompanyAttachment {
        val inserted = companyAttachmentTable.insertAttachment(attachment)
        runBlocking {
            companyAttachmentDao.insertOrUpdate(
                CompanyAttachmentEntity(
                    uuid = "company-att-${inserted.id}",
                    companyUuid = "company-${inserted.companyId}",
                    documentType = inserted.documentType,
                    attachmentPath = inserted.attachmentPath,
                    link = inserted.link,
                    comment = inserted.comment,
                    uploadDate = inserted.uploadDate,
                    userId = inserted.userId,
                    expiryDate = inserted.expiryDate,
                    notifyOnExpiry = inserted.notifyOnExpiry,
                    notificationDaysBefore = inserted.notificationDaysBefore,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        return inserted
    }

    fun deleteCompanyAttachment(id: Long): Boolean {
        val deleted = companyAttachmentTable.deleteAttachment(id)
        if (deleted) {
            runBlocking { companyAttachmentDao.delete("company-att-$id") }
        }
        return deleted
    }

    // --- جهات الاتصال (Contacts) ---

    fun getContactsForCompany(companyId: Long): List<Contact> = contactTable.getContactsForCompany(companyId)

    fun addContact(contact: Contact): Contact {
        val inserted = contactTable.insertContact(contact)
        runBlocking {
            contactDao.insertOrUpdate(
                ContactEntity(
                    uuid = "contact-${inserted.id}",
                    companyUuid = "company-${inserted.companyId}",
                    name = inserted.name,
                    phone = inserted.phone,
                    email = inserted.email,
                    role = inserted.role,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        return inserted
    }

    fun deleteContact(contactId: Long): Boolean {
        val deleted = contactTable.deleteContact(contactId)
        if (deleted) {
            runBlocking { contactDao.delete("contact-$contactId") }
        }
        return deleted
    }

    // --- العناوين (Addresses) ---

    fun getAddressesForCompany(companyId: Long): List<Address> = addressTable.getAddressesForCompany(companyId)

    fun addAddress(address: Address): Address {
        val inserted = addressTable.insertAddress(address)
        runBlocking {
            addressDao.insertOrUpdate(
                AddressEntity(
                    uuid = "address-${inserted.id}",
                    companyUuid = "company-${inserted.companyId}",
                    title = inserted.title,
                    isPrimary = inserted.isPrimary,
                    line1 = inserted.line1,
                    line2 = inserted.line2,
                    postalCode = inserted.postalCode,
                    city = inserted.city,
                    province = inserted.province,
                    country = inserted.country,
                    shippingNotes = inserted.shippingNotes,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        return inserted
    }

    fun deleteAddress(addressId: Long): Boolean {
        val deleted = addressTable.deleteAddress(addressId)
        if (deleted) {
            runBlocking { addressDao.delete("address-$addressId") }
        }
        return deleted
    }

    // --- قطع المصنّع (Manufacturer Parts) ---

    fun getManufacturerPartsForCompany(companyId: Long): List<ManufacturerPart> =
        manufacturerPartTable.getManufacturerPartsForCompany(companyId)

    fun addManufacturerPart(part: ManufacturerPart): ManufacturerPart {
        val company = getCompanyById(part.manufacturerId)
        val isMfg = company?.isManufacturer ?: true
        val inserted = manufacturerPartTable.insertManufacturerPart(part, isManufacturerCompany = isMfg)
        runBlocking {
            manufacturerPartDao.insertOrUpdate(
                ManufacturerPartEntity(
                    uuid = "mfg-part-${inserted.id}",
                    partUuid = "part-${inserted.partId}",
                    manufacturerUuid = "company-${inserted.manufacturerId}",
                    mpn = inserted.mpn,
                    description = inserted.description,
                    link = inserted.link,
                    metadata = inserted.metadata,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        return inserted
    }

    fun deleteManufacturerPart(id: Long): Boolean {
        val deleted = manufacturerPartTable.deleteManufacturerPart(id)
        if (deleted) {
            runBlocking { manufacturerPartDao.delete("mfg-part-$id") }
        }
        return deleted
    }

    // --- الخصائص الفنية لقطع المصنّع (Manufacturer Part Parameters) ---

    fun getParametersForManufacturerPart(manufacturerPartId: Long): List<ManufacturerPartParameter> =
        manufacturerPartParameterTable.getParametersForManufacturerPart(manufacturerPartId)

    fun addManufacturerPartParameter(parameter: ManufacturerPartParameter): ManufacturerPartParameter {
        val inserted = manufacturerPartParameterTable.insertParameter(parameter)
        runBlocking {
            manufacturerPartParameterDao.insertOrUpdate(
                ManufacturerPartParameterEntity(
                    uuid = "mfg-param-${inserted.id}",
                    manufacturerPartUuid = "mfg-part-${inserted.manufacturerPartId}",
                    name = inserted.name,
                    value = inserted.value,
                    units = inserted.units,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        return inserted
    }

    fun deleteManufacturerPartParameter(id: Long): Boolean {
        val deleted = manufacturerPartParameterTable.deleteParameter(id)
        if (deleted) {
            runBlocking { manufacturerPartParameterDao.delete("mfg-param-$id") }
        }
        return deleted
    }

    // --- مرفقات قطع المصنّع (Manufacturer Part Attachments) ---

    fun getAttachmentsForManufacturerPart(manufacturerPartId: Long): List<ManufacturerPartAttachment> =
        manufacturerPartAttachmentTable.getAttachmentsForManufacturerPart(manufacturerPartId)

    fun addManufacturerPartAttachment(attachment: ManufacturerPartAttachment): ManufacturerPartAttachment {
        val inserted = manufacturerPartAttachmentTable.insertAttachment(attachment)
        runBlocking {
            manufacturerPartAttachmentDao.insertOrUpdate(
                ManufacturerPartAttachmentEntity(
                    uuid = "mfg-part-att-${inserted.id}",
                    manufacturerPartUuid = "mfg-part-${inserted.manufacturerPartId}",
                    attachmentPath = inserted.attachmentPath,
                    link = inserted.link,
                    comment = inserted.comment,
                    uploadDate = inserted.uploadDate,
                    userId = inserted.userId,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        return inserted
    }

    fun deleteManufacturerPartAttachment(id: Long): Boolean {
        val deleted = manufacturerPartAttachmentTable.deleteAttachment(id)
        if (deleted) {
            runBlocking { manufacturerPartAttachmentDao.delete("mfg-part-att-$id") }
        }
        return deleted
    }

    // --- قطع الموردين (Supplier Parts) ---

    fun getSupplierPartsForCompany(companyId: Long): List<SupplierPart> =
        supplierPartTable.getSupplierPartsForCompany(companyId)

    fun addSupplierPart(part: SupplierPart): SupplierPart {
        val company = getCompanyById(part.supplierId)
        val isSup = company?.isSupplier ?: true
        val inserted = supplierPartTable.insertSupplierPart(part, isSupplierCompany = isSup)
        runBlocking {
            supplierPartDao.insertOrUpdate(
                SupplierPartEntity(
                    uuid = "sup-part-${inserted.id}",
                    partUuid = "part-${inserted.partId}",
                    supplierUuid = "company-${inserted.supplierId}",
                    sku = inserted.sku,
                    manufacturerPartUuid = inserted.manufacturerPartId?.let { "mfg-part-$it" },
                    description = inserted.description,
                    link = inserted.link,
                    note = inserted.note,
                    packaging = inserted.packaging,
                    packQuantity = inserted.packQuantity,
                    availableForPurchase = inserted.availableForPurchase,
                    active = inserted.active,
                    metadata = inserted.metadata,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        return inserted
    }

    fun deleteSupplierPart(id: Long): Boolean {
        val deleted = supplierPartTable.deleteSupplierPart(id)
        if (deleted) {
            runBlocking { supplierPartDao.delete("sup-part-$id") }
        }
        return deleted
    }

    // --- شرائح الأسعار (Supplier Price Breaks) ---

    fun getPriceBreaksForSupplierPart(supplierPartId: Long): List<SupplierPriceBreak> =
        supplierPriceBreakTable.getPriceBreaksForSupplierPart(supplierPartId)

    fun addPriceBreak(priceBreak: SupplierPriceBreak, defaultCompanyCurrency: String = "USD"): SupplierPriceBreak {
        val inserted = supplierPriceBreakTable.insertPriceBreak(priceBreak, defaultCompanyCurrency)
        runBlocking {
            supplierPriceBreakDao.insertOrUpdate(
                SupplierPriceBreakEntity(
                    uuid = "price-break-${inserted.id}",
                    supplierPartUuid = "sup-part-${inserted.supplierPartId}",
                    quantity = inserted.quantity,
                    price = inserted.price,
                    priceCurrency = inserted.priceCurrency,
                    packQuantity = inserted.packQuantity,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        return inserted
    }

    fun deletePriceBreak(id: Long): Boolean {
        val deleted = supplierPriceBreakTable.deletePriceBreak(id)
        if (deleted) {
            runBlocking { supplierPriceBreakDao.delete("price-break-$id") }
        }
        return deleted
    }

    fun getBestPriceForQuantity(supplierPartId: Long, quantity: Double): SupplierPriceBreak? =
        supplierPriceBreakTable.getBestPriceForQuantity(supplierPartId, quantity)

    // --- مساعدة ومزامنة ---

    fun processCompanyLogo(rawBytes: ByteArray, width: Int, height: Int): ProcessedImage {
        return imageProcessor.processAndCompressProductImage(rawBytes, width, height)
    }

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

        val response = batchSyncService.performBatchSync(payloads, Clock.System.now().toEpochMilliseconds() - 86400000)
        companyDao.updateSyncStatusForUuids(response.acceptedUuids, SyncStatus.SYNCED)
        return response.acceptedUuids.size
    }

    fun getCompanies(): List<Company> = companyTable.getAllCompanies()
}
