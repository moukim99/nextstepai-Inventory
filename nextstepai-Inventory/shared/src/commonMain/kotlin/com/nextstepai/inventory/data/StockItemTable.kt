package com.nextstepai.inventory.data

/**
 * حالة الوحدة المخزنية (StockItem Status).
 */
enum class StockStatus(val code: Int, val label: String) {
    OK(10, "سليم (OK)"),
    DAMAGED(50, "تالف (Damaged)"),
    DESTROYED(55, "مدمر (Destroyed)"),
    REJECTED(58, "مرفوض (Rejected)"),
    QUARANTINE(60, "قيد الفحص (Quarantine)"),
    EXPIRED(65, "منتهي الصلاحية (Expired)");

    companion object {
        fun fromCode(code: Int): StockStatus = entries.find { it.code == code } ?: OK
    }
}

/**
 * تمثيل موقع التخزين في المستودع (StockLocation).
 */
data class StockLocation(
    val id: Long,
    val name: String,
    val description: String = "",
    val parentId: Long? = null
)

/**
 * تمثيل سجل الوحدة المخزنية الفعلية (StockItem) المادية الموجودة على الرفوف وفق تحليل InvenTree.
 *
 * @property id المعرف الرقمي الفريد للوحدة المخزنية
 * @property partId معرف القطعة المرجعية التجريدية
 * @property supplierPartId ربط ببيانات التوريد المعتمدة
 * @property locationId معرف موقع التخزين في المستودع
 * @property quantity الكمية الفعلية المتوفرة
 * @property serial الرقم التسلسلي المميز (إذا كانت القطعة trackable)
 * @property batch رقم الشحنة أو التشغيلة (Batch/Lot)
 * @property status كود الحالة التشغيلية
 * @property packaging طريقة التغليف الفيزيائي (مثل: Reel, Box, Tray)
 * @property expiryDate تاريخ انتهاء الصلاحية
 * @property purchasePrice سعر الشراء والعملة
 * @property parentId معرف السجل الأب في حالات تجزئة المخزون (Split)
 * @property isBuilding يحدد ما إذا كانت الوحدة قيد التجميع والإنتاج حالياً
 * @property reviewNeeded مؤشر يفرض إعادة مراجعة الجودة
 * @property deleteOnDeplete حذف السجل آلياً عند وصول الكمية للصفر
 * @property link رابط خارجي للوحدة
 * @property notes ملاحظات تشغيلية
 * @property updated الطابع الزمني لآخر تحديث
 * @property allocatedQuantity الكميات المحجوزة لأوامر الإنتاج أو البيع
 */
data class StockItem(
    val id: Long = 0L,
    val partId: Long,
    val supplierPartId: Long? = null,
    val locationId: Long? = null,
    val quantity: Double = 1.0,
    val serial: String = "",
    val batch: String = "",
    val status: StockStatus = StockStatus.OK,
    val packaging: String = "Box",
    val expiryDate: String = "",
    val purchasePrice: Double = 0.0,
    val parentId: Long? = null,
    val isBuilding: Boolean = false,
    val reviewNeeded: Boolean = false,
    val deleteOnDeplete: Boolean = false,
    val link: String = "",
    val notes: String = "",
    val updated: String = "",
    val allocatedQuantity: Double = 0.0
) {
    /**
     * الرصيد الصافي المتاح للصرف الفعلي (الكمية الكلية - المحجوز).
     */
    val availableQuantity: Double
        get() = (quantity - allocatedQuantity).coerceAtLeast(0.0)
}

/**
 * محاكاة جدول إدارة المخزون الفعلي (StockItem Table) والقيود المنطقية المرافقة.
 */
class StockItemTable {
    private val stockItems = mutableListOf<StockItem>()
    private val locations = mutableListOf<StockLocation>()
    private var nextStockId = 1L
    private var nextLocationId = 1L

    init {
        seedSampleStockData()
    }

    private fun seedSampleStockData() {
        val loc1 = insertLocation("المستودع الرئيسي - رف A1", "مستودع المكونات الإلكترونية")
        val loc2 = insertLocation("مستودع التجميع - رف B3", "مستودع المنتجات النهائية")

        insertStockItem(
            StockItem(
                partId = 1L, // مقاومة 10K
                locationId = loc1.id,
                quantity = 250.0,
                batch = "BATCH-2025-01",
                packaging = "Reel",
                status = StockStatus.OK
            )
        )

        insertStockItem(
            StockItem(
                partId = 2L, // متحكم ESP32 (Trackable)
                locationId = loc1.id,
                quantity = 1.0,
                serial = "ESP32-SN-1001",
                batch = "LOT-ESP-88",
                packaging = "Tray",
                status = StockStatus.OK,
                allocatedQuantity = 0.0
            )
        )

        insertStockItem(
            StockItem(
                partId = 2L,
                locationId = loc1.id,
                quantity = 1.0,
                serial = "ESP32-SN-1002",
                batch = "LOT-ESP-88",
                packaging = "Tray",
                status = StockStatus.QUARANTINE,
                reviewNeeded = true
            )
        )

        insertStockItem(
            StockItem(
                partId = 4L, // مستشعر DHT22
                locationId = loc2.id,
                quantity = 30.0,
                batch = "BATCH-DHT-2025",
                packaging = "Box",
                status = StockStatus.OK,
                allocatedQuantity = 5.0
            )
        )
    }

    /**
     * إدراج موقع تخزين جديد.
     */
    fun insertLocation(name: String, description: String = ""): StockLocation {
        val loc = StockLocation(
            id = nextLocationId++,
            name = name,
            description = description
        )
        locations.add(loc)
        return loc
    }

    /**
     * جلب جميع مواقع التخزين.
     */
    fun getAllLocations(): List<StockLocation> = locations.toList()

    /**
     * إدراج وحدة مخزنية جديدة مع التحقق من القيود:
     * 1. قيد التتبع الرقمي (Serial Constraint): إذا أُدخل serial يجب أن تكون الكمية quantity = 1
     * 2. قيد التكرار الرقمي للرقم التسلسلي لنفس القطعة.
     */
    fun insertStockItem(item: StockItem): StockItem {
        val finalQuantity = if (item.serial.isNotBlank()) 1.0 else item.quantity

        if (item.serial.isNotBlank()) {
            val duplicateSerial = stockItems.any { it.partId == item.partId && it.serial == item.serial }
            require(!duplicateSerial) {
                "خطأ في الرقم التسلسلي: الرقم التسلسلي '${item.serial}' مستخدم بالفعل لقطعة أخرى من نفس النوع."
            }
        }

        val newItem = item.copy(
            id = if (item.id == 0L) nextStockId++ else item.id,
            quantity = finalQuantity,
            updated = "2025-02-15"
        )
        stockItems.add(newItem)
        return newItem
    }

    /**
     * جلب الوحدات المخزنية لقطعة محددة.
     */
    fun getStockItemsForPart(partId: Long): List<StockItem> {
        return stockItems.filter { it.partId == partId }
    }

    /**
     * تجزئة كمية مخزنية (Split Stock Item) وإنشاء وحدة فرعية من الأب.
     */
    fun splitStockItem(parentStockId: Long, splitQuantity: Double): StockItem {
        val parentIndex = stockItems.indexOfFirst { it.id == parentStockId }
        require(parentIndex != -1) { "الوحدة المخزنية الأصلية غير موجودة" }

        val parentItem = stockItems[parentIndex]
        require(parentItem.availableQuantity >= splitQuantity) { "الكمية المتاحة لا تكفي للتجزئة" }

        // خصم الكمية من السجل الأصلي
        stockItems[parentIndex] = parentItem.copy(quantity = parentItem.quantity - splitQuantity)

        // إنشاء سجل فرعي مشتق ومربوط بالأب
        val childItem = parentItem.copy(
            id = nextStockId++,
            quantity = splitQuantity,
            allocatedQuantity = 0.0,
            parentId = parentStockId
        )
        stockItems.add(childItem)
        return childItem
    }

    /**
     * جلب جميع السجلات المخزنية.
     */
    fun getAllStockItems(): List<StockItem> = stockItems.toList()
}
