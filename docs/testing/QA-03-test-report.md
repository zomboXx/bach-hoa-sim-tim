# QA-03 — Test report desktop/mobile/offline

- **Ngày lập:** 15/10/2026
- **Owner:** TV4 — Lê Văn Chiến (`@VanChien11-02`)
- **Reviewer cần xác nhận:** TV3 — Nguyễn Văn Thi (`@thinguyen135`)
- **Source under test:** `44b341b` trên branch `test/QA-03-desktop-mobile-offline`, tạo từ `origin/main` `19104cb`
**Kết luận local:** Pass; còn chờ CI và reviewer sau khi push PR

## Kết quả thực thi

| Lệnh | Môi trường | Kết quả |
|---|---|---|
| `pwsh -NoProfile -File scripts/test-qa03-live.ps1` | Chrome; PWA production build; API thật; PostgreSQL 17.11 disposable | `2/2 Pass` |
| `mvnw.cmd "-Dtest=InventoryReadApiTest,StocktakeSyncApiTest,StocktakeApprovalApiTest" test` | Java 24 compile target 21; ba PostgreSQL 17.11 Testcontainers sạch | `31/31 Pass`, `BUILD SUCCESS` |
| `pwsh -File scripts/verify.ps1 -SkipInstall` | Root gate | Policy/link/lint/format/build Pass; web demo `14/14`, web API `39/39`, backend `152/152`; `BUILD SUCCESS` |

Live suite cố ý làm mất phản hồi POST đầu sau khi server đã commit, giữ queue qua reload rồi gửi lại cùng `clientOperationId`. Server trả lại line cũ; quản lý duyệt qua API; oracle đọc lại API/PostgreSQL xác nhận balance gạo `100.000 → 98.000`, version tăng `0 → 1`, đúng một movement `STOCKTAKE_ADJUSTMENT` delta `-2.000`. Ca thứ hai xác nhận SALES nhận `403` và snapshot táo cũ nhận `409 CONFLICT` sau một checkout thật, không tạo adjustment kiểm kê.

## Viewport evidence

- Mobile `390 × 844`, offline sau khi lưu và trước reconnect: [`mobile-offline-390x844.png`](evidence/qa-03/mobile-offline-390x844.png).
- Desktop `1366 × 768`, sau duyệt: [`desktop-approved-1366x768.png`](evidence/qa-03/desktop-approved-1366x768.png).
- Regression tự động bổ sung `360 × 800`, `768 × 1024`, `1366 × 768` trong `stocktake-responsive.spec.ts`.

Hai ảnh là bằng chứng chọn lọc; trace và screenshot khi lỗi nằm trong thư mục tạm của runner rồi được dọn, không commit Playwright output theo repository policy.

## Lỗi tích hợp phát hiện và sửa

1. PWA API mode vẫn khóa ghi dù SYN-02/INV-03 đã Accepted: nối queue với route open/count, giữ scope và idempotency.
2. API lô không trả `version`: bổ sung field additive để client gửi `baseVersion` thật.
3. API sản phẩm không trả `unitCode`: bổ sung field additive để precision EA/KG không dựa vào đoán.
4. Component reload inventory ngay sau submit và có thể che phiếu bằng loading/timeout dù submit đã thành công: chỉ reload sau bước duyệt, khi tồn thực sự thay đổi.
5. Movement kiểm kê chưa có label consumer: ánh xạ `STOCKTAKE_ADJUSTMENT`/`STOCKTAKE` trong trang tồn.
6. Đồng bộ sau login và sau save có thể chồng nhau rồi bỏ qua queue vì dùng cờ `busy` chung: serialize bằng promise riêng và kiểm lại queue sau lượt đang chạy.

## Ngoại lệ và quyết định còn chờ

- CI và review độc lập của TV3 chỉ có sau push; báo cáo local không tự thay approval.
- API liệt kê tất cả phiên chờ duyệt cho màn quản lý chưa nằm trong contract Accepted; QA-03 duyệt bằng session ID nhận từ API live, không tự thêm route ngoài contract.
- API lưu phiên/kết quả đào tạo thuộc Sprint 4; QA-03 chỉ giữ regression cách ly dữ liệu demo hiện hành.
- Sáu ngoại lệ QA-02 tiếp tục theo dõi riêng tại [#34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34); không thay đổi trạng thái trong báo cáo này.
