# QA-02 — Bằng chứng sửa review luồng bán hàng ngày 17/10/2026

- QA owner: TV4 — Lê Văn Chiến (`@VanChien11-02`)
- Reviewer: TV1 — Nguyễn Đức Phát (`@zomboXx`)
- Base được review: `7ae6112`; bản sửa nằm trên PR [#53](https://github.com/zomboXx/bach-hoa-sim-tim/pull/53)
- Theo dõi: [Issue #34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34), `Refs #34 — S2-GAP-05, S2-GAP-06`
- Runtime: Windows, JDK 24.0.2 chạy Java release 21, Docker Desktop 29.8.2, PostgreSQL 17.11 disposable

## Nội dung sửa theo review

1. PWA API mode lấy quote ngay khi giỏ thay đổi, hiển thị đơn giá và tổng từ server trước checkout, dùng tổng đó để kiểm tiền khách đưa và chỉ cho phép CASH.
2. Khi POST checkout có kết quả chưa xác định do mất response, client giữ nguyên giỏ, tiền khách đưa, payload và `Idempotency-Key` trong phiên. Nút thử lại gửi đúng operation cũ; người dùng không thể sửa giỏ trong lúc chờ đối soát.
3. Product card API mode không còn lấy tồn từ demo state hoặc gán đơn vị `Cái`; khi catalog API chưa có số lượng/đơn vị đáng tin cậy, UI ghi rõ giá và tồn được server xác nhận thay vì hiển thị sai `Còn 0 Cái`.
4. Playwright consumer và live browser test bấm thanh toán lần hai sau khi server thật đã commit nhưng response đầu bị hủy. Test so sánh key và body của hai request; runner query PostgreSQL độc lập trước/sau.

Live runner dùng build và Playwright output tạm riêng cho mỗi lần chạy. Việc này tránh file `dist`/`test-results` của một phiên Windows bị khóa làm hỏng lần chạy kế tiếp; thư mục tạm được kiểm tra nằm dưới `%TEMP%` trước khi dọn.

## Lệnh và kết quả local

```powershell
pwsh -File scripts/test-qa02-gap05-live.ps1
pwsh -File scripts/test-qa02-gap06-live.ps1
pwsh -File scripts/verify.ps1 -SkipInstall
```

Kết quả:

- GAP-05 live: **2/2 Pass**. Suite gửi ba checkout request: một sale thường, một sale đã commit nhưng mất response và một replay cùng key/payload.
- Oracle GAP-05: `receipt_delta=0; invoice_delta=2; payment_delta=2; sale_movement_delta=2; revenue_delta=80000; balance_mismatches=0`. Vì chỉ có hai operation bán, kết quả chứng minh replay không tạo invoice/payment/movement thứ hai.
- GAP-06 live: **1/1 Pass**; oracle `receipt_delta=1; invoice_delta=1; payment_delta=1; sale_movement_delta=1; revenue_delta=40000; balance_mismatches=0`.
- Root gate: repository policy, Markdown, lint, format, typecheck và build Pass; demo Playwright **16/16**; API consumer **49/49**; backend Maven **165/165**, 0 failure/error/skipped; riêng `Qa02E2eTest` **11/11** trên PostgreSQL Testcontainers.

Trong môi trường agent, lệnh root đã hoàn tất policy/web rồi Maven Wrapper bị sandbox chặn đọc cache/network. QA chạy lại đúng bước backend bằng `.\mvnw.cmd verify` với quyền Docker/Maven và nhận `BUILD SUCCESS`; đây là giới hạn môi trường thực thi, không phải test failure.

## Điều kiện tích hợp

Đây vẫn là bằng chứng trên head PR trước merge. Không sửa kết luận lịch sử ngày 07/10 và chưa tích checkbox #34. Sau khi head mới có CI xanh và PR merge, TV4 chạy lại trên `main`, gắn URL CI/PR vào Issue #34; TV1 review evidence rồi mới chuyển từng GAP sang Pass hoặc đóng Issue.
