# QA-01 — Kế hoạch kiểm thử API nền tảng

- Trạng thái: **Ready for review — đã thực thi trên API tích hợp**
- Ngày cập nhật: 2026-09-29
- Owner: TV4 — Lê Văn Chiến (`@VanChien11-02`)
- Reviewer chính: TV1 — Nguyễn Đức Phát (`@zomboXx`)
- Reviewer liên quan: TV2 cho provider/API và TV3 cho contract consumer
- Backlog: `QA-01`
- Phụ thuộc: `BE-02`, `BE-03`

Tài liệu này định nghĩa cách kiểm chứng API xác thực, phân quyền, danh mục, sản phẩm và nhà cung cấp của Sprint 1. TV4 đã chạy trực tiếp source `5f0b14c` trên PostgreSQL disposable sạch; kết quả và evidence nằm trong test report và traceability.

## 1. Mục tiêu

- Chứng minh đăng nhập sai và request thiếu/sai quyền bị server từ chối.
- Chứng minh bốn vai trò server chỉ dùng được permission đã cấp.
- Chứng minh API catalog từ chối mã trùng và dữ liệu không hợp lệ bằng lỗi có cấu trúc.
- Nối từng requirement với contract, test case, automated test và bằng chứng thực thi.
- Phân biệt rõ test security chain của `BE-02` với CRUD thật của `BE-03`.
- Cung cấp kết quả `Pass`, `Fail` hoặc `Blocked` có căn cứ cho Sprint Review.

## 2. Tài liệu nguồn

Thứ tự ưu tiên khi có mâu thuẫn:

1. Quyết định mới nhất của Project Owner trong Issue/PR hoặc biên bản.
2. [Backlog Sprint 1](../project/governance/BACKLOG.md).
3. OpenAPI và permission matrix có trạng thái `Accepted` trong `contracts/`.
4. [Danh sách yêu cầu](../product/analysis/02-danh-sach-yeu-cau.md) và [Use Case](../product/analysis/03-actors-use-cases.md).
5. Code và automated test đã merge vào `main`.

Nguồn tham chiếu hiện tại:

- [BE-02 Issue #5](https://github.com/zomboXx/bach-hoa-sim-tim/issues/5) và [PR #14](https://github.com/zomboXx/bach-hoa-sim-tim/pull/14), đã merge vào `main` tại `9cd293d`.
- [BE-03 PR #13](https://github.com/zomboXx/bach-hoa-sim-tim/pull/13), đã merge vào nhánh tích hợp tại `bd6ef5c`.
- [Auth/session OpenAPI](../../contracts/auth-session.openapi.yaml) và [session/RBAC review](../../contracts/AUTH_SESSION_REVIEW.md).
- [ADR nền tảng Sprint 1](../architecture/adr/0002-sprint-1-platform.md).
- [ADR kiến trúc ứng dụng](../architecture/adr/0003-application-architecture.md).
- [Thiết kế schema DB-01](../architecture/database/DB-01_PHYSICAL_SCHEMA_DRAFT.md).
- [Ma trận truy vết QA-01](QA-01-traceability.md).
- [Ma trận quyền QA-01](QA-01-authorize.md).
- [Báo cáo kiểm thử QA-01](QA-01-test-report.md).

Auth/session contract đã Accepted ngày 29/09/2026 sau provider/consumer review. Trạng thái merge hoặc CI xanh vẫn không tự thay thế evidence QA trên đúng source tích hợp.

## 3. Phạm vi

### 3.1. BE-02 — Xác thực và phân quyền

- Đăng nhập đúng/sai.
- Đọc phiên hiện hành.
- Đăng xuất, thu hồi và hết hạn phiên.
- Bearer session ngẫu nhiên; CSDL chỉ lưu hash token.
- Tài khoản khóa, mất role hoặc permission sau khi đã đăng nhập.
- Bốn vai trò `SALES`, `STOCK`, `MANAGER`, `ADMIN`.
- Permission `catalog.read`, `catalog.write` và nguyên tắc deny-by-default.
- Phạm vi tổ chức/cửa hàng không bị ghi đè bằng header giả mạo.
- Rate limit đăng nhập và demo seed chỉ bật khi có cấu hình chủ động.

### 3.2. BE-03 — Catalog

- CRUD và trạng thái của category, product, supplier theo contract được chấp nhận.
- Tìm sản phẩm theo mã, barcode hoặc tên.
- Từ chối product code/barcode trùng.
- Từ chối trường thiếu, định dạng sai và giá trị ngoài miền hợp lệ.
- Lỗi validation có cấu trúc theo contract.
- Không ghi dở dữ liệu khi request bị từ chối.

## 4. Ngoài phạm vi

- `ACC-01`: API tạo, khóa/mở khóa và gán role cho tài khoản.
- Web adapter của `FE-01`, trừ consumer test cần để xác nhận contract.
- Nhận hàng, tồn kho, bán hàng, kiểm kê offline và báo cáo.
- Runtime Godot và dữ liệu đào tạo.
- Bán hàng offline, nhiều cửa hàng đồng thời và kiểm thử tải production.
- Đánh giá khả năng chống mọi hình thức tấn công; QA-01 chỉ kiểm tra security behavior đã cam kết.

## 5. Thành phần được kiểm thử

| Thành phần | Test level chính | Owner triển khai | Vai trò TV4 |
|---|---|---|---|
| Spring Security chain | Integration/provider | TV1 | Đối chiếu quyền, chạy độc lập và ghi report |
| Auth/session API | Integration/provider | TV1 | Kiểm tra positive/negative/session lifecycle |
| Catalog API | Integration/provider | TV2 | Kiểm tra CRUD, duplicate và validation |
| PostgreSQL/Flyway | Integration | TV2 | Chạy trên database sạch, kiểm tra hậu điều kiện |
| OpenAPI/permission contract | Contract review | TV1/TV2/TV3 | Truy vết operation/permission tới test |
| Web consumer | Consumer test | TV3 | Xác nhận 401/403/validation/network được xử lý |
| E2E đại diện | Playwright | TV3/TV4 | Chạy sau khi adapter API sẵn sàng |

Owner module vẫn chịu trách nhiệm self-test. TV4 không làm thay toàn bộ provider/consumer test; TV4 kiểm tra coverage, chạy lại độc lập, bổ sung khoảng trống và lập báo cáo.

## 6. Môi trường kiểm thử

### Môi trường đích

- Java 21.
- Spring Boot 3.5.
- PostgreSQL 17.
- Flyway migration từ database sạch.
- Maven Wrapper được commit trong `services/api`.
- Node.js 22 và Chrome/Chromium cho consumer/E2E khi `FE-01` sẵn sàng.

### Trạng thái tại lần cập nhật này

- Source được TV4 kiểm thử là `5f0b14c787a1c0ed72756e3355ea9d662f1a7952`, có cả BE-02 và BE-03.
- Java 24.0.2 compile `release 21`; Testcontainers chạy PostgreSQL 17.11 disposable sạch; không đặt `SIMTIM_DEMO_PASSWORD`.
- `services/api/mvnw.cmd verify` đạt 31/31 tests: 10 auth/bootstrap và 21 catalog.
- Root `pwsh -File scripts/verify.ps1 -SkipInstall` đạt repository policy, Markdown links, web lint/format/typecheck/build, 6 demo E2E, 23 API consumer E2E và 31 backend tests.
- 29/29 test case trong ma trận truy vết có method, expected, actual và evidence `E-QA-RERUN-5F0B14C`.
- Hai blocker ở checkpoint review cũ đã được owner sửa: route fixture không còn trùng và catalog fixture đã dùng login/Bearer thật. TV4 bổ sung coverage barcode trùng.

Khi chạy thật, report phải ghi commit SHA, profile, phiên bản Java/PostgreSQL, lệnh chạy và URL CI. Không ghi secret, token đầy đủ, mật khẩu demo hoặc database dump.

## 7. Dữ liệu và tài khoản kiểm thử

Cần chuẩn bị dữ liệu tách biệt, có thể tạo lại từ database sạch:

| Nhóm | Dữ liệu tối thiểu |
|---|---|
| Role | `SALES`, `STOCK`, `MANAGER`, `ADMIN` |
| Account | Một tài khoản active cho mỗi role; một tài khoản locked |
| Session | Hợp lệ, đã logout/revoked, hết hạn |
| Permission | Role có/không có `catalog.read`, `catalog.write`; permission bị thu hồi sau login |
| Scope | Hai ID tổ chức/cửa hàng dùng để kiểm tra giả mạo scope nếu contract hỗ trợ |
| Category | Một category hợp lệ và một category ngừng sử dụng |
| Product | Mã/barcode duy nhất; cặp dữ liệu trùng; dữ liệu thiếu/sai định dạng |
| Supplier | Một supplier hợp lệ; dữ liệu thiếu/sai định dạng; supplier ngừng hoạt động |

Giá trị mật khẩu phải được cung cấp qua biến môi trường/profile test và không commit. Mỗi test ghi dữ liệu tự tạo hoặc reset dữ liệu để không phụ thuộc thứ tự chạy.

## 8. Phương pháp kiểm thử

### 8.1. Truy vết

Với mỗi test case:

```text
Requirement → Use Case → Backlog → Contract/permission → Test → Result → Evidence
```

Nếu không tìm thấy requirement hoặc contract cho một behavior, ghi `Gap/TBD` và yêu cầu owner làm rõ; không tự biến mô tả implementation thành yêu cầu được duyệt.

### 8.2. Positive và negative testing

- Positive: role đúng, token hợp lệ, payload hợp lệ.
- Negative: sai mật khẩu, không token, sai permission, token revoked/expired, account locked.
- Boundary/validation: chuỗi rỗng, thiếu field, giá trị ngoài giới hạn, mã/barcode trùng.
- Persistence: sau request bị từ chối, truy vấn lại để chứng minh dữ liệu không thay đổi.
- Confidentiality: response/log không lộ password, password hash hoặc full token.

### 8.3. Evidence

Evidence được chấp nhận:

- Tên class/method test tại commit cụ thể.
- Log lệnh chạy đã loại secret.
- Link CI run tại commit được kiểm thử.
- Bug Issue có bước tái hiện và expected result.

Mô tả “owner nói đã chạy” chỉ được ghi `Reported`, không được ghi `Pass`.

## 9. Ma trận vai trò và quyền

Ma trận chi tiết nằm trong [QA-01-authorize.md](QA-01-authorize.md). Mỗi ô phải có nguồn từ contract/decision. Ô chưa có nguồn được ghi `TBD`, không dùng suy đoán UI của Sprint 0 làm quyền server.

## 10. Điều kiện bắt đầu

### Có thể bắt đầu thiết kế test khi

- Issue QA-01 có owner/reviewer và acceptance criteria.
- Requirement/Use Case liên quan đã xác định.
- Dependency và điểm chưa rõ được ghi công khai.

### Có thể bắt đầu thực thi BE-02/BE-03 khi

- Code cần test có commit/PR xác định và checkout được.
- OpenAPI/permission matrix ghi rõ trạng thái Draft hoặc Accepted.
- Migration chạy được từ PostgreSQL sạch.
- Có hướng dẫn cấu hình không chứa secret.
- Có tài khoản/dữ liệu test cho bốn role.
- Có lệnh test module được nối vào root gate/CI.

## 11. Điều kiện hoàn thành

QA-01 chỉ đủ điều kiện chuyển `In Review` khi:

- Tất cả requirement trong phạm vi có ít nhất một dòng traceability.
- Các operation của contract Accepted có positive và negative test phù hợp.
- Test đăng nhập sai, sai quyền, duplicate và validation đã chạy trên API thật.
- Test security kiểm tra cả response lẫn việc dữ liệu không bị thay đổi.
- Mọi kết quả `Pass` có commit, lệnh và evidence.
- Mọi `Fail` có Bug Issue hoặc quyết định xử lý được liên kết.
- Mọi `Blocked/Skipped` có lý do và rủi ro cụ thể.
- Root verification và module test đạt, hoặc lỗi được ghi rõ trong PR.
- Reviewer kiểm tra và chấp nhận test plan/report.

## 12. Cách ghi nhận lỗi

Mỗi bug phải có:

- Current behavior và expected behavior.
- Role, endpoint/operation và môi trường.
- Bước tái hiện tối thiểu.
- Request/response đã loại password/token/secret.
- Ảnh hưởng đến quyền, dữ liệu hoặc tương thích.
- Regression test dự kiến.
- Owner và reviewer.

Ưu tiên cao cho lỗi vượt quyền, lộ secret, ghi dữ liệu dù request thất bại, xử lý token revoked/expired sai và cho phép mã/barcode trùng.

## 13. Rủi ro và dependency

| Rủi ro/dependency | Ảnh hưởng | Cách xử lý |
|---|---|---|
| Regression route fixture trùng controller production | Có thể làm combined context không khởi tạo | Đã tách route; giữ `ApiBootstrapTest` trong root/module gate |
| Regression catalog fixture bỏ Bearer hoặc tắt security | Có thể tạo false-positive cho CRUD | Giữ fixture login HTTP và ba nhóm negative authorization tests |
| OpenAPI/permission matrix thay đổi sau Accepted | Endpoint, status/error code có thể lệch test | Mọi thay đổi contract cần review và cập nhật traceability |
| SRS còn trạng thái đề xuất | Có thể lệch phạm vi đã chốt | Theo backlog và quyết định PO mới nhất |
| CI cũ của PR #15 không chứa BE-03 CRUD | CI cũ không chứng minh duplicate/validation/catalog | Không tái sử dụng CI cũ; yêu cầu CI mới trên head chứa cả hai module |
| Khác Java local/CI | Local Java 24 có thể khác Java 21 đích | Compile `release 21`; bắt buộc theo dõi CI Java 21 sau push |
| Không có test data tái tạo | Test dễ phụ thuộc thứ tự hoặc dữ liệu cũ | Reset database/fixture trước suite |

## 14. Quy trình lặp lại cho các QA sau

1. Chọn một acceptance criterion.
2. Tìm requirement và Use Case nguồn.
3. Tìm operation/permission trong contract Accepted.
4. Viết ít nhất một positive và một negative case khi phù hợp.
5. Tìm automated test của owner; ghi đúng class/method và commit.
6. Chạy độc lập trên môi trường đã định nghĩa.
7. Ghi `Pass/Fail/Blocked` cùng evidence.
8. Nếu fail, tạo Bug Issue và yêu cầu regression test.
9. Chỉ kết luận khi traceability không còn khoảng trống P0/P1 chưa giải thích.
