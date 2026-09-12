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
 * تمثيل سجل الوحدة المخزنية الفعلية (StockItem) المادية الموجودة على الرفوف وفق تحليل InvenTree (24 حقل).
 *
 * @property id المعرف الرقمي الفريد للوحدة المخزنية
 * @property partId معرف القطعة المرجعية التجريدية (ForeignKey)
 * @property locationId معرف موقع التخزين في المستودع (ForeignKey)
 * @property quantity الكمية الفعلية المتوفرة
 * @property serial الرقم التسلسلي المميز (إذا كانت القطعة trackable)
 * @property batch رقم الشحنة أو التشغيلة (Batch/Lot)
 * @property status كود الحالة التشغيلية (OK, Damaged, Destroyed, Rejected, Quarantine, Expired)
 * @property packaging طريقة التغليف الفيزيائي (مثل: Reel, Box, Tray)
 * @property purchasePrice سعر الشراء الفعلي للوحدة
 * @property purchasePriceCurrency عملة سعر الشراء (مثل USD, SAR)
 * @property purchaseOrderId رابط أمر الشراء الذاتي (ForeignKey)
 * @property supplierPartId ربط ببيانات التوريد المعتمدة (SupplierPart)
 * @property salesOrderId رابط أمر البيع المخصص له هذه الكمية (ForeignKey)
 * @property customerId العميل المستلم للقطعة (ForeignKey)
 * @property buildId رابط أمر التصنيع المُنْتِج لهذا العنصر (ForeignKey)
 * @property isBuilding يحدد ما إذا كانت الوحدة ما زالت تحت التجميع والإنتاج
 * @property parentId معرف السجل الأب في حالات تجزئة المخزون (Split)
 * @property expiryDate تاريخ انتهاء الصلاحية
 * @property stocktakeDate تاريخ آخر جرد فعلي أُجري على الكمية
 * @property stocktakeUserId المستخدم الذي أجرى آخر جرد
 * @property reviewNeeded مؤشر يفرض إعادة مراجعة الجودة فنيًا
 * @property deleteOnDeplete حذف السجل تلقائيًا أو أرشفته عند وصول الكمية للصفر
 * @property link رابط خارجي أو توثيق للدفعة
 * @property notes ملاحظات تشغيلية
 * @property metadata بيانات ديناميكية إضافية (JSON)
 * @property updated الطابع الزمني لآخر تحديث
 * @property allocatedQuantity الكميات المحجوزة لأوامر الإنتاج أو البيع
 */
data class StockItem(
    val id: Long = 0L,
    val partId: Long,
    val locationId: Long? = null,
    val quantity: Double = 1.0,
    val serial: String = "",
    val batch: String = "",
    val status: StockStatus = StockStatus.OK,
    val packaging: String = "Box",
    val purchasePrice: Double = 0.0,
    val purchasePriceCurrency: String = "USD",
    val purchaseOrderId: Long? = null,
    val supplierPartId: Long? = null,
    val salesOrderId: Long? = null,
    val customerId: Long? = null,
    val buildId: Long? = null,
    val isBuilding: Boolean = false,
    val parentId: Long? = null,
    val expiryDate: String = "",
    val stocktakeDate: String = "",
    val stocktakeUserId: Long? = null,
    val reviewNeeded: Boolean = false,
    val deleteOnDeplete: Boolean = false,
    val link: String = "",
    val notes: String = "",
    val metadata: String = "{}",
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
     * إدراج موقع تخزين جديد مع تطبيق قيود الشجرة الهرمية والأسماء المكررة.
     */
    fun insertLocation(
        name: String,
        description: String = "",
        parentId: Long? = null,
        structural: Boolean = false,
        external: Boolean = false,
        icon: String = "warehouse"
    ): StockLocation {
        return insertLocation(
            StockLocation(
                name = name,
                description = description,
                parentId = parentId,
                structural = structural,
                external = external,
                icon = icon
            )
        )
    }

    /**
     * إدراج كائن موقع تخزين.
     */
    fun insertLocation(location: StockLocation): StockLocation {
        val trimmedName = location.name.trim()
        require(trimmedName.isNotBlank()) { "اسم الموقع التخزيني لا يمكن أن يكون فارغاً" }

        val duplicateName = locations.any {
            it.id != location.id && it.parentId == location.parentId && it.name.trim().equals(trimmedName, ignoreCase = true)
        }
        require(!duplicateName) {
            "اسم الموقع المخزني '$trimmedName' مستخدم بالفعل تحت هذا الموقع الأب."
        }

        val parentLoc = locations.find { it.id == location.parentId }
        val calculatedLevel = parentLoc?.let { it.level + 1 } ?: 0
        val calculatedTreeId = parentLoc?.treeId ?: (locations.maxOfOrNull { it.treeId } ?: 0) + 1

        val newLoc = location.copy(
            id = if (location.id == 0L) nextLocationId++ else location.id,
            name = trimmedName,
            level = calculatedLevel,
            treeId = calculatedTreeId
        )

        locations.removeAll { it.id == newLoc.id }
        locations.add(newLoc)
        return newLoc
    }

    /**
     * جلب جميع مواقع التخزين.
     */
    fun getAllLocations(): List<StockLocation> = locations.toList()

    /**
     * إدراج وحدة مخزنية جديدة مع التحقق من القيود:
     * 1. قيد الموقع الهيكلي (Structural Location Constraint): يمنع تخزين العناصر مباشرة في الموقع الهيكلي.
     * 2. قيد التتبع الرقمي (Serial Constraint): إذا أُدخل serial يجب أن تكون الكمية quantity = 1
     * 3. قيد التكرار الرقمي للرقم التسلسلي لنفس القطعة.
     */
    fun insertStockItem(item: StockItem): StockItem {
        if (item.locationId != null) {
            val targetLoc = locations.find { it.id == item.locationId }
            if (targetLoc != null && targetLoc.structural) {
                throw IllegalArgumentException(
                    "لا يمكن تخزين عناصر مخزنية مباشرة في موقع هيكلي (Structural Location: '${targetLoc.name}'). يُرجى اختيار موقع فرعي إجرائي."
                )
            }
        }

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

        val newParentQty = parentItem.quantity - splitQuantity
        if (newParentQty <= 0.0 && parentItem.deleteOnDeplete) {
            stockItems.removeAt(parentIndex)
        } else {
            stockItems[parentIndex] = parentItem.copy(quantity = newParentQty)
        }

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
     * إجراء عملية جرد فعلي (Stocktake) على وحدة مخزنية وتحديث تاريخ الجرد والمستخدم.
     */
    fun performStocktake(stockId: Long, userId: Long, stocktakeDate: String = "2025-02-15"): StockItem {
        val index = stockItems.indexOfFirst { it.id == stockId }
        require(index != -1) { "الوحدة المخزنية غير موجودة" }

        val updatedItem = stockItems[index].copy(
            stocktakeDate = stocktakeDate,
            stocktakeUserId = userId,
            reviewNeeded = false
        )
        stockItems[index] = updatedItem
        return updatedItem
    }

    /**
     * تقليل أو استهلاك كمية مخزنية مع تطبيق قاعدة deleteOnDeplete عند وصول الكمية للصفر.
     */
    fun consumeStockQuantity(stockId: Long, consumeQty: Double): StockItem? {
        val index = stockItems.indexOfFirst { it.id == stockId }
        require(index != -1) { "الوحدة المخزنية غير موجودة" }

        val item = stockItems[index]
        require(item.availableQuantity >= consumeQty) { "الكمية المتاحة لا تكفي للاستهلاك" }

        val remainingQty = item.quantity - consumeQty
        if (remainingQty <= 0.0 && item.deleteOnDeplete) {
            stockItems.removeAt(index)
            return null
        } else {
            val updated = item.copy(quantity = remainingQty.coerceAtLeast(0.0))
            stockItems[index] = updated
            return updated
        }
    }

    /**
     * جلب جميع السجلات المخزنية.
     */
    fun getAllStockItems(): List<StockItem> = stockItems.toList()
}
