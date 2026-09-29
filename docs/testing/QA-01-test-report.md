# QA-01 — Báo cáo kiểm thử API nền tảng

- Trạng thái: **Ready for review — local verification đạt**
- Ngày báo cáo: 2026-09-29
- Owner QA: TV4 — Lê Văn Chiến
- Source được kiểm thử: `5f0b14c787a1c0ed72756e3355ea9d662f1a7952`
- Dependency: BE-02 đã merge qua PR #14; BE-03 đã merge qua PR #13
- Issue/PR QA: Issue #8 / PR #15

## 1. Kết luận

Các blocker được nêu trong Request Changes của PR #15 đã được xử lý trong source đã pull và được TV4 chạy lại độc lập:

- Route test quyền của `ApiBootstrapTest` đã tách khỏi mapping controller sản phẩm thật.
- `CatalogApiTest` tạo fixture STOCK/SALES, login qua `/api/v1/auth/login` và gửi opaque Bearer cho positive tests.
- Negative tests vẫn kiểm tra thiếu token `401`, SALES ghi dữ liệu `403` và organization/store lệch session `403`; Spring Security không bị tắt.
- TV4 bổ sung regression test cho barcode trùng, kiểm tra cả `409` và số record không thay đổi.

Kết quả hiện hành là 31/31 automated tests đạt và 29/29 traceability cases đạt. QA-01 đủ điều kiện yêu cầu review lại. Issue #8 vẫn để mở cho tới khi reviewer chấp nhận thay đổi và PR/CI của head mới hoàn tất.

## 2. Môi trường và lệnh

- Windows; Java 24.0.2, compile `release 21`.
- Spring Boot 3.5.16; Testcontainers 1.21.4; Docker Desktop 29.4.0.
- PostgreSQL 17.11 từ `postgres:17-alpine`, database disposable sạch riêng cho mỗi suite.
- Flyway xác thực và áp dụng năm migration: V1–V4 cùng repeatable demo seed.
- Profile `demo`; không đặt `SIMTIM_DEMO_PASSWORD`.
- Không commit secret, raw token, database URL, Surefire XML hoặc output build.

Lệnh module:

```powershell
Push-Location services/api
.\mvnw.cmd verify
Pop-Location
```

Máy chạy có một phần tử `PATH` người dùng bị cấu hình sai, nối hai đường dẫn bằng dấu phẩy. Trước khi gọi Maven, TV4 loại đúng phần tử đó cho riêng tiến trình PowerShell. Đây là điều chỉnh môi trường local, không phải thay đổi code hay điều kiện để CI chạy.

Evidence ID: `E-QA-RERUN-5F0B14C`. Maven summary và Surefire XML nằm local trong `services/api/target/surefire-reports/`.

Root gate `pwsh -File scripts/verify.ps1 -SkipInstall` cũng đạt trên cùng source: repository policy và Markdown links OK; web lint/format/typecheck/build đạt; 6 demo E2E và 23 API consumer E2E đạt; backend 31/31 tests đạt. Evidence root gate: `E-QA-ROOT-5F0B14C`.

## 3. Kết quả theo suite

| Suite | Tests | Pass | Failure | Error | Skipped | Kết quả |
|---|---:|---:|---:|---:|---:|---|
| `ApiBootstrapTest` | 10 | 10 | 0 | 0 | 0 | Pass |
| `CatalogApiTest` | 21 | 21 | 0 | 0 | 0 | Pass |
| Tổng | 31 | 31 | 0 | 0 | 0 | Pass |

Chi tiết requirement, endpoint, class/method, expected và actual của 29 case nằm tại [QA-01-traceability.md](QA-01-traceability.md).

## 4. Đối chiếu Request Changes

| Nhận xét review | Cách xử lý/đối chiếu | Kết quả |
|---|---|---|
| Auth contract dùng sai `/api/auth/*` và `/api/me` | Đổi sang `/api/v1/auth/login`, `/api/v1/auth/session`, `/api/v1/auth/logout`; map đúng `ApiBootstrapTest` | Đạt |
| Không có SHA/lệnh/evidence QA thực tế | Ghi source `5f0b14c`, lệnh Maven, runtime, database và Surefire evidence local | Đạt |
| Fixture BE-02 trùng `/api/v1/products` | Source hiện dùng `/api/v1/products/__be02_security_fixture`; combined context khởi tạo được | Đạt |
| Catalog test không login/Bearer | Fixture login HTTP bằng STOCK/SALES và gửi Bearer thật | Đạt |
| Phải giữ negative authorization | Ba nhóm test missing token, SALES deny write và scope mismatch đều đạt | Đạt |
| CI cũ không có BE-03 | Root gate hiện tại chứa cả BE-02 và BE-03; 21 catalog tests chạy trên controller thật | Đạt local; chờ CI sau push |
| Thiếu duplicate barcode | Thêm `addProductBarcode_conflict_onDuplicateBarcode_withoutCreatingRecord` | Đạt |
| Cần cập nhật expected/actual và báo cáo | 29/29 dòng traceability đã có expected, actual, method và evidence | Đạt |

## 5. Phạm vi đã chứng minh

- Sai thông tin đăng nhập, malformed/missing Bearer và endpoint auth/session/logout thật.
- Opaque token/hash-only storage, BCrypt, expiry, revoke, khóa account, mất role/permission và rate limit.
- Quyền đọc/ghi của SALES, STOCK, MANAGER, ADMIN và organization/store scope.
- CRUD category, product, supplier; tìm product theo SKU/barcode/name.
- Duplicate category code, product SKU, product barcode và supplier code.
- Validation category rỗng và supplier email sai với status lỗi có cấu trúc.
- Request bị từ chối không vượt security; duplicate barcode không làm tăng số record.

## 6. Evidence lịch sử

Checkpoint trước sửa harness được giữ để truy vết: BE-03 `f9f3642` ghép main/BE-02 `9cd293d` có 27 test, 0 Pass, 11 Failure và 16 Error. Đó là kết quả reviewer cung cấp dẫn tới Request Changes, không phải kết quả của source hiện hành.

Không tạo Bug Issue mới vì lần chạy hiện tại không còn regression trong phạm vi. Nếu CI sau push hoặc review độc lập tái hiện lỗi, Issue #8 phải giữ mở và lỗi mới cần Bug Issue/regression evidence riêng.

## 7. Giới hạn và việc còn chờ

- Local dùng Java 24.0.2 nhưng compiler khóa `release 21`; CI Java 21 tại head mới cần đạt sau khi push.
- Header scope tạm của BE-03 chỉ được chấp nhận khi khớp session; chuyển controller sang lấy scope trực tiếp từ `SessionPrincipal` là follow-up implementation, không phải bypass trong test này.
- Việc đóng Issue #8 và merge PR #15 thuộc reviewer/Project Owner; báo cáo này chỉ kết luận evidence local đã đạt.

## 8. Nội dung báo cáo tuần

TV4 hoàn thành QA-01 trên source `5f0b14c`: cập nhật auth contract thật và ma trận truy vết, xác nhận test harness BE-02/BE-03 đã tích hợp Bearer session mà không tắt security, bổ sung regression test barcode trùng. Root verification đạt policy/links, web lint/format/typecheck/build, 6 demo E2E, 23 API consumer E2E và 31/31 backend tests trên PostgreSQL 17.11 disposable sạch; 29/29 traceability cases Pass. CI của head mới còn chờ sau push; Issue #8 giữ mở tới khi reviewer chấp nhận.

## 9. Đề nghị review lại

Reviewer kiểm tra:

1. Source/evidence `5f0b14c` và kết quả CI sau push.
2. Test barcode trùng có kiểm tra hậu điều kiện dữ liệu.
3. Mapping 29 case trong traceability khớp requirement, endpoint và method.
4. Contract/ma trận quyền Accepted không bị mở rộng ngoài BE-02/BE-03.

Nếu các mục trên đạt, QA-01 có thể được approve và đóng cùng PR.
