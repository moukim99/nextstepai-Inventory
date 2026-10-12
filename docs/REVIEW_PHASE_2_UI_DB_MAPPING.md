# [Review Phase 2/7] مطابقة قاعدة البيانات مع واجهات الإضافة

## 1. الارتباط بالخطة ومعلومات المرحلة
- **المرحلة:** المرحلة الثانية من الخطة العامة لمراجعة إضافة البيانات ([Issue #9](https://github.com/moukim99/nextstepai-Inventory/issues/9) / [Issue #11](https://github.com/moukim99/nextstepai-Inventory/issues/11)).
- **الفرع:** `feature/add-review`.
- **الهدف:** مقارنة الحقول والعلاقات الموجودة في قاعدة البيانات (`SQLite / Room`) مع نموذج وواجهة الإضافة الحالية لكل قسم من الأقسام التسعة، ورصد الحقول المفقودة أو غير المحفوظة، وتوثيق العلاقات، دون إحداث أي تعديل في الكود البرمجي في هذه المرحلة.

---

## 2. جدول المراجعة الشاملة للأقسام التسعة

---

### القسم 1: المواد والأجزاء (Parts)
- **الملفات والكيانات المراجعة:**
  - واجهات المستخدم: [`PartManagementForms.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/ui/PartManagementForms.kt#L1033) (`AddPartBottomSheet`), [`PartViewModel.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/ui/PartViewModel.kt#L704) (`addNewPart`)
  - المستودع وقاعدة البيانات: [`PartRepository.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/repository/PartRepository.kt#L300), [`PartEntity.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/PartEntity.kt), [`PartDao.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/PartDao.kt), جدول `parts` في [`SqliteDatabaseSchema.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/SqliteDatabaseSchema.kt).

#### مطابقة الحقول:
| حقل قاعدة البيانات (`PartEntity`) | النوع والقيود | حقل واجهة الإضافة (`AddPartBottomSheet`) | حالة الحقل والملاحظات |
| :--- | :--- | :--- | :--- |
| `uuid` | `TEXT PRIMARY KEY` | مولّد تلقائياً عبر `AppUuid.generate()` | داخلي معرف UUIDv7 للمزامنة. |
| `id` | `INTEGER` | مولّد تلقائياً عبر `SqliteNumericIdAllocator` | داخلي لتوافق المعرفات الرقمية القديمة. |
| `name` | `TEXT NOT NULL` | حقل نصي إلزامي `name` | متطابق ومتحقق منه. |
| `ipn` | `TEXT NOT NULL` | حقل `SmartIpnField` التوليد الذكي/يدوي | متطابق ومتحقق من منع التكرار. |
| `description` | `TEXT NOT NULL` | حقل نصي في القسم المتقدم | متطابق. |
| `categoryUuid` | `TEXT NULL (FK)` | قائمة منسدلة لاختيار التصنيف | متطابق (يربط `catId` بـ `cat-XXX`). |
| `units` | `TEXT NOT NULL` | اختيار الوحدة (افتراضي `pcs`) | متطابق. |
| `minimumStock` | `REAL NOT NULL` | حقل نصي رقمي | متطابق. |
| `maximumStock` | `REAL NULL` | حقل نصي رقمي في القسم المتقدم | متطابق. |
| `totalInStock` | `REAL NOT NULL` | **غير معروض بنموذج الإضافة** | **حقل محسوب/تراكمي**؛ لا يتم إدخاله عند إنشاء الصنف، بل عبر حركات استلام الشحنات (`AddStockBottomSheet`). |
| `revision` | `TEXT NOT NULL` | حقل نصي بالقسم المتقدم | متطابق. |
| `keywords` | `TEXT NOT NULL` | حقل نصي بالقسم المتقدم | متطابق. |
| `assembly` | `INTEGER NOT NULL` | اختيار طبيعة الصنف (تجميع) | متطابق. |
| `component` | `INTEGER NOT NULL` | اختيار طبيعة الصنف (مكوّن) | متطابق. |
| `salable` | `INTEGER NOT NULL` | اختيار طبيعة الصنف (قابل للبيع) | متطابق. |
| `purchaseable` | `INTEGER NOT NULL` | **غير مباشر** | يتم احتسابه في الواجهة برمجياً: `val isPurch = isComp || isSale`. |
| `trackable` | `INTEGER NOT NULL` | **مفقود بالواجهة** | يُمرر كقيمة ثابتة `false` في دالة `onConfirm` بالسطر 2314، ولا يوجد زر اختيار له بالواجهة. |
| `isTemplate` | `INTEGER NOT NULL` | زر تبديل في القسم المتقدم | متطابق. |
| `variantOfUuid` | `TEXT NULL (FK)` | اختيار القالب الأب | متطابق (يربط مع `part-$it`). |
| `virtual` | `INTEGER NOT NULL` | زر تبديل بالقسم المتقدم | متطابق. |
| `active` | `INTEGER NOT NULL` | يُمرر كقيمة `true` | داخلي/افتراضي عند الإضافة. |
| `locked` | `INTEGER NOT NULL` | يُمرر كقيمة `false` | داخلي/افتراضي عند الإضافة. |
| `defaultLocationUuid` | `TEXT NULL (FK)` | اختيار موقع التخزين الافتراضي | متطابق (يربط مع `loc-XXX`). |
| `defaultExpiryDays` | `INTEGER NULL` | حقل رقمي بالقسم المتقدم | متطابق. |
| `localImagePath` | `TEXT NULL` | منتقي الصور والمستندات | يُحفظ عبر `pendingAttachments` و`localImagePath`. |
| `link` | `TEXT NOT NULL` | حوار إدخال رابط خارجي | متطابق. |
| `metadata` | `TEXT NOT NULL` | مولّد تلقائياً `{}` | داخلي. |
| `version, syncStatus, isDeleted, updatedAt` | حقول مزامنة | غير معروضة بالواجهة | حقول محرك المزامنة غير المتصلة (Offline-First). |

#### فحص العلاقات:
- علاقة التصنيف (`categoryUuid -> part_categories`): مدعومة بالكامل، مع إمكانية إضافة تصنيف فوري سريع (`onCreateCategory`).
- علاقة الموقع الافتراضي (`defaultLocationUuid -> stock_locations`): مدعومة عبر BottomSheet اختيار الموقع.
- علاقة القالب والبدائل (`variantOfUuid -> parts`): مدعومة بعزل الأصناف التي تعمل كقوالب (`isTemplate = true`).

#### المشكلات المؤكدة:
1. **تجاهل حقل تنبيهات نقص المخزون:** الواجهة تحتوي على مفتاح `lowStockAlertsEnabled` (السطور 2221-2244)، لكن عند الضغط على حفظ في `onConfirm` (السطور 2300-2323) لا يتم تمريره ولا حفظه في أي حقل أو `metadata`.
2. **غياب التحكم في `trackable`:** يتم تمريره دائماً `false` دون إمكانية تحديده من المستخدم، رغم دعمه في قاعدة البيانات.

#### الأسئلة المفتوحة والقرارات المقترحة:
- **القرار المقترح:** إضافة حقل `trackable` في القسم المتقدم كخيار تتبع تسلسلي، وتمرير خيار تنبيهات النقص إما في `metadata` أو كحقل في حال إضافة جدول تنبيهات مخصص.

---

### القسم 2: المخزون (Stock Items)
- **الملفات والكيانات المراجعة:**
  - واجهات المستخدم: [`StockScreenForms.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/ui/StockScreenForms.kt#L2583) (`AddStockItemBottomSheet`), [`StockViewModel.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/ui/StockViewModel.kt#L820) (`addStockItem`)
  - المستودع وقاعدة البيانات: [`StockRepository.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/repository/StockRepository.kt#L394), [`StockItemEntity.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/StockItemEntity.kt), [`StockItemDao.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/StockItemDao.kt).

#### مطابقة الحقول:
| حقل قاعدة البيانات (`StockItemEntity`) | النوع والقيود | حقل واجهة الإضافة (`AddStockItemBottomSheet`) | حالة الحقل والملاحظات |
| :--- | :--- | :--- | :--- |
| `uuid` | `TEXT PRIMARY KEY` | مولّد تلقائياً `stock-$id` | داخلي. |
| `partUuid` | `TEXT NOT NULL (FK)` | اختيار الصنف (`selectedPartId`) | متطابق (يربط مع `part-$id`). |
| `locationUuid` | `TEXT NULL (FK)` | اختيار موقع التخزين (`selectedLocationId`) | متطابق، مع تصفية المواقع الهيكلية (`!structural`). |
| `quantity` | `REAL NOT NULL` | حقل إدخال الكمية أو الآلة الحاسبة | متطابق ومتحقق منه. |
| `serial` | `TEXT NOT NULL` | حقل الرقم التسلسلي | متطابق. |
| `batch` | `TEXT NOT NULL` | حقل رقم الدفعة (Batch) | متطابق. |
| `statusCode` | `INTEGER NOT NULL` | قائمة منسدلة لحالة المخزون (`StockStatus`) | متطابق (10: OK, 20: ATTENTION, 50: DESTROYED...). |
| `packaging` | `TEXT NOT NULL` | اختيار نوع التغليف (Reel, Box, Tray...) | متطابق. |
| `expiryDate` | `TEXT NOT NULL` | منتقي تاريخ انتهاء الصلاحية | متطابق. |
| `purchasePrice` | `REAL NOT NULL` | حقل سعر الشراء | متطابق. |
| `purchasePriceCurrency` | `TEXT NOT NULL` | منتقي العملة (USD, EUR, SAR...) | متطابق. |
| `purchaseOrderUuid` | `TEXT NULL (FK)` | اختيار أمر الشراء المرتبط | متطابق، ويؤدي الحفظ إلى استلام بند أمر الشراء تلقائياً. |
| `supplierPartUuid` | `TEXT NOT NULL (FK)` | اختيار رقم قطعة المورد (`SupplierPart`) | متطابق. |
| `salesOrderUuid` | `TEXT NULL (FK)` | غير معروض بنموذج الإضافة المباشر | داخلي؛ يُربط عند تخصيص المخزون لأمر بيع. |
| `customerUuid` | `TEXT NOT NULL` | غير معروض بنموذج الإضافة المباشر | داخلي؛ يُربط عند تسليم البضاعة للعميل. |
| `buildUuid, isBuilding` | `TEXT / BOOL` | غير معروض بنموذج الإضافة المباشر | يخص أوامر التصنيع أثناء قيد التشغيل (WIP). |
| `parentStockItemUuid` | `TEXT NULL (FK)` | غير معروض بنموذج الإضافة المباشر | يُستخدم فقط عند تقسيم الشحنة (`splitStockItem`). |
| `stocktakeDate, stocktakeUserUuid` | `TEXT / FK` | غير معروض بنموذج الإضافة | خاص بالجرد الدوري الفعلي للمستودع. |
| `reviewNeeded` | `INTEGER NOT NULL` | زر تبديل "تحتاج مراجعة فنية" | متطابق. |
| `deleteOnDeplete` | `INTEGER NOT NULL` | زر تبديل "حذف عند نفاد الرصيد" | متطابق. |
| `link` | `TEXT NOT NULL` | حقل الرابط | متطابق. |
| `notes` | `TEXT NOT NULL` | حقل الملاحظات النصية | متطابق. |
| `metadata` | `TEXT NOT NULL` | يستقبل `unitWeight` و`totalWeight` | يتم حفظ الأوزان بداخل JSON الـ `metadata` تلقائياً عبر `withWeightInfo`. |

#### فحص العلاقات:
- علاقة الصنف والموقع: خيارات واضحة مع إمكانية إنشاء صنف جديد أو موقع جديد مباشرة من داخل الواجهة.
- علاقة أمر الشراء وقطعة المورد: مترابطة وتوفر تكامل المشتريات تلقائياً.

#### المشكلات المؤكدة:
- لا توجد مشكلات فقدان بيانات جوهرية؛ الأوزان (`unitWeight`, `totalWeight`) تُحفظ بـ `metadata` JSON لأن الكيان لا يحتوي على أعمدة خاصة بها، وهذا سلوك معتمد ومستقر.

---

### القسم 3: مواقع التخزين (Stock Locations)
- **الملفات والكيانات المراجعة:**
  - واجهات المستخدم: [`StockScreenForms.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/ui/StockScreenForms.kt#L99) (`AddStockLocationBottomSheet`)
  - المستودع وقاعدة البيانات: [`StockLocationRepository.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/repository/StockLocationRepository.kt#L154), [`StockLocationEntity.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/StockLocationEntity.kt), [`StockLocationDao.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/StockLocationDao.kt).

#### مطابقة الحقول:
| حقل قاعدة البيانات (`StockLocationEntity`) | النوع والقيود | حقل واجهة الإضافة (`AddStockLocationBottomSheet`) | حالة الحقل والملاحظات |
| :--- | :--- | :--- | :--- |
| `uuid` | `TEXT PRIMARY KEY` | مولّد تلقائياً `loc-$id` | داخلي. |
| `id` | `INTEGER` | مولّد عبر `SqliteNumericIdAllocator` | داخلي. |
| `name` | `TEXT NOT NULL` | حقل نصي إلزامي `name` | متطابق. |
| `description` | `TEXT NOT NULL` | حقل الوصف | متطابق. |
| `parentId / parentUuid` | `INTEGER / TEXT (FK)` | اختيار الموقع الأب الشجري | متطابق؛ يدعم التحقق الهرمي وعزل المسافات الرتبية. |
| `ownerId` | `INTEGER NULL (FK)` | اختيار المسؤول من قائمة المستخدمين | متطابق. |
| `structural` | `INTEGER NOT NULL` | زر تبديل موقع هيكلي (لا يقبل تخزين عناصر) | متطابق. |
| `external` | `INTEGER NOT NULL` | زر تبديل موقع خارجي | متطابق. |
| `locationType` | `TEXT NOT NULL` | اختيار نوع الموقع (SITE, WAREHOUSE, RACK...) | متطابق. |
| `customCapacity` | `REAL NULL` | السعة الاستيعابية القصوى | متطابق. |
| `isBulkGenerated` | `INTEGER NOT NULL` | يُفعل تلقائياً عند التوليد الجماعي للأرفف | متطابق. |
| `address` | `TEXT NOT NULL` | حقل العنوان الفعلي للموقع | متطابق. |
| `icon, customIcon` | `TEXT NOT NULL` | اختيار الأيقونة والأيقونة المخصصة | متطابق. |
| `level, lft, rght, treeId` | `INTEGER` | حقول خوارزمية الشجرة المتداخلة MPTT | داخلية؛ تُحسب تلقائياً بواسطة محرك المزامنة وإدارة الشجرة. |
| `metadata` | `TEXT NOT NULL` | يستقبل: `contactPerson, contactPhone, capacityUnit, isPrimary` | يتم حفظ هذه الحقول في `metadata` JSON عبر دوال التوسيع في `StockLocation.kt`. |

#### فحص العلاقات:
- علاقة الشجرة الهرمية (`parentId`): ممتازة مع فحص ومنع الحلقات الدائرية (Cyclic Dependency Guard).
- علاقة المالك (`ownerId`): مرتبطة مع `AppUser`.

#### المشكلات والقرارات المقترحة:
- تم التحقق من حفظ `isPrimary` و`contactPerson` و`contactPhone` داخل `metadata` بشكل آمن في كل من `StockLocation` و`StockLocationRepository`.

---

### القسم 4: الشركات والموردون (Companies & Suppliers)
- **الملفات والكيانات المراجعة:**
  - واجهات المستخدم: [`CompanyScreenForms.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/ui/CompanyScreenForms.kt#L59) (`AddCompanyBottomSheet`), [`CompanyViewModel.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/ui/CompanyViewModel.kt#L380) (`addCompany`)
  - المستودع وقاعدة البيانات: [`CompanyRepository.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/repository/CompanyRepository.kt#L146), [`CompanyEntity.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/CompanyEntity.kt), [`CompanyDao.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/CompanyDao.kt).

#### مطابقة الحقول:
| حقل قاعدة البيانات (`CompanyEntity`) | النوع والقيود | حقل واجهة الإضافة (`AddCompanyBottomSheet`) | حالة الحقل والملاحظات |
| :--- | :--- | :--- | :--- |
| `uuid` | `TEXT PRIMARY KEY` | مولّد تلقائياً `company-$id` | داخلي. |
| `id` | `INTEGER` | مولّد عبر `SqliteNumericIdAllocator` | داخلي. |
| `name` | `TEXT NOT NULL` | حقل اسم الشركة الإلزامي | متطابق، مع فحص منع تكرار الاسم. |
| `description` | `TEXT NOT NULL` | حقل الوصف والنشاط | متطابق. |
| `website` | `TEXT NOT NULL` | الموقع الإلكتروني | متطابق. |
| `phone` | `TEXT NOT NULL` | الهاتف مع اختيار كود الدولة | متطابق. |
| `email` | `TEXT NOT NULL` | البريد الإلكتروني | متطابق. |
| `address` | **غير موجود في `CompanyEntity`** | حقل إدخال العنوان النصي بالواجهة | **مشكلة حرجة: لا يتم حفظه في جدول `companies`!** |
| `contact` | **غير موجود في `CompanyEntity`** | حقل اسم جهة الاتصال الأساسية | **مشكلة حرجة: لا يتم حفظه في جدول `companies`!** |
| `isSupplier` | `INTEGER NOT NULL` | زر تبديل (مورد) | متطابق. |
| `isManufacturer` | `INTEGER NOT NULL` | زر تبديل (مصنّع) | متطابق. |
| `isCustomer` | `INTEGER NOT NULL` | زر تبديل (عميل) | متطابق. |
| `active` | `INTEGER NOT NULL` | مفتاح التفعيل | متطابق. |
| `currency` | `TEXT NOT NULL` | اختيار العملة الافتراضية للتعامل | متطابق. |
| `logoPath` | `TEXT NULL` | رابط أو مسار الشعار | متطابق مع `imageUrl`. |
| `notes` | `TEXT NOT NULL` | حقل الملاحظات العامة | متطابق. |
| `parentUuid` | `TEXT NULL (FK)` | اختيار الشركة القابضة / الأب | متطابق. |

#### فحص العلاقات:
- الشركة تمتلك جداول فرعية منفصلة: عناوين (`company_addresses`) وجهات اتصال (`company_contacts`).

#### المشكلات المؤكدة بالأدلة:
- **فقدان بيانات العنوان (`address`) وجهة الاتصال (`contact`):**
  - في [`CompanyScreenForms.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/ui/CompanyScreenForms.kt#L89-L90)، يُدخل المستخدم `address` و`contact`.
  - يتم تمريرهما إلى `CompanyViewModel.addCompany` -> `Company(address = address, contact = contact)`.
  - في [`CompanyRepository.kt` السطور 156-178](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/repository/CompanyRepository.kt#L156-L178)، يتم الحفظ في `CompanyEntity` التي **لا تملك عموداً للعنوان أو جهة الاتصال**، ولا يتم استدعاء `insert` في جدول `company_addresses` أو `company_contacts` أو التخزين في `metadata`.
  - **النتيجة:** بمجرد إعادة تشغيل التطبيق أو القراءة من SQLite، يختفي العنوان واسم جهة الاتصال المدخل في نموذج الإضافة!

#### القرار المقترح:
- عند إضافة الشركة في `CompanyRepository.addCompany`، إذا تم إدخال `address` أو `contact` غير فارغ، يتم تلقائياً إنشاء سجل في `AddressEntity` وسجل في `ContactEntity` وربطهما بـ `companyId` المخصص، أو تضمينهما في `metadata`.

---

### القسم 5: أوامر الشراء (Purchase Orders)
- **الملفات والكيانات المراجعة:**
  - واجهات المستخدم: [`PurchaseOrderScreen.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/ui/PurchaseOrderScreen.kt#L847) (`AddPurchaseOrderBottomSheet`), [`PurchaseOrderViewModel.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/ui/PurchaseOrderViewModel.kt#L150)
  - المستودع وقاعدة البيانات: [`PurchaseOrderRepository.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/repository/PurchaseOrderRepository.kt), [`PurchaseOrderEntity.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/PurchaseOrderEntity.kt), [`PurchaseOrderDao.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/PurchaseOrderDao.kt).

#### مطابقة الحقول:
| حقل قاعدة البيانات (`PurchaseOrderEntity`) | النوع والقيود | حقل واجهة الإضافة (`AddPurchaseOrderBottomSheet`) | حالة الحقل والملاحظات |
| :--- | :--- | :--- | :--- |
| `uuid` | `TEXT PRIMARY KEY` | مولّد تلقائياً `po-$id` | داخلي. |
| `reference` | `TEXT NOT NULL` | حقل الرمز المرجعي (e.g. PO-2025-001) | متطابق. |
| `supplierId` | `INTEGER NOT NULL (FK)` | اختيار المورد من قائمة الموردين | متطابق. |
| `supplierName` | `TEXT NOT NULL` | يُستخرج تلقائياً من اسم المورد | متطابق. |
| `statusCode` | `INTEGER NOT NULL` | يُسجل بحالة مبدئية 10 (معلّق PENDING) | متطابق. |
| `description` | `TEXT NOT NULL` | حقل الوصف النصي | متطابق. |
| `orderCurrency` | `TEXT NOT NULL` | اختيار عملة الطلب | متطابق. |
| `targetDate` | `TEXT NOT NULL` | تاريخ التسليم المتوقع | متطابق. |
| `totalCost` | `REAL NOT NULL` | محسوب تلقائياً من بنود الشراء المضافة | متطابق. |
| `sourceType` | `TEXT NOT NULL` | اختيار المصدر (MANUAL, BUILD_ORDER, SALES_ORDER) | متطابق، مع دعم استيراد النواقص آلياً. |
| `sourceReferenceUuid` | `TEXT NULL (FK)` | المعرف المرجعي للأمر المرتبط | متطابق. |
| `destinationLocationUuid` | `TEXT NULL (FK)` | **غير موجود بنموذج الإضافة** | **حقل مفقود بالواجهة**: يدعمه الكيان والـ Repository لكن لا يوجد منتقي لاختياره في `AddPurchaseOrderBottomSheet`! |

#### بنود أمر الشراء (`purchase_order_lines`):
- يدعم النموذج إضافة بنود متعددة في نفس وقت إنشاء الأمر: `supplierPartId`, `quantity`, `purchasePrice`.

#### المشكلات المؤكدة:
- غياب خيار تحديد موقع الاستلام التخزيني الافتراضي (`destinationLocationUuid`) عند إنشاء أمر الشراء، مما يضطر النظام لاستخدام الموقع الافتراضي للقطعة كبديل اضطراري عند استلام الشحنة.

---

### القسم 6: أوامر البيع (Sales Orders)
- **الملفات والكيانات المراجعة:**
  - واجهات المستخدم: [`SalesOrderScreen.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/ui/SalesOrderScreen.kt#L401) (`AddSalesOrderBottomSheet`), [`SalesOrderViewModel.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/ui/SalesOrderViewModel.kt#L95)
  - المستودع وقاعدة البيانات: [`SalesOrderRepository.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/repository/SalesOrderRepository.kt), [`SalesOrderEntity.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/SalesOrderEntity.kt), [`SalesOrderDao.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/SalesOrderDao.kt).

#### مطابقة الحقول:
| حقل قاعدة البيانات (`SalesOrderEntity`) | النوع والقيود | حقل واجهة الإضافة (`AddSalesOrderBottomSheet`) | حالة الحقل والملاحظات |
| :--- | :--- | :--- | :--- |
| `uuid` | `TEXT PRIMARY KEY` | مولّد تلقائياً `so-$id` | داخلي. |
| `reference` | `TEXT NOT NULL` | حقل الرمز المرجعي (e.g. SO-2025-101) | متطابق. |
| `customerId / customerUuid` | `INTEGER / TEXT (FK)` | اختيار العميل من الشركات المصنفة كعملاء | متطابق. |
| `customerName` | `TEXT NOT NULL` | يُستخرج تلقائياً من اسم العميل المختار | متطابق. |
| `statusCode` | `INTEGER NOT NULL` | يُسجل كحالة معتمدة (APPROVED / PENDING) | متطابق. |
| `description` | `TEXT NOT NULL` | حقل الوصف النصي | متطابق. |
| `orderCurrency` | `TEXT NOT NULL` | عملة أمر البيع | متطابق. |
| `targetDate` | `TEXT NOT NULL` | تاريخ التسليم للعميل | متطابق. |
| `totalPrice` | `REAL NOT NULL` | محسوب تلقائياً من بنود البيع | متطابق. |
| `sourceType` | `TEXT NOT NULL` | افتراضي "MANUAL" | داخلي. |
| `notes` | `TEXT NOT NULL` | **غير موجود بنموذج الإضافة** | **حقل مفقود بالواجهة**: يدعمه الكيان في السطر 27، ولكن لا يوجد حقل ملاحظات في `AddSalesOrderBottomSheet`. |

#### بنود أمر البيع (`sales_order_lines`):
- يتم ربط الصنف والكمية وسعر الوحدة والكمية المحجوزة (`allocatedQuantity`) والمشحونة (`shippedQuantity`).

---

### القسم 7: أوامر التصنيع (Build / Manufacturing Orders)
- **الملفات والكيانات المراجعة:**
  - واجهات المستخدم: [`BuildOrderScreen.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/ui/BuildOrderScreen.kt#L1824) (`AddBuildOrderBottomSheet`), [`BuildOrderViewModel.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/ui/BuildOrderViewModel.kt)
  - المستودع وقاعدة البيانات: [`BuildOrderRepository.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/repository/BuildOrderRepository.kt), [`BuildOrderEntity.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/BuildOrderEntity.kt), [`BuildOrderDao.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/BuildOrderDao.kt).

#### مطابقة الحقول:
| حقل قاعدة البيانات (`BuildOrderEntity`) | النوع والقيود | حقل واجهة الإضافة (`AddBuildOrderBottomSheet`) | حالة الحقل والملاحظات |
| :--- | :--- | :--- | :--- |
| `uuid` | `TEXT PRIMARY KEY` | مولّد تلقائياً `bo-$id` | داخلي. |
| `reference` | `TEXT NOT NULL` | حقل الرمز المرجعي للأمر | متطابق. |
| `title` | `TEXT NOT NULL` | عنوان أمر التجميع والإنتاج | متطابق. |
| `partId / partName` | `INTEGER (FK) / TEXT` | اختيار الصنف التجميعي (`assembly = true`) | متطابق مع تصفية صارمة للأصناف التجميعية. |
| `quantity` | `REAL NOT NULL` | الكمية المستهدف تصنيعها | متطابق. |
| `completedQuantity` | `REAL NOT NULL` | قيمة مبدئية `0.0` | حقل تشغيلي داخلي يُحدث عند إنجاز المراحل. |
| `statusCode` | `INTEGER NOT NULL` | قيمة مبدئية 10 (PENDING) | متطابق. |
| `batch` | `TEXT NOT NULL` | رقم دفعة الإنتاج الناتجة | متطابق. |
| `startDate` | `TEXT NOT NULL` | تاريخ بدء التشغيل مع منتقي تقويم | متطابق. |
| `targetDate` | `TEXT NOT NULL` | تاريخ الانتهاء المستهدف مع منتقي تقويم | متطابق. |
| `takeFromLocationId` | `INTEGER NULL (FK)` | اختيار موقع سحب المكونات الأولية | متطابق. |
| `destinationLocationId` | `INTEGER NULL (FK)` | اختيار موقع إيداع المنتج النهائي | متطابق. |
| `parentId` | `INTEGER NULL (FK)` | اختيار أمر تصنيع أب (تجميعات متداخلة) | متطابق. |
| `salesOrderId` | `INTEGER NULL (FK)` | ربط اختياري بأمر بيع محدد للعميل | متطابق. |
| `issuedBy` | `TEXT NOT NULL` | اسم/مسؤول إصدار الأمر | متطابق. |
| `responsible` | `TEXT NOT NULL` | الفريق أو الموظف المنفذ | متطابق. |
| `notes` | `TEXT NOT NULL` | الملاحظات الفنية للتشغيل | متطابق. |
| `link` | `TEXT NOT NULL` | رابط المخطط أو الدليل الفني | متطابق. |

#### النتيجة:
- تطابق كامل وشامل (100%) بين حقول قاعدة البيانات ونموذج الإضافة دون أي نقص.

---

### القسم 8: قائمة المواد (BOM - Bill of Materials)
- **الملفات والكيانات المراجعة:**
  - واجهات المستخدم: [`BomScreen.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/ui/BomScreen.kt#L868) (`AddBomItemBottomSheet`), [`BomViewModel.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/ui/BomViewModel.kt)
  - المستودع وقاعدة البيانات: [`BomRepository.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/repository/BomRepository.kt), [`BomItemEntity.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/BomItemEntity.kt), [`BomItemDao.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/BomItemDao.kt).

#### مطابقة الحقول:
| حقل قاعدة البيانات (`BomItemEntity`) | النوع والقيود | حقل واجهة الإضافة (`AddBomItemBottomSheet`) | حالة الحقل والملاحظات |
| :--- | :--- | :--- | :--- |
| `uuid` | `TEXT PRIMARY KEY` | مولّد تلقائياً | داخلي. |
| `partId` | `INTEGER NOT NULL (FK)` | اختيار الصنف التجميعي الأب | متطابق. |
| `subPartId` | `INTEGER NOT NULL (FK)` | اختيار المكوّن الفرعي الداخلي | متطابق. |
| `quantity` | `REAL NOT NULL` | الكمية المطلوبة لتجميع الوحدة الواحدة | متطابق. |
| `reference` | `TEXT NOT NULL` | المرجع الهندسي في اللوحة (e.g. R1, C12, U3) | متطابق. |
| `optional` | `INTEGER NOT NULL` | اختيار: هل المكوّن اختياري؟ | متطابق. |
| `consumable` | `INTEGER NOT NULL` | اختيار: هل المكوّن مستهلك (قصدير، غراء..)؟ | متطابق. |
| `allowVariants` | `INTEGER NOT NULL` | اختيار: السماح باستخدام المتغيرات والبدائل | متطابق. |
| `inherited` | `INTEGER NOT NULL` | خيار التوريث من الصنف القالب | متطابق. |
| `note` | `TEXT NOT NULL` | ملاحظات التجميع | متطابق. |
| `phaseUuid` | `TEXT NULL (FK)` | ربط البند بمرحلة تصنيع (`manufacturing_phases`) | متطابق. |
| `checksum` | `TEXT NOT NULL` | محسوب تلقائياً من هيكل البند ومكوناته | حقل تحقق داخلي لضمان سلامة الـ BOM. |

#### البدائل وقوائم المواد المرتبطة:
- يتم اختيار البدائل (`substitutePartIds`) وحفظها في جدول `bom_item_substitutes`.
- تطابق كامل وتغطية ممتازة لمتطلبات التصنيع والهندسة المتقدمة.

---

### القسم 9: إدارة الموظفين (Staff Management)
- **الملفات والكيانات المراجعة:**
  - واجهات المستخدم: [`StaffManagementScreen.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/ui/StaffManagementScreen.kt#L55), [`StaffManagementViewModel.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/ui/StaffManagementViewModel.kt#L189) (`generateActivationCode`)
  - المستودع وقاعدة البيانات: [`StaffManagementDao.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/StaffManagementDao.kt), [`AppUserEntity.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/AppUserEntity.kt), [`UserPermissionEntity.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/UserPermissionEntity.kt), [`ActivationCodeEntity.kt`](file:///c:/Users/end19/Dev/nextstepai-Inventory/nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/ActivationCodeEntity.kt).

#### مطابقة الحقول ومسار العمل:
في هيكلية التطبيق الآمنة، لا يقوم المالك بإدخال كلمة مرور الموظف أو هاتفه يدوياً مباشرة، بل يتم العمل عبر **نظام دعوات وأكواد تفعيل مشفرة**:

| حقل قاعدة البيانات | الجدول | حقل واجهة إدارة الموظفين (`StaffManagementScreen`) | آلية الربط والحفظ |
| :--- | :--- | :--- | :--- |
| `code` | `activation_codes` | مولّد تلقائياً (6 أرقام فريدة) | يُعرض كرمز QR وبطاقة قابلة للنسخ والطباعة. |
| `label` | `activation_codes` | حقل تسمية/اسم الموظف أو المنصب | متطابق. |
| `assignedRole` | `activation_codes` | اختيار الدور (STOREKEEPER, PRODUCTION, SALES, MANAGER) | متطابق. |
| `permissionsJson` | `activation_codes` | مصفوفة الصلاحيات التفصيلية (قراءة/إضافة/تعديل/حذف) | تُولد كـ JSON محكم وتُخزن في جدول الأكواد. |
| `ownerId` | `activation_codes` / `app_users` | المعرف المرجعي للمالك الحالي | مقيد ببيئة المالك. |
| `userUuid` | `user_permissions` | يُربط عند تفعيل الكود من جهاز الموظف | يتم تفريغ الصلاحيات من `permissionsJson` إلى `user_permissions`. |
| `phoneNumber`, `passwordHash` | `app_users` | غير معروضة في شاشة المالك | **طبيعة وظيفية مقصودة:** يُدخلها الموظف بنفسه في شاشة تفعيل الحساب لأول مرة. |

#### النتيجة:
- توافق كامل ومصمم بشكل آمن لا يتطلب إدخال بيانات سرية في نموذج الإدارة الخاص بالمالك.

---

## 3. ملخص الفجوات والمشكلات المؤكدة عبر الأقسام

1. **الشركات والموردون:**
   - **المشكلة:** فقدان العنوان (`address`) وجهة الاتصال (`contact`) المدخلين في واجهة `AddCompanyBottomSheet` وعدم استمراريتهما في قاعدة البيانات المحلية.
   - **الأثر:** عند إعادة تشغيل التطبيق تظهر الحقول فارغة في بطاقة تفاصيل الشركة.
2. **المواد والأجزاء:**
   - **المشكلة 1:** حقل التنبيه بنقص المخزون (`lowStockAlertsEnabled`) موجود كـ Switch تفاعلي بالواجهة لكن لا يتم تمريره إلى دالة الحفظ ولا تخزينه.
   - **المشكلة 2:** حقل إمكانية التتبع (`trackable`) مُمرر دائماً بالقيمة الثابتة `false` برمجياً في زر التأكيد دون توفير تحكم به في واجهة الإضافة.
3. **أوامر الشراء:**
   - **المشكلة:** حقل الموقع التخزيني الافتراضي لاستلام المشتريات (`destinationLocationUuid`) موجود في قاعدة البيانات ومنطق الاستلام، ولكنه غائب عن واجهة `AddPurchaseOrderBottomSheet`.
4. **أوامر البيع:**
   - **المشكلة:** حقل الملاحظات العامة للأمر (`notes`) موجود في قاعدة البيانات لكنه غير معروض في واجهة `AddSalesOrderBottomSheet`.

---

## 4. التمييز بين المراجعة الكودية والتحقق العملي

- **ما تمت مراجعته بالكود وقراءته تفصيلياً (Static Inspection):**
  - فحص ملفات نماذج الإضافة التسعة (`BottomSheets / Forms`).
  - فحص كائنات ومستودعات البيانات (`DAOs`, `Entities`, `Repositories`, `ViewModels`).
  - تتبع مسار تمرير المعلمات من ضغطة زر الحفظ في واجهة المستخدم حتى استعلام `INSERT` في SQLite.
- **ما تم التحقق منه باختبارات مؤتمتة وتكاملية مسبقة (Automated Tests Verified):**
  - استقرار الـ Schema، وتوليد المعرفات الرقمية وUUIDs المتسلسلة دون تعارض.
  - عمليات تخصيص المخزون، وتوليد أكواد التفعيل، واستدعاءات استلام أوامر الشراء وفك حجز المخزون.
- **ما يحتاج اختبارات وتعديلات لاحقة (Future Execution Scope):**
  - إنشاء Issues برمجية مخصصة لإصلاح فقدان العنوان وجهة الاتصال في الشركات.
  - إضافة الحقول الغائبة في أوامر الشراء وأوامر البيع والأجزاء.

---
> [!NOTE]
> تم إعداد هذا التوثيق التزاماً بالقاعدة الصريحة لـ [Issue #11](https://github.com/moukim99/nextstepai-Inventory/issues/11) بعدم تنفيذ أي تعديلات برمجية على الكود أثناء مرحلة المراجعة.
