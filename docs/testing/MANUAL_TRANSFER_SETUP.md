# Chạy thử QR chuyển khoản

Trạng thái: hướng dẫn cho tính năng trong [PR #84](https://github.com/zomboXx/bach-hoa-sim-tim/pull/84), được Project Owner review và duyệt ngày 23/10/2026. Không dùng kết quả mô phỏng như bằng chứng đã nhận tiền từ ngân hàng.

[Kết quả kiểm thử cục bộ](MANUAL_TRANSFER_TEST_REPORT.md) ghi lệnh, số ca và đối soát tồn/doanh thu; chưa phải kết quả ngân hàng thật.

## Cấu hình người nhận

Đặt vào `infra/.env` riêng của máy (file được bỏ qua bởi Git), hoặc biến môi trường backend:

```dotenv
TRANSFER_ENABLED=true
TRANSFER_ORGANIZATION_ID=<UUID tổ chức>
TRANSFER_STORE_ID=<UUID cửa hàng>
TRANSFER_BANK_BIN=<BIN 6 chữ số của ngân hàng>
TRANSFER_ACCOUNT_NUMBER=<số tài khoản>
TRANSFER_ACCOUNT_NAME=<tên chủ tài khoản>
```

Không có giá trị mặc định cho tài khoản. Thiếu/sai cấu hình hoặc sai tổ chức/cửa hàng thì QR bị tắt, CASH vẫn hoạt động. Tài khoản demo dùng organization UUID `10000000-0000-0000-0000-000000000001`, store UUID `10000000-0000-0000-0000-000000000002`. Vietcombank có BIN `970436`, đối chiếu danh sách công khai [VietQR banks](https://api.vietqr.io/v2/banks). Tên cấu hình để nhân viên đọc; tên người nhận trong app ngân hàng cần kiểm tra trực tiếp.

Compose demo truyền các biến này vào API. Sau khi lưu cấu hình:

```powershell
docker compose --env-file infra/.env -f infra/compose.demo.yml up -d --build
```

Trên môi trường triển khai, cấu hình nằm ở dịch vụ backend; chỉ thay biến Vercel frontend sẽ không bật QR. Giữ nguyên volume PostgreSQL để giao dịch tồn tại qua lần khởi động lại.

## Thử bằng tay

1. Đăng nhập cùng nhân viên/cửa hàng trên điện thoại và máy tính, mở **Bán hàng**, thêm một sản phẩm.
2. Chọn **Chuyển khoản QR → Tạo QR và giữ hàng**. Hai máy cùng thấy giao dịch chờ và không sửa giỏ được.
3. Đối chiếu người nhận/số tiền/nội dung trên QR. Có thể chỉ quét để xem thông tin rồi thoát app ngân hàng; chưa cần chuyển tiền thật.
4. Kiểm tra đã nhận đủ tiền cho đúng đơn. Luồng mới không cần điền lại số tiền, mã ngân hàng hoặc checkbox.
5. Bấm **Xác nhận đã nhận tiền**. Hai máy xóa giỏ; **Hóa đơn → Lịch sử chuyển khoản** lưu mã đơn, người và thời gian xác nhận thủ công. Hóa đơn/doanh thu/tồn chỉ thay đổi một lần. Không tự sinh mã chứng từ ngân hàng.
6. Tạo giao dịch khác rồi hủy/hết hạn: giỏ giữ sản phẩm, hàng giữ được trả, không có bán thành công. Tiền đến muộn cần đối chiếu riêng; không xác nhận lại giao dịch kết thúc.
7. F5 trong tab đang đăng nhập để khôi phục phiên; giao dịch chờ vẫn ở Bán hàng, lịch sử nằm ở Hóa đơn. Thu ngân thấy của mình, quản lý thấy phạm vi cửa hàng.
8. Nhấp ô số lượng giữa nút +/- để nhập số, Enter hoặc rời ô để lưu. Đơn vị đếm chỉ nhận số nguyên, đơn vị lẻ theo precision (tối đa 3 số); nhập 0 bỏ dòng. Thử hai máy sửa cùng lúc: giá trị cũ phải báo xung đột thay vì ghi đè giỏ mới.

## Kiểm thử tự động

```powershell
# Từ gốc repo, Docker đang chạy; JAVA_HOME trỏ JDK hợp lệ.
pwsh -File scripts/verify.ps1
pwsh -File scripts/test-transfer-live.ps1
```

Gate backend chạy `ManualTransferE2eTest`, `VietQrTest` và `TransferMigrationTest`: PostgreSQL 17 sạch, nâng cấp V19 giữ chứng từ cũ, giữ hàng/khóa giỏ, retry, xác nhận đồng thời, quyền, mã ngân hàng trùng cho endpoint cũ, hủy/hết hạn và rollback.

Bài live dựng PostgreSQL/API/web riêng; tài khoản nhận giả có nhãn `TEST ONLY - DO NOT TRANSFER`. Hai browser context dùng HTTP API thật; giải mã SVG QR, nhập số lượng đồng bộ, F5 giữ phiên, xác nhận một nút và giả lập mất response sau commit. SQL kiểm tra `confirmation_method=EMPLOYEE`, có người xác nhận, không mã ngân hàng giả, đúng một hóa đơn/payment/movement, không giữ hàng dư và ledger khớp tồn. Nếu cổng mặc định bị dùng, đặt `SIMTIM_LIVE_TEST_PORT` thành cổng trống. Bài kiểm thử không gọi ngân hàng hoặc chuyển tiền.
