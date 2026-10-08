# Persistence Single-Source-of-Truth Refactor

This document is the implementation contract for `fix/persistence-single-source-of-truth`.
It intentionally does not mark the application migration complete: every step must be backed by code and a passing regression test before the legacy table is removed.

## Goals

1. SQLite is the sole authority for persisted business data.
2. A newly constructed repository/container returns the same state as the database, without requiring a warm in-memory cache.
3. Reads never merge stale cache entries with current database results.
4. Writes are atomic across the canonical record and its dependent records.
5. IDs remain stable across restarts and are not allocated by an empty in-memory table.
6. A failed database open or migration must preserve the original database.
7. Keep public repository/ViewModel contracts stable while migrating each domain.

## Required work and acceptance criteria

### P0 — eliminate stale-cache reads

- Replace `DAO → fill missing entries → Table → Repository` read paths with DAO-backed reads.
- Never merge database rows with cached rows. If a cache remains temporarily for UI performance, it is a disposable projection and must be replaced from a complete authoritative query (including deletion/tombstone state).
- Add a regression test where a row is updated/deleted directly through SQLite after a repository read; a new repository must return the current database state and must not return the deleted/stale row.

### P0 — remove sample data from persistence constructors

- Remove `seedSampleData()` / `seedSampleCompanies()` from legacy table constructors.
- Move demo content to an explicit development/demo seeder that is never invoked implicitly in production or in a fresh repository.
- Test that a fresh database/repository has only schema defaults and explicitly seeded data.

### P0 — canonical writes and atomicity

- For each domain, write to the DAO first and return a domain model derived from the persisted entity.
- Do not silently swallow DAO write failures after updating memory.
- Use SQLite transactions for multi-row operations (for example, part plus attachments/parameters; company plus related records).
- Keep soft-delete tombstones in the canonical database and exclude them from normal reads.

### P1 — identity safety

- Stop deriving identity from a newly constructed table's `nextId` counter.
- Keep a stable UUID as canonical identity; where existing public APIs require numeric IDs, allocate/map those IDs from persisted state and document the migration path.
- Add tests for insert-after-restart, sparse IDs, soft-deleted IDs, UUID-based IDs, and concurrent allocation.
- Never overwrite a UUIDv7 identity with a generated `part-0` or a new UUID on update.

### P1 — repository coverage

- Migrate `PartRepository`, `CompanyRepository`, and their dependent child entities first.
- Then migrate stock, BOM, purchase/build orders, pricing, notes, attachments, categories, parameters, and other `*Table` consumers.
- Do not delete a legacy table until every call site is migrated and a repository search confirms no production dependency remains.

### P1 — persistence test matrix

- DAO CRUD and logical deletion.
- Cold restart using a new connection, container, repositories, and ViewModels.
- Stale-cache invalidation after external database update/delete.
- Multi-entity rollback on a failed dependent write.
- Duplicate insert/idempotency behavior.
- Schema migration from the previous committed schema without destructive fallback.
- Existing end-to-end user journey test.

### P2 — architecture and operations

- Establish one application-scoped database lifecycle owner.
- Keep schema creation/migrations separate from connection lifecycle.
- Add useful migration errors and never delete a database automatically after an open failure.
- Add CI coverage for the persistence suite and publish test reports.
- Verify SQLite queries and schema remain portable to Cloudflare D1/Workers; keep network sync as a separate boundary.

## Merge gate

Do not merge to `master` until:
- the persistence regression suite passes in CI;
- all affected repositories read and write through the canonical DAO path;
- the stale-cache, ID-allocation, transaction rollback, and cold-restart tests pass;
- no destructive database fallback or implicit production seeding remains.

## Current status

This PR begins the refactor with a persisted-ID allocation helper and this acceptance contract. It is a **draft** until the remaining repository migrations and tests are implemented. The helper must be wired into write paths and tested before it can be considered production-ready.
