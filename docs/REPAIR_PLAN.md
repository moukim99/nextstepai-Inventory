# Repository Repair Plan

> Architectural and engineering audit based on the current repository state. This document is a planning artifact; it does not itself implement the listed fixes.

## Priority Legend

- **P0 — Critical:** data loss, security/authentication, or core reliability risk.
- **P1 — High:** major correctness, synchronization, testing, or delivery risk.
- **P2 — Medium:** architecture, maintainability, and performance improvements.
- **P3 — Low:** repository hygiene and documentation.

| ID | Priority | Area | Problem | Recommended Fix | Status |
|---|---|---|---|---|---|
| R-001 | P0 | Database | Room uses `fallbackToDestructiveMigration(dropAllTables = true)`, allowing schema changes to destroy local data. | Remove destructive fallback and introduce explicit Room migrations. | Planned |
| R-002 | P0 | Database | `SqliteDatabaseManager` can delete/recreate database files when database creation/migration fails. | Fail safely, preserve the existing DB, report the migration error, and recover only through explicit migration/recovery logic. | Planned |
| R-003 | P0 | Data architecture | The project has multiple data sources: Room/DAO, custom SQLite management, and legacy `*Table` classes. | Define one source of truth and migrate/remove the legacy data layer. | Planned |
| R-004 | P0 | Synchronization | `BatchSyncService` currently uses a mocked cloud endpoint and returns no real remote changes. | Implement the real sync API and end-to-end synchronization flow. | Planned |
| R-005 | P1 | Database | No committed Room migration files were found despite database versioning. | Add versioned migration classes and migration tests for every schema change. | Planned |
| R-006 | P1 | Authentication | Login flow appears to mark the user logged in after the repository call without sufficiently enforcing authentication success. | Implement explicit authentication states and reject failed/invalid credentials. | Planned |
| R-007 | P1 | Synchronization | Sync status definitions/handling are not consistently unified. | Define one sync-state model and use it across repositories, ViewModels, and sync services. | Planned |
| R-008 | P1 | Token storage | Android secure-token storage creates the DataStore repeatedly instead of using a shared lifecycle-managed instance. | Provide one application-scoped DataStore instance and inject it into the storage layer. | Planned |
| R-009 | P1 | Authentication | Missing sync/auth tokens can fall back to dummy tokens. | Remove fake-token fallbacks and fail explicitly when credentials are unavailable or expired. | Planned |
| R-010 | P1 | Synchronization | Retry, backoff, idempotency, conflict handling, and offline recovery need stronger guarantees. | Add bounded exponential backoff, idempotency keys, deterministic conflict resolution, and offline retry handling. | Planned |
| R-011 | P1 | Testing | Sync edge cases such as conflicts, duplicates, deletes, and offline recovery need dedicated coverage. | Add integration/unit tests for sync correctness and failure recovery. | Planned |
| R-012 | P1 | CI/CD | No GitHub Actions workflow was found. | Add CI for build, unit tests, lint/static checks, and relevant verification tasks. | Planned |
| R-013 | P2 | UI architecture | Large screens such as `StockScreen.kt`, `PartManagementScreen.kt`, and `CompanyScreen.kt` are excessively large. | Split screens into feature-focused composables, state holders, and reusable UI components. | Planned |
| R-014 | P2 | Database architecture | `SqliteDatabaseManager` contains too many responsibilities. | Separate connection management, schema/migration logic, query execution, and recovery concerns. | Planned |
| R-015 | P2 | Repository layer | `PartRepository` and `StockRepository` are large and contain mixed responsibilities. | Split repositories by domain responsibility/use case and isolate persistence details. | Planned |
| R-016 | P2 | Legacy layer | Legacy `*Table` classes duplicate persistence responsibilities and include in-memory/sample data. | Remove them after migrating their required behavior to the canonical data source. | Planned |
| R-017 | P2 | Coroutines | Repository code uses `runBlocking`, which can block callers and complicate structured concurrency. | Replace blocking calls with suspend APIs/Flows and let coroutine scopes own lifecycle. | Planned |
| R-018 | P2 | ViewModels | `App.kt` creates many ViewModels directly, increasing lifecycle/composition coupling. | Use lifecycle-aware ViewModel creation and dependency injection/factories where appropriate. | Planned |
| R-019 | P2 | Database lifecycle | Global/singleton database connection handling needs clearer ownership and shutdown/reuse semantics. | Establish application-scoped DB lifecycle and deterministic connection/resource management. | Planned |
| R-020 | P3 | Git hygiene | IDE files such as `.idea` content appear to be tracked despite ignore rules. | Remove unwanted IDE files from version control and keep only intentional project metadata. | Planned |
| R-021 | P3 | Build configuration | `settings.gradle.kts` uses fragile environment manipulation/reflection. | Replace reflection-based environment changes with supported Gradle configuration mechanisms. | Planned |
| R-022 | P3 | Documentation | Project architecture, data flow, sync behavior, and development workflow need clearer documentation. | Add concise architecture and development documentation after the core architecture is stabilized. | Planned |

## Recommended Execution Order

1. **P0 — Data safety:** R-001, R-002, R-003.
2. **P0 — Synchronization:** R-004.
3. **P1 — Database/auth/sync reliability:** R-005 through R-011.
4. **P1 — CI:** R-012.
5. **P2 — Architecture cleanup:** R-013 through R-019.
6. **P3 — Repository hygiene/documentation:** R-020 through R-022.

## Important Note

This plan is based on repository inspection and identifies risks and recommended work. Each item should be validated against the intended product requirements before implementation, especially database migration behavior, authentication semantics, and sync conflict rules.
