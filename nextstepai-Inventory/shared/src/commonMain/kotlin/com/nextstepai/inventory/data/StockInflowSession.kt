package com.nextstepai.inventory.data

import kotlin.time.Clock

/**
 * تمثيل عنصر ممسوح مفصل داخل سلة الاستلام الميداني السريع (ScannedInflowItem).
 */
data class ScannedInflowItem(
    val tempId: String = "scan-${Clock.System.now().toEpochMilliseconds()}-${(1000..9999).random()}",
    val barcode: String,
    val partId: Long? = null,
    val partUuid: String? = null,
    val name: String,
    val quantity: Double = 1.0,
    val locationId: Long = 1L,
    val locationName: String = "الرف الرئيسي",
    val isNewPart: Boolean = false,
    val croppedImageBytes: ByteArray? = null,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScannedInflowItem) return false
        return tempId == other.tempId && barcode == other.barcode && quantity == other.quantity
    }

    override fun hashCode(): Int {
        var result = tempId.hashCode()
        result = 31 * result + barcode.hashCode()
        result = 31 * result + quantity.hashCode()
        return result
    }
}

/**
 * حالة جلسة الاستلام الميداني الشاملة (StockInflowSessionState).
 * تدعم إدارة العناصر الممسوحة، والتراجع الخاطف (Undo Buffer)، وإحصائيات السلة.
 */
data class StockInflowSessionState(
    val sessionUuid: String = "inflow-session-${Clock.System.now().toEpochMilliseconds()}",
    val selectedInflowTypeId: String = "PURCHASE_ORDER",
    val purchaseOrderId: Long? = null,
    val poReference: String? = null,
    val scannedItems: List<ScannedInflowItem> = emptyList(),
    val lastAddedItemForUndo: ScannedInflowItem? = null,
    val undoWindowActive: Boolean = false,
    val defaultLocationId: Long = 1L,
    val defaultLocationName: String = "الرف الرئيسي - المستودع العام",
    val isProcessingCommit: Boolean = false
) {
    val totalItemsCount: Int get() = scannedItems.size
    val totalQuantityCount: Double get() = scannedItems.sumOf { it.quantity }

    /**
     * إضافة عنصر ممسوح للسلة وتعيينه في كبسولة التراجع الخاطف (Undo Buffer).
     */
    fun addItem(item: ScannedInflowItem): StockInflowSessionState {
        val updatedList = scannedItems + item
        return copy(
            scannedItems = updatedList,
            lastAddedItemForUndo = item,
            undoWindowActive = true
        )
    }

    /**
     * التراجع عن إضافة العنصر الأخير المضاف.
     */
    fun undoLastAddedItem(): StockInflowSessionState {
        val itemToUndo = lastAddedItemForUndo ?: return this
        val updatedList = scannedItems.filterNot { it.tempId == itemToUndo.tempId }
        return copy(
            scannedItems = updatedList,
            lastAddedItemForUndo = null,
            undoWindowActive = false
        )
    }

    /**
     * إغلاق نافذة التراجع الخاطف بعد انتهاء المهلة الزمنية.
     */
    fun clearUndoBuffer(): StockInflowSessionState {
        return copy(
            lastAddedItemForUndo = null,
            undoWindowActive = false
        )
    }

    /**
     * تعديل كمية عنصر محدد في السلة.
     */
    fun updateItemQuantity(tempId: String, newQuantity: Double): StockInflowSessionState {
        val updatedList = scannedItems.map { item ->
            if (item.tempId == tempId) item.copy(quantity = newQuantity) else item
        }
        return copy(scannedItems = updatedList)
    }

    /**
     * إزالة عنصر محدد من السلة.
     */
    fun removeItem(tempId: String): StockInflowSessionState {
        val updatedList = scannedItems.filterNot { it.tempId == tempId }
        return copy(scannedItems = updatedList)
    }
}
