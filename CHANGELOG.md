# Changelog

Các thay đổi đáng chú ý của dự án được ghi tại đây. Tài liệu này mô tả sản phẩm và repository; việc ghi nhận trách nhiệm cá nhân nằm trong `CONTRIBUTION_LOG.md`.

## Unreleased

### Changed

- Cập nhật điểm vào tài liệu dự án và mốc tiến độ ngày 30/09/2026; phân biệt bản nháp lịch sử với backlog/contract hiện hành, sửa trạng thái REQ-01 và Sprint 1 đã tích hợp, ghi rõ các PR Sprint 2 còn đang review.
- Trình bày owner/reviewer trên hai dòng riêng trong Issue Sprint 2, bỏ metadata lặp với tiêu đề/nhãn; cân lại phân công: TV4 nhận FE-02, TV2 nhận REP-01, TV3 tập trung SAL-01, TV1 điều phối và review.
- Hoàn thiện kickoff Sprint 2 trên GitHub: milestone hạn 07/10/2026, bảy Issue có owner/reviewer và tiêu chí kiểm thử; tách FE-02 để nối giao diện nhận/tồn với API, làm rõ contract giao dịch và cập nhật hướng dẫn bắt đầu theo nền Sprint 1 đã tích hợp.
- Đối chiếu từng yêu cầu với backlog và báo cáo tuần 4 cho REQ-01; ghi rõ phần chấp nhận/hoãn/loại theo xác nhận của Project Owner ngày 29/09/2026, chờ nhóm review và tích hợp, giữ bản nháp cũ để truy vết và cho phép ghi quyết định trên Issue/PR thay vì dựng biên bản cuộc họp.

- Ghi nhận Project Owner thông báo nhóm đã review và đồng ý hướng DB-01 gồm 39 bảng đích và migration theo sprint; ba ERD đã đồng bộ, migration Sprint 1 đạt local và CI PostgreSQL 17, đã tích hợp qua PR #11.
- Làm rõ tiêu chí Sprint 1 theo báo cáo Word hiện hành và ghi riêng hai ứng viên quản trị tài khoản, lịch sử giá chưa được lên lịch.
- Theo quyết định Project Owner ngày 26/09/2026, Sprint 1 dùng bốn vai trò server; người học là trạng thái của nhân viên.
- Ghi nhận phương án tiền nguyên VND và số lượng lẻ tối đa ba chữ số thập phân để DB-01 review; giới hạn xử lý hàng nhận sai lệch trong MVP ở việc ghi số lượng cùng lý do trên phiếu.
- Chốt kiến trúc Sprint 1 theo module nghiệp vụ: frontend feature modules và backend modular monolith với ports/adapters.
- Chuẩn hóa GitHub Flow theo Issue ngắn hạn, Pull Request có reviewer và GitHub Project làm nguồn trạng thái vận hành.
- Giữ `AGENTS.md` làm hướng dẫn portable; ngừng track cấu hình `.agents`, `.codex` và bản Word đang soạn, đồng thời chặn chúng bằng repository policy trong CI.

### Added

- **FE-02**: Tách feature nhận hàng/tồn kho Vue theo ADR 0003 với demo/API adapter riêng; API mode xác nhận phiếu bằng Bearer session và `Idempotency-Key`, đọc phiếu, tồn sản phẩm, lô/hạn và movement/source từ INV-01/02. Form chỉ chọn catalog ACTIVE, kiểm tra quantity tối đa ba chữ số thập phân, quan hệ giao/nhận/từ chối, VND nguyên và hiển thị field error server; màn hình responsive hiển thị on-hand/available cùng trạng thái hạn theo ngày nghiệp vụ Việt Nam.
- Bổ sung consumer/E2E FE-02 cho form hợp lệ/sai, pending/double-click, 401/403, timeout hoặc 5xx qua reload, GET đối soát không tự POST, retry cùng idempotency key, phân trang tồn, phiếu nhiều dòng, đọc lại tồn sau nhận và quyền SALES không lộ giá nhập.
- Xác minh FE-02 bằng root gate trên PostgreSQL 17 Testcontainers: repository policy và Markdown links đạt, web lint/format/typecheck/build đạt, demo E2E 6/6, API consumer/E2E 35/35 và backend integration 79/79; Maven kết thúc `BUILD SUCCESS`. Live Playwright với Spring API/PostgreSQL thật đạt 1/1.
- **INV-02**: Bổ sung API chỉ đọc tồn hiện tại theo sản phẩm, lô/hạn và biến động có chứng từ nguồn, giới hạn theo cửa hàng trong session, phân trang ổn định và không lộ giá vốn cho SALES. Công bố `InventorySalePort` chạy trong transaction của sales để khóa và lập kế hoạch FEFO, trừ tồn, ghi biến động SALE; bổ sung migration index, OpenAPI, provider test PostgreSQL và consumer compile test cho SAL-01.
- **INV-01**: Transaction nhận hàng — `POST /api/v1/inventory/receipts`, `GET /inventory/receipts`, `GET /inventory/receipts/{id}`. Xác nhận phiếu tạo lô, tăng tồn và ghi biến động RECEIPT nguyên tử trong một transaction; idempotency qua `Idempotency-Key` header; quyền `receipts.write`/`receipts.read` cho STOCK, MANAGER, ADMIN. Flyway V5 (inventory schema + audit.audit_logs) và V6 (grants). Provider integration test với Testcontainers.
- **SAL-01**: Transaction bán hàng CASH và hóa đơn chuẩn hóa theo wire `/api/v1/sales/...` (gồm alias `POST /api/v1/sales/checkout`) và permission `sales.*` — server tính giá từ `catalog.product_prices`, chọn lô còn hạn FEFO qua public boundary port `InventoryPort` (`vn.simtim.api.inventory.application`), lưu `sales.invoices` / `invoice_lines` / `invoice_line_batches` / `payments` (hỗ trợ đơn 0 VND và tính `change_amount`) và giảm `inventory.inventory_balances` nguyên tử trong một transaction. Bắt buộc kiểm tra phạm vi cửa hàng theo session token. Bổ sung Working Draft wire contract [sales-invoices.openapi.yaml](contracts/sales-invoices.openapi.yaml) và ma trận quyền [SALES_INVOICE_REVIEW.md](contracts/SALES_INVOICE_REVIEW.md). Flyway V7 tạo sales schema (sau INV-01 V5/V6). Permissions `sales.read` (tất cả vai trò, gồm cả preview quote) và `sales.write` (SALES, MANAGER, ADMIN cho checkout). 19 integration test trên PostgreSQL 17 Testcontainers đạt (tổng 50/50 pass). Defer: applied_promotion_id FK sang promotions (PRO-01B).
- Chuẩn bị bộ review Sprint 2 gọn gồm kế hoạch triển khai và contract ranh giới module; ghi các quyết định nghiệp vụ đã xác nhận, owner/reviewer và điểm kỹ thuật còn phải chốt trước code.
- BE-02 đã tích hợp qua PR #14: login/session/logout với opaque Bearer session thu hồi được, BCrypt, bốn vai trò và permission catalog read/write; server chặn giả mạo phạm vi và tải lại quyền/tài khoản ở mỗi request.
- OpenAPI session/RBAC đã Accepted ngày 29/09/2026, thay thế trạng thái Draft trước đó; login có rate limit và tài khoản demo chỉ được tạo khi người chạy chủ động cung cấp mật khẩu qua môi trường, không có mật khẩu mặc định.
- Bổ sung regression integration test cho barcode sản phẩm trùng; kiểm tra `409` và hậu điều kiện không tạo thêm record.
- Bổ sung hồ sơ QA-01 gồm test plan, ma trận quyền, ma trận requirement–contract–test và checkpoint report; tách bằng chứng owner báo cáo khỏi kết quả QA trực tiếp xác nhận.
- Khởi tạo API Spring Boot, Maven Wrapper, Flyway schema nền, PostgreSQL Compose và integration test health/migration cho `BE-01` trên nhánh triển khai.
- Chuẩn bị DDL đích 38 bảng từ báo cáo Word và một bảng phiên đăng nhập đề xuất, kèm đánh giá 3NF và kiểm tra ràng buộc cho DB-01; V2/V3 chỉ đưa 14 bảng Sprint 1 vào Flyway.
- Thêm migration core/IAM/catalog, seed demo chỉ theo profile và test ràng buộc vai trò/giá từ database sạch.
- Bổ sung Issue forms và runbook cấu hình GitHub cho Project Owner.
- Chuẩn bị bản đối chiếu và các quyết định còn thiếu cho REQ-01, giữ trạng thái draft tới khi nhóm xác nhận.
- Chuẩn bị bản đối chiếu báo cáo Word với backlog Sprint 1 để bổ sung chi tiết theo từng sprint mà không tự mở rộng cam kết MVP.

### Fixed

- FE-02 recovery không còn quét tối đa 10.000 phiếu ở client. `GET /api/v1/inventory/receipts` nhận `clientOperationId`, lọc theo organization/store của Bearer session; provider test xác nhận POST đã commit vẫn tìm lại được sau khi mất response và không rò phiếu cửa hàng khác. Bổ sung live Playwright test với Spring API/PostgreSQL 17 thật để chứng minh reload chỉ GET và không POST lần hai.
- API mode không còn nạp dữ liệu vận hành demo từ IndexedDB cho màn hình nhận/tồn hoặc fallback khi API lỗi. Token và session scope vẫn do auth adapter giữ trong bộ nhớ; `401` đưa người dùng về đăng nhập, còn nhận hàng không tạo offline queue.
- REP-01: Giải quyết xung đột phiên bản Flyway sau khi merge INV-01/INV-02/SAL-01 (đổi thành `V9__reports_permissions.sql`), cấp quyền `reports.read` cho vai trò MANAGER/ADMIN trong demo seed, bổ sung dọn `services/api/target` trong `scripts/clean.ps1` và hỗ trợ tự nhận diện named pipe Docker Desktop trên Windows trong `scripts/verify.ps1`.
- Hoàn tất checkpoint QA-01 sau Request Changes: cập nhật wire auth `/api/v1/auth/*`, đối chiếu đúng test method, ghi evidence local trên source tích hợp BE-02/BE-03 và xác nhận 31/31 API tests cùng 29/29 traceability cases đạt.
- FE-01 căn chỉnh auth adapter với contract BE-02: scope doanh nghiệp/cửa hàng, `/api/v1/auth/*`, Bearer token và session do server trả về. Thay thế đề xuất `/api/auth/login` và `/api/me` trước đó; contract đã Accepted ngày 29/09/2026 theo provider/consumer review, FE-01 chờ tích hợp PR #12.
- Sửa gate tích hợp BE-02/BE-03: tách route fixture kiểm thử quyền khỏi controller sản phẩm thật; test catalog đăng nhập HTTP bằng tài khoản fixture và gửi Bearer token, kiểm tra thiếu token, SALES ghi dữ liệu và giả mạo scope trên controller thật.
- Tách lại hai entry FE-01/BE-03 trong nhật ký đóng góp sau khi giải quyết conflict PR #12, giữ nguyên nội dung và thời gian ghi nhận.
- Token API chỉ giữ trong bộ nhớ; reload yêu cầu đăng nhập lại, đăng xuất thu hồi phiên server. Khóa form trong các thao tác auth và bỏ qua phản hồi khôi phục cũ sau đăng nhập/đăng xuất; không khôi phục vai trò API từ tài khoản demo.
- Điều hướng API dùng permissions và cờ đào tạo từ server, hỗ trợ hiển thị bốn role BE-02. Các giao dịch demo vẫn chạy trong demo mode; API mode chỉ xem dữ liệu minh họa cho tới khi adapter nghiệp vụ tương ứng được tích hợp.
- Bổ sung proxy API cho Vite dev/preview và consumer/E2E cho DTO, Bearer, permissions, logout, phản hồi chậm, token không lưu bền, HTTP lỗi và lỗi mạng.
- Cho CI tải parent commit trước khi kiểm tra whitespace, tránh quét nhầm toàn bộ baseline trong shallow checkout; đồng thời chỉ chạy `push` gate trên `main` để không lặp check của Pull Request.

## 0.2.1 — 2026-09-21

Đây là bản hardening repository của baseline Sprint 0 để bắt đầu Sprint 1; không bao gồm implementation Sprint 1.

### Changed

- Tổ chức baseline theo `apps`, `services`, `contracts`, `infra`, `scripts`, `docs` và `archive`; chỉ PWA Sprint 0 là code active đã nghiệm thu.
- Ghi nhận bốn thành viên và CODEOWNERS, bổ sung ADR cùng kế hoạch kickoff để chia PR Sprint 1 theo dependency rõ ràng.
- Thêm lệnh root `setup`, `dev`, `verify`, `clean`; CI tách repository policy và web baseline gate.
- Bổ sung ESLint, Prettier và kiểm tra link nội bộ; chuẩn hóa đường dẫn tài liệu theo product, architecture, project, deliverables và archive.
- Chuẩn hóa cấu trúc repository: chuyển Prototype 01 vào `archive/prototype-v1/` và đưa changelog lên root.
- Bổ sung bản đồ tài liệu, hướng dẫn đóng góp, quy tắc agent, EditorConfig, Git attributes và cấu hình Codex an toàn.
- Thêm skill `verify-sim-tim` và lệnh `npm run verify` dùng chung giữa máy phát triển và CI.

### Fixed

- Bổ sung key ổn định cho các danh sách Vue/SVG để tránh tái sử dụng DOM sai khi dữ liệu thay đổi.

## 0.2.0 — 2026-09-14

### Added

- Prototype 02 bằng Vue 3, TypeScript và Vite với 11 màn hình theo ba vai trò demo.
- Luồng nhận hàng, tồn, bán hàng, hóa đơn, báo cáo và khuyến mãi dùng chung dữ liệu IndexedDB.
- Kiểm kê theo lô, hàng đợi offline mô phỏng, phát hiện xung đột và bước quản lý duyệt.
- Một ca đào tạo 2D bằng Vue/SVG, có bàn phím, cảm ứng, tìm đường và phản hồi hành động sai.
- Playwright E2E và GitHub Actions cho build cùng kiểm thử browser.

### Fixed

- Giới hạn Service Worker cache ở static assets công khai để tránh lỗi response có `Vary: Origin`.

### Known limitations

- Chưa có REST backend, xác thực server, PostgreSQL hoặc runtime Godot trong baseline này.
- Quyền frontend, thanh toán và đồng bộ máy chủ chỉ phục vụ trình diễn.

## 0.1.0 — 2026-09-10

### Added

- Prototype HTML/CSS/JavaScript đầu tiên cho màn hình nhân viên bán hàng và khu đào tạo bảy chapter.

Prototype này hiện được lưu tại `archive/prototype-v1/`.
