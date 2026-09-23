package com.nextstepai.inventory.data

import com.nextstepai.inventory.data.db.ManufacturingPhaseDao
import com.nextstepai.inventory.data.db.ManufacturingPhaseEntity

/**
 * نموذج بيانات مرحلة التصنيع والإنتاج (ManufacturingPhase Domain Model).
 *
 * @property uuid المعرف المرجعي العالمي الفريد للمرحلة
 * @property id المعرف التسلسلي
 * @property partUuid المعرف المرجعي للمنتج الأب المجمع المرتبط (NULL = مرحلة عامة)
 * @property name اسم مرحلة الإنتاج (مثل: تجهيز وتحضير المواد)
 * @property sequenceOrder ترتيب التتابع للمرحلة
 * @property description شرح تفصيلي عن مرحلة التشغيل
 * @property isSystemDefault مرحلة قياسية مسبقة في النظام حظر حذفها
 */
data class ManufacturingPhase(
    val uuid: String = "",
    val id: Long = 0L,
    val partUuid: String? = null,
    val name: String,
    val sequenceOrder: Int = 1,
    val description: String = "",
    val isSystemDefault: Boolean = false
)

/**
 * إدارة وتخزين المراحل التصنيعية في الذاكرة وقاعدة البيانات.
 */
class ManufacturingPhaseTable(
    private val dao: ManufacturingPhaseDao = ManufacturingPhaseDao()
) {
    private val memoryPhases = mutableListOf<ManufacturingPhase>()
    private var nextId = 1L

    init {
        seedInitialPhases()
    }

    private fun seedInitialPhases() {
        val defaultPhases = listOf(
            ManufacturingPhase(
                uuid = "phase-def-1",
                id = 1L,
                name = "تجهيز وتحضير المواد (Preparation)",
                sequenceOrder = 1,
                description = "تحضير وتجهيز المكونات والأجزاء الأولية قبل بدء الخلط أو التركيب",
                isSystemDefault = true
            ),
            ManufacturingPhase(
                uuid = "phase-def-2",
                id = 2L,
                name = "تصنيع / خلط ومعالجة أولية (Processing)",
                sequenceOrder = 2,
                description = "عمليات التصنيع الأولية كتشكيل، خلط المواد، أو معالجة القطع",
                isSystemDefault = true
            ),
            ManufacturingPhase(
                uuid = "phase-def-3",
                id = 3L,
                name = "تجميع وتركيب (Assembly)",
                sequenceOrder = 3,
                description = "تركيب وتلحيم المكونات مع بعضها على بوردة أو هيكل المنتج الأب",
                isSystemDefault = true
            ),
            ManufacturingPhase(
                uuid = "phase-def-4",
                id = 4L,
                name = "فحص واختبار جودة (Quality Inspection)",
                sequenceOrder = 4,
                description = "فحص الأداء الفني والجودة وضمان المطابقة للمواصفات",
                isSystemDefault = true
            ),
            ManufacturingPhase(
                uuid = "phase-def-5",
                id = 5L,
                name = "تعبئة وتغليف (Packaging)",
                sequenceOrder = 5,
                description = "تغليف المنتج النهائي وتجهيزه للتخزين أو الشحن",
                isSystemDefault = true
            )
        )
        memoryPhases.addAll(defaultPhases)
        nextId = 6L
    }

    fun getAllPhases(partUuid: String? = null): List<ManufacturingPhase> {
        val dbPhases = runCatching {
            dao.getAllPhases(partUuid).map { entity ->
                ManufacturingPhase(
                    uuid = entity.uuid,
                    id = entity.id,
                    partUuid = entity.partUuid,
                    name = entity.name,
                    sequenceOrder = entity.sequenceOrder,
                    description = entity.description,
                    isSystemDefault = entity.isSystemDefault
                )
            }
        }.getOrDefault(emptyList())

        val combinedMap = LinkedHashMap<String, ManufacturingPhase>()
        memoryPhases.forEach { combinedMap[it.uuid] = it }
        dbPhases.forEach { combinedMap[it.uuid] = it }

        return combinedMap.values
            .filter { partUuid == null || it.partUuid == null || it.partUuid == partUuid }
            .sortedBy { it.sequenceOrder }
    }

    fun insertPhase(phase: ManufacturingPhase): ManufacturingPhase {
        val uuid = if (phase.uuid.isBlank()) "phase-usr-${nextId++}" else phase.uuid
        val newPhase = phase.copy(
            uuid = uuid,
            id = if (phase.id == 0L) nextId++ else phase.id
        )
        memoryPhases.removeIf { it.uuid == uuid }
        memoryPhases.add(newPhase)

        runCatching {
            dao.insertOrUpdate(
                ManufacturingPhaseEntity(
                    uuid = newPhase.uuid,
                    id = newPhase.id,
                    partUuid = newPhase.partUuid,
                    name = newPhase.name,
                    sequenceOrder = newPhase.sequenceOrder,
                    description = newPhase.description,
                    isSystemDefault = newPhase.isSystemDefault
                )
            )
        }
        return newPhase
    }

    fun deletePhase(uuid: String): Boolean {
        memoryPhases.removeIf { it.uuid == uuid }
        runCatching { dao.deletePhase(uuid) }
        return true
    }
}
