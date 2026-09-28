# QA-01 — Ma trận truy vết

- Trạng thái: **Draft — chưa có test API nào được QA xác nhận Pass**
- Ngày cập nhật: 2026-09-28
- Owner: TV4 — Lê Văn Chiến
- Phạm vi: `BE-02`, `BE-03`

## Cách đọc

Mỗi dòng nối một yêu cầu với contract/policy và một tình huống kiểm thử. Cột `Automated test/evidence` phải được thay bằng đúng tên class/method và commit sau khi nhánh triển khai có thể checkout.

Trạng thái:

- `Planned`: đã thiết kế, chưa tới lúc chạy.
- `Reported`: owner/CI báo có coverage, TV4 chưa chạy độc lập.
- `Blocked`: dependency chưa sẵn sàng hoặc chưa có contract đủ rõ.
- `Pass`: TV4 đã chạy trên API thật và có evidence.
- `Fail`: TV4 đã chạy và kết quả khác expected.
- `Skipped`: chủ động không chạy, phải có quyết định và lý do.

Không chuyển từ `Reported` sang `Pass` chỉ bằng cách đọc mô tả PR.

## BE-02 — Xác thực và phân quyền

Các operation/permission dưới đây được lấy từ handoff BE-02 ngày 2026-09-28 và vẫn là Draft đến khi contract được review/merge.

| Test ID | Requirement | Use Case | Backlog | Contract/policy Draft | Trường hợp kiểm thử | Kết quả mong đợi | Automated test/evidence | Trạng thái |
|---|---|---|---|---|---|---|---|---|
| `QA01-AUTH-001` | `FR-AUTH-01` | `UC-AUTH-01` | `BE-02` | `POST /api/auth/login` | Đăng nhập bằng tài khoản active và mật khẩu đúng | Trả Bearer session có hạn; không trả password/hash | Tên method TBD; owner báo auth tests tại `4801d07` | Reported |
| `QA01-AUTH-002` | `FR-AUTH-01` | `UC-AUTH-01` | `BE-02` | `POST /api/auth/login` | Đăng nhập sai mật khẩu | Bị từ chối, không tạo session, không tiết lộ tài khoản tồn tại | Tên method TBD; handoff ghi “đăng nhập sai” | Reported |
| `QA01-AUTH-003` | `FR-AUTH-01`, `FR-AUTH-03` | `UC-AUTH-01` | `BE-02` | `GET /api/me` | Bearer session hợp lệ đọc phiên hiện hành | Trả đúng user, role, permission, scope và `trainingEnabled` | Tên method TBD; cần đối chiếu OpenAPI | Blocked |
| `QA01-AUTH-004` | `FR-AUTH-03`, `NFR-SEC-02` | `UC-AUTH-01` | `BE-02` | Security chain | Gọi API bảo vệ khi không có/malformed Bearer token | Bị từ chối trước controller; dữ liệu không đổi | Tên method TBD; owner báo security tests | Reported |
| `QA01-AUTH-005` | `FR-AUTH-01`; BE-02 AC | `UC-AUTH-01` | `BE-02` | Account status policy | Đăng nhập hoặc dùng session của account bị khóa | Bị từ chối; không tạo/không chấp nhận session | Tên method TBD; handoff ghi “tài khoản khóa” | Reported |
| `QA01-AUTH-006` | `FR-AUTH-02` | `UC-AUTH-02` | `BE-02` | `POST /api/auth/logout` | Đăng xuất bằng session hợp lệ | Session được đánh dấu thu hồi; response không lộ token hash | Tên method TBD; handoff ghi “logout” | Reported |
| `QA01-AUTH-007` | `FR-AUTH-02` | `UC-AUTH-02` | `BE-02` | Revocation policy | Dùng lại token sau logout/revoke | Bị từ chối; API nghiệp vụ không chạy | Tên method TBD; handoff ghi “dùng lại phiên thu hồi” | Reported |
| `QA01-AUTH-008` | `NFR-SEC-03` | `UC-AUTH-01` | `BE-02` | Expiration policy | Dùng token hết hạn | Bị từ chối; không tự gia hạn | Tên method TBD; handoff ghi “expiry” | Reported |
| `QA01-RBAC-001` | `FR-AUTH-03`, `NFR-SEC-02` | `UC-CAT-01/02`, `UC-SUP-01` | `BE-02` | `catalog.read` | Role được cấp `catalog.read` gọi GET catalog test endpoint | Được phép; principal lấy từ session server | Tên method TBD; controller chỉ tồn tại trong test | Reported |
| `QA01-RBAC-002` | `FR-AUTH-03`, `NFR-SEC-02` | `UC-CAT-01/02`, `UC-SUP-01` | `BE-02` | `catalog.write` | `SALES` gọi POST/PUT/DELETE catalog test endpoint | Bị từ chối; dữ liệu không đổi | Tên method TBD; handoff ghi test quyền bốn role | Reported |
| `QA01-RBAC-003` | `FR-AUTH-03`, `NFR-SEC-02` | `UC-CAT-01/02`, `UC-SUP-01` | `BE-02` | `catalog.write` | `STOCK`, `MANAGER`, `ADMIN` gọi write test endpoint | Được phép theo permission matrix Draft | Tên method TBD; cần reviewer contract xác nhận | Reported |
| `QA01-RBAC-004` | `FR-AUTH-03`, `NFR-SEC-02` | `UC-AUTH-01` | `BE-02` | Reload authorization mỗi request | Thu hồi role/permission sau khi đã cấp session | Request tiếp theo dùng quyền mới và bị từ chối nếu thiếu quyền | Tên method TBD; handoff ghi “mất vai trò/thu hồi permission” | Reported |
| `QA01-RBAC-005` | `FR-AUTH-03`, `NFR-SEC-02` | `UC-AUTH-01` | `BE-02` | Store/organization scope | Gửi header giả mạo tổ chức/cửa hàng khác với session | Không vượt được scope server; request bị từ chối hoặc dùng scope session theo contract | Tên method TBD; handoff ghi test phạm vi cửa hàng | Reported |
| `QA01-SEC-001` | `NFR-SEC-01` | `UC-AUTH-01` | `BE-02` | BCrypt password policy | Kiểm tra bản ghi user sau khi seed/login | Không có mật khẩu thuần; hash BCrypt hợp lệ | Tên method TBD; handoff ghi BCrypt | Reported |
| `QA01-SEC-002` | BE-02 security design | `UC-AUTH-01` | `BE-02` | Opaque session storage | Đối chiếu token trả cho client với dữ liệu `auth_sessions` | CSDL chỉ lưu token hash, không lưu full Bearer token | Tên method TBD; handoff ghi “token hash” | Reported |
| `QA01-SEC-003` | BE-02 security design | `UC-AUTH-01` | `BE-02` | Login rate limit | Gửi login request vượt ngưỡng từ cùng IP | Request vượt ngưỡng bị giới hạn theo error contract; không tạo session | Tên method TBD; handoff ghi “rate limit” | Reported |
| `QA01-SEC-004` | BE-02 demo policy | `UC-AUTH-01` | `BE-02` | Demo profile opt-in | Khởi động không có `SIMTIM_DEMO_PASSWORD`, sau đó bật demo profile có biến | Không tự tạo/reset demo user khi thiếu cấu hình; tạo có kiểm soát khi đủ cấu hình | Tên method TBD; handoff ghi “demo opt-in” | Reported |

### Giới hạn bằng chứng BE-02 hiện tại

Handoff báo 10 API tests đạt tại commit `4801d07` và [CI run 36404996237](https://github.com/zomboXx/bach-hoa-sim-tim/actions/runs/36404996237). Checkout QA hiện tại chưa có commit/code BE-02, nên chưa thể đối chiếu chính xác 17 dòng trên với từng test method hoặc tự chạy lại. Các dòng `Reported` vẫn không phải `Pass`.

Controller catalog của BE-02 chỉ là test fixture để kiểm tra Spring Security chain. Nó không phải bằng chứng CRUD/validation của BE-03.

## BE-03 — Danh mục, sản phẩm và nhà cung cấp

Endpoint, operation ID, error code và field name phải được cập nhật từ OpenAPI Accepted của BE-03. Không sử dụng API draft trong tài liệu kiến trúc làm kết quả cuối cùng.

| Test ID | Requirement | Use Case | Backlog | Contract | Trường hợp kiểm thử | Kết quả mong đợi | Automated test/evidence | Trạng thái |
|---|---|---|---|---|---|---|---|---|
| `QA01-CAT-001` | `FR-CAT-01` | `UC-CAT-01` | `BE-03` | TBD | Tạo category hợp lệ bằng role có `catalog.write` | Tạo đúng scope và trả representation theo contract | Chưa có API BE-03 | Blocked |
| `QA01-CAT-002` | `FR-CAT-02`, `FR-CAT-03` | `UC-CAT-02` | `BE-03` | TBD | Tạo product hợp lệ với code, tên, category, đơn vị, barcode, giá và trạng thái | Product được tạo một lần với dữ liệu đúng | Chưa có API BE-03 | Blocked |
| `QA01-CAT-003` | `FR-CAT-05`, `BR-01` | `UC-CAT-02` | `BE-03` | TBD | Tạo product có code trùng trong cùng phạm vi | Lỗi duplicate có cấu trúc; không có record thứ hai | Chưa có API BE-03 | Blocked |
| `QA01-CAT-004` | `FR-CAT-05`, `BR-01` | `UC-CAT-02` | `BE-03` | TBD | Tạo product có barcode trùng trong cùng phạm vi | Lỗi duplicate có cấu trúc; không có record thứ hai | Chưa có API BE-03 | Blocked |
| `QA01-CAT-005` | `FR-CAT-02`, `FR-CAT-03` | `UC-CAT-02` | `BE-03` | TBD | Thiếu field bắt buộc hoặc gửi giá trị sai miền/định dạng | Lỗi validation có `code/message/fieldErrors` theo contract; DB không đổi | Chưa có API BE-03 | Blocked |
| `QA01-CAT-006` | `FR-CAT-04` | `UC-CAT-02` | `BE-03` | TBD | Tìm product bằng mã nội bộ | Trả đúng product trong scope, không rò dữ liệu scope khác | Chưa có API BE-03 | Blocked |
| `QA01-CAT-007` | `FR-CAT-04` | `UC-CAT-02` | `BE-03` | TBD | Tìm product bằng barcode | Trả đúng product trong scope | Chưa có API BE-03 | Blocked |
| `QA01-CAT-008` | `FR-CAT-04` | `UC-CAT-02` | `BE-03` | TBD | Tìm product bằng tên | Kết quả và quy tắc match/paging đúng contract | Chưa có API BE-03 | Blocked |
| `QA01-CAT-009` | `FR-CAT-01`, `FR-CAT-02` | `UC-CAT-01/02` | `BE-03` | TBD | Update hoặc ngừng sử dụng category/product theo quyền | Trạng thái đổi đúng; role thiếu quyền bị từ chối; không xóa ngoài policy | Chưa có API BE-03 | Blocked |
| `QA01-SUP-001` | `FR-REC-01` | `UC-SUP-01` | `BE-03` | TBD | Tạo supplier hợp lệ bằng role có quyền | Supplier được tạo đúng scope | Chưa có API BE-03 | Blocked |
| `QA01-SUP-002` | `FR-REC-01` | `UC-SUP-01` | `BE-03` | TBD | Thiếu field bắt buộc hoặc dữ liệu supplier sai định dạng | Lỗi validation có cấu trúc; DB không đổi | Chưa có API BE-03 | Blocked |
| `QA01-SUP-003` | `FR-REC-01` | `UC-SUP-01` | `BE-03` | TBD | Xem, sửa và ngừng sử dụng supplier theo quyền | CRUD/trạng thái đúng contract; sai quyền bị từ chối | Chưa có API BE-03 | Blocked |

## Coverage summary hiện tại

| Nhóm | Số dòng | QA Pass | QA Fail | Reported | Blocked |
|---|---:|---:|---:|---:|---:|
| BE-02 auth/RBAC/security | 17 | 0 | 0 | 16 | 1 |
| BE-03 catalog/supplier | 12 | 0 | 0 | 0 | 12 |
| Tổng | 29 | 0 | 0 | 16 | 13 |

`Reported` chỉ phản ánh handoff của owner. Summary phải được cập nhật sau mỗi lần TV4 chạy test và không được dùng bảng này để tuyên bố QA-01 hoàn thành ở trạng thái hiện tại.

## Câu hỏi cần reviewer chốt

1. OpenAPI operation ID, request/response và error code chính thức của login, me và logout là gì?
2. `catalog.read/write` áp dụng đồng nhất cho category, product và supplier hay tách permission?
3. `STOCK` có quyền ghi toàn bộ catalog hay chỉ nghiệp vụ hàng hóa cụ thể?
4. `ADMIN` luôn bị giới hạn một cửa hàng hay có scope quản trị khác?
5. Quy tắc normalize/case-sensitivity của product code và barcode trước khi kiểm tra trùng là gì?
6. Field bắt buộc và format của supplier là gì?
7. Delete là xóa vật lý hay chuyển trạng thái inactive?
