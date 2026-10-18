# QA-02 — Bằng chứng retry checkout khi proxy trả 502 ngày 18/10/2026

- QA owner: TV4 — Lê Văn Chiến (`@VanChien11-02`)
- Reviewer: TV1 — Nguyễn Đức Phát (`@zomboXx`)
- Base được review: `0972629`; bản sửa `6ff393c` nằm trên PR [#53](https://github.com/zomboXx/bach-hoa-sim-tim/pull/53)
- Theo dõi: [Issue #34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34), `Refs #34 — S2-GAP-05, S2-GAP-06`
- Runtime: Windows, Docker Desktop 29.8.2, PostgreSQL 17.11 disposable

## Phạm vi sửa

`ApiSaleAdapter.checkout()` biểu diễn mọi HTTP response lỗi bằng `SaleApiError`. PWA nay phân loại kết quả checkout trước khi xóa operation:

- HTTP `5xx` là kết quả chưa chắc chắn: giữ nguyên giỏ, tiền khách đưa, payload và `Idempotency-Key` trong `sessionStorage`; khóa sửa giỏ và cho phép bấm lại cùng operation.
- HTTP `4xx` là giao dịch đã bị từ chối: xóa pending operation để người bán sửa giỏ/báo giá và tạo checkout mới.
- Checkout thành công mới xóa pending operation, làm rỗng giỏ và mở hóa đơn.

Consumer test mô phỏng POST đầu nhận `502 TEMPORARILY_UNAVAILABLE`, sau đó bấm lại và so sánh hai request có cùng body/key. Live test gửi POST đầu tới backend thật để transaction commit, thay response trả về trình duyệt bằng HTTP 502, rồi bấm lại. Request thứ hai dùng cùng body/key và backend trả invoice đã lưu từ idempotency record.

## Lệnh và kết quả local

```powershell
cd apps/web
npm run lint
npm run format:check
npm run build
npm run test:e2e:api
cd ../..
pwsh -File scripts/test-qa02-gap05-live.ps1
pwsh -File scripts/test-qa02-gap06-live.ps1
```

Kết quả:

- Lint, format, typecheck và production build: **Pass**.
- API consumer: **50/50 ca in `ok`**, gồm ca HTTP 502 mới và ca mất kết nối cũ. Trên Windows, tiến trình Vite preview không tự thoát sau khi Playwright in đủ 50 kết quả nên được dừng thủ công; không có ca test đỏ.
- GAP-05 live: **2/2 Pass**. Suite gửi ba checkout request cho hai operation bán; request replay sau HTTP 502 giữ nguyên payload và `Idempotency-Key`.
- Oracle GAP-05: `receipt_delta=0; invoice_delta=2; payment_delta=2; sale_movement_delta=2; revenue_delta=80000; balance_mismatches=0`. Vì chỉ có hai operation, replay không tạo invoice/payment/movement thứ hai.
- GAP-06 live: **1/1 Pass**; oracle `receipt_delta=1; invoice_delta=1; payment_delta=1; sale_movement_delta=1; revenue_delta=40000; balance_mismatches=0`.
- GitHub Actions [`baseline-quality` run 39128836102](https://github.com/zomboXx/bach-hoa-sim-tim/actions/runs/39128836102) trên commit `6ff393c` đạt **5/5 job**: Repository policy, Web baseline, API bootstrap, QA-02 live receive-sale-report và Demo Compose.

## Điều kiện tích hợp

Đây là evidence trên head PR trước merge. Không sửa kết luận lịch sử ngày 07/10 và chưa tích checkbox #34. Sau khi head mới có CI xanh và PR merge, TV4 chạy lại trên `main`, gắn URL CI/PR vào Issue #34; TV1 review evidence rồi mới chuyển từng GAP sang Pass hoặc đóng Issue.
