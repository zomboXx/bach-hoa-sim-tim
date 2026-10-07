# QA-02 — Kế hoạch kiểm thử E2E nhận–bán–báo cáo

- Trạng thái: **Đang thực thi — chưa đủ điều kiện nghiệm thu P0**
- Ngày cập nhật: 2026-10-07
- Owner: TV4 — Lê Văn Chiến (`@VanChien11-02`)
- Reviewer: TV1 — Nguyễn Đức Phát (`@zomboXx`)
- Issue: [QA-02 #24](https://github.com/zomboXx/bach-hoa-sim-tim/issues/24)
- Nhánh: `feature/QA-02-e2e`

## 1. Mục tiêu và oracle

Chứng minh bằng HTTP thật và PostgreSQL độc lập luồng tạo dữ liệu danh mục/giá hợp lệ → nhận hàng → tồn/lô → quote/checkout CASH → hóa đơn → báo cáo. Response API chỉ là đầu vào quan sát; kết luận transaction, số tiền và tồn phải được đối chiếu thêm từ các bảng nguồn.

Oracle bắt buộc:

- invoice/report: `SUM(sales.invoices.grand_total)` và `COUNT(*)` trên hóa đơn `COMPLETED`, không join nhân bản dòng;
- stock: `inventory.inventory_balances.on_hand_quantity = SUM(inventory.stock_movements.quantity_delta)` theo lô;
- atomicity: khi checkout lỗi, invoice/line/allocation/payment/SALE movement không được tạo và mọi balance giữ nguyên;
- concurrency: hai checkout tranh đơn vị cuối chỉ có một giao dịch commit;
- quyền: đi qua Bearer security chain thật, không tắt filter hoặc tự dựng principal trong test E2E.

## 2. Fixture cách ly

`Qa02E2eTest` bắt buộc tự khởi động PostgreSQL 17 disposable qua Testcontainers, không đọc URL/user/password DB từ biến môi trường. Flyway chạy từ V1 đến V11 và demo seed chỉ chứa catalog nền. Suite tạo bốn user fixture SALES/STOCK/MANAGER/ADMIN, đăng nhập qua HTTP, dọn chứng từ theo thứ tự khóa ngoại trước từng ca và xóa user/session khi kết thúc. Vì không có đường dẫn tới DB ngoài, các lệnh dọn fixture không thể chạy nhầm trên DB dùng chung hoặc DB vận hành.

Fixture nghiệp vụ có:

- EA (`precision_scale=0`) và KG (`precision_scale=3`);
- lô hết hạn hôm qua, hôm nay, +7, +8, không hạn và BLOCKED trong provider suite INV-02;
- nhận đủ, nhận sai lệch/từ chối toàn bộ, replay cùng key/payload;
- giá có hiệu lực, quantity lẻ KG và hóa đơn 0 VND trong SAL-01/PRO-01B suite;
- không truy cập DB vận hành, không giữ token/URL/password trong evidence commit.

## 3. Tầng kiểm thử

| Tầng | Suite | Mục đích |
|---|---|---|
| Provider API | `InventoryReceiptApiTest`, `InventoryReadApiTest`, `SaleApiTest` | Quy tắc receipt, hạn/FEFO, tiền, invoice và promotion |
| QA xuyên module | `Qa02E2eTest` | Luồng HTTP thật, rollback, cạnh tranh, oracle ledger/report độc lập |
| Consumer PWA | `apps/web/tests/api-*.spec.ts` | Auth, nhận/tồn, báo cáo và promotion với mock wire |
| Demo regression | `apps/web/tests/workflows.spec.ts` | Giữ luồng demo nhận–bán–hóa đơn và training cách ly |
| Root gate | `scripts/verify.ps1` | Policy, links, web checks/E2E và Maven verify cùng một lệnh |

## 4. Ca bắt buộc

Chi tiết requirement → contract → test → evidence nằm tại [QA-02-traceability.md](QA-02-traceability.md). Nhóm P0 gồm receipt accepted/rejected/replay, expiry/blocked/FEFO, precision/rounding, price changed, insufficient stock, double checkout, checkout replay, RBAC/store scope, invoice privacy, report reconciliation và PWA API mode.

PRO-01B là P1 và có nhóm regression riêng. P1 không chặn kết luận P0 nếu chưa merge; tại source hiện tại PR #25 đã có trong lịch sử `origin/main`, nhưng QA vẫn báo riêng.

## 5. Lệnh và evidence

```powershell
Push-Location services/api
.\mvnw.cmd "-Dtest=vn.simtim.api.qa.Qa02E2eTest" test
Pop-Location

pwsh -File scripts/verify.ps1 -SkipInstall
```

Evidence local là Maven/Surefire và Playwright output của đúng source SHA; không commit `target/`, Playwright report, token hoặc database dump. Evidence CI/PR phải được gắn vào [Issue #24](https://github.com/zomboXx/bach-hoa-sim-tim/issues/24) sau push bởi người có quyền repository.

## 6. Quy tắc kết luận

- **Pass**: dependency provider/consumer đã merge vào source được test, QA owner trực tiếp chạy và oracle đạt.
- **Fail**: implementation đã có nhưng khác expected hoặc kiểm tra source/runtime chứng minh thiếu hành vi bắt buộc.
- **Blocked**: contract/implementation hoặc live consumer cần thiết chưa tồn tại để chạy ca hợp lệ.

TV4 tự chạy không thay thế review độc lập của TV1. Issue chỉ đủ điều kiện đóng khi mọi P0 Fail/Blocked được xử lý, root CI xanh và TV1 kiểm tra oracle/evidence.
