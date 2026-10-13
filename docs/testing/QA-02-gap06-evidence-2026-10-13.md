# QA-02 — Bằng chứng S2-GAP-06 ngày 13/10/2026

- QA owner: TV4 — Lê Văn Chiến (`@VanChien11-02`)
- Reviewer: TV1 — Nguyễn Đức Phát (`@zomboXx`)
- Provider/consumer head đã kiểm: `7406e2c` trên PR [#53](https://github.com/zomboXx/bach-hoa-sim-tim/pull/53)
- Theo dõi: [Issue #34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34), `Refs #34 — S2-GAP-06`
- Runtime: Windows, JDK 24 chạy Java release 21, Docker Desktop 29.4.0, PostgreSQL 17.11 disposable

## Phạm vi kiểm

`qa02-receive-sale-report.live.spec.ts` chạy một luồng browser duy nhất trên cùng API và PostgreSQL sạch:

1. STOCK đăng nhập, xác nhận một phiếu nhận và browser chứng minh chỉ gửi một POST.
2. SALES đăng nhập, quote rồi checkout CASH qua API thật và đọc lại hóa đơn.
3. MANAGER đăng nhập, đọc revenue/inventory report; revenue và invoice count phải khớp hóa đơn vừa checkout.
4. Runner query PostgreSQL độc lập trước/sau luồng để kiểm receipt, invoice, payment, doanh thu và `inventory_balances.on_hand_quantity = SUM(stock_movements.quantity_delta)` theo batch.

Runner chờ demo inventory seed hoàn tất trước khi chụp baseline. Điều này tránh tính dữ liệu seed chạy trong `ApplicationRunner` thành dữ liệu do test tạo, nhưng không nới oracle: mỗi lần chạy vẫn bắt buộc tăng đúng một receipt, một invoice, một payment và không có batch lệch ledger.

## Lệnh và kết quả local

```powershell
pwsh -File scripts/test-qa02-gap05-live.ps1
pwsh -File scripts/test-qa02-gap06-live.ps1
pwsh -File scripts/verify.ps1 -SkipInstall
```

Kết quả:

- GAP-05 live consumer: **2/2 Pass** — quote/checkout/invoice API và mất response không tự POST lần hai.
- GAP-06 live flow: **1/1 Pass** — nhận → bán CASH → hóa đơn → báo cáo trên cùng API/DB.
- DB oracle: `receipt_delta=1; invoice_delta=1; payment_delta=1; revenue_delta=40000; balance_mismatches=0`.
- Root gate: repository policy/Markdown/lint/format/typecheck/build Pass; demo Playwright **14/14**; API consumer **49/49**; backend Maven **165/165**, 0 failure/error/skipped; riêng `Qa02E2eTest` **11/11** trên PostgreSQL 17.11 Testcontainers; `BUILD SUCCESS` trong 4:09.

Lần chạy oracle đầu tiên dừng sau khi browser Pass vì giả định demo seed không có receipt. QA không dùng kết quả đó làm Pass; runner được sửa để chờ seed hoàn tất và so sánh delta trước/sau, sau đó chạy lại toàn bộ GAP-06 đạt kết quả phía trên.

## CI và điều kiện đóng

Workflow `baseline-quality` có job `QA-02 live receive-sale-report`: dựng Compose sạch, chạy test `@qa02-gap06`, query cùng file `qa02-gap06-db-summary.sql` trước/sau và luôn xóa volume khi kết thúc. URL run phải được bổ sung vào PR #53 và Issue #34 sau khi GitHub Actions hoàn tất.

Đây là bằng chứng QA trên head PR, chưa phải bằng chứng sau merge. Theo quy tắc #34, chưa đổi các dòng lịch sử ngày 07/10 hoặc tích checkbox chỉ từ kết quả local này. Sau khi PR #53 merge vào `main`, TV4 phải chạy lại GAP-06/root gate trên `main`, gắn URL CI/PR và TV1 review trước khi chuyển GAP sang Pass hoặc đóng #34.
