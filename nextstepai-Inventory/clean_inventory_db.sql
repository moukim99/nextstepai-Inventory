PRAGMA foreign_keys=OFF;
BEGIN TRANSACTION;

-- ==========================================================
-- 1. جداول النظام المحلية (Local-Only - لا تتزامن)
-- ==========================================================
CREATE TABLE app_settings (
    uuid TEXT PRIMARY KEY NOT NULL DEFAULT 'default-settings',
    notificationTime TEXT NOT NULL DEFAULT '09:00',
    soundEnabled INTEGER NOT NULL DEFAULT 1,
    vibrationEnabled INTEGER NOT NULL DEFAULT 1,
    docExpiryWarningDays INTEGER NOT NULL DEFAULT 30,
    lowStockAlertsEnabled INTEGER NOT NULL DEFAULT 1,
    themeMode TEXT NOT NULL DEFAULT 'SYSTEM',
    language TEXT NOT NULL DEFAULT 'ar',
    defaultCurrency TEXT NOT NULL DEFAULT 'USD',
    scannerBeepEnabled INTEGER NOT NULL DEFAULT 1,
    biometricLockEnabled INTEGER NOT NULL DEFAULT 0,
    syncWifiOnly INTEGER NOT NULL DEFAULT 0
);
INSERT INTO app_settings VALUES('default-settings','09:00',1,1,7,1,'LIGHT','ar','USD',1,0,0);

CREATE TABLE inflow_preferences (
    uuid TEXT PRIMARY KEY NOT NULL DEFAULT 'default-inflow',
    pinnedInflowIds TEXT NOT NULL DEFAULT 'PURCHASE_ORDER,INTERNAL_BUILD',
    customInflowText TEXT NOT NULL DEFAULT ''
);
INSERT INTO inflow_preferences VALUES('default-inflow','PURCHASE_ORDER,CUSTOMER_RETURN','');

CREATE TABLE app_users (
    uuid TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT '',
    active INTEGER NOT NULL DEFAULT 1,
    version INTEGER NOT NULL DEFAULT 1,
    syncStatus TEXT NOT NULL DEFAULT 'SYNCHRONIZED',
    isDeleted INTEGER NOT NULL DEFAULT 0,
    updatedAt INTEGER NOT NULL DEFAULT 0,
    lastModifiedByDeviceUuid TEXT
);
INSERT INTO app_users VALUES('usr-001','مدير الإنتاج والتصنيع','مدير الإنتاج والتصنيع',1,1,'SYNCHRONIZED',0,1700000000000,'dev-local');
INSERT INTO app_users VALUES('usr-002','مشرف خط التجميع','مشرف خط التجميع',1,1,'SYNCHRONIZED',0,1700000000000,'dev-local');
INSERT INTO app_users VALUES('usr-003','مهندس الجودة والسلامة','مهندس الجودة السلامة',1,1,'SYNCHRONIZED',0,1700000000000,'dev-local');
INSERT INTO app_users VALUES('usr-004','مدير المستودع والخدمات اللوجستية','مدير المستودع',1,1,'SYNCHRONIZED',0,1700000000000,'dev-local');
INSERT INTO app_users VALUES('usr-005','مدير النظام (Admin)','مدير النظام',1,1,'SYNCHRONIZED',0,1700000000000,'dev-local');
INSERT INTO app_users VALUES('usr-006','فريق التشغيل والتجميع','فريق التشغيل',1,1,'SYNCHRONIZED',0,1700000000000,'dev-local');

-- ==========================================================
-- 2. التصنيفات والمواد (Parts & Categories)
-- ==========================================================
CREATE TABLE part_categories (
    uuid TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    parentUuid TEXT,
    description TEXT NOT NULL DEFAULT '',
    structural INTEGER NOT NULL DEFAULT 0,
    defaultLocationUuid TEXT,
    version INTEGER NOT NULL DEFAULT 1,
    syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
    isDeleted INTEGER NOT NULL DEFAULT 0,
    updatedAt INTEGER NOT NULL DEFAULT 0,
    lastModifiedByDeviceUuid TEXT
);

INSERT INTO part_categories VALUES('cat-001','الكترونيات وصنع اللوحات',NULL,'المكونات الإلكترونية الدقيقة والشرائح',1,'loc-001',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO part_categories VALUES('cat-002','المكونات السلبية (Passive)','cat-001','المكثفات والمقاومات والملفات',0,'loc-002',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO part_categories VALUES('cat-003','المتحكمات والمعالجات','cat-001','المتحكمات الدقيقة ARM و AVR و ESP',0,'loc-002',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO part_categories VALUES('cat-004','أنظمة وإمدادات الطاقة',NULL,'محولات الجهد والبطاريات والمزودات',0,'loc-001',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO part_categories VALUES('cat-005','الهياكل والأجزاء الميكانيكية',NULL,'علب التغليف والمشتتات والزنبركات',0,'loc-003',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO part_categories VALUES('cat-006','المستشعرات والمقاييس','cat-001','مستشعرات الحرارة والرطوبة والضغط والحركة',0,'loc-002',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO part_categories VALUES('cat-007','الموصلات والكابلات (Connectors)','cat-001','كابلات الشريط والمقابس والمنافذ',0,'loc-002',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO part_categories VALUES('cat-008','الشاشات ووحدات العرض','cat-001','شاشات OLED و LCD ومصفوفات LED',0,'loc-002',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO part_categories VALUES('cat-009','المواد الخام والكيميائية',NULL,'قصدير اللحام ومذيبات IPA والمعجون الحراري',0,'loc-004',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO part_categories VALUES('cat-010','قطع الغيار والصيانة',NULL,'شفرات قطع، محركات بديلة، ورؤوس الكاوية',0,'loc-004',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO part_categories VALUES('cat-011','مواد التغليف والتعبئة',NULL,'كراتين وأكياس مضادة للكهرباء الساكنة',0,'loc-004',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO part_categories VALUES('cat-012','المنتجات التامة والتجميعات',NULL,'المنتجات المكتملة المصنعة الجاهزة للبيع',1,'loc-003',1,'SYNCHRONIZED',0,1738000000000,'dev-local');

CREATE TABLE parts (
    uuid TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    ipn TEXT NOT NULL DEFAULT '',
    description TEXT NOT NULL DEFAULT '',
    categoryUuid TEXT,
    units TEXT NOT NULL DEFAULT 'pcs',
    minimumStock REAL NOT NULL DEFAULT 0.0,
    maximumStock REAL,
    totalInStock REAL NOT NULL DEFAULT 0.0,
    revision TEXT NOT NULL DEFAULT '',
    keywords TEXT NOT NULL DEFAULT '',
    assembly INTEGER NOT NULL DEFAULT 0,
    component INTEGER NOT NULL DEFAULT 1,
    isTemplate INTEGER NOT NULL DEFAULT 0,
    variantOfUuid TEXT,
    trackable INTEGER NOT NULL DEFAULT 0,
    purchaseable INTEGER NOT NULL DEFAULT 1,
    salable INTEGER NOT NULL DEFAULT 0,
    virtual INTEGER NOT NULL DEFAULT 0,
    active INTEGER NOT NULL DEFAULT 1,
    locked INTEGER NOT NULL DEFAULT 0,
    defaultLocationUuid TEXT,
    defaultExpiryDays INTEGER,
    link TEXT NOT NULL DEFAULT '',
    localImagePath TEXT,
    metadata TEXT NOT NULL DEFAULT '{}',
    version INTEGER NOT NULL DEFAULT 1,
    syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
    isDeleted INTEGER NOT NULL DEFAULT 0,
    updatedAt INTEGER NOT NULL DEFAULT 0,
    lastModifiedByDeviceUuid TEXT
);

INSERT INTO parts VALUES('part-1','مقاومة 100K Ohm','RES-10K-001','مقاومة كربونية 1/4 واط بنسبة سماحية 5%','cat-001','pcs',100.0,NULL,250.0,'','resistor resistance 10k electronic',0,1,0,NULL,0,1,0,0,1,0,NULL,NULL,'',NULL,'{"labelImagePath":"files/labels/parts/part_part-1.webp", "labelGeneratedAt":1790462075552, "labelSnapshotData":"مقاومة 100K Ohm|RES-10K-001|1|true|false|false"}',1,'SYNCHRONIZED',0,1790462075553,'dev-local');
INSERT INTO parts VALUES('part-2','متحكم ESP32 Wi-Fi/BT','MCU-ESP32-WROOM','وحدة متحكم دقيق ESP32 مزود بـ Wi-Fi و Bluetooth','cat-002','pcs',10.0,NULL,8.0,'','esp32 micro-controller wifi bluetooth',0,1,0,NULL,0,1,1,0,1,0,NULL,NULL,'',NULL,'{"labelImagePath":"files/labels/parts/part_part-2.webp", "labelGeneratedAt":1790459458505, "labelSnapshotData":"متحكم ESP32 Wi-Fi/BT|MCU-ESP32-WROOM|2|true|false|true"}',1,'SYNCHRONIZED',0,1790459458506,'dev-local');
INSERT INTO parts VALUES('part-3','قالب مستشعر الحرارة والرطوبة','TMP-SENSOR-TMPL','قالب تجريدي لسلسلة مستشعرات الحرارة','cat-008','pcs',0.0,NULL,0.0,'','sensor temperature humidity template',1,0,1,NULL,0,0,0,0,1,0,'loc-001',NULL,'',NULL,'{}',1,'SYNCHRONIZED',0,1790439079772,'dev-local');
INSERT INTO parts VALUES('part-4','مستشعر DHT22 الدقيق','TMP-SENSOR-DHT22','مستشعر حرارة ورطوبة رقمي عالي الدقة (مشتق من القالب)','cat-008','pcs',5.0,NULL,30.0,'','dht22 sensor temperature variant',1,1,0,'part-3',0,1,1,0,0,0,NULL,NULL,'',NULL,'{}',1,'SYNCHRONIZED',0,1790439057437,'dev-local');
INSERT INTO parts VALUES('part-5','محرك تحريك ميكانيكي Servo','GEN-0001','محرك سيرفو صغير للتجميعات','cat-005','unit',20.0,NULL,0.0,'','servo motor mechanical',1,0,1,NULL,0,0,0,0,1,0,'loc-006',NULL,'https://nextstepai.com',NULL,'{}',1,'SYNCHRONIZED',0,1790466768227,'dev-local');
INSERT INTO parts VALUES('part-uuid-1','متحكم STM32F407VGT6 ARM Cortex-M4','MCU-STM32-001','Microcontroller 168MHz 1MB Flash 100-LQFP','cat-003','pcs',50.0,500.0,320.0,'A2','MCU, STM32, ARM, ST',0,1,0,NULL,1,1,0,0,1,0,'loc-002',365,'https://www.st.com',NULL,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO parts VALUES('part-uuid-2','مكثف سيراميك 10uF 25V 0805 SMD','CAP-0805-10U','Multilayer Ceramic Capacitor 10uF 10% 0805','cat-002','pcs',1000.0,20000.0,8500.0,'V1','CAP, Ceramic, SMD, 0805',0,1,0,NULL,0,1,0,0,1,0,'loc-003',730,'',NULL,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO parts VALUES('part-uuid-3','وحدة استشعار الحرارة الصناعية','ASY-SENS-003','وحدة استشعار الحرارة والرطوبة المجمعة للأنظمة الصناعية','cat-006','unit',10.0,150.0,45.0,'A1','Sensor, Temp, Humidity, Assembly',1,0,0,NULL,1,0,1,0,1,0,'loc-004',365,'https://nextstepai.com/sensors',NULL,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO parts VALUES('part-uuid-4','لوحة التحكم الرئيسية Industrial Mainboard v2','ASY-MB-002','لوحة تحكم إلكترونية صناعية متكاملة للتطبيقات المتقدمة','cat-012','unit',5.0,50.0,18.0,'B2','Mainboard, PCB, Controller, Assembly',1,0,0,NULL,1,0,1,0,1,0,'loc-004',365,'https://nextstepai.com/mainboard',NULL,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO parts VALUES('part-uuid-5','مقاومة سطحي 10k Ohm 1/4W 0805 1%','RES-0805-10K','Thick Film Chip Resistor 10k Ohm 1% 0805','cat-002','pcs',2000.0,50000.0,18000.0,'V1','RES, SMD, 0805, 10K',0,1,0,NULL,0,1,0,0,1,0,'loc-003',1095,'',NULL,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO parts VALUES('part-uuid-6','محول طاقة 12V 5A Power Supply Module','PWR-12V-5A','AC-DC Step Down Converter Module 12V 60W','cat-004','pcs',15.0,200.0,60.0,'R3','Power, Supply, 12V, Module',0,1,0,NULL,1,1,1,0,1,0,'loc-001',365,'',NULL,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO parts VALUES('part-uuid-7','متحكم ESP32-WROOM-32D Wi-Fi/BT','MCU-ESP32-32D','Wi-Fi + BT + BLE MCU Module Dual Core 240MHz','cat-003','pcs',40.0,1000.0,210.0,'C1','ESP32, Wi-Fi, Bluetooth, MCU',0,1,0,NULL,1,1,1,0,1,0,'loc-002',365,'https://www.espressif.com',NULL,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO parts VALUES('part-uuid-8','شاشة عرض OLED 0.96 inch I2C 128x64','DSP-OLED-096','Monochrome 0.96 SSD1306 OLED Display Module','cat-008','pcs',20.0,300.0,95.0,'V2','OLED, Display, I2C, SSD1306',0,1,0,NULL,1,1,1,0,1,0,'loc-002',365,'',NULL,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO parts VALUES('part-uuid-9','مرحل حماية Relay Module 5V 10A 2-Channel','RLY-5V-2CH','Optocoupler Isolated Relay Board 5V 10A 250VAC','cat-001','pcs',25.0,400.0,110.0,'A1','Relay, 5V, Module, Switch',0,1,0,NULL,1,1,1,0,1,0,'loc-001',365,'',NULL,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO parts VALUES('part-uuid-10','منظم جهد الخط السلس LDO 3.3V AMS1117','IC-AMS1117-33','800mA Low Dropout Voltage Regulator SOT-223','cat-001','pcs',100.0,5000.0,1200.0,'V1','Regulator, LDO, 3.3V, AMS1117',0,1,0,NULL,0,1,0,0,1,0,'loc-002',730,'',NULL,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO parts VALUES('part-uuid-11','مستشعر حرارة ورطوبة رقمي DHT22/AM2302','SNS-DHT22-DIG','High Precision Digital Temperature & Humidity Sensor','cat-006','pcs',30.0,500.0,140.0,'B1','Sensor, DHT22, Temperature, Humidity',0,1,0,NULL,1,1,1,0,1,0,'loc-002',365,'',NULL,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO parts VALUES('part-uuid-12','موصل طرفي Screw Terminal Block 2-Pin 5mm','CON-TERM-2P','PCB Mount Screw Terminal Block 2 Pin Pitch 5.0mm','cat-007','pcs',500.0,10000.0,3400.0,'V1','Connector, Terminal, Screw, PCB',0,1,0,NULL,0,1,0,0,1,0,'loc-003',1095,'',NULL,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO parts VALUES('part-uuid-13','صندوق هيكل ألومنيوم مقاوم للماء IP67','ENC-ALU-IP67','Diecast Aluminum Enclosure Box 120x80x40mm','cat-005','pcs',10.0,200.0,75.0,'A3','Enclosure, Aluminum, IP67, Box',0,1,0,NULL,1,1,1,0,1,0,'loc-001',1095,'',NULL,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO parts VALUES('part-uuid-14','قصدير لحام Sn63/Pb37 0.8mm 500g Reel','CHM-SLD-500G','Rosin Core Solder Wire 63/37 0.8mm Reel','cat-009','reel',5.0,50.0,22.0,'V1','Solder, Flux, Wire, Sn63',0,1,0,NULL,0,1,0,0,1,0,'loc-004',730,'',NULL,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO parts VALUES('part-uuid-15','جهاز التحكم بالحرارة الذكي Smart Thermostat','PRD-THRM-ST1','منتج نهائي ذكي للتحكم بالحرارة والتكييف عبر الإنترنت','cat-012','unit',5.0,80.0,28.0,'C1','Product, Thermostat, IoT, Final',1,0,0,NULL,1,0,1,0,1,0,'loc-005',365,'https://nextstepai.com/thermostat',NULL,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');

-- ==========================================================
-- 3. قائمة المواد والتصنيع (BOM)
-- ==========================================================
CREATE TABLE bom_items (
    uuid TEXT PRIMARY KEY NOT NULL,
    partUuid TEXT NOT NULL,
    subPartUuid TEXT NOT NULL,
    quantity REAL NOT NULL DEFAULT 1.0,
    reference TEXT NOT NULL DEFAULT '',
    optional INTEGER NOT NULL DEFAULT 0,
    consumable INTEGER NOT NULL DEFAULT 0,
    allowVariants INTEGER NOT NULL DEFAULT 0,
    inherited INTEGER NOT NULL DEFAULT 0,
    note TEXT NOT NULL DEFAULT '',
    checksum TEXT NOT NULL DEFAULT '',
    phaseUuid TEXT,
    version INTEGER NOT NULL DEFAULT 1,
    syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
    isDeleted INTEGER NOT NULL DEFAULT 0,
    updatedAt INTEGER NOT NULL DEFAULT 0,
    lastModifiedByDeviceUuid TEXT
);

INSERT INTO bom_items VALUES('bom-001','part-uuid-3','part-uuid-11',1.0,'U1',0,0,1,0,'تركيب مستشعر الحرارة DHT22','',NULL,1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO bom_items VALUES('bom-002','part-uuid-3','part-uuid-2',2.0,'C1, C2',0,0,0,0,'لحام مكثفات الاستقرار','',NULL,1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO bom_items VALUES('bom-003','part-uuid-3','part-uuid-5',3.0,'R1-R3',0,0,1,0,'مقاومات الرفع والإشارة','',NULL,1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO bom_items VALUES('bom-004','part-uuid-3','part-uuid-12',1.0,'J1',0,0,0,0,'منفذ توصيل الخرج 2-Pin','',NULL,1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO bom_items VALUES('bom-005','part-uuid-4','part-uuid-1',1.0,'U1',0,0,1,0,'المعالج الرئيسي STM32F407','',NULL,1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO bom_items VALUES('bom-006','part-uuid-4','part-uuid-2',6.0,'C1-C6',0,0,1,0,'مكثفات التنعيم للتغذية','',NULL,1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO bom_items VALUES('bom-007','part-uuid-4','part-uuid-5',8.0,'R1-R8',0,0,1,0,'مقاومات الإشارة والتثبيت','',NULL,1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO bom_items VALUES('bom-008','part-uuid-4','part-uuid-10',2.0,'U2, U3',0,0,0,0,'منظمات الجهد AMS1117','',NULL,1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO bom_items VALUES('bom-009','part-uuid-4','part-uuid-12',4.0,'J1-J4',0,0,0,0,'منافذ التوصيل اللولبية','',NULL,1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO bom_items VALUES('bom-010','part-uuid-15','part-uuid-4',1.0,'PCB1',0,0,0,0,'لوحة التحكم الرئيسية Industrial Mainboard','',NULL,1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO bom_items VALUES('bom-011','part-uuid-15','part-uuid-7',1.0,'MOD1',0,0,1,0,'وحدة الواي فاي ESP32','',NULL,1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO bom_items VALUES('bom-012','part-uuid-15','part-uuid-8',1.0,'DISP1',0,0,0,0,'شاشة العرض OLED 0.96','',NULL,1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO bom_items VALUES('bom-013','part-uuid-15','part-uuid-9',1.0,'RLY1',0,0,0,0,'وحدة المرحل 2-Channel','',NULL,1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO bom_items VALUES('bom-014','part-uuid-15','part-uuid-13',1.0,'BOX1',0,0,0,0,'صندوق الهيكل الألومنيوم المقاوم للماء','',NULL,1,'SYNCHRONIZED',0,1738000000000,'dev-local');

CREATE TABLE bom_item_substitutes (
    uuid TEXT PRIMARY KEY NOT NULL,
    bomItemUuid TEXT NOT NULL,
    substitutePartUuid TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
    isDeleted INTEGER NOT NULL DEFAULT 0,
    updatedAt INTEGER NOT NULL DEFAULT 0,
    lastModifiedByDeviceUuid TEXT
);
INSERT INTO bom_item_substitutes VALUES('sub-bom-001','bom-001','part-uuid-11',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO bom_item_substitutes VALUES('sub-bom-002','bom-005','part-uuid-1',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO bom_item_substitutes VALUES('sub-bom-003','bom-011','part-uuid-7',1,'SYNCHRONIZED',0,1738000000000,'dev-local');

-- ==========================================================
-- 4. المواقع والمخزون (Locations & Stock)
-- ==========================================================
CREATE TABLE stock_location_types (
    uuid TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL UNIQUE,
    description TEXT NOT NULL DEFAULT '',
    icon TEXT NOT NULL DEFAULT 'warehouse',
    customIcon TEXT NOT NULL DEFAULT '',
    length REAL NOT NULL DEFAULT 0.0,
    width REAL NOT NULL DEFAULT 0.0,
    height REAL NOT NULL DEFAULT 0.0,
    maxWeight REAL NOT NULL DEFAULT 0.0,
    maxVolume REAL NOT NULL DEFAULT 0.0,
    metadata TEXT NOT NULL DEFAULT '{}',
    version INTEGER NOT NULL DEFAULT 1,
    syncStatus TEXT NOT NULL DEFAULT 'PENDING',
    isDeleted INTEGER NOT NULL DEFAULT 0,
    updatedAt INTEGER NOT NULL DEFAULT 0,
    lastModifiedByDeviceUuid TEXT
);
INSERT INTO stock_location_types VALUES('location-type-4','moukim','','warehouse','',0.0,0.0,0.0,0.0,0.0,'{}',1,'SYNCHRONIZED',0,1789227837167,'dev-local');

CREATE TABLE stock_locations (
    uuid TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    parentUuid TEXT,
    structural INTEGER NOT NULL DEFAULT 0,
    external INTEGER NOT NULL DEFAULT 0,
    locationTypeUuid TEXT,
    locationType TEXT NOT NULL DEFAULT 'SHELF',
    customCapacity REAL,
    isBulkGenerated INTEGER NOT NULL DEFAULT 0,
    address TEXT NOT NULL DEFAULT '',
    icon TEXT NOT NULL DEFAULT 'warehouse',
    customIcon TEXT NOT NULL DEFAULT '',
    level INTEGER NOT NULL DEFAULT 0,
    lft INTEGER NOT NULL DEFAULT 0,
    rght INTEGER NOT NULL DEFAULT 0,
    treeId INTEGER NOT NULL DEFAULT 1,
    metadata TEXT NOT NULL DEFAULT '{}',
    version INTEGER NOT NULL DEFAULT 1,
    syncStatus TEXT NOT NULL DEFAULT 'PENDING',
    isDeleted INTEGER NOT NULL DEFAULT 0,
    updatedAt INTEGER NOT NULL DEFAULT 0,
    lastModifiedByDeviceUuid TEXT
);

INSERT INTO stock_locations VALUES('loc-001','المستودع المركزي - الجزائر العاصمة','المستودع الرئيسي للمواد والقطع',NULL,1,0,NULL,'WAREHOUSE',NULL,0,'','warehouse','',0,1,12,1,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO stock_locations VALUES('loc-002','رف الشرائح والدائريات A-01','مخصص للمتحكمات والشريحات SMD','loc-001',1,0,NULL,'SHELF',NULL,0,'','shelves','',1,2,5,1,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO stock_locations VALUES('loc-003','رف المكونات السلبية B-02','مخصص للمكثفات والمقاومات والملفات','loc-001',1,0,NULL,'SHELF',NULL,0,'','shelves','',1,6,9,1,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO stock_locations VALUES('loc-004','خط الإنتاج والتجميع الرئيسي P-10','موقع تجميع اللوحات وأوامر البناء','loc-001',1,0,NULL,'LINE',NULL,0,'','factory','',1,10,11,1,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO stock_locations VALUES('loc-006','منطقة الفحص والجودة (Quarantine Zone)','منطقة عزل المنتجات قيد الفحص','loc-001',1,0,NULL,'AREA',NULL,0,'','shield','',1,17,18,1,'{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO stock_locations VALUES('loc-005','مخزن وهران للتوزيع الغربي','مستودع فرعي للشحن الإقليمي',NULL,1,0,NULL,'WAREHOUSE',NULL,0,'','warehouse','',0,13,16,6,'{"isPrimary":false}',1,'SYNCHRONIZED',0,1790359275597,'dev-local');

CREATE TABLE stock_items (
    uuid TEXT PRIMARY KEY NOT NULL,
    partUuid TEXT NOT NULL,
    locationUuid TEXT,
    quantity REAL NOT NULL DEFAULT 1.0,
    serial TEXT NOT NULL DEFAULT '',
    batch TEXT NOT NULL DEFAULT '',
    statusCode INTEGER NOT NULL DEFAULT 10,
    packaging TEXT NOT NULL DEFAULT 'Box',
    expiryDate TEXT NOT NULL DEFAULT '',
    notes TEXT NOT NULL DEFAULT '',
    purchasePrice REAL NOT NULL DEFAULT 0.0,
    purchasePriceCurrency TEXT NOT NULL DEFAULT 'USD',
    purchaseOrderUuid TEXT,
    supplierPartUuid TEXT NOT NULL DEFAULT '',
    salesOrderUuid TEXT,
    customerUuid TEXT NOT NULL DEFAULT '',
    buildUuid TEXT,
    isBuilding INTEGER NOT NULL DEFAULT 0,
    parentStockItemUuid TEXT,
    stocktakeDate TEXT NOT NULL DEFAULT '',
    stocktakeUserUuid TEXT,
    reviewNeeded INTEGER NOT NULL DEFAULT 0,
    deleteOnDeplete INTEGER NOT NULL DEFAULT 0,
    link TEXT NOT NULL DEFAULT '',
    metadata TEXT NOT NULL DEFAULT '{}',
    version INTEGER NOT NULL DEFAULT 1,
    syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
    isDeleted INTEGER NOT NULL DEFAULT 0,
    updatedAt INTEGER NOT NULL DEFAULT 0,
    lastModifiedByDeviceUuid TEXT
);

INSERT INTO stock_items VALUES('stock-001','part-uuid-1','loc-002',320.0,'SN-2025-00101','BATCH-2025-A1',10,'Tray','2028-12-31','دفعة شريحات جديدة تم اختبار استقرارها',7.8,'USD','po-001','sup-p-001',NULL,'',NULL,0,NULL,'2025-01-01','usr-001',0,0,'','{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO stock_items VALUES('stock-002','part-uuid-2','loc-003',8500.0,'','BATCH-CAP-99',10,'Reel','2030-12-31','بكرة مكثفات 0805 معتمدة',0.02,'USD',NULL,'',NULL,'',NULL,0,NULL,'2025-01-01','usr-001',0,0,'','{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO stock_items VALUES('stock-003','part-uuid-3','loc-004',45.0,'SN-SENS-0001','PROD-2025-02',10,'Box','2028-06-30','وحدات استشعار حرارة مجمعة جاهزة للشحن',45.0,'USD',NULL,'',NULL,'comp-003','bo-002',0,NULL,'2025-01-12','usr-001',0,0,'','{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO stock_items VALUES('stock-004','part-uuid-4','loc-004',18.0,'SN-MB2-0001','PROD-2025-01',10,'Antistatic Box','2028-06-30','لوحات تحكم جاهزة للشحن والتركيب',95.0,'USD',NULL,'',NULL,'comp-003','bo-001',0,NULL,'2025-01-12','usr-001',0,0,'','{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO stock_items VALUES('stock-005','part-uuid-5','loc-003',18000.0,'','BATCH-RES-2025',10,'Reel','2030-12-31','بكرات مقاومات سطحي 10k',0.005,'USD',NULL,'',NULL,'',NULL,0,NULL,'2025-01-05','usr-001',0,0,'','{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');

-- جدول سجلات الحركات (Append-only Ledger: غير قابل للتعديل أو الحذف، لا يحتاج version أو isDeleted أو updatedAt)
CREATE TABLE stock_item_tracking (
    uuid TEXT PRIMARY KEY NOT NULL,
    stockItemUuid TEXT NOT NULL,
    trackingTypeCode INTEGER NOT NULL DEFAULT 10,
    label TEXT NOT NULL DEFAULT '',
    notes TEXT NOT NULL DEFAULT '',
    deltas TEXT NOT NULL DEFAULT '{}',
    userUuid TEXT,
    createdAt INTEGER NOT NULL,
    syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
    lastModifiedByDeviceUuid TEXT
);
INSERT INTO stock_item_tracking VALUES('track-001','stock-001',10,'إضافة مخزون جديد','تم استلام 320 قطعة من المورد شركة النور','{"quantity": 320.0}','usr-001',1736496000000,'SYNCHRONIZED','dev-local');
INSERT INTO stock_item_tracking VALUES('track-002','stock-004',20,'إنتاج وتجميع مخرجات','تجميع 18 لوحة تحكم رئيسية بنجاح','{"quantity": 18.0}','usr-001',1736677800000,'SYNCHRONIZED','dev-local');
INSERT INTO stock_item_tracking VALUES('track-003','stock-009',30,'نقل مخزون إقليمي','تم نقل 28 جهاز إلى مستودع وهران للتوزيع','{"quantity": 28.0}','usr-001',1737388800000,'SYNCHRONIZED','dev-local');

-- ==========================================================
-- 5. الشركات والعناوين والموردين (Companies & Partners)
-- ==========================================================
CREATE TABLE companies (
    uuid TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    phone TEXT NOT NULL DEFAULT '',
    email TEXT NOT NULL DEFAULT '',
    isSupplier INTEGER NOT NULL DEFAULT 1,
    isManufacturer INTEGER NOT NULL DEFAULT 0,
    isCustomer INTEGER NOT NULL DEFAULT 0,
    currency TEXT NOT NULL DEFAULT 'USD',
    logoPath TEXT,
    parentUuid TEXT,
    website TEXT NOT NULL DEFAULT '',
    active INTEGER NOT NULL DEFAULT 1,
    notes TEXT NOT NULL DEFAULT '',
    metadata TEXT NOT NULL DEFAULT '{}',
    version INTEGER NOT NULL DEFAULT 1,
    syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
    isDeleted INTEGER NOT NULL DEFAULT 0,
    updatedAt INTEGER NOT NULL DEFAULT 0,
    lastModifiedByDeviceUuid TEXT
);

INSERT INTO companies VALUES('comp-001','شركة النور للحلول الإلكترونية','مورد رئيسي للشرائح والمكونات الإلكترونية','+213 21 55 44 33','info@alnoor-tech.dz',1,0,0,'USD',NULL,NULL,'https://alnoor-tech.dz',1,'مورد موثوق بضمان سنتين','{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO companies VALUES('comp-002','STMicroelectronics NV','مصنع عالمي للشرائح الميكروية والمتحكمات','+33 1 58 07 20 00','contact@st.com',0,1,0,'EUR',NULL,NULL,'https://www.st.com',1,'المصنع الأصلي لشريحة STM32','{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');
INSERT INTO companies VALUES('comp-003','مؤسسة الأمل للصناعات الأوتوماتيكية','عميل واستشاري أنظمة أتمتة صناعية','+213 31 88 99 00','orders@alamal-auto.dz',0,0,1,'DZD',NULL,NULL,'https://alamal-auto.dz',1,'عميل فئة A للوحات التحكم','{}',1,'SYNCHRONIZED',0,1738000000000,'dev-local');

-- ==========================================================
-- 6. أوامر الشراء، التصنيع، والمبيعات (Operations & Orders)
-- ==========================================================
CREATE TABLE purchase_orders (
    uuid TEXT PRIMARY KEY NOT NULL,
    reference TEXT NOT NULL,
    supplierUuid TEXT NOT NULL,
    supplierName TEXT NOT NULL DEFAULT '',
    statusCode INTEGER NOT NULL DEFAULT 10,
    description TEXT NOT NULL DEFAULT '',
    orderCurrency TEXT NOT NULL DEFAULT 'USD',
    targetDate TEXT NOT NULL DEFAULT '',
    totalCost REAL NOT NULL DEFAULT 0.0,
    sourceType TEXT NOT NULL DEFAULT 'MANUAL',
    sourceReferenceUuid TEXT,
    destinationLocationUuid TEXT,
    version INTEGER NOT NULL DEFAULT 1,
    syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
    isDeleted INTEGER NOT NULL DEFAULT 0,
    updatedAt INTEGER NOT NULL DEFAULT 0,
    lastModifiedByDeviceUuid TEXT
);
INSERT INTO purchase_orders VALUES('po-001','PO-2025-001','comp-001','شركة النور للحلول الإلكترونية',30,'طلب توريد ميكروكنترولرات ومكونات إلكترونية','USD','2025-01-15',2496.0,'MANUAL',NULL,NULL,1,'SYNCHRONIZED',0,1738000000000,'dev-local');

CREATE TABLE build_orders (
    uuid TEXT PRIMARY KEY NOT NULL,
    reference TEXT NOT NULL,
    title TEXT NOT NULL DEFAULT '',
    partUuid TEXT NOT NULL,
    partName TEXT NOT NULL DEFAULT '',
    quantity REAL NOT NULL DEFAULT 1.0,
    completedQuantity REAL NOT NULL DEFAULT 0.0,
    statusCode INTEGER NOT NULL DEFAULT 10,
    batch TEXT NOT NULL DEFAULT '',
    targetDate TEXT NOT NULL DEFAULT '',
    startDate TEXT NOT NULL DEFAULT '',
    completionDate TEXT NOT NULL DEFAULT '',
    creationDate TEXT NOT NULL DEFAULT '',
    parentBuildUuid TEXT,
    salesOrderUuid TEXT,
    takeFromLocationUuid TEXT,
    destinationLocationUuid TEXT,
    issuedBy TEXT NOT NULL DEFAULT '',
    responsible TEXT NOT NULL DEFAULT '',
    notes TEXT NOT NULL DEFAULT '',
    link TEXT NOT NULL DEFAULT '',
    version INTEGER NOT NULL DEFAULT 1,
    syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
    isDeleted INTEGER NOT NULL DEFAULT 0,
    updatedAt INTEGER NOT NULL DEFAULT 0,
    lastModifiedByDeviceUuid TEXT
);
INSERT INTO build_orders VALUES('bo-001','BO-2025-01','تصنيع 20 لوحة تحكم رئيسية Mainboard v2','part-uuid-4','لوحة التحكم الرئيسية Industrial Mainboard v2',20.0,18.0,30,'PROD-2025-01','2025-01-20','2025-01-10','2025-01-18','2025-01-08',NULL,NULL,'loc-002','loc-004','مهندس الإنتاج أحمد','فريق التجميع قسم B','تم اكتمال أمر البناء واجتياز جميع الفحوصات بنجاح','https://nextstepai.com/builds/bo-001',1,'SYNCHRONIZED',0,1738000000000,'dev-local');

-- سجل الإشعارات (Append-only Log / الإشعارات مقروءة محلياً فقط)
CREATE TABLE notifications_history (
    uuid TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    notificationType TEXT NOT NULL DEFAULT 'COMPANY_DOC_EXPIRY',
    targetEntityUuid TEXT NOT NULL,
    companyUuid TEXT,
    deepLink TEXT,
    scheduledDate INTEGER NOT NULL,
    isRead INTEGER NOT NULL DEFAULT 0,
    isTriggered INTEGER NOT NULL DEFAULT 0,
    createdAt INTEGER NOT NULL DEFAULT 0,
    syncStatus TEXT NOT NULL DEFAULT 'PENDING',
    lastModifiedByDeviceUuid TEXT
);
INSERT INTO notifications_history VALUES('notif-001','تنبيه قرب انتهاء وثيقة تجارية','السجل التجاري لشركة النور للحلول ينتهي في 2026-12-31','COMPANY_DOC_EXPIRY','comp-att-001','comp-001','company/comp-001',1735689600000,0,1,1735689600000,'SYNCHRONIZED','dev-local');

-- ==========================================================
-- 7. الفهارس (Optimized Production Indexes)
-- ==========================================================
CREATE INDEX idx_parts_categoryUuid ON parts(categoryUuid);
CREATE INDEX idx_parts_defaultLocationUuid ON parts(defaultLocationUuid);
CREATE INDEX idx_parts_sync ON parts(syncStatus, isDeleted, updatedAt);

CREATE INDEX idx_bom_items_partUuid ON bom_items(partUuid);
CREATE INDEX idx_bom_items_subPartUuid ON bom_items(subPartUuid);
CREATE INDEX idx_bom_items_sync ON bom_items(syncStatus, isDeleted, updatedAt);

CREATE INDEX idx_stock_items_partUuid ON stock_items(partUuid);
CREATE INDEX idx_stock_items_locationUuid ON stock_items(locationUuid);
CREATE INDEX idx_stock_items_sync ON stock_items(syncStatus, isDeleted, updatedAt);
CREATE INDEX idx_stock_locations_parentUuid ON stock_locations(parentUuid);

CREATE INDEX idx_stock_tracking_itemUuid ON stock_item_tracking(stockItemUuid);
CREATE INDEX idx_stock_tracking_sync ON stock_item_tracking(syncStatus, createdAt);

CREATE INDEX idx_build_orders_partUuid ON build_orders(partUuid);
CREATE INDEX idx_build_orders_sync ON build_orders(syncStatus, isDeleted, updatedAt);

CREATE INDEX idx_companies_sync ON companies(syncStatus, isDeleted, updatedAt);

CREATE INDEX idx_purchase_orders_supplierUuid ON purchase_orders(supplierUuid);

CREATE INDEX idx_notifications_targetEntity ON notifications_history(targetEntityUuid);
CREATE INDEX idx_notifications_companyUuid ON notifications_history(companyUuid);
CREATE INDEX idx_notifications_triggered_schedule ON notifications_history(isTriggered, scheduledDate);
CREATE INDEX idx_notifications_unread ON notifications_history(isRead);

COMMIT;
