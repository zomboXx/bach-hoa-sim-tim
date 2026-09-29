# QA-01 — Ma trận truy vết

- Trạng thái: **Ready for review — 29/29 test case đạt**
- Ngày cập nhật: 2026-09-29
- Owner: TV4 — Lê Văn Chiến
- Implementation/test commit: `5f0b14c787a1c0ed72756e3355ea9d662f1a7952`
- Dependency đã tích hợp: BE-02 qua PR #14; BE-03 qua PR #13
- Phạm vi: `BE-02`, `BE-03`

## Cách đọc

Mỗi dòng nối requirement/Use Case với wire contract, automated test, expected, actual và evidence. `Pass` chỉ được dùng vì TV4 đã chạy trực tiếp đúng checkout tích hợp có cả BE-02 và BE-03. Kết quả cũ do reviewer cung cấp vẫn được giữ ở phần lịch sử, không bị viết lại thành Pass.

## Evidence

### `E-QA-RERUN-5F0B14C` — lần chạy xác nhận của TV4

- Source: `5f0b14c787a1c0ed72756e3355ea9d662f1a7952`.
- Lệnh: `services/api/mvnw.cmd verify`.
- Runtime: Java 24.0.2, Maven compile `release 21`, Testcontainers 1.21.4, Docker Desktop 29.4.0.
- Database: hai PostgreSQL 17.11 disposable sạch từ image `postgres:17-alpine`; Flyway xác thực và áp dụng V1–V4 cùng demo seed.
- Profile: `demo`; không đặt `SIMTIM_DEMO_PASSWORD`; tài khoản STOCK/SALES do suite tạo, login qua HTTP rồi xóa ở teardown.
- Kết quả: `ApiBootstrapTest` 10/10 Pass; `CatalogApiTest` 21/21 Pass; tổng 31 Pass, 0 Failure, 0 Error, 0 Skipped.
- Evidence local: Maven summary và `services/api/target/surefire-reports/TEST-*.xml`; output build, token, mật khẩu và URL database không được commit.
- Ghi chú môi trường: máy có một phần tử `PATH` sai chứa hai đường dẫn nối bằng dấu phẩy; TV4 chỉ loại phần tử đó trong tiến trình test và khởi động Docker Desktop, không sửa cấu hình project hay tắt security.

### `E-QA-INT-20260929` — checkpoint lỗi trước khi owner sửa harness

Reviewer chạy BE-03 `f9f3642` ghép main/BE-02 `9cd293d` và ghi nhận 27 test: 0 Pass, 11 Failure, 16 Error. Nguyên nhân là route fixture BE-02 trùng controller BE-03 và `CatalogApiTest` chưa login/gửi Bearer. Evidence này giải thích lịch sử Request Changes của PR #15; nó không phải kết quả hiện hành.

## BE-02 — Xác thực và phân quyền

Wire đã Accepted: `POST /api/v1/auth/login`, `GET /api/v1/auth/session`, `POST /api/v1/auth/logout`; session là opaque Bearer có organization/store scope.

| Test ID | Requirement / Use Case | Contract/policy | Class#method | Expected | Actual 29/09 | Evidence | Status |
|---|---|---|---|---|---|---|---|
| `QA01-AUTH-001` | `FR-AUTH-01` / `UC-AUTH-01` | `POST /api/v1/auth/login` | `ApiBootstrapTest#fourRolesEnforceCatalogPermissionsAndTrainingDoesNotEscalate` | `200`; Bearer 43 ký tự, expiry và session scope | Đúng expected cho bốn role | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-AUTH-002` | `FR-AUTH-01` / `UC-AUTH-01` | `POST /api/v1/auth/login` | `ApiBootstrapTest#wrongCredentialsHaveGenericErrorAndSessionStoresOnlyHash` | Sai user/password trả `401 INVALID_CREDENTIALS` giống nhau | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-AUTH-003` | `FR-AUTH-01`, `FR-AUTH-03` / `UC-AUTH-01` | `GET /api/v1/auth/session` | `ApiBootstrapTest#fourRolesEnforceCatalogPermissionsAndTrainingDoesNotEscalate` | `200`; đúng user, scope, roles, permissions, training flag; không lộ token/hash | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-AUTH-004` | `FR-AUTH-03`, `NFR-SEC-02` / `UC-AUTH-01` | Bearer security chain | `ApiBootstrapTest#rejectsMalformedLoginAndUnsupportedAuthentication`; `CatalogApiTest#catalogRequestsWithoutBearerAreRejected` | Missing/Basic/malformed Bearer trả `401`; dữ liệu không đổi | `401 UNAUTHENTICATED` trên security fixture và controller catalog thật | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-AUTH-005` | `FR-AUTH-01`; BE-02 AC / `UC-AUTH-01` | Account/session state | `ApiBootstrapTest#rejectsSpoofedTenantAndRechecksAccountAndRoleState` | Account bị khóa hoặc mất store role bị từ chối ở request sau | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-AUTH-006` | `FR-AUTH-02` / `UC-AUTH-02` | `POST /api/v1/auth/logout` | `ApiBootstrapTest#logoutAndExpiryInvalidateBearerSessions` | Logout hợp lệ `204`, session bị revoke | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-AUTH-007` | `FR-AUTH-02` / `UC-AUTH-02` | Revoked opaque session | `ApiBootstrapTest#logoutAndExpiryInvalidateBearerSessions` | Dùng lại token sau logout trả `401` | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-AUTH-008` | `NFR-SEC-03` / `UC-AUTH-01` | Session expiry | `ApiBootstrapTest#logoutAndExpiryInvalidateBearerSessions` | Token hết hạn trả `401`, không tự gia hạn | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-RBAC-001` | `FR-AUTH-03`, `NFR-SEC-02` / catalog Use Cases | `catalog.read` | `ApiBootstrapTest#fourRolesEnforceCatalogPermissionsAndTrainingDoesNotEscalate`; `CatalogApiTest#salesCanReadCatalogButCannotCreateUpdateOrDelete` | Bốn role đọc được; thiếu token bị từ chối | `200` cho role hợp lệ; `401` khi thiếu Bearer | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-RBAC-002` | `FR-AUTH-03`, `NFR-SEC-02` / catalog Use Cases | `catalog.write` | `ApiBootstrapTest#fourRolesEnforceCatalogPermissionsAndTrainingDoesNotEscalate`; `CatalogApiTest#salesCanReadCatalogButCannotCreateUpdateOrDelete` | SALES POST/PUT/DELETE trả `403`; dữ liệu không đổi | `403 FORBIDDEN` trên category/unit/product/supplier thật | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-RBAC-003` | `FR-AUTH-03`, `NFR-SEC-02` / catalog Use Cases | `catalog.write` | `ApiBootstrapTest#fourRolesEnforceCatalogPermissionsAndTrainingDoesNotEscalate` | STOCK/MANAGER/ADMIN đi qua security chain | Đúng expected; STOCK tiếp tục chạy CRUD catalog thật | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-RBAC-004` | `FR-AUTH-03`, `NFR-SEC-02` / `UC-AUTH-01` | Reload grants mỗi request | `ApiBootstrapTest#requiresStoreAssignmentAndReloadsPermissionGrants` | Gỡ `catalog.write`: POST `403`, GET vẫn `200`; store inactive `401` | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-RBAC-005` | `FR-AUTH-03`, `NFR-SEC-02` / `UC-AUTH-01` | Organization/store scope | `ApiBootstrapTest#rejectsSpoofedTenantAndRechecksAccountAndRoleState`; `CatalogApiTest#catalogRejectsOrganizationAndStoreOutsideSessionScope` | Header scope lệch session trả `403` | `403 FORBIDDEN` trên bốn nhóm controller thật | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-SEC-001` | `NFR-SEC-01` / `UC-AUTH-01` | BCrypt cost 12 | `ApiBootstrapTest#wrongCredentialsHaveGenericErrorAndSessionStoresOnlyHash` | Password không lưu plain text; BCrypt verify fixture | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-SEC-002` | BE-02 security design / `UC-AUTH-01` | Opaque token + SHA-256 hash | `ApiBootstrapTest#wrongCredentialsHaveGenericErrorAndSessionStoresOnlyHash` | Client nhận raw token 43 ký tự; DB chỉ giữ hash 32 byte | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-SEC-003` | BE-02 security design / `UC-AUTH-01` | 20 login requests/phút/IP | `ApiBootstrapTest#loginRequestsAreRateLimited` | Request 21 trả `429 RATE_LIMITED`, `Retry-After: 60` | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-SEC-004` | BE-02 demo policy / `UC-AUTH-01` | Demo opt-in | `ApiBootstrapTest#demoBootstrapRequiresSuppliedPasswordAndDoesNotResetExistingAccounts` | Không password không tạo account; có password tạo bốn account và không reset hash | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |

## BE-03 — Danh mục, sản phẩm và nhà cung cấp

Positive tests tạo STOCK session thật qua `/api/v1/auth/login`; negative tests giữ thiếu Bearer, SALES không có `catalog.write` và scope mismatch. Security không bị disable.

| Test ID | Requirement / Use Case | Endpoint/contract | Class#method | Expected | Actual 29/09 | Evidence | Status |
|---|---|---|---|---|---|---|---|
| `QA01-CAT-001` | `FR-CAT-01` / `UC-CAT-01` | `POST /api/v1/categories` | `CatalogApiTest#createCategory_then_conflict_on_duplicateCode` | Tạo `201`; lặp code trả `409` | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-CAT-002` | `FR-CAT-02`, `FR-CAT-03` / `UC-CAT-02` | `POST /api/v1/products` | `CatalogApiTest#createProduct_then_updateAndDelete` | Tạo product hợp lệ `201` | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-CAT-003` | `FR-CAT-05`, `BR-01` / `UC-CAT-02` | Duplicate SKU | `CatalogApiTest#createProduct_conflict_on_duplicateSku` | `409`; không tạo product trùng | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-CAT-004` | `FR-CAT-05`, `BR-01` / `UC-CAT-02` | `POST /api/v1/products/{id}/barcodes` | `CatalogApiTest#addProductBarcode_conflict_onDuplicateBarcode_withoutCreatingRecord` | Barcode trùng trả `409`; số record trước/sau không đổi | `409`; danh sách barcode giữ nguyên kích thước | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-CAT-005` | `FR-CAT-01` / `UC-CAT-01` | Category validation | `CatalogApiTest#createCategory_invalidPayload_returns422` | Payload rỗng trả `422` có `errors` | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-CAT-006` | `FR-CAT-04` / `UC-CAT-02` | `GET /api/v1/products/search?sku=...` | `CatalogApiTest#searchProduct_bySku` | `200`, đúng một product | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-CAT-007` | `FR-CAT-04` / `UC-CAT-02` | `GET /api/v1/products/search?barcode=...` | `CatalogApiTest#searchProduct_byBarcode` | `200`, đúng một product | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-CAT-008` | `FR-CAT-04` / `UC-CAT-02` | `GET /api/v1/products/search?name=...` | `CatalogApiTest#searchProduct_byName` | `200`, đúng kết quả theo tên | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-CAT-009` | `FR-CAT-01`, `FR-CAT-02` / `UC-CAT-01/02` | Category/product PUT, DELETE, GET | `CatalogApiTest#updateAndDeleteCategory`; `#createProduct_then_updateAndDelete` | Update `200`; delete `204`; category đã xóa `404` | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-SUP-001` | `FR-REC-01` / `UC-SUP-01` | `POST /api/v1/suppliers` | `CatalogApiTest#createSupplier_then_updateAndDelete` | Tạo supplier `201` trong session scope | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-SUP-002` | `FR-REC-01` / `UC-SUP-01` | Supplier email validation | `CatalogApiTest#createSupplier_invalidEmail_returns422` | Email sai trả `422` | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |
| `QA01-SUP-003` | `FR-REC-01` / `UC-SUP-01` | Supplier GET/PUT/DELETE | `CatalogApiTest#listSuppliers_returnsDemoData`; `#getSupplier_found`; `#createSupplier_then_updateAndDelete` | List/get/update `200`; delete `204` | Đúng expected | `E-QA-RERUN-5F0B14C` | Pass |

## Tổng hợp

| Suite | Tests | Pass | Failure | Error | Skipped |
|---|---:|---:|---:|---:|---:|
| `ApiBootstrapTest` | 10 | 10 | 0 | 0 | 0 |
| `CatalogApiTest` | 21 | 21 | 0 | 0 | 0 |
| Tổng automated methods | 31 | 31 | 0 | 0 | 0 |

- Traceability cases: 29/29 Pass.
- Blocker cũ duplicate mapping và thiếu Bearer fixture: đã được owner sửa và TV4 xác nhận lại.
- Khoảng trống duplicate barcode: đã bổ sung regression integration test tại `5f0b14c`.
- Không có Fail/Blocked còn lại trong phạm vi QA-01 tại lần chạy này.
