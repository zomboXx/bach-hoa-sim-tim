# API module

BE-01, BE-02 và BE-03 đã tích hợp qua PR #11, #14 và #13. Sprint 2 đã tích hợp API inventory, checkout tiền mặt, promotions và reports theo [backlog](../../docs/project/governance/BACKLOG.md); các khoảng trống nghiệm thu P0 nằm trong [QA-02](../../docs/testing/QA-02-test-report.md). Runtime hỗ trợ Java 21; integration tests dùng PostgreSQL 17 qua Testcontainers.

Flyway V1 tạo schema nền; V2/V3 thêm **14 bảng Sprint 1** theo [bản thiết kế đích 39 bảng](../../docs/architecture/database/DB-01_PHYSICAL_SCHEMA_DRAFT.md). V4 thêm quyền; V5/V6 thêm inventory/receipt; V7 thêm sales invoices; V8 thêm index đọc tồn; V9 thêm quyền báo cáo; V10/V11 thêm promotions và snapshot hóa đơn. Các bảng sync/training còn thuộc backlog sau.

INV-02 exposes read-only `GET /api/v1/inventory/products`, `/batches` and `/movements` under `inventory.read`; organization/store always come from the bearer session. See the [inventory OpenAPI](../../contracts/inventory.openapi.yaml). The application package also publishes `InventorySalePort` for SAL-01; its implementation requires the caller's active transaction and never commits independently.

Để chạy trọn bộ web/API/PostgreSQL, dùng [Compose demo](../../infra/README.md). Nếu chỉ chạy API khi phát triển, khởi động PostgreSQL bằng `docker compose --env-file infra/.env -f infra/compose.yml up -d --wait` từ root, đặt `DB_PASSWORD` bằng `POSTGRES_PASSWORD` trong `infra/.env`, rồi chạy API với profile `demo`:

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

POST `/api/v1/auth/login` with `organizationCode: SIMTIM`, `storeCode: MAIN`, username and supplied password. Send returned `accessToken` as `Authorization: Bearer <token>` for GET `/api/v1/auth/session` and POST `/api/v1/auth/logout`. A logout, expired token, locked account or lost store role invalidates access. No cookies, JWT parsing or Basic authentication are supported. See the [Accepted OpenAPI](../../contracts/auth-session.openapi.yaml) and [review/matrix](../../contracts/AUTH_SESSION_REVIEW.md).

Receipt recovery uses `GET /api/v1/inventory/receipts?clientOperationId=<uuid>` with `receipts.read`. The filter is enforced by both organization and store from the authenticated session and returns either an empty array or the single matching receipt; client-supplied scope headers cannot broaden it.

Session lifetime defaults to 8 hours (`simtim.auth.session-lifetime`); login limit defaults to 20 requests/minute/IP (`simtim.auth.login-requests-per-minute`). Scope comes from the authenticated principal. Temporary BE-03 scope headers must match it. Other API paths require explicit policy when their module is implemented.

Run `./mvnw verify` (`.\mvnw.cmd verify` on Windows) to compile and test migrations/demo/health plus auth, store scope, revocation, four-role permissions, price overlap and authenticated catalog CRUD against fresh PostgreSQL 17 Testcontainers. The root `scripts/verify.ps1` includes this gate and the web baseline gate. Leave `SIMTIM_DEMO_PASSWORD` unset when running tests; tests create their own credentials and roll them back or remove them at suite teardown. The BE-02 permission fixture uses a dedicated route; catalog tests log in over HTTP and verify missing-token, SALES write-denial and scope-spoofing cases against the real BE-03 controllers.

If Docker is unavailable, the test can use an existing **empty, disposable** PostgreSQL database instead: set `SIMTIM_TEST_DB_URL`, `SIMTIM_TEST_DB_USER` and `SIMTIM_TEST_DB_PASSWORD` before running `verify`. The test applies the migration and assumes it may create schema objects in that database. Never point these variables at a shared or production database.
