package com.nextstepai.inventory.data

/**
 * نموذج بيانات الشركة والجهة التجارية (Company) المستوحى من نظام InvenTree.
 * يمثل الشركات الخارجية المتعامل معها (موردين، مصنعين، عملاء).
 *
 * @property id المعرف الرقمي الفريد للشركة
 * @property name الاسم التجاري أو الرسمي للشركة (إلزامي وفريد)
 * @property description وصف نشاط الشركة وطبيعة التعامل
 * @property website رابط الموقع الإلكتروني الرسمي
 * @property phone رقم الهاتف الأساسي
 * @property email عنوان البريد الإلكتروني الرسمي
 * @property address العنوان الفيزيائي أو المستودع
 * @property contact اسم جهة الاتصال الأساسية
 * @property isSupplier مؤشر يحدد ما إذا كانت الشركة تعمل كمورد
 * @property isManufacturer مؤشر يحدد ما إذا كانت الشركة تعمل كمصنّع أصلي
 * @property isCustomer مؤشر يحدد ما إذا كانت الشركة تعمل كعميل
 * @property active حالة نشاط الشركة
 * @property currency رمز العملة الافتراضية المعتمدة (مثل USD, EUR, DZD)
 * @property imageUrl مسار الشعار أو صورة الشركة
 * @property link رابط خارجي إضافي
 * @property notes ملاحظات تعاقدية وفنية بصيغة Markdown
 */
data class Company(
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val website: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val contact: String = "",
    val isSupplier: Boolean = true,
    val isManufacturer: Boolean = false,
    val isCustomer: Boolean = false,
    val active: Boolean = true,
    val currency: String = "USD",
    val imageUrl: String? = null,
    val link: String = "",
    val notes: String = "",
    val metadata: String = "{}",
    val parentId: Long? = null,
    val uuid: String = ""
) {
    /**
     * المعرف الفريد السلسلي للشركة للاستخدام الموحد في المسارات وحفظ البيانات.
     */
    val effectiveUuid: String
        get() = if (uuid.isNotBlank()) uuid else "company-$id"
}

/**
 * محاكاة جدول إدارة الشركات والعلاقات التجارية (Company Table).
 */
@Deprecated("Legacy in-memory storage table. Scheduled for migration to SQLite DAOs.")
class CompanyTable {
    private val companies = mutableListOf<Company>()
    private var nextCompanyId = 1L

    init {
        seedSampleCompanies()
    }

    private fun seedSampleCompanies() {
        insertCompany(
            Company(
                name = "شركة التقنيات المتقدمة للتوريد",
                description = "مورد رئيسي للمكونات الإلكترونية والمتحكمات",
                website = "https://advanced-tech.com",
                phone = "+966 11 234 5678",
                email = "supply@advanced-tech.com",
                address = "الرياض - المنطقة الصناعية الثانية",
                contact = "م. أحمد علي",
                isSupplier = true,
                isManufacturer = false,
                isCustomer = false,
                currency = "USD"
            )
        )

        insertCompany(
            Company(
                name = "Espressif Systems Technology",
                description = "المصنع الأصلي لشريحة ومتحكمات ESP32",
                website = "https://espressif.com",
                phone = "+86 21 6103 0218",
                email = "sales@espressif.com",
                address = "Shanghai, China",
                contact = "Global Sales Dept",
                isSupplier = true,
                isManufacturer = true,
                isCustomer = false,
                currency = "USD"
            )
        )

        insertCompany(
            Company(
                name = "مؤسسة الابتكار والحلول الذكية",
                description = "عميل رئيسي لشراء واستلام أجهزة ومستشعرات المخزون المجمعة",
                website = "https://smart-solutions.com",
                phone = "+966 12 987 6543",
                email = "info@smart-solutions.com",
                address = "جدة - حي الزهراء",
                contact = "سارة السليمان",
                isSupplier = false,
                isManufacturer = false,
                isCustomer = true,
                currency = "USD"
            )
        )
    }

    /**
     * إدراج شركة جديدة مع التحقق من قيد فرادة الاسم (`name unique constraint`).
     */
    fun insertCompany(company: Company): Company {
        require(company.name.isNotBlank()) { "اسم الشركة إلزامي ولا يمكن أن يكون فارغاً" }

        val duplicateName = companies.any { it.name.trim().equals(company.name.trim(), ignoreCase = true) && it.id != company.id }
        require(!duplicateName) {
            "اسم الشركة '${company.name}' مسجل بالفعل. يرجى استخدام اسم آخر لتجنب الازدواجية التجارية."
        }

        val newCompany = company.copy(
            id = if (company.id == 0L) nextCompanyId++ else company.id
        )
        companies.add(newCompany)
        return newCompany
    }

    /**
     * البحث والفلترة حسب نص البحث وأدوار الشركة (الموردون، المصنعون، العملاء).
     */
    fun searchCompanies(
        query: String = "",
        supplierOnly: Boolean = false,
        manufacturerOnly: Boolean = false,
        customerOnly: Boolean = false,
        activeOnly: Boolean = true
    ): List<Company> {
        val q = query.trim().lowercase()
        return companies.filter { c ->
            val matchesActive = !activeOnly || c.active
            val matchesRole = (!supplierOnly || c.isSupplier) &&
                    (!manufacturerOnly || c.isManufacturer) &&
                    (!customerOnly || c.isCustomer)

            val matchesQuery = q.isEmpty() ||
                    c.name.lowercase().contains(q) ||
                    c.description.lowercase().contains(q) ||
                    c.email.lowercase().contains(q) ||
                    c.contact.lowercase().contains(q)

            matchesActive && matchesRole && matchesQuery
        }
    }

    /**
     * جلب كافة الشركات.
     */
    fun getAllCompanies(): List<Company> = companies.toList()

    /**
     * جلب شركة حسب المعرف الرقمي.
     */
    fun getCompanyById(id: Long): Company? = companies.find { it.id == id }

    /**
     * تحديث بيانات شركة حالية مع التحقق من الفرادة.
     */
    fun updateCompany(company: Company): Company {
        require(company.id != 0L) { "المعرف الرقمي للشركة غير صالح للتحديث" }
        require(company.name.isNotBlank()) { "اسم الشركة إلزامي ولا يمكن أن يكون فارغاً" }
        require(company.parentId == null || company.parentId != company.id) { "لا يمكن اختيار الشركة لنفسها كشركة أم" }

        val duplicateName = companies.any { it.name.trim().equals(company.name.trim(), ignoreCase = true) && it.id != company.id }
        require(!duplicateName) {
            "اسم الشركة '${company.name}' مسجل بالفعل لشركة أخرى."
        }

        val index = companies.indexOfFirst { it.id == company.id }
        require(index != -1) { "الشركة المطلوبة غير موجودة لتحديثها" }
        companies[index] = company
        return company
    }

    /**
     * حذف شركة حسب المعرف.
     */
    fun deleteCompany(id: Long): Boolean {
        return companies.removeIf { it.id == id }
    }
}
