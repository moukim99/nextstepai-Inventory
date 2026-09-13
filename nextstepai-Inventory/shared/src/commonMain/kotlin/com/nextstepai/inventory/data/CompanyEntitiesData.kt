package com.nextstepai.inventory.data

import kotlin.time.Clock

/**
 * نموذج البيانات لجهة اتصال تابعة لشركة (Contact).
 */
data class Contact(
    val id: Long = 0L,
    val companyId: Long,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val role: String = ""
)

/**
 * نموذج البيانات لعنوان تابع لشركة (Address).
 */
data class Address(
    val id: Long = 0L,
    val companyId: Long,
    val title: String = "الفرع الرئيسي",
    val isPrimary: Boolean = false,
    val line1: String,
    val line2: String = "",
    val postalCode: String = "",
    val city: String = "",
    val province: String = "",
    val country: String = "",
    val shippingNotes: String = ""
)

/**
 * نموذج البيانات لمرفقات ووثائق الشركة العامة (CompanyAttachment).
 */
data class CompanyAttachment(
    val id: Long = 0L,
    val companyId: Long,
    val documentType: String = "سجل تجاري",
    val attachmentPath: String = "",
    val link: String = "",
    val comment: String = "",
    val uploadDate: Long = Clock.System.now().toEpochMilliseconds(),
    val userId: Long? = 1L,
    val expiryDate: String = "",
    val notifyOnExpiry: Boolean = true,
    val notificationDaysBefore: Int = 30
)

/**
 * نموذج البيانات لقطع المصنّع الأصلي (ManufacturerPart).
 */
data class ManufacturerPart(
    val id: Long = 0L,
    val partId: Long,
    val manufacturerId: Long,
    val mpn: String,
    val description: String = "",
    val link: String = "",
    val metadata: String = "{}",
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * نموذج البيانات للخصائص والمعاملات الفنية لقطعة المصنّع (ManufacturerPartParameter).
 */
data class ManufacturerPartParameter(
    val id: Long = 0L,
    val manufacturerPartId: Long,
    val name: String,
    val value: String,
    val units: String = ""
)

/**
 * نموذج البيانات لمرفقات وأوراق مواصفات قطع المصنّع (ManufacturerPartAttachment).
 */
data class ManufacturerPartAttachment(
    val id: Long = 0L,
    val manufacturerPartId: Long,
    val attachmentPath: String = "",
    val link: String = "",
    val comment: String = "",
    val uploadDate: Long = Clock.System.now().toEpochMilliseconds(),
    val userId: Long? = 1L
)

/**
 * نموذج البيانات لقطع الموردين (SupplierPart).
 */
data class SupplierPart(
    val id: Long = 0L,
    val partId: Long,
    val supplierId: Long,
    val sku: String,
    val manufacturerPartId: Long? = null,
    val description: String = "",
    val link: String = "",
    val note: String = "",
    val packaging: String = "",
    val packQuantity: String = "1",
    val availableForPurchase: Boolean = true,
    val active: Boolean = true,
    val metadata: String = "{}",
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * نموذج البيانات لشريحة سعر المورد بناءً على الكمية (SupplierPriceBreak).
 */
data class SupplierPriceBreak(
    val id: Long = 0L,
    val supplierPartId: Long,
    val quantity: Double,
    val price: Double,
    val priceCurrency: String = "USD",
    val packQuantity: String = "1",
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * جدول محاكاة جهات الاتصال في الذاكرة (Contact Table).
 */
class ContactTable {
    private val contacts = mutableListOf<Contact>()
    private var nextId = 1L

    fun insertContact(contact: Contact): Contact {
        require(contact.name.isNotBlank()) { "اسم جهة الاتصال إلزامي ولا يمكن أن يكون فارغاً" }
        require(contact.companyId != 0L) { "معرف الشركة إلزامي لربط جهة الاتصال" }

        val newContact = contact.copy(id = if (contact.id == 0L) nextId++ else contact.id)
        contacts.add(newContact)
        return newContact
    }

    fun updateContact(contact: Contact): Contact {
        require(contact.id != 0L) { "معرف جهة الاتصال غير صالح للتحديث" }
        require(contact.name.isNotBlank()) { "اسم جهة الاتصال إلزامي" }

        val index = contacts.indexOfFirst { it.id == contact.id }
        require(index != -1) { "جهة الاتصال غير موجودة لتحديثها" }
        contacts[index] = contact
        return contact
    }

    fun deleteContact(id: Long): Boolean {
        return contacts.removeIf { it.id == id }
    }

    fun getContactsForCompany(companyId: Long): List<Contact> {
        return contacts.filter { it.companyId == companyId }
    }

    fun getAllContacts(): List<Contact> = contacts.toList()
}

/**
 * جدول محاكاة مرفقات الشركات العامة في الذاكرة (CompanyAttachment Table).
 */
class CompanyAttachmentTable {
    private val attachments = mutableListOf<CompanyAttachment>()
    private var nextId = 1L

    fun insertAttachment(attachment: CompanyAttachment): CompanyAttachment {
        require(attachment.companyId != 0L) { "معرف الشركة إلزامي لربط المرفق" }
        require(attachment.attachmentPath.isNotBlank() || attachment.link.isNotBlank()) { "يجب تزويد مسار الملف المرفق أو الرابط الإلكتروني" }

        val newAttachment = attachment.copy(
            id = if (attachment.id == 0L) nextId++ else attachment.id,
            uploadDate = if (attachment.uploadDate == 0L) Clock.System.now().toEpochMilliseconds() else attachment.uploadDate
        )
        attachments.add(newAttachment)
        return newAttachment
    }

    fun deleteAttachment(id: Long): Boolean {
        return attachments.removeIf { it.id == id }
    }

    fun getAttachmentsForCompany(companyId: Long): List<CompanyAttachment> {
        return attachments.filter { it.companyId == companyId }
    }
}

/**
 * جدول محاكاة العناوين في الذاكرة (Address Table).
 */
class AddressTable {
    private val addresses = mutableListOf<Address>()
    private var nextId = 1L

    init {
        seedSampleAddresses()
    }

    private fun seedSampleAddresses() {
        insertAddress(
            Address(
                companyId = 1L,
                title = "المقر الرئيسي",
                isPrimary = true,
                line1 = "المنطقة الصناعية الثانية",
                city = "الرياض",
                country = "المملكة العربية السعودية"
            )
        )
        insertAddress(
            Address(
                companyId = 2L,
                title = "المصنع الرئيسي",
                isPrimary = true,
                line1 = "Zhangjiang Hi-Tech Park",
                city = "شنغهاي",
                country = "الصين"
            )
        )
        insertAddress(
            Address(
                companyId = 3L,
                title = "المقر الرئيسي",
                isPrimary = true,
                line1 = "واحة التقنية - حي الزهراء",
                city = "الرياض",
                country = "المملكة العربية السعودية"
            )
        )
    }

    fun insertAddress(address: Address): Address {
        require(address.line1.isNotBlank()) { "السطر الأول من العنوان إلزامي" }
        require(address.companyId != 0L) { "معرف الشركة إلزامي لربط العنوان" }

        if (address.isPrimary) {
            resetPrimaryForCompany(address.companyId)
        }

        val newAddress = address.copy(id = if (address.id == 0L) nextId++ else address.id)
        addresses.add(newAddress)
        return newAddress
    }

    fun updateAddress(address: Address): Address {
        require(address.id != 0L) { "معرف العنوان غير صالح للتحديث" }
        require(address.line1.isNotBlank()) { "السطر الأول من العنوان إلزامي" }

        val index = addresses.indexOfFirst { it.id == address.id }
        require(index != -1) { "العنوان غير موجود لتحديثه" }

        if (address.isPrimary) {
            resetPrimaryForCompany(address.companyId, excludeAddressId = address.id)
        }

        addresses[index] = address
        return address
    }

    fun deleteAddress(id: Long): Boolean {
        return addresses.removeIf { it.id == id }
    }

    fun getAddressesForCompany(companyId: Long): List<Address> {
        return addresses.filter { it.companyId == companyId }
    }

    fun getPrimaryAddressForCompany(companyId: Long): Address? {
        return addresses.find { it.companyId == companyId && it.isPrimary }
    }

    private fun resetPrimaryForCompany(companyId: Long, excludeAddressId: Long = 0L) {
        for (i in addresses.indices) {
            if (addresses[i].companyId == companyId && addresses[i].id != excludeAddressId && addresses[i].isPrimary) {
                addresses[i] = addresses[i].copy(isPrimary = false)
            }
        }
    }
}

/**
 * جدول محاكاة قطع المصنّع في الذاكرة (ManufacturerPart Table).
 */
class ManufacturerPartTable {
    private val parts = mutableListOf<ManufacturerPart>()
    private var nextId = 1L

    init {
        seedSampleManufacturerParts()
    }

    private fun seedSampleManufacturerParts() {
        insertManufacturerPart(
            ManufacturerPart(
                partId = 1L,
                manufacturerId = 2L,
                mpn = "ESP32-WROOM-32U",
                description = "وحدة متحكم ESP32 Wi-Fi + Bluetooth"
            )
        )
    }

    fun insertManufacturerPart(part: ManufacturerPart, isManufacturerCompany: Boolean = true): ManufacturerPart {
        require(isManufacturerCompany) { "لا يمكن إضافة قطعة مصنّع لشركة غير معرفة كمصنّع (isManufacturer = false)" }
        require(part.mpn.isNotBlank()) { "رقم القطعة لدى المصنّع (MPN) إلزامي" }
        require(part.manufacturerId != 0L) { "معرف المصنّع إلزامي" }
        require(part.partId != 0L) { "معرف القطعة الداخلية إلزامي" }

        val duplicate = parts.any {
            it.manufacturerId == part.manufacturerId &&
                    it.mpn.trim().equals(part.mpn.trim(), ignoreCase = true) &&
                    it.id != part.id
        }
        require(!duplicate) { "رقم القطعة للمصنّع (MPN '${part.mpn}') مسجل بالفعل لهذا المصنّع." }

        val newPart = part.copy(id = if (part.id == 0L) nextId++ else part.id)
        parts.add(newPart)
        return newPart
    }

    fun updateManufacturerPart(part: ManufacturerPart): ManufacturerPart {
        require(part.id != 0L) { "معرف قطعة المصنع غير صالح للتحديث" }
        require(part.mpn.isNotBlank()) { "رقم القطعة (MPN) إلزامي" }

        val duplicate = parts.any {
            it.manufacturerId == part.manufacturerId &&
                    it.mpn.trim().equals(part.mpn.trim(), ignoreCase = true) &&
                    it.id != part.id
        }
        require(!duplicate) { "رقم القطعة للمصنّع (MPN '${part.mpn}') مسجل بالفعل لهذا المصنّع." }

        val index = parts.indexOfFirst { it.id == part.id }
        require(index != -1) { "قطعة المصنع غير موجودة لتحديثها" }
        parts[index] = part.copy(updatedAt = Clock.System.now().toEpochMilliseconds())
        return parts[index]
    }

    fun deleteManufacturerPart(id: Long): Boolean {
        return parts.removeIf { it.id == id }
    }

    fun getManufacturerPartsForCompany(manufacturerId: Long): List<ManufacturerPart> {
        return parts.filter { it.manufacturerId == manufacturerId }
    }

    fun getManufacturerPartsForPart(partId: Long): List<ManufacturerPart> {
        return parts.filter { it.partId == partId }
    }
}

/**
 * جدول محاكاة الخصائص الفنية لقطعة المصنّع في الذاكرة (ManufacturerPartParameter Table).
 */
class ManufacturerPartParameterTable {
    private val parameters = mutableListOf<ManufacturerPartParameter>()
    private var nextId = 1L

    fun insertParameter(parameter: ManufacturerPartParameter): ManufacturerPartParameter {
        require(parameter.manufacturerPartId != 0L) { "معرف قطعة المصنّع إلزامي" }
        require(parameter.name.isNotBlank()) { "اسم المعامل الفني إلزامي" }

        val duplicate = parameters.any {
            it.manufacturerPartId == parameter.manufacturerPartId &&
                    it.name.trim().equals(parameter.name.trim(), ignoreCase = true) &&
                    it.id != parameter.id
        }
        require(!duplicate) { "المعامل التقني ('${parameter.name}') مسجل بالفعل لهذه القطعة المصنّعة." }

        val newParam = parameter.copy(id = if (parameter.id == 0L) nextId++ else parameter.id)
        parameters.add(newParam)
        return newParam
    }

    fun deleteParameter(id: Long): Boolean {
        return parameters.removeIf { it.id == id }
    }

    fun getParametersForManufacturerPart(manufacturerPartId: Long): List<ManufacturerPartParameter> {
        return parameters.filter { it.manufacturerPartId == manufacturerPartId }
    }
}

/**
 * جدول محاكاة مرفقات قطع المصنّع في الذاكرة (ManufacturerPartAttachment Table).
 */
class ManufacturerPartAttachmentTable {
    private val attachments = mutableListOf<ManufacturerPartAttachment>()
    private var nextId = 1L

    fun insertAttachment(attachment: ManufacturerPartAttachment): ManufacturerPartAttachment {
        require(attachment.manufacturerPartId != 0L) { "معرف قطعة المصنّع إلزامي لربط المرفق" }
        require(attachment.attachmentPath.isNotBlank() || attachment.link.isNotBlank()) { "يجب تزويد مسار الملف المرفق أو الرابط الإلكتروني" }

        val newAttachment = attachment.copy(
            id = if (attachment.id == 0L) nextId++ else attachment.id,
            uploadDate = if (attachment.uploadDate == 0L) Clock.System.now().toEpochMilliseconds() else attachment.uploadDate
        )
        attachments.add(newAttachment)
        return newAttachment
    }

    fun deleteAttachment(id: Long): Boolean {
        return attachments.removeIf { it.id == id }
    }

    fun getAttachmentsForManufacturerPart(manufacturerPartId: Long): List<ManufacturerPartAttachment> {
        return attachments.filter { it.manufacturerPartId == manufacturerPartId }
    }
}

/**
 * جدول محاكاة قطع الموردين في الذاكرة (SupplierPart Table).
 */
class SupplierPartTable {
    private val parts = mutableListOf<SupplierPart>()
    private var nextId = 1L

    init {
        seedSampleSupplierParts()
    }

    private fun seedSampleSupplierParts() {
        insertSupplierPart(
            SupplierPart(
                partId = 1L,
                supplierId = 1L,
                sku = "SKU-ADV-101",
                description = "حساس حرارة دقيق DHT22"
            )
        )
        insertSupplierPart(
            SupplierPart(
                partId = 2L,
                supplierId = 2L,
                sku = "SKU-ESP-001",
                description = "شريحة متحكم ESP32-S3"
            )
        )
    }

    fun insertSupplierPart(part: SupplierPart, isSupplierCompany: Boolean = true): SupplierPart {
        require(isSupplierCompany) { "لا يمكن إضافة قطعة مورد لشركة غير معرفة كمورد (isSupplier = false)" }
        require(part.sku.isNotBlank()) { "كود المورد الخاص (SKU) إلزامي" }
        require(part.supplierId != 0L) { "معرف المورد إلزامي" }
        require(part.partId != 0L) { "معرف القطعة الداخلية إلزامي" }

        val duplicate = parts.any {
            it.supplierId == part.supplierId &&
                    it.sku.trim().equals(part.sku.trim(), ignoreCase = true) &&
                    it.id != part.id
        }
        require(!duplicate) { "كود المورد (SKU '${part.sku}') مسجل بالفعل لدى هذا المورد." }

        val newPart = part.copy(id = if (part.id == 0L) nextId++ else part.id)
        parts.add(newPart)
        return newPart
    }

    fun updateSupplierPart(part: SupplierPart): SupplierPart {
        require(part.id != 0L) { "معرف قطعة المورد غير صالح للتحديث" }
        require(part.sku.isNotBlank()) { "كود SKU إلزامي" }

        val duplicate = parts.any {
            it.supplierId == part.supplierId &&
                    it.sku.trim().equals(part.sku.trim(), ignoreCase = true) &&
                    it.id != part.id
        }
        require(!duplicate) { "كود المورد (SKU '${part.sku}') مسجل بالفعل لدى هذا المورد." }

        val index = parts.indexOfFirst { it.id == part.id }
        require(index != -1) { "قطعة المورد غير موجودة لتحديثها" }
        parts[index] = part
        return part
    }

    fun deleteSupplierPart(id: Long): Boolean {
        return parts.removeIf { it.id == id }
    }

    fun getSupplierPartsForCompany(supplierId: Long): List<SupplierPart> {
        return parts.filter { it.supplierId == supplierId }
    }

    fun getSupplierPartsForPart(partId: Long): List<SupplierPart> {
        return parts.filter { it.partId == partId }
    }
}

/**
 * جدول محاكاة شرائح أسعار الموردين في الذاكرة (SupplierPriceBreak Table).
 */
class SupplierPriceBreakTable {
    private val breaks = mutableListOf<SupplierPriceBreak>()
    private var nextId = 1L

    fun insertPriceBreak(priceBreak: SupplierPriceBreak, defaultCompanyCurrency: String = "USD"): SupplierPriceBreak {
        require(priceBreak.supplierPartId != 0L) { "معرف قطعة المورد إلزامي" }
        require(priceBreak.quantity > 0) { "الكمية يجب أن تكون أكبر من 0" }
        require(priceBreak.price >= 0) { "السعر يجب أن لا يكون بالسالب" }

        val duplicate = breaks.any {
            it.supplierPartId == priceBreak.supplierPartId &&
                    it.quantity == priceBreak.quantity &&
                    it.id != priceBreak.id
        }
        require(!duplicate) { "شريحة السعر للكمية (${priceBreak.quantity}) مسجلة بالفعل لهذه القطعة." }

        val currencyToUse = priceBreak.priceCurrency.ifBlank { defaultCompanyCurrency }
        val newBreak = priceBreak.copy(
            id = if (priceBreak.id == 0L) nextId++ else priceBreak.id,
            priceCurrency = currencyToUse,
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
        breaks.add(newBreak)
        return newBreak
    }

    fun updatePriceBreak(priceBreak: SupplierPriceBreak): SupplierPriceBreak {
        require(priceBreak.id != 0L) { "معرف شريحة السعر غير صالح للتحديث" }
        require(priceBreak.quantity > 0) { "الكمية يجب أن تكون أكبر من 0" }

        val duplicate = breaks.any {
            it.supplierPartId == priceBreak.supplierPartId &&
                    it.quantity == priceBreak.quantity &&
                    it.id != priceBreak.id
        }
        require(!duplicate) { "شريحة السعر للكمية (${priceBreak.quantity}) مسجلة بالفعل." }

        val index = breaks.indexOfFirst { it.id == priceBreak.id }
        require(index != -1) { "شريحة السعر غير موجودة لتحديثها" }
        breaks[index] = priceBreak.copy(updatedAt = Clock.System.now().toEpochMilliseconds())
        return breaks[index]
    }

    fun deletePriceBreak(id: Long): Boolean {
        return breaks.removeIf { it.id == id }
    }

    fun getPriceBreaksForSupplierPart(supplierPartId: Long): List<SupplierPriceBreak> {
        return breaks.filter { it.supplierPartId == supplierPartId }.sortedBy { it.quantity }
    }

    /**
     * حساب السعر الفردي الأنسب بناءً على كمية الطلب الشريحية.
     */
    fun getBestPriceForQuantity(supplierPartId: Long, orderQty: Double): SupplierPriceBreak? {
        val tiers = getPriceBreaksForSupplierPart(supplierPartId)
        if (tiers.isEmpty()) return null
        return tiers.filter { it.quantity <= orderQty }.maxByOrNull { it.quantity } ?: tiers.minByOrNull { it.quantity }
    }
}
