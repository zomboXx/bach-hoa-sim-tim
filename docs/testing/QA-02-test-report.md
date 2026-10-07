# QA-02 — Báo cáo kiểm thử E2E nhận–bán–báo cáo

- Kết luận: **Chưa đạt nghiệm thu P0 — automated core gate đạt, còn 3 Fail và 3 Blocked**
- Ngày chạy: 2026-10-07 (Asia/Bangkok)
- Owner chạy test: TV4 — Lê Văn Chiến
- Reviewer độc lập: TV1 — Nguyễn Đức Phát; đã review head `d18b2b3`, bản sửa inline `840a82a` chờ xác nhận lại
- Source test sau review: `840a82a1101cbef5351407d498542c74b8ae237d`
- Integration fix đã chạy gate: `654992fc8b44be9719b7fa4f82028894f2f9e6f8`
- Base tích hợp: `4a665f7b9aab5e80500b36272ff551512af91f32`

## 1. Kết quả hiện hành

Suite QA xuyên module mới chạy 5/5 Pass trên HTTP thật và PostgreSQL sạch. Nó chứng minh receipt accepted/rejected và replay, CASH checkout/invoice/report, rollback đa sản phẩm, cạnh tranh lô cuối, quyền report và oracle ledger độc lập.

TV1 đã chạy độc lập head `d18b2b3` và xác nhận root gate xanh, đồng thời nêu hai khoảng trống trong test QA-02. Bản sửa `840a82a` loại hoàn toàn đường cấu hình `SIMTIM_TEST_DB_URL` để mọi lần chạy bắt buộc dùng Testcontainers disposable, và đổi oracle report sang hóa đơn hai dòng để bắt được lỗi report join nhân bản invoice.

Lần chạy đầu đã phát hiện REP-01 dùng các tên `quantity_on_hand`, `product_batch_id`, `DEPLETED` không tồn tại trong migration hiện hành. Source QA sửa adapter theo `on_hand_quantity`, `batch_id`, `EXHAUSTED` và thêm join đầy đủ organization/store/product; lần chạy lại đạt. Đây là lỗi mà controller mock trước đó không thể bắt.

Không kết luận toàn bộ QA-02 Pass. `expectedTotal/PRICE_CHANGED`, unit precision EA, invoice privacy SALES, checkout idempotency và PWA API-mode bán hàng chưa đạt hoặc chưa có implementation để chạy.

## 2. Môi trường và lệnh

- Windows; Java 24.0.2, Maven compiler `release 21`.
- Docker Desktop 29.4.0; Testcontainers 1.21.4.
- PostgreSQL 17.11 image `postgres:17-alpine`, database `simtim_qa02_test` disposable.
- Flyway: 12 migration entries, V1–V11 + repeatable demo seed.

```powershell
Push-Location services/api
.\mvnw.cmd "-Dtest=vn.simtim.api.qa.Qa02E2eTest" test
Pop-Location
```

Kết quả sau review `E-QA02-REVIEW-840A82A`: 5 tests, 0 failure, 0 error, 0 skipped; `BUILD SUCCESS`, 49.152 giây. PostgreSQL luôn do Testcontainers tạo; suite không còn nhận URL DB ngoài.

Trước lần chạy xác nhận có hai lần khởi tạo không tới assertion: lần đầu do một phần tử `PATH` máy local ghép hai đường dẫn bằng dấu phẩy, lần hai do Docker daemon đang tắt. Sau khi chỉ loại phần tử `PATH` lỗi trong tiến trình test và khởi động Docker Desktop, suite chạy đầy đủ và đạt kết quả trên; không sửa cấu hình máy hay dùng DB thay thế.

Baseline trước thay đổi cũng đã chạy `services/api/mvnw.cmd verify`: 124 tests, 0 failure/error/skipped trên PostgreSQL 17.11; `BUILD SUCCESS` trong 2:07. Root gate sau thay đổi được ghi ở mục 5.

## 3. Oracle độc lập đã kiểm

- Hóa đơn: hai dòng APPLE KG + RICE EA; header 75.000 VND; một payment 75.000; change 5.000; hai invoice-line allocation.
- Ledger: APPLE receipt +2.500 KG/sale -1.250 KG và RICE receipt +1 EA/sale -1 EA; APPLE balance và `SUM(quantity_delta)` đều 1.250 KG.
- Revenue: API trả 75.000 và count 1, khớp hai query trực tiếp `SUM(grand_total)`/`COUNT(*)` trên `sales.invoices`. Assertion `invoice_lines=2` khiến query report join sai theo dòng không thể vô tình trả đúng.
- Rollback: giỏ gồm một dòng đủ và một dòng thiếu trả 409; invoice/payment/SALE movement đều 0, cả hai balance không đổi.
- Concurrency: hai request đồng thời cùng lấy đơn vị cuối cho kết quả một 201 + một 409; chỉ một invoice/payment/SALE movement commit.

## 4. Kết quả theo nhóm

| Nhóm | Pass | Fail | Blocked | Nhận xét |
|---|---:|---:|---:|---|
| Receipt/replay | 4 | 0 | 0 | Có rejected=100% và hậu điều kiện 0 stock |
| Inventory/quantity | 3 | 1 | 0 | Thiếu precision theo unit EA/KG |
| Sales/money/concurrency | 5 | 1 | 1 | Thiếu price-change và checkout replay key |
| RBAC/scope/privacy | 2 | 1 | 0 | SALES còn thấy invoice người khác cùng store |
| Reports | 2 | 0 | 0 | Revenue và inventory đã có DB oracle thật |
| PWA | 0 | 0 | 2 | Consumer mock/demo có, live sales API E2E chưa có |
| PRO-01B P1 | 1 | 0 | 0 | Báo riêng, không dùng để che P0 gaps |
| **Tổng traceability** | **17** | **3** | **3** | Chưa đủ điều kiện nghiệm thu P0 |

## 5. Root gate và CI

`Qa02E2eTest` có hậu tố `Test`, nên Maven Surefire tự chạy trong `mvnw verify`, và `scripts/verify.ps1` gọi Maven verify sau web gate. Không cần một script phụ có thể bị CI bỏ qua.

Lệnh local tại commit hồ sơ `6dbead254a006be815241befab21a684c004765e` (chứa source test `2e6eb1b18d3b8918887871b9101c25edc0385f6e` và integration fix `654992fc8b44be9719b7fa4f82028894f2f9e6f8`):

```powershell
pwsh -File scripts/verify.ps1
```

Kết quả `E-QA02-ROOT-20261007`:

- repository policy và Markdown links: Pass;
- web lint/format/typecheck/build: Pass;
- demo Playwright: 6/6 Pass;
- API-consumer Playwright: 36/36 Pass;
- backend Maven verify: 129 tests, 0 failure, 0 error, 0 skipped; `BUILD SUCCESS` trong 2:17.

`npm ci` hoàn tất nhưng `npm audit` báo một dependency mức `high`; root gate hiện không coi audit warning là failure. Cảnh báo này không làm thay đổi kết quả các ca QA-02, nhưng cần được dependency owner triage riêng.

Lần chạy root đầu tiên dừng ở ESLint vì merge head `4a665f7` chứa hai import `emptyOperationalState` liên tiếp trong `App.vue`. QA xóa đúng import trùng, chạy lại toàn bộ root gate và có kết quả xanh ở trên. Đây là sửa lỗi tích hợp, không thay đổi hành vi sản phẩm.

TV1 đã cung cấp independent evidence trên head `d18b2b3`: `pwsh -File scripts/verify.ps1 -SkipInstall` đạt repository policy, Markdown, demo 6/6, API consumer 36/36 và backend 129/129 trên PostgreSQL Testcontainers. URL CI của bản sửa `840a82a` phải được bổ sung sau khi push và CI hoàn tất. Không commit output build hoặc token.

TV4 đã chạy lại cùng root gate trên bản sửa `840a82a`: repository policy/Markdown/web lint-format-typecheck-build đạt, demo 6/6, API consumer 36/36 và backend 129/129; Maven `BUILD SUCCESS` trong 2:52 trên PostgreSQL 17.11 Testcontainers.

PR checkpoint: [#33](https://github.com/zomboXx/bach-hoa-sim-tim/pull/33). CI URL của head sau review sẽ được gắn vào report và Issue #24 khi workflow hoàn tất.

## 6. Quyết định trước khi approve

TV1 đã Request Changes cho hai khoảng trống test và bản sửa nằm ở `840a82a`; reviewer cần xác nhận lại sau CI. Ba Fail và ba Blocked P0 vẫn phải giao đúng owner/provider, không yêu cầu TV4 tự sửa toàn bộ. PR chỉ là checkpoint nên body phải dùng `Refs #24`, không dùng `Closes #24`; chỉ sau khi các gap merge vào main, QA chạy lại root gate sạch và gắn CI/PR evidence lên [Issue #24](https://github.com/zomboXx/bach-hoa-sim-tim/issues/24) mới được đổi các dòng tương ứng sang Pass hoặc đóng Issue.
