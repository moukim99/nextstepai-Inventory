package com.nextstepai.inventory.data

/**
 * قالب المعامل القياسي الفني (PartParameterTemplate) المستوحي من InvenTree.
 * يحدد الاسم، الوحدة القياسية، الخيارات المتاحة، أو الخيار المنطقي (Checkbox).
 *
 * @property id المعرف الفريد للقالب
 * @property name اسم المعامل الفني (مثل: "المقاومة", "جهد التشغيل", "السعة") - فريد على مستوى النظام
 * @property units الوحدة القياسية (مثل: "Ω", "V", "µF") - تُعطّل إذا كان checkbox = true
 * @property description وصف المعامل الفني
 * @property choices قائمة الخيارات المسموح بها مفصولة - تُعطّل إذا كان checkbox = true
 * @property checkbox مفتاح منطقي نعم/لا (Boolean / Checkbox)
 */
data class PartParameterTemplate(
    val id: Long = 0L,
    val name: String,
    val units: String = "",
    val description: String = "",
    val choices: List<String> = emptyList(),
    val checkbox: Boolean = false
)

/**
 * الجدول الوسيط PartCategoryParameterTemplate لربط قوالب المعاملات بتصنيفات القطع.
 *
 * @property id المعرف الفريد السجل الرابط
 * @property categoryId معرّف التصنيف الشجري المرتبط (PartCategory)
 * @property parameterTemplateId معرّف قالب المعامل الفني المرتبط (PartParameterTemplate)
 * @property defaultValue القيمة الافتراضية للمعامل عند إنشاء قطعة جديدة في هذا التصنيف
 */
data class PartCategoryParameterTemplate(
    val id: Long = 0L,
    val categoryId: Long,
    val parameterTemplateId: Long,
    val defaultValue: String? = null
)

/**
 * سجل القيمة الفنية المحددة لقطعة معينة (Part Parameter Instance).
 *
 * @property id المعرف الفريد لسجل القيمة
 * @property partId معرّف القطعة المرتبطة (Part)
 * @property templateId معرّف قالب المعامل (PartParameterTemplate)
 * @property data القيمة النصية الخام المسجلة للمعامل الفني (مثل: "10k", "50V", "True")
 * @property dataNumeric القيمة العددية الصافية المستخرجة تلقائياً للفرز والترتيب الرياضي
 */
data class PartParameter(
    val id: Long = 0L,
    val partId: Long,
    val templateId: Long,
    val data: String,
    val dataNumeric: Double? = null
) {
    // الخاصية التوافقية لقراءة القيمة النصية
    val value: String get() = data
}

/**
 * نموذج عرض مركب لمعامل القطعة مع بيانات القالب المرجعية والقيمة المسجلة.
 */
data class PartParameterView(
    val parameter: PartParameter,
    val template: PartParameterTemplate
)

/**
 * نموذج العرض المركب لقالب معامل التصنيف مع معلومات القالب والإشارة إلى التوريث.
 *
 * @property categoryTemplate السجل الوسيط
 * @property template معلومات قالب المعامل القياسي
 * @property isInherited هل هذا القالب موروث من تصنيف أب أعلى في الشجرة؟
 * @property sourceCategoryName اسم التصنيف المصدر (سواء الحالي أو الأب الموروث منه)
 */
data class CategoryParameterTemplateView(
    val categoryTemplate: PartCategoryParameterTemplate,
    val template: PartParameterTemplate,
    val isInherited: Boolean,
    val sourceCategoryName: String
)

/**
 * إدارة وتخزين جدول PartCategoryParameterTemplate وقوالب ومعاملات القطع في الذاكرة مع قواعد العمل والإلزام.
 */
class PartCategoryParameterTable(
    private val partTable: PartTable = PartTable()
) {
    private val parameterTemplates = mutableListOf<PartParameterTemplate>()
    private val categoryParameterTemplates = mutableListOf<PartCategoryParameterTemplate>()
    private val partParameters = mutableListOf<PartParameter>()

    private var nextTemplateId = 1L
    private var nextCategoryTemplateId = 1L
    private var nextParameterId = 1L

    init {
        seedInitialTemplates()
    }

    /**
     * غرس بيانات أولية وقوالب قياسية تجريبية.
     */
    private fun seedInitialTemplates() {
        val resTemp = insertParameterTemplate(
            PartParameterTemplate(
                name = "المقاومة الكهربائية",
                units = "Ω",
                description = "قيمة المقاومة بالأوم",
                choices = listOf("100", "1k", "10k", "100k")
            )
        )

        val voltTemp = insertParameterTemplate(
            PartParameterTemplate(
                name = "جهد التشغيل",
                units = "V",
                description = "الجهد الكهربائي الافتراضي بالأنفولت",
                choices = listOf("3.3V", "5V", "12V", "24V")
            )
        )

        val tempTemp = insertParameterTemplate(
            PartParameterTemplate(
                name = "نطاق درجات الحرارة",
                units = "°C",
                description = "درجة حرارة التشغيل المسموح بها"
            )
        )

        // ربط قوالب المعاملات بالتصنيفات الأولى من PartTable
        val categories = partTable.getAllCategories()
        val cat1 = categories.firstOrNull { it.name.contains("مكونات إلكترونية") }
        val cat2 = categories.firstOrNull { it.name.contains("لوحات تحكم") }

        if (cat1 != null) {
            insertCategoryParameterTemplate(
                categoryId = cat1.id,
                parameterTemplateId = resTemp.id,
                defaultValue = "10k"
            )
        }

        if (cat2 != null) {
            insertCategoryParameterTemplate(
                categoryId = cat2.id,
                parameterTemplateId = voltTemp.id,
                defaultValue = "3.3V"
            )
            insertCategoryParameterTemplate(
                categoryId = cat2.id,
                parameterTemplateId = tempTemp.id,
                defaultValue = "-40 to +85"
            )
        }
    }

    /**
     * إدراج قالب معامل قياسي جديد مع تطبيق قواعد العمل:
     * 1. قيد تفرد الاسم (Unique Name Constraint).
     * 2. حظر الوحدة والخيارات إذا تم تفعيل المفتاح المنطقي (checkbox = true).
     */
    fun insertParameterTemplate(template: PartParameterTemplate): PartParameterTemplate {
        require(template.name.isNotBlank()) {
            "اسم قالب المعامل الفني إلزامي ولا يمكن أن يكون فارغاً."
        }

        val nameExists = parameterTemplates.any {
            it.name.trim().equals(template.name.trim(), ignoreCase = true)
        }
        require(!nameExists) {
            "اسم قالب المعامل '${template.name.trim()}' موجود بالفعل بالنظام. (قيد الفرادة Unique Name Constraint)"
        }

        // معالجة التعارض الوظيفي: إذا كان checkbox = true يتم تعطيل وتفريغ وحدات القياس والخيارات
        val sanitizedTemplate = if (template.checkbox) {
            template.copy(units = "", choices = emptyList())
        } else {
            template
        }

        val newTemplate = sanitizedTemplate.copy(
            id = if (sanitizedTemplate.id == 0L) nextTemplateId++ else sanitizedTemplate.id
        )
        parameterTemplates.add(newTemplate)
        return newTemplate
    }

    /**
     * إدراج سجل جديد في جدول PartCategoryParameterTemplate مع التحقق من الشروط والقواعد:
     * 1. التفرد المركب (unique_together = ['category', 'parameter_template']).
     * 2. تطابق القيمة الافتراضية مع خيارات القالب إن وُجدت خيارات محددة.
     */
    fun insertCategoryParameterTemplate(
        categoryId: Long,
        parameterTemplateId: Long,
        defaultValue: String? = null
    ): PartCategoryParameterTemplate {
        // 1. التحقق من التفرد المركب (Unique Constraint)
        val exists = categoryParameterTemplates.any {
            it.categoryId == categoryId && it.parameterTemplateId == parameterTemplateId
        }
        require(!exists) {
            "قالب المعامل الفني مرتبط بالفعل بهذا التصنيف! (قيد التفرد unique_together مُفعّل)"
        }

        // 2. التحقق من وجود قالب المعامل والتصنيف
        val template = parameterTemplates.find { it.id == parameterTemplateId }
            ?: throw IllegalArgumentException("قالب المعامل المرفق غير موجود (#$parameterTemplateId)")

        // 3. التحقق من القيمة الافتراضية مقارنة بالخيارات المتاحة المحددة
        if (!defaultValue.isNull_orBlank() && template.choices.isNotEmpty()) {
            require(template.choices.contains(defaultValue)) {
                "القيمة الافتراضية '$defaultValue' غير متوافقة مع الخيارات المتاحة للقالب (${template.choices.joinToString()})"
            }
        }

        val record = PartCategoryParameterTemplate(
            id = nextCategoryTemplateId++,
            categoryId = categoryId,
            parameterTemplateId = parameterTemplateId,
            defaultValue = defaultValue
        )
        categoryParameterTemplates.add(record)
        return record
    }

    /**
     * جلب كافة القوالب القياسية المتاحة للنظام.
     */
    fun getAllParameterTemplates(): List<PartParameterTemplate> = parameterTemplates.toList()

    /**
     * جلب سجلات PartCategoryParameterTemplate الخاصة بتصنيف محدد، مع دعم التوريث من التصنيفات الأب.
     */
    fun getCategoryParameterTemplatesForCategory(
        categoryId: Long,
        includeInherited: Boolean = true
    ): List<CategoryParameterTemplateView> {
        val result = mutableListOf<CategoryParameterTemplateView>()
        val allCategories = partTable.getAllCategories()
        val processedTemplateIds = mutableSetOf<Long>()

        var currentCatId: Long? = categoryId

        while (currentCatId != null) {
            val cat = allCategories.find { it.id == currentCatId } ?: break
            val isCurrentCat = (currentCatId == categoryId)

            val matches = categoryParameterTemplates.filter { it.categoryId == currentCatId }
            for (match in matches) {
                if (!processedTemplateIds.contains(match.parameterTemplateId)) {
                    val template = parameterTemplates.find { it.id == match.parameterTemplateId }
                    if (template != null) {
                        result.add(
                            CategoryParameterTemplateView(
                                categoryTemplate = match,
                                template = template,
                                isInherited = !isCurrentCat,
                                sourceCategoryName = cat.name
                            )
                        )
                        processedTemplateIds.add(match.parameterTemplateId)
                    }
                }
            }

            if (!includeInherited) break
            currentCatId = cat.parentId
        }

        return result
    }

    /**
     * التوريث والتأثير العكسي على القطع (Auto Generation for Part Parameters):
     * عند إنشاء قطعة جديدة تابعة لتصنيف ما، ينشئ النظام تلقائياً سجلات PartParameter
     * لكل القوالب المرتبطة بالتصنيف وتعبئتها بالقيمة الافتراضية defaultValue مع حساب data_numeric.
     */
    fun autoGenerateParametersForPart(partId: Long, categoryId: Long?): List<PartParameter> {
        if (categoryId == null) return emptyList()

        val templates = getCategoryParameterTemplatesForCategory(categoryId, includeInherited = true)
        val generated = mutableListOf<PartParameter>()

        for (item in templates) {
            val defaultValue = item.categoryTemplate.defaultValue ?: ""
            val numericVal = parseDataNumeric(defaultValue)
            val param = PartParameter(
                id = nextParameterId++,
                partId = partId,
                templateId = item.template.id,
                data = defaultValue,
                dataNumeric = numericVal
            )
            partParameters.add(param)
            generated.add(param)
        }

        return generated
    }

    /**
     * إدراج معامل جديد لقطعة معينة (PartParameter) مع تطبيق القيود:
     * 1. قيد التفرد المركب (unique_together = ['part', 'template']).
     * 2. التحقق من مطابقة القيمة مع خيارات القالب (choices) أو الخيار المنطقي (checkbox).
     * 3. الاستخراج التلقائي للقيمة العددية الصافية (data_numeric).
     */
    fun insertPartParameter(
        partId: Long,
        templateId: Long,
        data: String
    ): PartParameter {
        val template = parameterTemplates.find { it.id == templateId }
            ?: throw IllegalArgumentException("قالب المعامل الفني المطلوب غير موجود (#$templateId)")

        // 1. قيد التفرد المركب (unique_together)
        val exists = partParameters.any { it.partId == partId && it.templateId == templateId }
        require(!exists) {
            "المعامل الفني '${template.name}' مضاف بالفعل لهذه القطعة! (قيد التفرد unique_together مُفعّل)"
        }

        // 2. التحقق من قيود القالب
        if (template.checkbox) {
            val lower = data.trim().lowercase()
            require(lower == "true" || lower == "false" || lower == "نعم" || lower == "لا") {
                "القيمة المسجلة للمعامل المنطقي '${template.name}' يجب أن تكون True أو False."
            }
        } else if (template.choices.isNotEmpty()) {
            require(template.choices.contains(data.trim())) {
                "القيمة '$data' غير متوافقة مع الخيارات المتاحة للقالب (${template.choices.joinToString()})"
            }
        }

        // 3. استخراج القيمة العددية الصافية data_numeric
        val numericVal = parseDataNumeric(data)

        val newParam = PartParameter(
            id = nextParameterId++,
            partId = partId,
            templateId = templateId,
            data = data.trim(),
            dataNumeric = numericVal
        )
        partParameters.add(newParam)
        return newParam
    }

    /**
     * دالة تحليل واستخراج القيمة العددية الصافية (SI Prefix Parsing).
     * تحول القيم النصية مثل "10k" -> 10000.0 و "2.2uF" -> 0.0000022.
     */
    fun parseDataNumeric(input: String): Double? {
        if (input.isBlank()) return null
        val clean = input.trim()

        val regex = Regex("""^([+-]?\d*(?:\.\d+)?)\s*([kKMmµunp])?""")
        val match = regex.find(clean) ?: return clean.toDoubleOrNull()

        val (numStr, prefix) = match.destructured
        if (numStr.isEmpty() || numStr == "+" || numStr == "-") return null
        val num = numStr.toDoubleOrNull() ?: return null

        val multiplier = when (prefix) {
            "k", "K" -> 1_000.0
            "M" -> 1_000_000.0
            "m" -> 0.001
            "u", "µ" -> 0.000001
            "n" -> 0.000000001
            "p" -> 0.000000000001
            else -> 1.0
        }

        return num * multiplier
    }

    /**
     * جلب المعاملات الفنية لقطعة معينة.
     */
    fun getParametersForPart(partId: Long): List<PartParameter> {
        return partParameters.filter { it.partId == partId }
    }

    /**
     * جلب المعاملات الفنية لقطعة معينة مع بيانات القوالب للعرض.
     */
    fun getPartParametersWithTemplates(partId: Long): List<PartParameterView> {
        return partParameters
            .filter { it.partId == partId }
            .mapNotNull { param ->
                val template = parameterTemplates.find { it.id == param.templateId }
                if (template != null) PartParameterView(param, template) else null
            }
    }

    /**
     * حساب شارة مؤشر استخدام القالب (Template Usage Metric):
     * يعيد عدد القيم المسجلة للقطع (PartParameter) المعرّفة على هذا القالب.
     */
    fun getTemplateUsageMetric(templateId: Long): Int {
        return partParameters.count { it.templateId == templateId }
    }

    /**
     * حذف قالب معامل قياسي بتطبيق الحذف المتتابع (CASCADE):
     * مسح القالب، ومسح كافة الارتباطات التابعة له في PartCategoryParameterTemplate و PartParameter.
     */
    fun deleteParameterTemplate(templateId: Long): Boolean {
        categoryParameterTemplates.removeIf { it.parameterTemplateId == templateId }
        partParameters.removeIf { it.templateId == templateId }
        return parameterTemplates.removeIf { it.id == templateId }
    }

    /**
     * حذف ارتباط قالب المعامل الفني بتصنيف معينة (CASCADE Behavior).
     */
    fun deleteCategoryParameterTemplate(id: Long): Boolean {
        return categoryParameterTemplates.removeIf { it.id == id }
    }
}

private fun String?.isNull_orBlank(): Boolean = this == null || this.isBlank()
