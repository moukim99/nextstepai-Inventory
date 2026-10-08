# NextStepAI Inventory — Architecture & Engineering Documentation

## 1. System Overview

NextStepAI Inventory is an offline-first inventory, manufacturing, and supply chain management system built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**.

### Core Tenets
- **Data Persistence by Default**: All business entities (Parts, Stock Items, Purchase Orders, Sales Orders, Build Orders, Companies, Locations, Allocations) are permanently stored in local SQLite databases.
- **Offline-First & Cloud Synchronization**: Local modifications are tagged with revision metadata and synchronization states (`PENDING`, `SYNCED`, `CONFLICT`), enabling synchronization with Cloudflare Workers / remote backends.
- **Zero Blocking Coroutines**: Elimination of `runBlocking` ensures smooth UI rendering and non-blocking IO dispatching.
- **Dependency Injection**: Centralized lifecycle-aware dependency management via `AppContainer` singletons and Compose `CompositionLocal`.

---

## 2. Layered Architecture

```
┌────────────────────────────────────────────────────────┐
│                   Compose Multiplatform UI             │
│        (Screens, Dialogs, BottomSheets, Components)    │
└───────────────────────────▲────────────────────────────┘
                            │ StateFlow / UI Events
┌───────────────────────────┴────────────────────────────┐
│                    ViewModel Layer                     │
│    (PartViewModel, StockViewModel, CompanyViewModel...) │
└───────────────────────────▲────────────────────────────┘
                            │ Coroutine Dispatchers.IO
┌───────────────────────────┴────────────────────────────┐
│                   Repository Layer                     │
│  (PartRepository, StockRepository, Domain Facades)     │
└─────────────▲──────────────────────────────▲───────────┘
              │                              │
┌─────────────┴─────────────┐  ┌─────────────┴───────────┐
│     SQLite DAO Layer      │  │    Batch Sync Engine    │
│  (PartDao, StockItemDao)  │  │   (BatchSyncService)    │
└─────────────▲─────────────┘  └─────────────────────────┘
              │
┌─────────────┴──────────────────────────────────────────┐
│              SqliteDatabaseManager                     │
│   Schema (28 Tables) | Migrations | Thread-Safe JDBC   │
└────────────────────────────────────────────────────────┘
```

---

## 3. Persistence Layer (SQLite)

The persistence engine was modularized from monolithic structures into dedicated components:

1. **`SqliteDatabaseManager`**:
   - Connection lifecycle manager providing thread-safe SQLite connections.
   - Delegates database table creation, schema migrations, and initial seeding.
2. **`SqliteDatabaseSchema`**:
   - Manages DDL definitions for all 28 relational tables (including UUID primary keys, foreign keys, timestamps, and indexing).
3. **`SqliteDatabaseMigrations`**:
   - Idempotent migration runner applying schema alterations (e.g., column migrations for barcode, packaging, currency).
4. **`SqliteDatabaseSeeder`**:
   - Populates initial reference categories, system users, default locations, and core configuration records.
5. **`SqliteConnectionDrivers`**:
   - Encapsulates driver abstraction (`ThreadSafeSQLiteConnection`, `JdbcSqliteConnection`) ensuring thread safety across multi-threaded coroutine dispatchers.

### DAO Pattern
DAOs execute native prepared statements (`conn.prepare(...).use { ... }`) synchronously without blocking coroutine dispatchers, allowing callers in both synchronous test fixtures and coroutine scopes (`Dispatchers.IO`) to operate cleanly.

### Deprecation of In-Memory `*Table` Classes
Legacy in-memory `*Table` classes (e.g., `PartTable`, `StockItemTable`) have been annotated with `@Deprecated` and superseded by the canonical SQLite DAOs.

---

## 4. Repository & Domain Layer

Repositories encapsulate persistence details and business validation rules:

- **`PartRepository`**: Manages components, technical parameters, pricing models, BOM linkages, and search indexing. Delegates category management to `PartCategoryRepository`.
- **`StockRepository`**: Manages physical inventory items, serial/batch tracking, stocktakes, transfers, and splits. Delegates warehouse topology to `StockLocationRepository`.
- **`PartCategoryRepository`**: Dedicated domain repository for category trees and parameter templates.
- **`StockLocationRepository`**: Dedicated domain repository for warehouse hierarchy, duplicate detection, and structural location nodes.
- **`PartAllocationRepository`**: Live reservation ledger tracking `HARD` (committed) and `SOFT` (reserved) quantities to prevent over-allocation.
- **`CompanyRepository`**: Manages suppliers, manufacturers, contacts, bank accounts, and price break matrix.
- **`PurchaseOrderRepository`**: Manages inbound supply orders, statuses, lines, and receipt workflows.
- **`SalesOrderRepository`**: Manages outbound customer orders, allocations, and dispatching.
- **`BuildOrderRepository`**: Manages multi-stage manufacturing workflows, phase tracking, and component consumption.

---

## 5. Dependency Injection (`AppContainer`)

The application employs container-based dependency injection via `AppContainer`:

```kotlin
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("No AppContainer provided in CompositionLocal")
}
```

- Instantiated at the root Compose level via `remember { AppContainer() }`.
- Shared repository singletons eliminate cache discrepancies and prevent redundant SQLite connections.
- Provides dedicated factory methods (`createPartViewModel()`, `createStockViewModel()`, etc.) for testability and lifecycle isolation.

---

## 6. Coroutine & Concurrency Model

All blocking `runBlocking` invocations have been eliminated:
- DAOs execute direct synchronous JDBC statements.
- ViewModels invoke Repository methods from within `viewModelScope.launch(Dispatchers.IO) { ... }`.
- Synchronous UI queries execute instantaneously against SQLite local caches without blocking the Compose main thread.

---

## 7. Testing & Build Instructions

### Running Tests
Execute the JVM test suite across shared multiplatform modules:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :shared:jvmTest --console=plain
```

### Git Branching & Local Development
All refactoring work is maintained on local feature branches (e.g., `fix/persistent-inventory-data`) before remote publication, adhering to Conventional Commits standards.
