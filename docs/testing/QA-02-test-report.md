# QA-02 — Báo cáo kiểm thử E2E nhận–bán–báo cáo

- Kết luận: **Chưa đạt nghiệm thu P0 — automated core gate đạt, còn 3 Fail và 3 Blocked**
- Ngày chạy: 2026-10-07 (Asia/Bangkok)
- Owner chạy test: TV4 — Lê Văn Chiến
- Reviewer độc lập còn chờ: TV1 — Nguyễn Đức Phát
- Source test: `2e6eb1b18d3b8918887871b9101c25edc0385f6e`
- Integration fix đã chạy gate: `654992fc8b44be9719b7fa4f82028894f2f9e6f8`
- Base tích hợp: `4a665f7b9aab5e80500b36272ff551512af91f32`

## 1. Kết quả hiện hành

Suite QA xuyên module mới chạy 5/5 Pass trên HTTP thật và PostgreSQL sạch. Nó chứng minh receipt accepted/rejected và replay, CASH checkout/invoice/report, rollback đa sản phẩm, cạnh tranh lô cuối, quyền report và oracle ledger độc lập.

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

Kết quả `E-QA02-API-2E6EB1B`: 5 tests, 0 failure, 0 error, 0 skipped; `BUILD SUCCESS`, 43.846 giây ở lần chạy xác nhận cuối.

Baseline trước thay đổi cũng đã chạy `services/api/mvnw.cmd verify`: 124 tests, 0 failure/error/skipped trên PostgreSQL 17.11; `BUILD SUCCESS` trong 2:07. Root gate sau thay đổi được ghi ở mục 5.

## 3. Oracle độc lập đã kiểm

- Hóa đơn: header 50.000 VND; một payment 50.000; change 10.000; một invoice-line allocation.
- Ledger: receipt +2.500 KG và sale -1.250 KG; balance và `SUM(quantity_delta)` đều 1.250 KG.
- Revenue: API trả 50.000 và count 1, khớp query trực tiếp trên `sales.invoices` không join line.
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

Lệnh local tại commit `654992fc8b44be9719b7fa4f82028894f2f9e6f8` (chứa source test `2e6eb1b18d3b8918887871b9101c25edc0385f6e`):

```powershell
pwsh -File scripts/verify.ps1 -SkipInstall
```

Kết quả `E-QA02-ROOT-20261007`:

- repository policy và Markdown links: Pass;
- web lint/format/typecheck/build: Pass;
- demo Playwright: 6/6 Pass;
- API-consumer Playwright: 36/36 Pass;
- backend Maven verify: 129 tests, 0 failure, 0 error, 0 skipped; `BUILD SUCCESS` trong 2:39.

Lần chạy root đầu tiên dừng ở ESLint vì merge head `4a665f7` chứa hai import `emptyOperationalState` liên tiếp trong `App.vue`. QA xóa đúng import trùng, chạy lại toàn bộ root gate và có kết quả xanh ở trên. Đây là sửa lỗi tích hợp, không thay đổi hành vi sản phẩm.

Chưa có URL CI/PR vì nhánh chưa được push trong phiên này. Không commit output build hoặc token. Việc tự chạy của TV4 là evidence owner, không phải sign-off QA độc lập của TV1.

## 6. Quyết định trước khi approve

TV1 cần Request Changes cho ba Fail và xác nhận owner/provider cho ba Blocked. Chỉ sau khi fix merge vào main, QA rebase/merge main mới, chạy lại root gate sạch và gắn CI/PR evidence lên [Issue #24](https://github.com/zomboXx/bach-hoa-sim-tim/issues/24) thì mới đổi các dòng tương ứng sang Pass.
