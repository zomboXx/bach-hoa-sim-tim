# API bootstrap (BE-01)

Status: local implementation branch, pending review. Requires Java 21 for supported runtime and a running Docker daemon for integration tests.

The first Flyway migration creates only the `core` schema. DB-01 must approve physical business tables before later migrations add them. This module currently serves only `GET /actuator/health`; authentication and business endpoints belong to later backlog items.

From repository root, start PostgreSQL as described in [infra](../../infra/README.md), then set `DB_PASSWORD` to the same local value and run:

```powershell
cd services/api
.\mvnw.cmd spring-boot:run
```

On Linux/macOS use `./mvnw spring-boot:run`. The API listens on port 8080 by default. Check `http://localhost:8080/actuator/health` for `{ "status": "UP" }`.

Run `./mvnw verify` (`.\mvnw.cmd verify` on Windows) to compile and test against a fresh PostgreSQL 17 Testcontainer. The root `scripts/verify.ps1` includes this gate and the web baseline gate.

If Docker is unavailable, the test can use an existing **empty, disposable** PostgreSQL database instead: set `SIMTIM_TEST_DB_URL`, `SIMTIM_TEST_DB_USER` and `SIMTIM_TEST_DB_PASSWORD` before running `verify`. The test applies the migration and assumes it may create schema objects in that database. Never point these variables at a shared or production database.
