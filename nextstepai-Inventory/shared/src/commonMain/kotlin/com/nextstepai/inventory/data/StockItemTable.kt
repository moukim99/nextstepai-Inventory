package com.nextstepai.inventory.data

import com.nextstepai.inventory.util.DateTimeUtils
import kotlin.time.Clock

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
 * استخراج وزن الوحدة الواحدة (unitWeight) المودع في الـ metadata JSON للوحدة المخزنية.
 */
val StockItem.unitWeight: Double?
    get() {
        val idx = metadata.indexOf("\"unitWeight\":")
        if (idx == -1) return null
        val sub = metadata.substring(idx + 13).trimStart()
        val numStr = sub.takeWhile { it.isDigit() || it == '.' }
        return numStr.toDoubleOrNull()
    }

/**
 * استخراج الوزن الإجمالي المحمّل (totalWeight) المودع في الـ metadata JSON للوحدة المخزنية.
 */
val StockItem.totalWeight: Double?
    get() {
        val idx = metadata.indexOf("\"totalWeight\":")
        if (idx == -1) return null
        val sub = metadata.substring(idx + 14).trimStart()
        val numStr = sub.takeWhile { it.isDigit() || it == '.' }
        return numStr.toDoubleOrNull()
    }

/**
 * دمج وتحديث بيانات أوزان الدفعة والمكونات بذكاء داخل الـ metadata JSON.
 */
fun StockItem.withWeightInfo(unitWeight: Double?, totalWeight: Double?): StockItem {
    val map = mutableMapOf<String, String>()
    val clean = metadata.trim().removePrefix("{").removeSuffix("}").trim()
    if (clean.isNotBlank()) {
        val regex = """"(.*?)"\s*:\s*("(.*?)"|[\d\.]+|true|false|null)""".toRegex()
        regex.findAll(clean).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2]
            map[key] = value
        }
    }
    if (unitWeight != null && unitWeight > 0.0) {
        map["unitWeight"] = unitWeight.toString()
    } else {
        map.remove("unitWeight")
    }
    if (totalWeight != null && totalWeight > 0.0) {
        map["totalWeight"] = totalWeight.toString()
    } else {
        map.remove("totalWeight")
    }
    val updatedMetadata = map.entries.joinToString(prefix = "{", postfix = "}") { (k, v) -> "\"$k\":$v" }
    return this.copy(metadata = updatedMetadata)
}

/**
 * محاكاة جدول إدارة المخزون الفعلي (StockItem Table) ومواقع التخزين وأنواعها مع القيود المنطقية.
 */
@Deprecated("Legacy in-memory storage table. Scheduled for migration to SQLite DAOs.")
class StockItemTable {
    private val stockItems = mutableListOf<StockItem>()
    private val locations = mutableListOf<StockLocation>()
    private val trackingLogs = mutableListOf<StockItemTracking>()
    private val testResults = mutableListOf<StockItemTestResult>()
    private val stockAttachments = mutableListOf<StockItemAttachment>()
    private var nextStockId = 1L
    private var nextLocationId = 1L
    private var nextTrackingId = 1L
    private var nextTestResultId = 1L
    private var nextAttachmentId = 1L

    init {
        seedSampleStockData()
    }

    fun clearAll() {
        stockItems.clear()
        locations.clear()
        trackingLogs.clear()
        testResults.clear()
        stockAttachments.clear()
        nextStockId = 1L
        nextLocationId = 1L
        nextTrackingId = 1L
        nextTestResultId = 1L
        nextAttachmentId = 1L
    }

    private fun seedSampleStockData() {
        val loc1 = insertLocation("المستودع الرئيسي - رف A1", "مستودع المكونات الإلكترونية", locationType = "SHELF")
        val loc2 = insertLocation("مستودع التجميع - رف B3", "مستودع المنتجات النهائية", locationType = "WAREHOUSE")

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
        locationType: String = "SHELF",
        icon: String = "warehouse"
    ): StockLocation {
        return insertLocation(
            StockLocation(
                name = name,
                description = description,
                parentId = parentId,
                structural = structural,
                external = external,
                locationType = locationType,
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

        val maxExisting = locations.maxOfOrNull { it.id } ?: 0L
        if (nextLocationId <= maxExisting) {
            nextLocationId = maxExisting + 1L
        }
        val assignedId = if (location.id == 0L) nextLocationId++ else location.id
        val isNewLoc = location.id == 0L || location.labelGeneratedAt == null
        val locWithBaseInfo = location.copy(
            id = assignedId,
            uuid = location.uuid.ifBlank { "loc-$assignedId" },
            name = trimmedName,
            level = calculatedLevel,
            treeId = calculatedTreeId
        )

        // الأتمتة التلقائية: توليد وحفظ لقطة البيانات التأسيسية (Initial Label Snapshot) فور الإنشاء
        val finalLoc = if (isNewLoc) {
            val now = Clock.System.now().toEpochMilliseconds()
            val initialSnapshot = "$trimmedName|${location.parentId}|${locWithBaseInfo.effectiveCapacity.toInt()}|${location.locationType}"
            val imagePath = "files/labels/locations/loc_${locWithBaseInfo.effectiveUuid}.webp"
            locWithBaseInfo.withLabelSnapshot(imagePath, now, initialSnapshot)
        } else {
            locWithBaseInfo
        }

        locations.removeAll { it.id == finalLoc.id }
        locations.add(finalLoc)
        return finalLoc
    }

    /**
     * إدراج مجموعة مواقع تخزينية دفعة واحدة (Bulk Location Generation).
     */
    fun insertBatchLocations(newLocations: List<StockLocation>): List<StockLocation> {
        val inserted = mutableListOf<StockLocation>()
        newLocations.forEach { loc ->
            val created = insertLocation(loc)
            inserted.add(created)
        }
        return inserted
    }

    /**
     * جلب جميع مواقع التخزين.
     */
    fun getAllLocations(): List<StockLocation> = locations.toList()

    /**
     * حذف موقع تخزيني من الذاكرة المحلية المؤقتة.
     */
    fun deleteLocation(locationId: Long): Boolean {
        return locations.removeAll { it.id == locationId }
    }

    /**
     * حساب المسار الهرمي التراكمي الكامل لموقع تخزيني بمرور الأباء وصولاً للجذر مع حماية الحلقات الدائرية.
     */
    fun getFullPathForLocation(locationId: Long?, separator: String = " / "): String {
        if (locationId == null) return "موقع رئيسي (Root)"
        val visited = mutableSetOf<Long>()
        val pathNames = mutableListOf<String>()
        var currId: Long? = locationId

        while (currId != null && !visited.contains(currId)) {
            visited.add(currId)
            val loc = locations.find { it.id == currId } ?: break
            pathNames.add(0, loc.name)
            currId = loc.parentId
        }

        return if (pathNames.isNotEmpty()) pathNames.joinToString(separator) else "موقع #$locationId"
    }

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
            updated = DateTimeUtils.getCurrentDate()
        )
        stockItems.add(newItem)

        val trackingNotes = if (newItem.link.isNotBlank()) {
            if (newItem.notes.isNotBlank()) "مرجع الوثيقة: ${newItem.link} | ${newItem.notes}" else "مرجع الوثيقة: ${newItem.link}"
        } else {
            newItem.notes
        }

        val trackingLabel = if (newItem.batch.startsWith("BATCH-") || newItem.link.isNotBlank()) {
            "استلام شحنة مخزنية (Stock In)"
        } else {
            "إنشاء وحدة مخزنية جديدة"
        }

        recordTracking(
            stockItemId = newItem.id,
            trackingType = StockTrackingType.CREATED,
            userId = newItem.stocktakeUserId,
            label = trackingLabel,
            notes = trackingNotes,
            deltas = "{\"quantity\":[0.0,${newItem.quantity}],\"batch\":\"${newItem.batch}\",\"status\":[0,${newItem.status.code}],\"locationId\":${newItem.locationId}}"
        )

        return newItem
    }

    /**
     * تسجيل حركة تاريخية غير قابلة للتعديل في سجل التتبع (Tracking Log).
     */
    fun recordTracking(
        stockItemId: Long,
        trackingType: StockTrackingType,
        userId: Long? = null,
        label: String = trackingType.label,
        notes: String = "",
        deltas: String = "{}"
    ): StockItemTracking {
        val tracking = StockItemTracking(
            id = nextTrackingId++,
            stockItemId = stockItemId,
            date = DateTimeUtils.getCurrentDateTime(),
            trackingType = trackingType,
            userId = userId,
            label = label,
            notes = notes,
            deltas = deltas
        )
        trackingLogs.add(tracking)
        return tracking
    }

    /**
     * جلب سجل الحركات والتتبع لوحدة مخزنية محددة.
     */
    fun getTrackingForStockItem(stockItemId: Long): List<StockItemTracking> {
        return trackingLogs.filter { it.stockItemId == stockItemId }
    }

    /**
     * جلب كافة سجلات التتبع التاريخية.
     */
    fun getAllTrackingLogs(): List<StockItemTracking> = trackingLogs.toList()

    /**
     * إدراج نتيجة فحص جودة واختبار فني جديد (StockItemTestResult).
     */
    fun addTestResult(testResult: StockItemTestResult): StockItemTestResult {
        require(testResult.test.isNotBlank()) { "اسم الاختبار الفني لا يمكن أن يكون فارغاً" }
        require(stockItems.any { it.id == testResult.stockItemId }) { "الوحدة المخزنية الخاضعة للفحص غير موجودة" }

        val newResult = testResult.copy(
            id = if (testResult.id == 0L) nextTestResultId++ else testResult.id,
            test = testResult.test.trim(),
            value = testResult.value.ifBlank { "Passed" }
        )
        testResults.add(newResult)
        return newResult
    }

    /**
     * جلب سجلات نتائج الفحص والجودة لوحدة مخزنية محددة.
     */
    fun getTestResultsForStockItem(stockItemId: Long): List<StockItemTestResult> {
        return testResults.filter { it.stockItemId == stockItemId }
    }

    /**
     * جلب كافة نتائج الفحوصات الفنية.
     */
    fun getAllTestResults(): List<StockItemTestResult> = testResults.toList()

    /**
     * إدراج مرفق أو وثيقة جديدة لعنصر مخزني مع التحقق من القيود المنطقية:
     * 1. يلزم توفر أحد الحقلين (إما attachment أو link).
     * 2. التوليد الآلي للوصف comment من اسم الملف الأصلي إذا تُرك فارغاً.
     */
    fun addStockItemAttachment(attachmentItem: StockItemAttachment): StockItemAttachment {
        val hasFile = !attachmentItem.attachment.isNullOrBlank()
        val hasLink = !attachmentItem.link.isNullOrBlank()
        require(hasFile || hasLink) {
            "خطأ في إدخال المرفق: يجب تقديم ملف مرفق محلي (attachment) أو رابط ويب خارجي (link) على الأقل!"
        }
        require(stockItems.any { it.id == attachmentItem.stockItemId }) {
            "العنصر المخزني المرتبط بالمرفق غير موجود"
        }

        val autoComment = if (attachmentItem.comment.isBlank()) {
            val file = attachmentItem.attachment
            val url = attachmentItem.link
            when {
                !file.isNullOrBlank() -> file.substringAfterLast('/').substringAfterLast('\\')
                !url.isNullOrBlank() -> url
                else -> "مرفق مخزني"
            }
        } else {
            attachmentItem.comment.trim()
        }

        val record = attachmentItem.copy(
            id = if (attachmentItem.id == 0L) nextAttachmentId++ else attachmentItem.id,
            comment = autoComment,
            uploadDate = attachmentItem.uploadDate.ifBlank { DateTimeUtils.getCurrentDate() }
        )
        stockAttachments.add(record)
        return record
    }

    /**
     * جلب كافة الوثائق والمرفقات المرتبطة بعنصر مخزني محدد.
     */
    fun getAttachmentsForStockItem(stockItemId: Long): List<StockItemAttachment> {
        return stockAttachments.filter { it.stockItemId == stockItemId }
    }

    /**
     * حذف مرفق مخزني محدد بالـ ID.
     */
    fun deleteStockItemAttachment(id: Long): Boolean {
        return stockAttachments.removeIf { it.id == id }
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

        recordTracking(
            stockItemId = parentStockId,
            trackingType = StockTrackingType.SPLIT,
            label = "تجزئة رصيد مخزني",
            deltas = "{\"quantity\":[${parentItem.quantity},$newParentQty],\"child_id\":${childItem.id}}"
        )

        recordTracking(
            stockItemId = childItem.id,
            trackingType = StockTrackingType.CREATED,
            label = "وحدة فرعية ناتجة عن تجزئة",
            deltas = "{\"quantity\":[0.0,$splitQuantity],\"parent_id\":$parentStockId}"
        )

        return childItem
    }

    /**
     * تنفيذ النقل المخزني الذري (Quick Stock Transfer) ونقل الكمية كلياً أو جزئياً بين المواقع.
     * @return Pair مكون من (العنصر المخزني المصدر المعدل، العنصر المخزني الهدف المعدل أو المنشأة حديثاً إن وجد)
     */
    fun transferStockItem(
        itemId: Long,
        sourceLocationId: Long?,
        targetLocationId: Long,
        quantityToTransfer: Double,
        reason: String,
        notes: String = ""
    ): Pair<StockItem, StockItem?> {
        val sourceIndex = stockItems.indexOfFirst { it.id == itemId }
        require(sourceIndex != -1) { "العنصر المخزني المرتبط بطلب النقل غير موجود (#$itemId)" }

        val sourceItem = stockItems[sourceIndex]
        require(quantityToTransfer > 0.0) { "كمية النقل يجب أن تكون أكبر من الصفر" }
        require(quantityToTransfer <= sourceItem.quantity) {
            "الكمية المطلوب نقلها ($quantityToTransfer) أكبر من الكمية المتاحة في السجل الحالي (${sourceItem.quantity})"
        }

        val targetLoc = locations.find { it.id == targetLocationId }
        if (targetLoc != null && targetLoc.structural) {
            throw IllegalArgumentException("لا يمكن نقل مواد مباشرة إلى موقع هيكلي ('${targetLoc.name}')")
        }

        val reasonText = "نقل من موقع #${sourceLocationId ?: "بدون"} إلى #$targetLocationId | السبب: $reason${if (notes.isNotBlank()) " ($notes)" else ""}"

        // الحالة (أ) - نقل الكمية بالكامل (Full Transfer)
        if (quantityToTransfer == sourceItem.quantity) {
            val updatedSource = sourceItem.copy(
                locationId = targetLocationId,
                updated = DateTimeUtils.getCurrentDate()
            )
            stockItems[sourceIndex] = updatedSource

            recordTracking(
                stockItemId = itemId,
                trackingType = StockTrackingType.MOVE,
                label = "نقل موقع التخزين بالكامل",
                notes = reasonText,
                deltas = "{\"locationId\":[${sourceLocationId ?: "null"},$targetLocationId],\"quantity\":$quantityToTransfer}"
            )

            return Pair(updatedSource, null)
        } else {
            // الحالة (ب) - نقل جزئي (Partial Transfer)
            val remainingSourceQty = sourceItem.quantity - quantityToTransfer
            val updatedSource = sourceItem.copy(
                quantity = remainingSourceQty,
                updated = DateTimeUtils.getCurrentDate()
            )
            stockItems[sourceIndex] = updatedSource

            recordTracking(
                stockItemId = itemId,
                trackingType = StockTrackingType.MOVE,
                label = "نقل جزئي للرصيد المخزني",
                notes = reasonText,
                deltas = "{\"locationId\":[${sourceLocationId ?: "null"},$targetLocationId],\"quantity\":[${sourceItem.quantity},$remainingSourceQty]}"
            )

            // فحص الموقع الجديد: هل يوجد سجل لنفس القطعة ونفس الدفعة في الموقع الهدف؟
            val targetMatchIndex = stockItems.indexOfFirst {
                it.id != itemId &&
                        it.locationId == targetLocationId &&
                        it.partId == sourceItem.partId &&
                        it.batch == sourceItem.batch &&
                        it.status == sourceItem.status
            }

            if (targetMatchIndex != -1) {
                val existingTarget = stockItems[targetMatchIndex]
                val updatedTarget = existingTarget.copy(
                    quantity = existingTarget.quantity + quantityToTransfer,
                    updated = DateTimeUtils.getCurrentDate()
                )
                stockItems[targetMatchIndex] = updatedTarget

                recordTracking(
                    stockItemId = existingTarget.id,
                    trackingType = StockTrackingType.MOVE,
                    label = "تزويد رصيد بنقل جزئي من موقع آخر",
                    notes = reasonText,
                    deltas = "{\"locationId\":[${sourceLocationId ?: "null"},$targetLocationId],\"quantity\":[${existingTarget.quantity},${updatedTarget.quantity}]}"
                )

                return Pair(updatedSource, updatedTarget)
            } else {
                // إنشاء سجل مخزني جديد في الموقع الهدف بالكمية المنقولة
                val newTargetItem = sourceItem.copy(
                    id = nextStockId++,
                    locationId = targetLocationId,
                    quantity = quantityToTransfer,
                    allocatedQuantity = 0.0,
                    parentId = sourceItem.id,
                    updated = DateTimeUtils.getCurrentDate()
                )
                stockItems.add(newTargetItem)

                recordTracking(
                    stockItemId = newTargetItem.id,
                    trackingType = StockTrackingType.MOVE,
                    label = "وحدة مخزنية ناتجة عن نقل جزئي",
                    notes = reasonText,
                    deltas = "{\"locationId\":[${sourceLocationId ?: "null"},$targetLocationId],\"quantity\":[0.0,$quantityToTransfer],\"parent_id\":${sourceItem.id}}"
                )

                return Pair(updatedSource, newTargetItem)
            }
        }
    }

    /**
     * إجراء عملية جرد فعلي (Stocktake) على وحدة مخزنية وتحديث تاريخ الجرد والمستخدم.
     */
    fun performStocktake(stockId: Long, userId: Long, stocktakeDate: String = DateTimeUtils.getCurrentDate()): StockItem {
        val index = stockItems.indexOfFirst { it.id == stockId }
        require(index != -1) { "الوحدة المخزنية غير موجودة" }

        val oldItem = stockItems[index]
        val updatedItem = oldItem.copy(
            stocktakeDate = stocktakeDate,
            stocktakeUserId = userId,
            reviewNeeded = false
        )
        stockItems[index] = updatedItem

        recordTracking(
            stockItemId = stockId,
            trackingType = StockTrackingType.COUNT,
            userId = userId,
            label = "جرد فعلي ميداني",
            deltas = "{\"quantity\":[${oldItem.quantity},${oldItem.quantity}],\"stocktake_date\":\"$stocktakeDate\"}"
        )

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
