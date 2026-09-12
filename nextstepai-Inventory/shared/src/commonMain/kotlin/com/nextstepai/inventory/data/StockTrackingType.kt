package com.nextstepai.inventory.data

/**
 * أكواد وأنواع الحركات المخزنية المعتمدة في سجل التتبع الميداني التاريخي (StockTrackingType).
 */
enum class StockTrackingType(val code: Int, val label: String) {
    CREATED(10, "إنشاء عنصر مخزني (Stock created)"),
    MOVE(20, "نقل موقع التخزين (Stock moved)"),
    COUNT(30, "جرد فعلي (Stock counted)"),
    ADJUST(40, "تعديل الكمية (Quantity adjusted)"),
    SPLIT(50, "تجزئة كمية (Stock split)"),
    STATUS(60, "تغيير حالة (Status changed)"),
    MERGED(70, "دمج رصيد (Stock merged)"),
    CONVERTED(80, "تحويل نوع (Stock converted)");

    companion object {
        fun fromCode(code: Int): StockTrackingType = entries.find { it.code == code } ?: CREATED
    }
}
