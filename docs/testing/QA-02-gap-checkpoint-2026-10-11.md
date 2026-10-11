# QA-02 — Checkpoint tái hiện sáu GAP tại Issue #34

- Ngày lập: 2026-10-11 (Asia/Bangkok)
- Owner QA: TV4 — Lê Văn Chiến (`@VanChien11-02`)
- Reviewer: TV1 — Nguyễn Đức Phát (`@zomboXx`)
- Theo dõi: [Issue #34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34)
- Nhánh chuẩn bị: `test/QA-02-gap-repro`
- Base: `origin/main` tại `bc692b38ca36ada6e6ad7f07678327174485cd81`

## 1. Quyết định phạm vi

Theo phản hồi của TV1 tại Issue #34, cả sáu GAP tiếp tục nằm tại #34; không tạo lại #36–#41, không tạo Issue con mới và không đưa các GAP này vào năm đầu việc Sprint 3 đã chốt. TV4 chịu trách nhiệm tái hiện, oracle QA, traceability, consumer/live E2E và chạy lại sau khi code sửa lỗi được tích hợp. Việc này không mặc nhiên giao cho TV4 sửa toàn bộ mã sản phẩm FE/BE.

Owner, reviewer và thứ tự triển khai phần sửa sản phẩm vẫn chờ TV1 ghi tại #34. Vì chưa có PR sửa lỗi đã merge và chưa có CI của bản sửa, sáu GAP dưới đây đều giữ trạng thái chưa hoàn thành; checkpoint này không thay thế kết luận lịch sử **17 Pass, 3 Fail, 3 Blocked** ngày 07/10/2026.

## 2. Test tái hiện và oracle

| GAP | Test/bước tái hiện | Expected và oracle độc lập | Trạng thái checkpoint |
|---|---|---|---|
| S2-GAP-01 — unit precision | `Qa02GapReproduction#gap01_fractionalEaReceiptIsRejectedWithoutAnyWrite` gửi receipt EA `0.500` | HTTP 422; query trực tiếp xác nhận 0 receipt line, batch, balance và movement của sản phẩm | **Fail tái hiện**: HTTP 201 và mỗi bảng đều có 1 record |
| S2-GAP-02 — price changed | `#gap02_priceChangedAfterQuoteRejectsCheckoutAndRollsBack`: quote APPLE, đổi giá hiệu lực, checkout với `expectedTotal` cũ | HTTP 409 `PRICE_CHANGED`; 0 invoice/payment/SALE movement; balance không đổi | **Fail tái hiện**: HTTP 201 theo giá mới 41.000; invoice/payment/movement được ghi, balance giảm |
| S2-GAP-03 — invoice privacy | `#gap03_salesCannotReadAnotherSellerInvoiceInTheSameStore`: SALES A bán, SALES B cùng store list/detail | List không chứa invoice; detail 404; DB vẫn chứng minh invoice tồn tại và `sold_by` khác B | **Fail tái hiện**: SALES B thấy invoice trong list và detail trả 200 |
| S2-GAP-04 — checkout idempotency | `#gap04_checkoutReplayWritesOneInvoiceAndRejectsKeyReuseWithAnotherPayload`: replay cùng key rồi đổi payload | Cùng invoice ID; đúng 1 invoice/payment/SALE movement; balance chỉ trừ một lần; payload khác trả 409 `IDEMPOTENCY_KEY_REUSED` | **Fail tái hiện**: ba request tạo 3 invoice/payment/movement và trừ hết 4 KG |
| S2-GAP-05 — PWA sales API | `qa02-sales-report.live.spec.ts`: SALES phải thấy route bán/hóa đơn, gửi quote rồi checkout CASH và hiển thị invoice API | Browser bắt được đúng `POST /sales/quote` và `POST /sales/checkout`; invoice hiển thị từ response thật | Expected-red đã viết; API mode hiện không cấp route sales và `pay()` chỉ chạy demo |
| S2-GAP-06 — live receive–sell–report | Kết hợp `inventory-recovery.live.spec.ts`, ca sales ở trên và ca manager report trong `qa02-sales-report.live.spec.ts` | Cùng backend/PostgreSQL sạch; report browser lấy `/reports/revenue` và `/reports/inventory`; bước cuối phải đối chiếu DB invoice/ledger bằng QA API suite | Một phần đã có; toàn chuỗi còn Blocked bởi S2-GAP-05 và chưa được đưa vào root CI |

Các test expected-red là đặc tả chạy được, không phải bằng chứng Pass. `Qa02GapReproduction` không có hậu tố `Test`, nên root gate hiện chỉ compile mà không tự chạy bốn lỗi sản phẩm chưa được sửa. Khi từng provider PR đã merge, ca tương ứng phải được chuyển vào provider suite hoặc `Qa02E2eTest` có hậu tố `Test` để root gate thực thi trước khi đổi trạng thái.

## 3. Lệnh chạy

```powershell
Push-Location services/api
.\mvnw.cmd "-Dtest=vn.simtim.api.qa.Qa02GapReproduction" test
Pop-Location

Push-Location apps/web
$env:API_PROXY_TARGET = "http://127.0.0.1:8080"
$env:SIMTIM_LIVE_PASSWORD = "<local-demo-password>"
npm run test:e2e:live -- qa02-sales-report.live.spec.ts
Pop-Location

pwsh -File scripts/verify.ps1 -SkipInstall
```

Không ghi mật khẩu thật, token, DB dump, `target/` hoặc Playwright output vào Git. Backend test chỉ dùng PostgreSQL 17 disposable do Testcontainers tạo. Live PWA phải trỏ tới API local dùng DB test sạch, không dùng DB vận hành.

## 4. Evidence ngày 11/10

- `testCompile`: **Pass** — 13 test source files compile bằng Java release 21 trên JDK 24.
- Lần chạy `Qa02E2eTest` đầu: **Error trước assertion** — một phần tử `PATH` local ghép hai đường dẫn bằng dấu phẩy; đã xác nhận có thể lọc riêng trong tiến trình, không sửa cấu hình máy.
- Sau khi khởi động Docker Desktop 29.4.0 và lọc `PATH` trong riêng tiến trình test, `Qa02GapReproduction` chạy trên PostgreSQL 17.11 disposable, Flyway V1–V14 + demo seed: **9 tests, 4 failure, 0 error, 0 skipped** trong 1:53. Năm ca kế thừa từ `Qa02E2eTest` đạt; cả bốn expected-red GAP backend đều Fail đúng khoảng trống đã ghi, không phải lỗi khởi tạo.
- Live PWA specs: đã compile/lint/typecheck nhưng chưa chạy vì cần API local cùng tài khoản demo/password do người chạy cung cấp. Hai GAP PWA vẫn Blocked và chưa được phép ghi Pass.
- Root gate `pwsh -File scripts/verify.ps1 -SkipInstall`: **Pass** — repository policy và Markdown links đạt; web lint/format/typecheck/build đạt; demo Playwright 14/14; API consumer Playwright 39/39; backend Maven 152/152, 0 failure/error/skipped trên PostgreSQL 17.11 Testcontainers; `BUILD SUCCESS` trong 6:18. Kết quả này xác nhận checkpoint không làm đỏ gate hiện hành nhưng không được dùng để đổi bốn expected-red thành Pass.
- PR/CI URL: chưa có vì checkpoint mới chỉ ở local; sau khi push/mở PR phải dùng `Refs #34`, gắn CI URL vào PR và Issue #34, không dùng `Closes #34`.

## 5. Điều kiện cập nhật từng ô tại #34

Chỉ tích một GAP khi đồng thời có: owner/reviewer được TV1 ghi rõ, PR sửa lỗi đã merge, test provider/consumer tương ứng vào root gate và đạt, QA chạy lại trên PostgreSQL sạch với query hậu điều kiện, và URL PR/CI được ghi tại #34. Issue #34 chỉ đóng khi cả sáu điều kiện đều hoàn tất; trạng thái Issue/PR đơn lẻ không đủ để suy ra Pass.
