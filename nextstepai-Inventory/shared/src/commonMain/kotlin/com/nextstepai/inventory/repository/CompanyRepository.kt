package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.*
import com.nextstepai.inventory.data.db.*
import com.nextstepai.inventory.media.ImageProcessor
import com.nextstepai.inventory.media.ProcessedImage
import com.nextstepai.inventory.sync.BatchSyncService
import com.nextstepai.inventory.sync.SyncPayload
import com.nextstepai.inventory.sync.SyncStatus
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
    private val companyBankAccountTable: CompanyBankAccountTable = CompanyBankAccountTable(),
    private val companyLegalRecordTable: CompanyLegalRecordTable = CompanyLegalRecordTable(),
    private val manufacturerPartTable: ManufacturerPartTable = ManufacturerPartTable(),
    private val manufacturerPartParameterTable: ManufacturerPartParameterTable = ManufacturerPartParameterTable(),
    private val manufacturerPartAttachmentTable: ManufacturerPartAttachmentTable = ManufacturerPartAttachmentTable(),
    private val supplierPartTable: SupplierPartTable = SupplierPartTable(),
    private val supplierPriceBreakTable: SupplierPriceBreakTable = SupplierPriceBreakTable(),
    private val companyDao: CompanyDao = CompanyDao(),
    private val companyAttachmentDao: CompanyAttachmentDao = CompanyAttachmentDao(),
    private val contactDao: ContactDao = ContactDao(),
    private val addressDao: AddressDao = AddressDao(),
    private val companyBankAccountDao: CompanyBankAccountDao = CompanyBankAccountDao(),
    private val companyLegalRecordDao: CompanyLegalRecordDao = CompanyLegalRecordDao(),
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
     * البحث والفلترة في الشركات من قاعدة البيانات الدائمة (SQLite).
     */
    fun searchCompanies(
        query: String = "",
        supplierOnly: Boolean = false,
        manufacturerOnly: Boolean = false,
        customerOnly: Boolean = false,
        activeOnly: Boolean = true
    ): List<Company> {
        val entities = companyDao.getCompaniesPaged(
            supplierOnly = supplierOnly,
            manufacturerOnly = manufacturerOnly,
            customerOnly = customerOnly,
            limit = 500,
            offset = 0
        )
        if (entities.isNotEmpty()) {
            var result = entities.mapIndexed { index, entity ->
                val parsedId = entity.uuid.removePrefix("company-").toLongOrNull() ?: (index + 1L)
                Company(
                    id = parsedId,
                    name = entity.name,
                    description = entity.description,
                    website = entity.website,
                    phone = entity.phone,
                    email = entity.email,
                    isSupplier = entity.isSupplier,
                    isManufacturer = entity.isManufacturer,
                    isCustomer = entity.isCustomer,
                    active = entity.active,
                    currency = entity.currency,
                    imageUrl = entity.logoPath,
                    notes = entity.notes
                )
            }
            if (activeOnly) {
                result = result.filter { it.active }
            }
            if (query.isNotBlank()) {
                result = result.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true)
                }
            }
            return result
        }
        return emptyList()
    }

    /**
     * جلب شركة حسب المعرف من SQLite.
     */
    fun getCompanyById(id: Long): Company? {
        val companyInTable = companyTable.getCompanyById(id)
        if (companyInTable != null) return companyInTable
        val companies = searchCompanies()
        val found = companies.find { it.id == id }
        if (found != null) {
            companyTable.insertCompany(found)
        }
        return found
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
                    website = inserted.website,
                    phone = inserted.phone,
                    email = inserted.email,
                    isSupplier = inserted.isSupplier,
                    isManufacturer = inserted.isManufacturer,
                    isCustomer = inserted.isCustomer,
                    active = inserted.active,
                    currency = inserted.currency,
                    logoPath = inserted.imageUrl,
                    notes = inserted.notes,
                    metadata = inserted.metadata,
                    parentUuid = inserted.parentId?.let { "company-$it" },
                    syncStatus = SyncStatus.PENDING
                )
            )
        return inserted
    }

    /**
     * تحديث شركة حالية.
     */
    fun updateCompany(company: Company): Company {
        val updated = companyTable.updateCompany(company)
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
                    logoPath = updated.imageUrl,
                    notes = updated.notes,
                    metadata = updated.metadata,
                    parentUuid = updated.parentId?.let { "company-$it" },
                    syncStatus = SyncStatus.PENDING
                )
            )
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

    fun getAttachmentsForCompany(companyId: Long): List<CompanyAttachment> {
        val cached = companyAttachmentTable.getAttachmentsForCompany(companyId)
        val entities = companyAttachmentDao.getForCompany("company-$companyId")
        if (entities.isNotEmpty()) {
            val cachedUuids = cached.map { "company-att-${it.id}" }.toSet()
            val missing = entities.filter { it.uuid !in cachedUuids }
            if (missing.isNotEmpty()) {
                missing.forEach { entity ->
                    val parsedId = entity.uuid.removePrefix("company-att-").toLongOrNull() ?: 0L
                    companyAttachmentTable.insertAttachment(
                        CompanyAttachment(
                            id = parsedId,
                            companyId = companyId,
                            documentType = entity.documentType,
                            attachmentPath = entity.attachmentPath,
                            link = entity.link,
                            comment = entity.comment,
                            uploadDate = entity.uploadDate,
                            userId = entity.userId,
                            expiryDate = entity.expiryDate,
                            notifyOnExpiry = entity.notifyOnExpiry,
                            notificationDaysBefore = entity.notificationDaysBefore
                        )
                    )
                }
                return companyAttachmentTable.getAttachmentsForCompany(companyId)
            }
        }
        return cached
    }

    fun addCompanyAttachment(attachment: CompanyAttachment): CompanyAttachment {
        val inserted = companyAttachmentTable.insertAttachment(attachment)
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
        return inserted
    }

    fun deleteCompanyAttachment(id: Long): Boolean {
        val deleted = companyAttachmentTable.deleteAttachment(id)
        if (deleted) {
            companyAttachmentDao.delete("company-att-$id")
        }
        return deleted
    }

    // --- جهات الاتصال (Contacts) ---

    fun getContactsForCompany(companyId: Long): List<Contact> {
        val cached = contactTable.getContactsForCompany(companyId)
        val entities = contactDao.getContactsForCompany("company-$companyId")
        if (entities.isNotEmpty()) {
            val cachedUuids = cached.map { "contact-${it.id}" }.toSet()
            val missing = entities.filter { it.uuid !in cachedUuids }
            if (missing.isNotEmpty()) {
                missing.forEach { entity ->
                    val parsedId = entity.uuid.removePrefix("contact-").toLongOrNull() ?: 0L
                    contactTable.insertContact(
                        Contact(
                            id = parsedId,
                            companyId = companyId,
                            name = entity.name,
                            phone = entity.phone,
                            email = entity.email,
                            role = entity.role,
                            isPrimary = entity.isPrimary
                        )
                    )
                }
                return contactTable.getContactsForCompany(companyId)
            }
        }
        return cached
    }

    fun addContact(contact: Contact): Contact {
        val inserted = contactTable.insertContact(contact)
        contactDao.insertOrUpdate(
                ContactEntity(
                    uuid = "contact-${inserted.id}",
                    companyUuid = "company-${inserted.companyId}",
                    name = inserted.name,
                    phone = inserted.phone,
                    email = inserted.email,
                    role = inserted.role,
                    isPrimary = inserted.isPrimary,
                    syncStatus = SyncStatus.PENDING
                )
            )
        return inserted
    }

    fun deleteContact(contactId: Long): Boolean {
        val deleted = contactTable.deleteContact(contactId)
        if (deleted) {
            contactDao.delete("contact-$contactId")
        }
        return deleted
    }

    // --- العناوين (Addresses) ---

    fun getAddressesForCompany(companyId: Long): List<Address> {
        val cached = addressTable.getAddressesForCompany(companyId)
        val entities = addressDao.getAddressesForCompany("company-$companyId")
        if (entities.isNotEmpty()) {
            val cachedUuids = cached.map { "address-${it.id}" }.toSet()
            val missing = entities.filter { it.uuid !in cachedUuids }
            if (missing.isNotEmpty()) {
                missing.forEach { entity ->
                    val parsedId = entity.uuid.removePrefix("address-").toLongOrNull() ?: 0L
                    addressTable.insertAddress(
                        Address(
                            id = parsedId,
                            companyId = companyId,
                            title = entity.title,
                            isPrimary = entity.isPrimary,
                            line1 = entity.line1,
                            line2 = entity.line2,
                            postalCode = entity.postalCode,
                            city = entity.city,
                            province = entity.province,
                            country = entity.country,
                            shippingNotes = entity.shippingNotes
                        )
                    )
                }
                return addressTable.getAddressesForCompany(companyId)
            }
        }
        return cached
    }

    fun addAddress(address: Address): Address {
        val inserted = addressTable.insertAddress(address)
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
        return inserted
    }

    fun deleteAddress(addressId: Long): Boolean {
        val deleted = addressTable.deleteAddress(addressId)
        if (deleted) {
            addressDao.delete("address-$addressId")
        }
        return deleted
    }

    // --- الحسابات البنكية للشركة (Company Bank Accounts) ---

    fun getBankAccountsForCompany(companyId: Long): List<CompanyBankAccount> {
        val cached = companyBankAccountTable.getBankAccountsForCompany(companyId)
        val entities = companyBankAccountDao.getForCompany("company-$companyId")
        if (entities.isNotEmpty()) {
            val cachedUuids = cached.map { "bank-${it.id}" }.toSet()
            val missing = entities.filter { it.uuid !in cachedUuids }
            if (missing.isNotEmpty()) {
                missing.forEach { entity ->
                    val parsedId = entity.uuid.removePrefix("bank-").toLongOrNull() ?: 0L
                    companyBankAccountTable.insertBankAccount(
                        CompanyBankAccount(
                            id = parsedId,
                            companyId = companyId,
                            bankName = entity.bankName,
                            accountName = entity.accountName,
                            accountNumber = entity.accountNumber,
                            iban = entity.iban,
                            swiftBic = entity.swiftBic,
                            currency = entity.currency,
                            branchName = entity.branchName,
                            isPrimary = entity.isPrimary,
                            updatedAt = entity.updatedAt
                        )
                    )
                }
                return companyBankAccountTable.getBankAccountsForCompany(companyId)
            }
        }
        return cached
    }

    fun addBankAccount(account: CompanyBankAccount): CompanyBankAccount {
        val inserted = companyBankAccountTable.insertBankAccount(account)
        companyBankAccountDao.insertOrUpdate(
                CompanyBankAccountEntity(
                    uuid = "bank-${inserted.id}",
                    companyUuid = "company-${inserted.companyId}",
                    bankName = inserted.bankName,
                    accountName = inserted.accountName,
                    accountNumber = inserted.accountNumber,
                    iban = inserted.iban,
                    swiftBic = inserted.swiftBic,
                    currency = inserted.currency,
                    branchName = inserted.branchName,
                    isPrimary = inserted.isPrimary,
                    syncStatus = SyncStatus.PENDING
                )
            )
        return inserted
    }

    fun deleteBankAccount(accountId: Long): Boolean {
        val deleted = companyBankAccountTable.deleteBankAccount(accountId)
        if (deleted) {
            companyBankAccountDao.delete("bank-$accountId")
        }
        return deleted
    }

    // --- السجلات القانونية والتراخيص (Company Legal Records) ---

    fun getLegalRecordForCompany(companyId: Long): CompanyLegalRecord? {
        val cached = companyLegalRecordTable.getLegalRecordForCompany(companyId)
        if (cached != null) return cached
        val entity = companyLegalRecordDao.getForCompany("company-$companyId") ?: return null
        val parsedId = entity.uuid.removePrefix("legal-").toLongOrNull() ?: 0L
        val record = CompanyLegalRecord(
            id = parsedId,
            companyId = companyId,
            commercialRegisterNumber = entity.commercialRegisterNumber,
            taxId = entity.taxId,
            nationalIdNumber = entity.nationalIdNumber,
            importLicenseNumber = entity.importLicenseNumber,
            manufacturingLicenseNumber = entity.manufacturingLicenseNumber,
            activityCodes = entity.activityCodes,
            issuingAuthority = entity.issuingAuthority,
            issueDate = entity.issueDate,
            expiryDate = entity.expiryDate,
            updatedAt = entity.updatedAt
        )
        return companyLegalRecordTable.saveOrUpdateLegalRecord(record)
    }

    fun saveOrUpdateLegalRecord(record: CompanyLegalRecord): CompanyLegalRecord {
        val inserted = companyLegalRecordTable.saveOrUpdateLegalRecord(record)
        companyLegalRecordDao.insertOrUpdate(
                CompanyLegalRecordEntity(
                    uuid = "legal-${inserted.id}",
                    companyUuid = "company-${inserted.companyId}",
                    commercialRegisterNumber = inserted.commercialRegisterNumber,
                    taxId = inserted.taxId,
                    nationalIdNumber = inserted.nationalIdNumber,
                    importLicenseNumber = inserted.importLicenseNumber,
                    manufacturingLicenseNumber = inserted.manufacturingLicenseNumber,
                    activityCodes = inserted.activityCodes,
                    issuingAuthority = inserted.issuingAuthority,
                    issueDate = inserted.issueDate,
                    expiryDate = inserted.expiryDate,
                    syncStatus = SyncStatus.PENDING
                )
            )
        return inserted
    }

    fun deleteLegalRecord(companyId: Long): Boolean {
        val record = getLegalRecordForCompany(companyId) ?: return false
        val deleted = companyLegalRecordTable.deleteLegalRecord(record.id)
        if (deleted) {
            companyLegalRecordDao.delete("legal-${record.id}")
        }
        return deleted
    }

    // --- قطع المصنّع (Manufacturer Parts) ---

    fun getManufacturerPartsForCompany(companyId: Long): List<ManufacturerPart> {
        val cached = manufacturerPartTable.getManufacturerPartsForCompany(companyId)
        val entities = manufacturerPartDao.getForCompany("company-$companyId")
        if (entities.isNotEmpty()) {
            val cachedUuids = cached.map { "mfg-part-${it.id}" }.toSet()
            val missing = entities.filter { it.uuid !in cachedUuids }
            if (missing.isNotEmpty()) {
                missing.forEach { entity ->
                    val parsedId = entity.uuid.removePrefix("mfg-part-").toLongOrNull() ?: 0L
                    val partId = entity.partUuid.removePrefix("part-").toLongOrNull() ?: 0L
                    manufacturerPartTable.insertManufacturerPart(
                        ManufacturerPart(
                            id = parsedId,
                            partId = partId,
                            manufacturerId = companyId,
                            mpn = entity.mpn,
                            description = entity.description,
                            link = entity.link,
                            metadata = entity.metadata,
                            updatedAt = entity.updatedAt
                        ),
                        isManufacturerCompany = true
                    )
                }
                return manufacturerPartTable.getManufacturerPartsForCompany(companyId)
            }
        }
        return cached
    }

    fun getManufacturerPartsForPart(partId: Long): List<ManufacturerPart> {
        val cached = manufacturerPartTable.getManufacturerPartsForPart(partId)
        val entities = manufacturerPartDao.getForPart("part-$partId")
        if (entities.isNotEmpty()) {
            val cachedUuids = cached.map { "mfg-part-${it.id}" }.toSet()
            val missing = entities.filter { it.uuid !in cachedUuids }
            if (missing.isNotEmpty()) {
                missing.forEach { entity ->
                    val parsedId = entity.uuid.removePrefix("mfg-part-").toLongOrNull() ?: 0L
                    val mfgId = entity.manufacturerUuid.removePrefix("company-").toLongOrNull() ?: 0L
                    manufacturerPartTable.insertManufacturerPart(
                        ManufacturerPart(
                            id = parsedId,
                            partId = partId,
                            manufacturerId = mfgId,
                            mpn = entity.mpn,
                            description = entity.description,
                            link = entity.link,
                            metadata = entity.metadata,
                            updatedAt = entity.updatedAt
                        ),
                        isManufacturerCompany = true
                    )
                }
                return manufacturerPartTable.getManufacturerPartsForPart(partId)
            }
        }
        return cached
    }

    fun addManufacturerPart(part: ManufacturerPart): ManufacturerPart {
        val company = getCompanyById(part.manufacturerId)
        val isMfg = company?.isManufacturer ?: true
        val inserted = manufacturerPartTable.insertManufacturerPart(part, isManufacturerCompany = isMfg)
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
        return inserted
    }

    fun deleteManufacturerPart(id: Long): Boolean {
        val deleted = manufacturerPartTable.deleteManufacturerPart(id)
        if (deleted) {
            manufacturerPartDao.delete("mfg-part-$id")
        }
        return deleted
    }

    // --- الخصائص الفنية لقطع المصنّع (Manufacturer Part Parameters) ---

    fun getParametersForManufacturerPart(manufacturerPartId: Long): List<ManufacturerPartParameter> {
        val cached = manufacturerPartParameterTable.getParametersForManufacturerPart(manufacturerPartId)
        val entities = manufacturerPartParameterDao.getForManufacturerPart("mfg-part-$manufacturerPartId")
        if (entities.isNotEmpty()) {
            val cachedUuids = cached.map { "mfg-param-${it.id}" }.toSet()
            val missing = entities.filter { it.uuid !in cachedUuids }
            if (missing.isNotEmpty()) {
                missing.forEach { entity ->
                    val parsedId = entity.uuid.removePrefix("mfg-param-").toLongOrNull() ?: 0L
                    manufacturerPartParameterTable.insertParameter(
                        ManufacturerPartParameter(
                            id = parsedId,
                            manufacturerPartId = manufacturerPartId,
                            name = entity.name,
                            value = entity.value,
                            units = entity.units
                        )
                    )
                }
                return manufacturerPartParameterTable.getParametersForManufacturerPart(manufacturerPartId)
            }
        }
        return cached
    }

    fun addManufacturerPartParameter(parameter: ManufacturerPartParameter): ManufacturerPartParameter {
        val inserted = manufacturerPartParameterTable.insertParameter(parameter)
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
        return inserted
    }

    fun deleteManufacturerPartParameter(id: Long): Boolean {
        val deleted = manufacturerPartParameterTable.deleteParameter(id)
        if (deleted) {
            manufacturerPartParameterDao.delete("mfg-param-$id")
        }
        return deleted
    }

    // --- مرفقات قطع المصنّع (Manufacturer Part Attachments) ---

    fun getAttachmentsForManufacturerPart(manufacturerPartId: Long): List<ManufacturerPartAttachment> {
        val cached = manufacturerPartAttachmentTable.getAttachmentsForManufacturerPart(manufacturerPartId)
        val entities = manufacturerPartAttachmentDao.getForManufacturerPart("mfg-part-$manufacturerPartId")
        if (entities.isNotEmpty()) {
            val cachedUuids = cached.map { "mfg-part-att-${it.id}" }.toSet()
            val missing = entities.filter { it.uuid !in cachedUuids }
            if (missing.isNotEmpty()) {
                missing.forEach { entity ->
                    val parsedId = entity.uuid.removePrefix("mfg-part-att-").toLongOrNull() ?: 0L
                    manufacturerPartAttachmentTable.insertAttachment(
                        ManufacturerPartAttachment(
                            id = parsedId,
                            manufacturerPartId = manufacturerPartId,
                            attachmentPath = entity.attachmentPath,
                            link = entity.link,
                            comment = entity.comment,
                            uploadDate = entity.uploadDate,
                            userId = entity.userId
                        )
                    )
                }
                return manufacturerPartAttachmentTable.getAttachmentsForManufacturerPart(manufacturerPartId)
            }
        }
        return cached
    }

    fun addManufacturerPartAttachment(attachment: ManufacturerPartAttachment): ManufacturerPartAttachment {
        val inserted = manufacturerPartAttachmentTable.insertAttachment(attachment)
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
        return inserted
    }

    fun deleteManufacturerPartAttachment(id: Long): Boolean {
        val deleted = manufacturerPartAttachmentTable.deleteAttachment(id)
        if (deleted) {
            manufacturerPartAttachmentDao.delete("mfg-part-att-$id")
        }
        return deleted
    }

    // --- قطع الموردين (Supplier Parts) ---

    fun getSupplierPartsForCompany(companyId: Long): List<SupplierPart> {
        val cached = supplierPartTable.getSupplierPartsForCompany(companyId)
        val entities = supplierPartDao.getForCompany("company-$companyId")
        if (entities.isNotEmpty()) {
            val cachedUuids = cached.map { "sup-part-${it.id}" }.toSet()
            val missing = entities.filter { it.uuid !in cachedUuids }
            if (missing.isNotEmpty()) {
                missing.forEach { entity ->
                    val parsedId = entity.uuid.removePrefix("sup-part-").toLongOrNull() ?: 0L
                    val partId = entity.partUuid.removePrefix("part-").toLongOrNull() ?: 0L
                    val mfgPartId = entity.manufacturerPartUuid?.removePrefix("mfg-part-")?.toLongOrNull()
                    supplierPartTable.insertSupplierPart(
                        SupplierPart(
                            id = parsedId,
                            partId = partId,
                            supplierId = companyId,
                            sku = entity.sku,
                            manufacturerPartId = mfgPartId,
                            description = entity.description,
                            link = entity.link,
                            note = entity.note,
                            packaging = entity.packaging,
                            packQuantity = entity.packQuantity,
                            availableForPurchase = entity.availableForPurchase,
                            active = entity.active,
                            metadata = entity.metadata,
                            updatedAt = entity.updatedAt
                        ),
                        isSupplierCompany = true
                    )
                }
                return supplierPartTable.getSupplierPartsForCompany(companyId)
            }
        }
        return cached
    }

    fun getSupplierPartsForPart(partId: Long): List<SupplierPart> {
        val cached = supplierPartTable.getSupplierPartsForPart(partId)
        val entities = supplierPartDao.getForPart("part-$partId")
        if (entities.isNotEmpty()) {
            val cachedUuids = cached.map { "sup-part-${it.id}" }.toSet()
            val missing = entities.filter { it.uuid !in cachedUuids }
            if (missing.isNotEmpty()) {
                missing.forEach { entity ->
                    val parsedId = entity.uuid.removePrefix("sup-part-").toLongOrNull() ?: 0L
                    val supplierId = entity.supplierUuid.removePrefix("company-").toLongOrNull() ?: 0L
                    val mfgPartId = entity.manufacturerPartUuid?.removePrefix("mfg-part-")?.toLongOrNull()
                    supplierPartTable.insertSupplierPart(
                        SupplierPart(
                            id = parsedId,
                            partId = partId,
                            supplierId = supplierId,
                            sku = entity.sku,
                            manufacturerPartId = mfgPartId,
                            description = entity.description,
                            link = entity.link,
                            note = entity.note,
                            packaging = entity.packaging,
                            packQuantity = entity.packQuantity,
                            availableForPurchase = entity.availableForPurchase,
                            active = entity.active,
                            metadata = entity.metadata,
                            updatedAt = entity.updatedAt
                        ),
                        isSupplierCompany = true
                    )
                }
                return supplierPartTable.getSupplierPartsForPart(partId)
            }
        }
        return cached
    }

    fun addSupplierPart(part: SupplierPart): SupplierPart {
        val company = getCompanyById(part.supplierId)
        val isSup = company?.isSupplier ?: true
        val inserted = supplierPartTable.insertSupplierPart(part, isSupplierCompany = isSup)
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
        return inserted
    }

    fun deleteSupplierPart(id: Long): Boolean {
        val deleted = supplierPartTable.deleteSupplierPart(id)
        if (deleted) {
            supplierPartDao.delete("sup-part-$id")
        }
        return deleted
    }

    // --- شرائح الأسعار (Supplier Price Breaks) ---

    fun getPriceBreaksForSupplierPart(supplierPartId: Long): List<SupplierPriceBreak> {
        val cached = supplierPriceBreakTable.getPriceBreaksForSupplierPart(supplierPartId)
        val entities = supplierPriceBreakDao.getForSupplierPart("sup-part-$supplierPartId")
        if (entities.isNotEmpty()) {
            val cachedUuids = cached.map { "price-break-${it.id}" }.toSet()
            val missing = entities.filter { it.uuid !in cachedUuids }
            if (missing.isNotEmpty()) {
                missing.forEach { entity ->
                    val parsedId = entity.uuid.removePrefix("price-break-").toLongOrNull() ?: 0L
                    supplierPriceBreakTable.insertPriceBreak(
                        SupplierPriceBreak(
                            id = parsedId,
                            supplierPartId = supplierPartId,
                            quantity = entity.quantity,
                            price = entity.price,
                            priceCurrency = entity.priceCurrency,
                            packQuantity = entity.packQuantity
                        )
                    )
                }
                return supplierPriceBreakTable.getPriceBreaksForSupplierPart(supplierPartId)
            }
        }
        return cached
    }

    fun addPriceBreak(priceBreak: SupplierPriceBreak, defaultCompanyCurrency: String = "USD"): SupplierPriceBreak {
        val inserted = supplierPriceBreakTable.insertPriceBreak(priceBreak, defaultCompanyCurrency)
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
        return inserted
    }

    fun deletePriceBreak(id: Long): Boolean {
        val deleted = supplierPriceBreakTable.deletePriceBreak(id)
        if (deleted) {
            supplierPriceBreakDao.delete("price-break-$id")
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

    fun getCompanies(): List<Company> {
        val cached = companyTable.getAllCompanies()
        val persisted = searchCompanies()
        if (persisted.isEmpty()) return cached
        val persistedMap = persisted.associateBy { it.id }
        val cachedMap = cached.associateBy { it.id }
        return (persistedMap + (cachedMap - persistedMap.keys)).values.toList()
    }
}
