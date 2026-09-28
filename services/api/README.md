# API module (BE-01 / BE-02)

BE-01 is integrated through PR #11. BE-02 is a local implementation proposal on `codex/be-02-auth`, pending contract/code review. Requires Java 21 for the supported runtime; integration tests use Docker or a disposable PostgreSQL database configured as described below.

Flyway V1 creates the `core` schema. V2 adds core/IAM tables and V3 adds catalog tables, following the Project Owner's DB-01 direction. They represent **14 Sprint 1 tables** of the [39-table target draft](../../docs/architecture/database/DB-01_PHYSICAL_SCHEMA_DRAFT.md). V4 adds permission codes/grants without changing V1–V3. Migration of inventory, sales, sync, audit and training tables waits for their owning sprint. This branch serves health and login/session/logout; catalog CRUD belongs to BE-03.

From repository root, start PostgreSQL as described in [infra](../../infra/README.md), then set `DB_PASSWORD` to the same local value. To load the **demo-only** organization, store, four roles, category, units, products, supplier and prices, run:

```powershell
cd services/api
$env:SPRING_PROFILES_ACTIVE='demo'
.\mvnw.cmd spring-boot:run
```

On Linux/macOS use `SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run`. Without the demo profile, migrations install the schema and permission catalog; no organization/account/demo product is created. The API listens on port 8080 by default. Check `http://localhost:8080/actuator/health` for `{ "status": "UP" }`.

## Local login demo

No default password is committed or generated. To opt into four local demo accounts (`sales`, `stock`, `manager`, `admin`), supply `SIMTIM_DEMO_PASSWORD` before starting with profile `demo`. It must be at least 12 characters and at most 72 UTF-8 bytes. Accounts are inserted only when missing; restarting never resets an existing password. Do not enable the demo profile on a shared deployment.

```powershell
$demoSecret = Read-Host 'Local demo password' -AsSecureString
$env:SIMTIM_DEMO_PASSWORD = [System.Net.NetworkCredential]::new('', $demoSecret).Password
try { .\mvnw.cmd spring-boot:run } finally { Remove-Item Env:SIMTIM_DEMO_PASSWORD }
```

POST `/api/v1/auth/login` with `organizationCode: SIMTIM`, `storeCode: MAIN`, username and supplied password. Send returned `accessToken` as `Authorization: Bearer <token>` for GET `/api/v1/auth/session` and POST `/api/v1/auth/logout`. A logout, expired token, locked account or lost store role invalidates access. No cookies, JWT parsing or Basic authentication are supported. See the [Draft OpenAPI](../../contracts/auth-session.openapi.yaml) and [review/matrix](../../contracts/AUTH_SESSION_REVIEW.md).

Session lifetime defaults to 8 hours (`simtim.auth.session-lifetime`); login limit defaults to 20 requests/minute/IP (`simtim.auth.login-requests-per-minute`). Scope comes from the authenticated principal. Temporary BE-03 scope headers must match it. Other API paths require explicit policy when their module is implemented.

Run `./mvnw verify` (`.\mvnw.cmd verify` on Windows) to compile and test migrations/demo/health plus auth, store scope, revocation, four-role permissions and price overlap against a fresh PostgreSQL 17 Testcontainer. The root `scripts/verify.ps1` includes this gate and the web baseline gate. Leave `SIMTIM_DEMO_PASSWORD` unset when running tests; tests create and roll back their own credentials.

If Docker is unavailable, the test can use an existing **empty, disposable** PostgreSQL database instead: set `SIMTIM_TEST_DB_URL`, `SIMTIM_TEST_DB_USER` and `SIMTIM_TEST_DB_PASSWORD` before running `verify`. The test applies the migration and assumes it may create schema objects in that database. Never point these variables at a shared or production database.
