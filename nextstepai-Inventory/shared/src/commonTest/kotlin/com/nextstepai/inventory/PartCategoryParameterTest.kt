package com.nextstepai.inventory

import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartCategoryParameterTable
import com.nextstepai.inventory.data.PartParameterTemplate
import com.nextstepai.inventory.data.PartTable
import com.nextstepai.inventory.repository.PartRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PartCategoryParameterTest {

    @Test
    fun testUniqueConstraintForCategoryAndTemplate() {
        val partTable = PartTable()
        val catTable = PartCategoryParameterTable(partTable)

        val temp = catTable.insertParameterTemplate(
            PartParameterTemplate(name = "جهد النواة المباشر", units = "V")
        )
        val category = partTable.getAllCategories().first()

        // إضافة الربط الأول بين التصنيف والقالب
        catTable.insertCategoryParameterTemplate(
            categoryId = category.id,
            parameterTemplateId = temp.id,
            defaultValue = "5V"
        )

        // محاولة تكرار نفس القالب لنفس التصنيف يجب أن تطلق استثناء بسبب قيد التفرد المركب (unique_together)
        assertFailsWith<IllegalArgumentException> {
            catTable.insertCategoryParameterTemplate(
                categoryId = category.id,
                parameterTemplateId = temp.id,
                defaultValue = "3.3V"
            )
        }
    }

    @Test
    fun testDefaultValueValidationAgainstChoices() {
        val partTable = PartTable()
        val catTable = PartCategoryParameterTable(partTable)

        val tempWithChoices = catTable.insertParameterTemplate(
            PartParameterTemplate(
                name = "دقة التردد",
                choices = listOf("10MHz", "20MHz", "50MHz")
            )
        )
        val category = partTable.getAllCategories().first()

        // قيمة غير موجودة في القائمة المسموحة يجب أن تفشل
        assertFailsWith<IllegalArgumentException> {
            catTable.insertCategoryParameterTemplate(
                categoryId = category.id,
                parameterTemplateId = tempWithChoices.id,
                defaultValue = "100MHz"
            )
        }

        // قيمة موجودة في القائمة يجب أن تنجح
        val validRecord = catTable.insertCategoryParameterTemplate(
            categoryId = category.id,
            parameterTemplateId = tempWithChoices.id,
            defaultValue = "20MHz"
        )
        assertEquals("20MHz", validRecord.defaultValue)
    }

    @Test
    fun testParameterTemplateInheritanceFromParentCategory() {
        val partTable = PartTable()
        val catTable = PartCategoryParameterTable(partTable)

        val parentCat = partTable.insertCategory("الالكترونيات العامة")
        val childCat = partTable.insertCategory("المكثفات", parentId = parentCat.id)

        val parentTemp = catTable.insertParameterTemplate(
            PartParameterTemplate(name = "درجة أمان الحرارة", units = "°C")
        )

        val childTemp = catTable.insertParameterTemplate(
            PartParameterTemplate(name = "سعة المكثف", units = "µF")
        )

        // ربط القالب الأول بالتصنيف الأب
        catTable.insertCategoryParameterTemplate(
            categoryId = parentCat.id,
            parameterTemplateId = parentTemp.id,
            defaultValue = "105°C"
        )

        // ربط القالب الثاني بالتصنيف الفرعي الابن
        catTable.insertCategoryParameterTemplate(
            categoryId = childCat.id,
            parameterTemplateId = childTemp.id,
            defaultValue = "100µF"
        )

        // جلب القوالب للتصنيف الابن مع تمكين التوريث
        val childTemplatesView = catTable.getCategoryParameterTemplatesForCategory(childCat.id, includeInherited = true)

        assertEquals(2, childTemplatesView.size)
        assertTrue(childTemplatesView.any { it.template.name == "سعة المكثف" && !it.isInherited })
        assertTrue(childTemplatesView.any { it.template.name == "درجة أمان الحرارة" && it.isInherited })
    }

    @Test
    fun testAutoGenerationOfPartParametersOnPartCreation() {
        val partTable = PartTable()
        val catTable = PartCategoryParameterTable(partTable)
        val repository = PartRepository(
            partTable = partTable,
            categoryParameterTable = catTable
        )

        val category = partTable.getAllCategories().first()
        val temp = catTable.insertParameterTemplate(
            PartParameterTemplate(name = "معامل الجهد المطلوب", units = "V")
        )

        catTable.insertCategoryParameterTemplate(
            categoryId = category.id,
            parameterTemplateId = temp.id,
            defaultValue = "12V"
        )

        // إنشاء قطعة جديدة ينبغي أن يولد تلقائياً سجلات المعاملات الفنية من القوالب
        val newPart = repository.addPart(
            Part(
                name = "لوحة امداد طاقة 12V",
                categoryId = category.id
            )
        )

        val partParameters = repository.getPartParameters(newPart.id)
        assertNotNull(partParameters)
        assertTrue(partParameters.isNotEmpty())
        assertTrue(partParameters.any { it.value == "12V" })
    }

    @Test
    fun testPartParameterTemplateConstraintsAndCascadeDelete() {
        val partTable = PartTable()
        val catTable = PartCategoryParameterTable(partTable)

        // 1. اختبار قيد فرادة اسم القالب (Unique Name Constraint)
        catTable.insertParameterTemplate(PartParameterTemplate(name = "الجهد الكهربائي المميز"))
        assertFailsWith<IllegalArgumentException> {
            catTable.insertParameterTemplate(PartParameterTemplate(name = "الجهد الكهربائي المميز"))
        }

        // 2. اختبار معالجة التعارض الوظيفي مع checkbox = true
        val checkboxTemp = catTable.insertParameterTemplate(
            PartParameterTemplate(
                name = "مطابق لـ RoHS",
                units = "V", // ينبغي تفريغه
                choices = listOf("نعم", "لا"), // ينبغي تفريغه
                checkbox = true
            )
        )
        assertTrue(checkboxTemp.checkbox)
        assertEquals("", checkboxTemp.units)
        assertTrue(checkboxTemp.choices.isEmpty())

        // 3. اختبار الحذف المتتابع (CASCADE Delete) وحساب مؤشر الاستخدام
        val category = partTable.getAllCategories().first()
        catTable.insertCategoryParameterTemplate(
            categoryId = category.id,
            parameterTemplateId = checkboxTemp.id,
            defaultValue = "true"
        )

        val repository = PartRepository(partTable = partTable, categoryParameterTable = catTable)
        val part = repository.addPart(Part(name = "قطعة اختبارية", categoryId = category.id))

        assertEquals(1, catTable.getTemplateUsageMetric(checkboxTemp.id))

        // عند حذف القالب الأساسي، يجب مسحه ومسح كافة الارتباطات التابعة له CASCADE
        catTable.deleteParameterTemplate(checkboxTemp.id)
        assertEquals(0, catTable.getTemplateUsageMetric(checkboxTemp.id))
        assertTrue(repository.getPartParameters(part.id).none { it.templateId == checkboxTemp.id })
    }

    @Test
    fun testPartParameterUniqueConstraintsAndNumericParsing() {
        val partTable = PartTable()
        val catTable = PartCategoryParameterTable(partTable)

        val tempRes = catTable.insertParameterTemplate(
            PartParameterTemplate(name = "قيمة المقاومة الاختبارية", units = "Ω")
        )
        val part = partTable.insertPart(Part(name = "مقاومة رقمية"))

        // 1. اختبار استخراج القيمة العددية الصافية data_numeric (SI Prefix Parsing)
        val param1 = catTable.insertPartParameter(
            partId = part.id,
            templateId = tempRes.id,
            data = "10k"
        )
        assertEquals("10k", param1.data)
        assertEquals(10000.0, param1.dataNumeric)

        // 2. اختبار قيد الفرادة المركب (unique_together = ['part', 'template'])
        assertFailsWith<IllegalArgumentException> {
            catTable.insertPartParameter(
                partId = part.id,
                templateId = tempRes.id,
                data = "20k"
            )
        }

        // 3. اختبار تحليل تحويلات البادئات الأخرى (50V -> 50.0, 2.2uF -> 0.0000022)
        assertEquals(50.0, catTable.parseDataNumeric("50V"))
        assertEquals(0.0000022, catTable.parseDataNumeric("2.2uF"))
    }
}
