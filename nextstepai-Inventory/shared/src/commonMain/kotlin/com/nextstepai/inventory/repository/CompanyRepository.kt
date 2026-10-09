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

    private fun resolveCompanyId(uuid: String): Long? {
        val legacyId = uuid.removePrefix("company-").toLongOrNull()
        if (legacyId != null && legacyId > 0L) return legacyId
        return companyDao.getCompanyByUuid(uuid)?.id?.takeIf { it > 0L }
    }

    private fun resolveCompanyUuid(id: Long): String =
        companyDao.getCompanyById(id)?.uuid ?: "company-$id"

    private fun CompanyEntity.toCompany(): Company {
        val parsedId = id.takeIf { it > 0L } ?: uuid.removePrefix("company-").toLongOrNull() ?: 0L
        return Company(
            id = parsedId,
            uuid = uuid,
            name = name,
            description = description,
            website = website,
            phone = phone,
            email = email,
            isSupplier = isSupplier,
            isManufacturer = isManufacturer,
            isCustomer = isCustomer,
            active = active,
            currency = currency,
            imageUrl = logoPath,
            notes = notes,
            metadata = metadata,
            parentId = parentUuid?.let { resolveCompanyId(it) }
        )
    }

    /**
     * جلب كافة الشركات المتاحة من قاعدة البيانات الدائمة (SQLite) دون دمج ذاكرة قديمة.
     */
    fun getCompanies(): List<Company> {
        val pageSize = 250
        val result = mutableListOf<Company>()
        var offset = 0
        while (true) {
            val page = companyDao.getCompaniesPaged(limit = pageSize, offset = offset)
            result += page.map { it.toCompany() }
            if (page.size < pageSize) break
            offset += page.size
        }
        return result
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
        val q = query.trim().lowercase()
        return getCompanies().filter { comp ->
            val matchesActive = !activeOnly || comp.active
            val matchesSupplier = !supplierOnly || comp.isSupplier
            val matchesMfg = !manufacturerOnly || comp.isManufacturer
            val matchesCustomer = !customerOnly || comp.isCustomer
            val matchesQuery = q.isEmpty() ||
                comp.name.lowercase().contains(q) ||
                comp.description.lowercase().contains(q) ||
                comp.email.lowercase().contains(q)
            matchesActive && matchesSupplier && matchesMfg && matchesCustomer && matchesQuery
        }
    }

    /**
     * جلب شركة حسب المعرف من SQLite مباشرة.
     */
    fun getCompanyById(id: Long): Company? =
        companyDao.getCompanyById(id)?.toCompany()

    fun getCompanyByUuid(uuid: String): Company? {
        val entity = companyDao.getCompanyByUuid(uuid)
        return entity?.toCompany()
    }

    /**
     * إضافة شركة جديدة مع تخصيص المعرف الرقمي من SQLite وحفظ الكيان في قاعدة البيانات الدائمة أولاً.
     */
    fun addCompany(company: Company): Company {
        val existingWithSameName = companyDao.getCompanyByName(company.name)
        require(existingWithSameName == null) { "اسم الشركة '${company.name}' مسجل مسبقاً، لا يمكن تكرار الاسم." }

        val allocatedId = if (company.id > 0L) company.id else SqliteNumericIdAllocator.nextId("companies", "company-")
        val companyWithId = company.copy(
            id = allocatedId,
            uuid = if (company.uuid.isNotBlank()) company.uuid else "company-$allocatedId"
        )
        val now = Clock.System.now().toEpochMilliseconds()
        val inserted = companyDao.insert(
            CompanyEntity(
                uuid = companyWithId.effectiveUuid,
                name = companyWithId.name,
                description = companyWithId.description,
                website = companyWithId.website,
                phone = companyWithId.phone,
                email = companyWithId.email,
                isSupplier = companyWithId.isSupplier,
                isManufacturer = companyWithId.isManufacturer,
                isCustomer = companyWithId.isCustomer,
                active = companyWithId.active,
                currency = companyWithId.currency,
                logoPath = companyWithId.imageUrl,
                notes = companyWithId.notes,
                metadata = companyWithId.metadata,
                parentUuid = companyWithId.parentId?.let(::resolveCompanyUuid),
                syncStatus = SyncStatus.PENDING,
                isDeleted = false,
                updatedAt = now,
                id = companyWithId.id
            )
        )
        if (!inserted) {
            throw IllegalStateException("فشل حفظ بيانات الشركة في قاعدة البيانات")
        }
        runCatching { companyTable.insertCompany(companyWithId) }
        return companyWithId
    }

    /**
     * تحديث شركة حالية في SQLite مباشرة مع مزامنة جدول الذاكرة.
     */
    fun updateCompany(company: Company): Company {
        val targetUuid = company.effectiveUuid
        val existing = companyDao.getCompanyByUuid(targetUuid)
        requireNotNull(existing) { "الشركة المطلوب تعديلها غير موجودة بالمنظومة" }

        val existingWithSameName = companyDao.getCompanyByName(company.name)
        if (existingWithSameName != null && existingWithSameName.uuid != targetUuid) {
            throw IllegalArgumentException("اسم الشركة '${company.name}' مسجل مسبقاً، لا يمكن تكرار الاسم.")
        }

        val now = Clock.System.now().toEpochMilliseconds()
        val updated = companyDao.update(
            CompanyEntity(
                uuid = targetUuid,
                name = company.name,
                description = company.description,
                website = company.website,
                phone = company.phone,
                email = company.email,
                isSupplier = company.isSupplier,
                isManufacturer = company.isManufacturer,
                isCustomer = company.isCustomer,
                active = company.active,
                currency = company.currency,
                logoPath = company.imageUrl,
                notes = company.notes,
                metadata = company.metadata,
                parentUuid = company.parentId?.let(::resolveCompanyUuid),
                version = existing.version + 1,
                syncStatus = SyncStatus.PENDING,
                isDeleted = false,
                updatedAt = now
            )
        )
        if (!updated) {
            throw IllegalStateException("فشل تحديث بيانات الشركة في قاعدة البيانات")
        }
        runCatching { companyTable.updateCompany(company) }
        return company
    }

    /**
     * حذف شركة منطقياً من SQLite.
     */
    fun deleteCompany(id: Long): Boolean {
        val company = getCompanyById(id) ?: return false
        val deleted = companyDao.delete(company.effectiveUuid)
        if (deleted) {
            runCatching { companyTable.deleteCompany(id) }
        }
        return deleted
    }

    fun deleteCompanyByUuid(uuid: String): Boolean {
        val current = companyDao.getCompanyByUuid(uuid)
        val deleted = companyDao.delete(uuid)
        if (deleted) {
            val parsedId = current?.id ?: uuid.removePrefix("company-").toLongOrNull()
            if (parsedId != null && parsedId > 0L) {
                runCatching { companyTable.deleteCompany(parsedId) }
            }
        }
        return deleted
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

    // --- مابرز الكيانات التابعة (Sub-Entity Mappings) ---

    private fun CompanyAttachmentEntity.toCompanyAttachment(): CompanyAttachment {
        val parsedId = uuid.removePrefix("company-att-").toLongOrNull() ?: 0L
        val parsedCompanyId = resolveCompanyId(companyUuid) ?: 0L
        return CompanyAttachment(
            id = parsedId,
            companyId = parsedCompanyId,
            documentType = documentType,
            attachmentPath = attachmentPath,
            link = link,
            comment = comment,
            uploadDate = uploadDate,
            userId = userId,
            expiryDate = expiryDate,
            notifyOnExpiry = notifyOnExpiry,
            notificationDaysBefore = notificationDaysBefore
        )
    }

    private fun ContactEntity.toContact(): Contact {
        val parsedId = uuid.removePrefix("contact-").toLongOrNull() ?: 0L
        val parsedCompanyId = resolveCompanyId(companyUuid) ?: 0L
        return Contact(
            id = parsedId,
            companyId = parsedCompanyId,
            name = name,
            phone = phone,
            email = email,
            role = role,
            isPrimary = isPrimary
        )
    }

    private fun AddressEntity.toAddress(): Address {
        val parsedId = uuid.removePrefix("address-").toLongOrNull() ?: 0L
        val parsedCompanyId = resolveCompanyId(companyUuid) ?: 0L
        return Address(
            id = parsedId,
            companyId = parsedCompanyId,
            title = title,
            isPrimary = isPrimary,
            line1 = line1,
            line2 = line2,
            postalCode = postalCode,
            city = city,
            province = province,
            country = country,
            shippingNotes = shippingNotes
        )
    }

    private fun CompanyBankAccountEntity.toCompanyBankAccount(): CompanyBankAccount {
        val parsedId = uuid.removePrefix("bank-").toLongOrNull() ?: 0L
        val parsedCompanyId = resolveCompanyId(companyUuid) ?: 0L
        return CompanyBankAccount(
            id = parsedId,
            companyId = parsedCompanyId,
            bankName = bankName,
            accountName = accountName,
            accountNumber = accountNumber,
            iban = iban,
            swiftBic = swiftBic,
            currency = currency,
            branchName = branchName,
            isPrimary = isPrimary,
            updatedAt = updatedAt
        )
    }

    private fun CompanyLegalRecordEntity.toCompanyLegalRecord(): CompanyLegalRecord {
        val parsedId = uuid.removePrefix("legal-").toLongOrNull() ?: 0L
        val parsedCompanyId = resolveCompanyId(companyUuid) ?: 0L
        return CompanyLegalRecord(
            id = parsedId,
            companyId = parsedCompanyId,
            commercialRegisterNumber = commercialRegisterNumber,
            taxId = taxId,
            nationalIdNumber = nationalIdNumber,
            importLicenseNumber = importLicenseNumber,
            manufacturingLicenseNumber = manufacturingLicenseNumber,
            activityCodes = activityCodes,
            issuingAuthority = issuingAuthority,
            issueDate = issueDate,
            expiryDate = expiryDate,
            updatedAt = updatedAt
        )
    }

    private fun ManufacturerPartEntity.toManufacturerPart(): ManufacturerPart {
        val parsedId = uuid.removePrefix("mfg-part-").toLongOrNull() ?: 0L
        val parsedPartId = partUuid.removePrefix("part-").toLongOrNull() ?: 0L
        val parsedMfgId = resolveCompanyId(manufacturerUuid) ?: 0L
        return ManufacturerPart(
            id = parsedId,
            partId = parsedPartId,
            manufacturerId = parsedMfgId,
            mpn = mpn,
            description = description,
            link = link,
            metadata = metadata,
            updatedAt = updatedAt
        )
    }

    private fun ManufacturerPartParameterEntity.toManufacturerPartParameter(): ManufacturerPartParameter {
        val parsedId = uuid.removePrefix("mfg-param-").toLongOrNull() ?: 0L
        val parsedMfgPartId = manufacturerPartUuid.removePrefix("mfg-part-").toLongOrNull() ?: 0L
        return ManufacturerPartParameter(
            id = parsedId,
            manufacturerPartId = parsedMfgPartId,
            name = name,
            value = value,
            units = units
        )
    }

    private fun ManufacturerPartAttachmentEntity.toManufacturerPartAttachment(): ManufacturerPartAttachment {
        val parsedId = uuid.removePrefix("mfg-part-att-").toLongOrNull() ?: 0L
        val parsedMfgPartId = manufacturerPartUuid.removePrefix("mfg-part-").toLongOrNull() ?: 0L
        return ManufacturerPartAttachment(
            id = parsedId,
            manufacturerPartId = parsedMfgPartId,
            attachmentPath = attachmentPath,
            link = link,
            comment = comment,
            uploadDate = uploadDate,
            userId = userId
        )
    }

    private fun SupplierPartEntity.toSupplierPart(): SupplierPart {
        val parsedId = uuid.removePrefix("sup-part-").toLongOrNull() ?: 0L
        val parsedPartId = partUuid.removePrefix("part-").toLongOrNull() ?: 0L
        val parsedSupId = resolveCompanyId(supplierUuid) ?: 0L
        val parsedMfgPartId = manufacturerPartUuid?.removePrefix("mfg-part-")?.toLongOrNull()
        return SupplierPart(
            id = parsedId,
            partId = parsedPartId,
            supplierId = parsedSupId,
            sku = sku,
            manufacturerPartId = parsedMfgPartId,
            description = description,
            link = link,
            note = note,
            packaging = packaging,
            packQuantity = packQuantity,
            availableForPurchase = availableForPurchase,
            active = active,
            metadata = metadata,
            updatedAt = updatedAt
        )
    }

    private fun SupplierPriceBreakEntity.toSupplierPriceBreak(): SupplierPriceBreak {
        val parsedId = uuid.removePrefix("price-break-").toLongOrNull() ?: 0L
        val parsedSupPartId = supplierPartUuid.removePrefix("sup-part-").toLongOrNull() ?: 0L
        return SupplierPriceBreak(
            id = parsedId,
            supplierPartId = parsedSupPartId,
            quantity = quantity,
            price = price,
            priceCurrency = priceCurrency,
            packQuantity = packQuantity,
            updatedAt = updatedAt
        )
    }

    // --- مرفقات الشركات العامة (Company Attachments) ---

    fun getAttachmentsForCompany(companyId: Long): List<CompanyAttachment> {
        return companyAttachmentDao.getForCompany(resolveCompanyUuid(companyId)).map { it.toCompanyAttachment() }
    }

    fun addCompanyAttachment(attachment: CompanyAttachment): CompanyAttachment {
        require(attachment.companyId != 0L) { "معرف الشركة إلزامي لربط المرفق" }
        val finalPath = if (attachment.attachmentPath.isBlank() && attachment.link.isBlank()) {
            "${attachment.documentType.ifBlank { "وثيقة" }}.pdf"
        } else attachment.attachmentPath

        val uploadDate = if (attachment.uploadDate == 0L) Clock.System.now().toEpochMilliseconds() else attachment.uploadDate
        val allocatedId = if (attachment.id > 0L) attachment.id else SqliteNumericIdAllocator.nextId("company_attachments", "company-att-")
        val attachmentToInsert = attachment.copy(
            id = allocatedId,
            attachmentPath = finalPath,
            uploadDate = uploadDate
        )
        companyAttachmentDao.insertOrUpdate(
            CompanyAttachmentEntity(
                uuid = "company-att-$allocatedId",
                companyUuid = resolveCompanyUuid(attachmentToInsert.companyId),
                documentType = attachmentToInsert.documentType,
                attachmentPath = attachmentToInsert.attachmentPath,
                link = attachmentToInsert.link,
                comment = attachmentToInsert.comment,
                uploadDate = attachmentToInsert.uploadDate,
                userId = attachmentToInsert.userId,
                expiryDate = attachmentToInsert.expiryDate,
                notifyOnExpiry = attachmentToInsert.notifyOnExpiry,
                notificationDaysBefore = attachmentToInsert.notificationDaysBefore,
                syncStatus = SyncStatus.PENDING
            )
        )
        runCatching { companyAttachmentTable.insertAttachment(attachmentToInsert) }
        return attachmentToInsert
    }

    fun deleteCompanyAttachment(id: Long): Boolean {
        val deleted = companyAttachmentDao.delete("company-att-$id")
        if (deleted) {
            runCatching { companyAttachmentTable.deleteAttachment(id) }
        }
        return deleted
    }

    // --- جهات الاتصال (Contacts) ---

    fun getContactsForCompany(companyId: Long): List<Contact> {
        return contactDao.getContactsForCompany(resolveCompanyUuid(companyId)).map { it.toContact() }
    }

    fun addContact(contact: Contact): Contact {
        require(contact.name.isNotBlank()) { "اسم جهة الاتصال إلزامي ولا يمكن أن يكون فارغاً" }
        require(contact.companyId != 0L) { "معرف الشركة إلزامي لربط جهة الاتصال" }

        val existing = contactDao.getContactsForCompany(resolveCompanyUuid(contact.companyId))
        val isFirst = existing.isEmpty()
        val shouldBePrimary = contact.isPrimary || isFirst

        val allocatedId = if (contact.id > 0L) contact.id else SqliteNumericIdAllocator.nextId("contacts", "contact-")
        val contactToInsert = contact.copy(id = allocatedId, isPrimary = shouldBePrimary)
        contactDao.insertOrUpdate(
            ContactEntity(
                uuid = "contact-$allocatedId",
                companyUuid = resolveCompanyUuid(contactToInsert.companyId),
                name = contactToInsert.name,
                phone = contactToInsert.phone,
                email = contactToInsert.email,
                role = contactToInsert.role,
                isPrimary = contactToInsert.isPrimary,
                syncStatus = SyncStatus.PENDING
            )
        )
        runCatching { contactTable.insertContact(contactToInsert) }
        return contactToInsert
    }

    fun deleteContact(contactId: Long): Boolean {
        val deleted = contactDao.delete("contact-$contactId")
        if (deleted) {
            runCatching { contactTable.deleteContact(contactId) }
        }
        return deleted
    }

    // --- العناوين (Addresses) ---

    fun getAddressesForCompany(companyId: Long): List<Address> {
        return addressDao.getAddressesForCompany(resolveCompanyUuid(companyId)).map { it.toAddress() }
    }

    fun addAddress(address: Address): Address {
        require(address.line1.isNotBlank()) { "السطر الأول من العنوان إلزامي" }
        require(address.companyId != 0L) { "معرف الشركة إلزامي لربط العنوان" }

        val existing = addressDao.getAddressesForCompany(resolveCompanyUuid(address.companyId))
        val isFirst = existing.isEmpty()
        val shouldBePrimary = address.isPrimary || isFirst

        val allocatedId = if (address.id > 0L) address.id else SqliteNumericIdAllocator.nextId("addresses", "address-")
        val addressToInsert = address.copy(id = allocatedId, isPrimary = shouldBePrimary)
        addressDao.insertOrUpdate(
            AddressEntity(
                uuid = "address-$allocatedId",
                companyUuid = resolveCompanyUuid(addressToInsert.companyId),
                title = addressToInsert.title,
                isPrimary = addressToInsert.isPrimary,
                line1 = addressToInsert.line1,
                line2 = addressToInsert.line2,
                postalCode = addressToInsert.postalCode,
                city = addressToInsert.city,
                province = addressToInsert.province,
                country = addressToInsert.country,
                shippingNotes = addressToInsert.shippingNotes,
                syncStatus = SyncStatus.PENDING
            )
        )
        runCatching { addressTable.insertAddress(addressToInsert) }
        return addressToInsert
    }

    fun deleteAddress(addressId: Long): Boolean {
        val deleted = addressDao.delete("address-$addressId")
        if (deleted) {
            runCatching { addressTable.deleteAddress(addressId) }
        }
        return deleted
    }

    // --- الحسابات البنكية للشركة (Company Bank Accounts) ---

    fun getBankAccountsForCompany(companyId: Long): List<CompanyBankAccount> {
        return companyBankAccountDao.getForCompany(resolveCompanyUuid(companyId)).map { it.toCompanyBankAccount() }
    }

    fun addBankAccount(account: CompanyBankAccount): CompanyBankAccount {
        require(account.companyId != 0L) { "معرف الشركة إلزامي لربط الحساب البنكي" }
        require(account.bankName.isNotBlank()) { "اسم البنك إلزامي" }
        require(account.accountName.isNotBlank()) { "اسم صاحب الحساب إلزامي" }

        val allocatedId = if (account.id > 0L) account.id else SqliteNumericIdAllocator.nextId("company_bank_accounts", "bank-")
        val now = Clock.System.now().toEpochMilliseconds()
        val accountToInsert = account.copy(id = allocatedId, updatedAt = now)
        companyBankAccountDao.insertOrUpdate(
            CompanyBankAccountEntity(
                uuid = "bank-$allocatedId",
                companyUuid = resolveCompanyUuid(accountToInsert.companyId),
                bankName = accountToInsert.bankName,
                accountName = accountToInsert.accountName,
                accountNumber = accountToInsert.accountNumber,
                iban = accountToInsert.iban,
                swiftBic = accountToInsert.swiftBic,
                currency = accountToInsert.currency,
                branchName = accountToInsert.branchName,
                isPrimary = accountToInsert.isPrimary,
                syncStatus = SyncStatus.PENDING,
                updatedAt = now
            )
        )
        runCatching { companyBankAccountTable.insertBankAccount(accountToInsert) }
        return accountToInsert
    }

    fun deleteBankAccount(accountId: Long): Boolean {
        val deleted = companyBankAccountDao.delete("bank-$accountId")
        if (deleted) {
            runCatching { companyBankAccountTable.deleteBankAccount(accountId) }
        }
        return deleted
    }

    // --- السجلات القانونية والتراخيص (Company Legal Records) ---

    fun getLegalRecordForCompany(companyId: Long): CompanyLegalRecord? {
        return companyLegalRecordDao.getForCompany(resolveCompanyUuid(companyId))?.toCompanyLegalRecord()
    }

    fun saveOrUpdateLegalRecord(record: CompanyLegalRecord): CompanyLegalRecord {
        require(record.companyId != 0L) { "معرف الشركة إلزامي لربط السجل القانوني" }

        val existing = companyLegalRecordDao.getForCompany(resolveCompanyUuid(record.companyId))
        val allocatedId = if (record.id > 0L) {
            record.id
        } else if (existing != null) {
            existing.uuid.removePrefix("legal-").toLongOrNull() ?: SqliteNumericIdAllocator.nextId("company_legal_records", "legal-")
        } else {
            SqliteNumericIdAllocator.nextId("company_legal_records", "legal-")
        }
        val now = Clock.System.now().toEpochMilliseconds()
        val recordToSave = record.copy(id = allocatedId, updatedAt = now)
        companyLegalRecordDao.insertOrUpdate(
            CompanyLegalRecordEntity(
                uuid = "legal-$allocatedId",
                companyUuid = resolveCompanyUuid(recordToSave.companyId),
                commercialRegisterNumber = recordToSave.commercialRegisterNumber,
                taxId = recordToSave.taxId,
                nationalIdNumber = recordToSave.nationalIdNumber,
                importLicenseNumber = recordToSave.importLicenseNumber,
                manufacturingLicenseNumber = recordToSave.manufacturingLicenseNumber,
                activityCodes = recordToSave.activityCodes,
                issuingAuthority = recordToSave.issuingAuthority,
                issueDate = recordToSave.issueDate,
                expiryDate = recordToSave.expiryDate,
                syncStatus = SyncStatus.PENDING,
                updatedAt = now
            )
        )
        runCatching { companyLegalRecordTable.saveOrUpdateLegalRecord(recordToSave) }
        return recordToSave
    }

    fun deleteLegalRecord(companyId: Long): Boolean {
        val record = getLegalRecordForCompany(companyId) ?: return false
        val deleted = companyLegalRecordDao.delete("legal-${record.id}")
        if (deleted) {
            runCatching { companyLegalRecordTable.deleteLegalRecord(record.id) }
        }
        return deleted
    }

    // --- قطع المصنّع (Manufacturer Parts) ---

    fun getManufacturerPartsForCompany(companyId: Long): List<ManufacturerPart> {
        return manufacturerPartDao.getForCompany(resolveCompanyUuid(companyId)).map { it.toManufacturerPart() }
    }

    fun getManufacturerPartsForPart(partId: Long): List<ManufacturerPart> {
        return manufacturerPartDao.getForPart("part-$partId").map { it.toManufacturerPart() }
    }

    fun addManufacturerPart(part: ManufacturerPart): ManufacturerPart {
        val company = getCompanyById(part.manufacturerId)
        val isMfg = company?.isManufacturer ?: true
        require(isMfg) { "لا يمكن إضافة قطعة مصنّع لشركة غير معرفة كمصنّع (isManufacturer = false)" }
        require(part.mpn.isNotBlank()) { "رقم القطعة لدى المصنّع (MPN) إلزامي" }
        require(part.manufacturerId != 0L) { "معرف المصنّع إلزامي" }
        require(part.partId != 0L) { "معرف القطعة الداخلية إلزامي" }

        val existing = getManufacturerPartsForCompany(part.manufacturerId)
        val duplicate = existing.any {
            it.mpn.trim().equals(part.mpn.trim(), ignoreCase = true) && it.id != part.id
        }
        require(!duplicate) { "رقم القطعة للمصنّع (MPN '${part.mpn}') مسجل بالفعل لهذا المصنّع." }

        val allocatedId = if (part.id > 0L) part.id else SqliteNumericIdAllocator.nextId("manufacturer_parts", "mfg-part-")
        val now = Clock.System.now().toEpochMilliseconds()
        val partToInsert = part.copy(id = allocatedId, updatedAt = now)
        manufacturerPartDao.insertOrUpdate(
            ManufacturerPartEntity(
                uuid = "mfg-part-$allocatedId",
                partUuid = "part-${partToInsert.partId}",
                manufacturerUuid = resolveCompanyUuid(partToInsert.manufacturerId),
                mpn = partToInsert.mpn,
                description = partToInsert.description,
                link = partToInsert.link,
                metadata = partToInsert.metadata,
                syncStatus = SyncStatus.PENDING,
                updatedAt = now
            )
        )
        runCatching { manufacturerPartTable.insertManufacturerPart(partToInsert, isManufacturerCompany = isMfg) }
        return partToInsert
    }

    fun deleteManufacturerPart(id: Long): Boolean {
        val deleted = manufacturerPartDao.delete("mfg-part-$id")
        if (deleted) {
            runCatching { manufacturerPartTable.deleteManufacturerPart(id) }
        }
        return deleted
    }

    // --- الخصائص الفنية لقطع المصنّع (Manufacturer Part Parameters) ---

    fun getParametersForManufacturerPart(manufacturerPartId: Long): List<ManufacturerPartParameter> {
        return manufacturerPartParameterDao.getForManufacturerPart("mfg-part-$manufacturerPartId")
            .map { it.toManufacturerPartParameter() }
    }

    fun addManufacturerPartParameter(parameter: ManufacturerPartParameter): ManufacturerPartParameter {
        require(parameter.manufacturerPartId != 0L) { "معرف قطعة المصنّع إلزامي" }
        require(parameter.name.isNotBlank()) { "اسم المعامل الفني إلزامي" }

        val existing = getParametersForManufacturerPart(parameter.manufacturerPartId)
        val duplicate = existing.any {
            it.name.trim().equals(parameter.name.trim(), ignoreCase = true) && it.id != parameter.id
        }
        require(!duplicate) { "المعامل التقني ('${parameter.name}') مسجل بالفعل لهذه القطعة المصنّعة." }

        val allocatedId = if (parameter.id > 0L) parameter.id else SqliteNumericIdAllocator.nextId("manufacturer_part_parameters", "mfg-param-")
        val parameterToInsert = parameter.copy(id = allocatedId)
        manufacturerPartParameterDao.insertOrUpdate(
            ManufacturerPartParameterEntity(
                uuid = "mfg-param-$allocatedId",
                manufacturerPartUuid = "mfg-part-${parameterToInsert.manufacturerPartId}",
                name = parameterToInsert.name,
                value = parameterToInsert.value,
                units = parameterToInsert.units,
                syncStatus = SyncStatus.PENDING
            )
        )
        runCatching { manufacturerPartParameterTable.insertParameter(parameterToInsert) }
        return parameterToInsert
    }

    fun deleteManufacturerPartParameter(id: Long): Boolean {
        val deleted = manufacturerPartParameterDao.delete("mfg-param-$id")
        if (deleted) {
            runCatching { manufacturerPartParameterTable.deleteParameter(id) }
        }
        return deleted
    }

    // --- مرفقات قطع المصنّع (Manufacturer Part Attachments) ---

    fun getAttachmentsForManufacturerPart(manufacturerPartId: Long): List<ManufacturerPartAttachment> {
        return manufacturerPartAttachmentDao.getForManufacturerPart("mfg-part-$manufacturerPartId")
            .map { it.toManufacturerPartAttachment() }
    }

    fun addManufacturerPartAttachment(attachment: ManufacturerPartAttachment): ManufacturerPartAttachment {
        require(attachment.manufacturerPartId != 0L) { "معرف قطعة المصنّع إلزامي لربط المرفق" }
        require(attachment.attachmentPath.isNotBlank() || attachment.link.isNotBlank()) { "يجب تزويد مسار الملف المرفق أو الرابط الإلكتروني" }

        val uploadDate = if (attachment.uploadDate == 0L) Clock.System.now().toEpochMilliseconds() else attachment.uploadDate
        val allocatedId = if (attachment.id > 0L) attachment.id else SqliteNumericIdAllocator.nextId("manufacturer_part_attachments", "mfg-part-att-")
        val attachmentToInsert = attachment.copy(id = allocatedId, uploadDate = uploadDate)
        manufacturerPartAttachmentDao.insertOrUpdate(
            ManufacturerPartAttachmentEntity(
                uuid = "mfg-part-att-$allocatedId",
                manufacturerPartUuid = "mfg-part-${attachmentToInsert.manufacturerPartId}",
                attachmentPath = attachmentToInsert.attachmentPath,
                link = attachmentToInsert.link,
                comment = attachmentToInsert.comment,
                uploadDate = attachmentToInsert.uploadDate,
                userId = attachmentToInsert.userId,
                syncStatus = SyncStatus.PENDING
            )
        )
        runCatching { manufacturerPartAttachmentTable.insertAttachment(attachmentToInsert) }
        return attachmentToInsert
    }

    fun deleteManufacturerPartAttachment(id: Long): Boolean {
        val deleted = manufacturerPartAttachmentDao.delete("mfg-part-att-$id")
        if (deleted) {
            runCatching { manufacturerPartAttachmentTable.deleteAttachment(id) }
        }
        return deleted
    }

    // --- قطع الموردين (Supplier Parts) ---

    fun getSupplierPartsForCompany(companyId: Long): List<SupplierPart> {
        return supplierPartDao.getForCompany(resolveCompanyUuid(companyId)).map { it.toSupplierPart() }
    }

    fun getSupplierPartsForPart(partId: Long): List<SupplierPart> {
        return supplierPartDao.getForPart("part-$partId").map { it.toSupplierPart() }
    }

    fun addSupplierPart(part: SupplierPart): SupplierPart {
        val company = getCompanyById(part.supplierId)
        val isSup = company?.isSupplier ?: true
        require(isSup) { "لا يمكن إضافة قطعة مورد لشركة غير معرفة كمورد (isSupplier = false)" }
        require(part.sku.isNotBlank()) { "كود المورد الخاص (SKU) إلزامي" }
        require(part.supplierId != 0L) { "معرف المورد إلزامي" }
        require(part.partId != 0L) { "معرف القطعة الداخلية إلزامي" }

        val existing = getSupplierPartsForCompany(part.supplierId)
        val duplicate = existing.any {
            it.sku.trim().equals(part.sku.trim(), ignoreCase = true) && it.id != part.id
        }
        require(!duplicate) { "كود المورد (SKU '${part.sku}') مسجل بالفعل لدى هذا المورد." }

        val allocatedId = if (part.id > 0L) part.id else SqliteNumericIdAllocator.nextId("supplier_parts", "sup-part-")
        val now = Clock.System.now().toEpochMilliseconds()
        val partToInsert = part.copy(id = allocatedId, updatedAt = now)
        supplierPartDao.insertOrUpdate(
            SupplierPartEntity(
                uuid = "sup-part-$allocatedId",
                partUuid = "part-${partToInsert.partId}",
                supplierUuid = resolveCompanyUuid(partToInsert.supplierId),
                sku = partToInsert.sku,
                manufacturerPartUuid = partToInsert.manufacturerPartId?.let { "mfg-part-$it" },
                description = partToInsert.description,
                link = partToInsert.link,
                note = partToInsert.note,
                packaging = partToInsert.packaging,
                packQuantity = partToInsert.packQuantity,
                availableForPurchase = partToInsert.availableForPurchase,
                active = partToInsert.active,
                metadata = partToInsert.metadata,
                syncStatus = SyncStatus.PENDING,
                updatedAt = now
            )
        )
        runCatching { supplierPartTable.insertSupplierPart(partToInsert, isSupplierCompany = isSup) }
        return partToInsert
    }

    fun deleteSupplierPart(id: Long): Boolean {
        val deleted = supplierPartDao.delete("sup-part-$id")
        if (deleted) {
            runCatching { supplierPartTable.deleteSupplierPart(id) }
        }
        return deleted
    }

    // --- شرائح الأسعار (Supplier Price Breaks) ---

    fun getPriceBreaksForSupplierPart(supplierPartId: Long): List<SupplierPriceBreak> {
        return supplierPriceBreakDao.getForSupplierPart("sup-part-$supplierPartId")
            .map { it.toSupplierPriceBreak() }
            .sortedBy { it.quantity }
    }

    fun addPriceBreak(priceBreak: SupplierPriceBreak, defaultCompanyCurrency: String = "USD"): SupplierPriceBreak {
        require(priceBreak.supplierPartId != 0L) { "معرف قطعة المورد إلزامي" }
        require(priceBreak.quantity > 0) { "الكمية يجب أن تكون أكبر من 0" }
        require(priceBreak.price >= 0) { "السعر يجب أن لا يكون بالسالب" }

        val existing = getPriceBreaksForSupplierPart(priceBreak.supplierPartId)
        val duplicate = existing.any {
            it.quantity == priceBreak.quantity && it.id != priceBreak.id
        }
        require(!duplicate) { "شريحة السعر للكمية (${priceBreak.quantity}) مسجلة بالفعل لهذه القطعة." }

        val currencyToUse = priceBreak.priceCurrency.ifBlank { defaultCompanyCurrency }
        val allocatedId = if (priceBreak.id > 0L) priceBreak.id else SqliteNumericIdAllocator.nextId("supplier_price_breaks", "price-break-")
        val now = Clock.System.now().toEpochMilliseconds()
        val breakToInsert = priceBreak.copy(
            id = allocatedId,
            priceCurrency = currencyToUse,
            updatedAt = now
        )
        supplierPriceBreakDao.insertOrUpdate(
            SupplierPriceBreakEntity(
                uuid = "price-break-$allocatedId",
                supplierPartUuid = "sup-part-${breakToInsert.supplierPartId}",
                quantity = breakToInsert.quantity,
                price = breakToInsert.price,
                priceCurrency = breakToInsert.priceCurrency,
                packQuantity = breakToInsert.packQuantity,
                syncStatus = SyncStatus.PENDING,
                updatedAt = now
            )
        )
        runCatching { supplierPriceBreakTable.insertPriceBreak(breakToInsert, defaultCompanyCurrency) }
        return breakToInsert
    }

    fun deletePriceBreak(id: Long): Boolean {
        val deleted = supplierPriceBreakDao.delete("price-break-$id")
        if (deleted) {
            runCatching { supplierPriceBreakTable.deletePriceBreak(id) }
        }
        return deleted
    }

    fun getBestPriceForQuantity(supplierPartId: Long, quantity: Double): SupplierPriceBreak? {
        val tiers = getPriceBreaksForSupplierPart(supplierPartId)
        if (tiers.isEmpty()) return null
        return tiers.filter { it.quantity <= quantity }.maxByOrNull { it.quantity } ?: tiers.minByOrNull { it.quantity }
    }

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
}
