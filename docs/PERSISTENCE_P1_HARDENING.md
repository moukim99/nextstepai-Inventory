# Persistence P1 Hardening — Implementation Notes

This branch builds on `fix/persistence-single-source-of-truth`. It is intended to be reviewed as a stacked PR and must not be merged until the CI checks and data-migration behavior are reviewed.

## Repair map

### 1. Prevent stale in-memory data from reappearing after SQLite returns no rows

**Files**
- `nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/repository/PartRepository.kt`
- `nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/PartEntitiesDaos.kt`

**Change**
Repository reads for parameter templates, part parameters, category-template links, test templates, attachments, notes, pricing, internal prices, sale prices, starred part IDs, and related-part links now treat the DAO result as authoritative. Empty DAO results are returned as empty/null rather than falling back to legacy table caches. Pricing recalculation reads internal price rows from SQLite, not `PartInternalPriceTable`. For custom UUID parts, repository read models restore the already-resolved numeric domain ID rather than parsing it from UUID text.

**Why**
Previously, deleting the last row in SQLite could make an old, cached row visible again. This violated the single-source-of-truth rule and could make the UI show deleted data.

### 2. Preserve numeric domain IDs for non-numeric UUIDs across cold restart

**Files**
- `nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/PartEntity.kt`
- `nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/PartDao.kt`
- `nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/CompanyEntity.kt`
- `nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/CompanyDao.kt`
- `nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/SqliteDatabaseSchema.kt`
- `nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/repository/PartRepository.kt`
- `nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/repository/CompanyRepository.kt`
- `nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/PartEntitiesDaos.kt`

**Change**
The `parts.id` and `companies.id` compatibility columns are now included in entity mapping and explicit DAO reads/writes. Existing databases are checked for the column before backfill. Existing prefixed UUIDs recover their numeric suffix; rows with custom UUIDs and no numeric ID receive a stable ID once. Company child lookups resolve the company's persisted UUID from its numeric ID, so custom company UUIDs remain usable in child relations.

**Why**
Parsing a UUIDv7/custom UUID as a numeric suffix produced ID `0` after reload. Multiple unrelated domain objects could then collide in numeric-ID-based APIs.

### 3. Make part soft-delete cascades atomic in SQLite

**File**
- `nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/repository/PartRepository.kt`

**Change**
`deletePartByUuid` starts an `IMMEDIATE` write transaction, soft-deletes the part and its SQLite dependents within that transaction, commits only when every dependent update succeeds, and rolls back/reports the failure otherwise. Cache cleanup and deleting the label image remain post-commit best-effort work because filesystem/in-memory operations cannot participate in the SQLite transaction.

**Why**
The former implementation swallowed a dependent-row exception and reported success even when only part of the cascade had completed.

### 4. Serialize numeric ID allocation across independent SQLite connections

**File**
- `nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/SqliteNumericIdAllocator.kt`

**Change**
The sequence-row creation, increment, and read now occur between `BEGIN IMMEDIATE` and `COMMIT`; an exception triggers `ROLLBACK`. The in-process lock remains as an additional guard.

**Why**
A JVM-level lock does not serialize independent processes. SQLite's write transaction must protect the whole read/modify/read sequence, not just the individual update statement.

### 5. Fail closed if an existing database prevents company-name uniqueness

**File**
- `nextstepai-Inventory/shared/src/commonMain/kotlin/com/nextstepai/inventory/data/db/SqliteDatabaseSchema.kt`

**Change**
Before creating `idx_companies_unique_name`, schema initialization searches for duplicate active names after `LOWER(TRIM(name))`. If duplicates exist, initialization fails with the conflicting normalized names rather than silently ignoring index creation. After creation, the code verifies the index exists.

**Migration behavior**
No legacy company rows are silently deleted or merged. If this check blocks an existing database, resolve the listed duplicate active names deliberately, then rerun initialization.

## Added verification coverage

File: `nextstepai-Inventory/shared/src/commonTest/kotlin/com/nextstepai/inventory/SingleSourceOfTruthVerificationTest.kt`

New or expanded cases:
- `testFinding4NonNumericUuidv7Preservation`: asserts numeric ID persistence through close/reopen and lookup by either numeric ID or UUID.
- `testStaleAttachmentCacheCannotResurrectDeletedRows`: deletes a row from SQLite while the legacy cache retains it, then asserts the repository returns empty.
- `testDeletedParameterRowsCannotReturnFromLegacyMemoryCache`: verifies a deleted parameter does not return from the legacy cache.
- `testCategoryTemplateReadsDoNotFallBackToSeededMemoryRows`: verifies empty SQLite template/link rows do not return seeded in-memory results.
- `testPricingRecalculationUsesCurrentSQLitePricesNotStaleMemory`: confirms pricing recalculation sees the current SQLite prices rather than a stale price cache.
- `testCustomCompanyUuidKeepsNumericIdentityAndChildRelationsAfterRestart`: verifies custom company UUID, numeric-ID lookup, and child relation after restart.
- `testPartCascadeDeleteRollsBackWhenDependentDeleteFails`: injects a missing dependent table to verify failure and rollback.
- `testCompanyUniqueIndexMigrationFailsClosedOnLegacyDuplicates`: verifies duplicate active names block index migration explicitly.
- Existing allocator concurrency and CRUD/tombstone checks remain in the suite.

## Validation and merge gate

- The repository workflow runs `./gradlew :shared:jvmTest --no-daemon --stacktrace`; it does not currently run Android assembly or emulator tests.
- Verify the GitHub Actions result for the **latest head commit** after all changes have landed. A successful older run is not sufficient.
- Review migration behavior against a copy of any real database before rollout, particularly if the duplicate-name check reports legacy conflicts.
- Keep this PR open and do not merge it until CI passes and the changes have been reviewed.
