# API bootstrap (BE-01)

Status: local implementation branch, pending review. Requires Java 21 for the supported runtime; integration tests use Docker or a disposable PostgreSQL database configured as described below.

Flyway V1 creates the `core` schema. V2 adds core/IAM tables and V3 adds catalog tables, following the Project Owner's DB-01 direction. They represent **14 Sprint 1 tables** of the [39-table target draft](../../docs/architecture/database/DB-01_PHYSICAL_SCHEMA_DRAFT.md). Migration of inventory, sales, sync, audit and training tables waits for their owning sprint. This module currently serves only `GET /actuator/health`; authentication and business endpoints belong to later backlog items.

From repository root, start PostgreSQL as described in [infra](../../infra/README.md), then set `DB_PASSWORD` to the same local value. To load the **demo-only** organization, store, four roles, category, units, products, supplier and prices, run:

```powershell
cd services/api
$env:SPRING_PROFILES_ACTIVE='demo'
.\mvnw.cmd spring-boot:run
```

On Linux/macOS use `SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run`. Without the demo profile, migrations create only empty tables. No demo login account or credential is seeded. The API listens on port 8080 by default. Check `http://localhost:8080/actuator/health` for `{ "status": "UP" }`.

Run `./mvnw verify` (`.\mvnw.cmd verify` on Windows) to compile and test migrations, demo data, health, four-role restriction and price overlap against a fresh PostgreSQL 17 Testcontainer. The root `scripts/verify.ps1` includes this gate and the web baseline gate.

If Docker is unavailable, the test can use an existing **empty, disposable** PostgreSQL database instead: set `SIMTIM_TEST_DB_URL`, `SIMTIM_TEST_DB_USER` and `SIMTIM_TEST_DB_PASSWORD` before running `verify`. The test applies the migration and assumes it may create schema objects in that database. Never point these variables at a shared or production database.
