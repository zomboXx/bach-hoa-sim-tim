# Kiểm thử QR chuyển khoản thủ công — 22/10/2026

Bản ghi bằng chứng cục bộ ngày 22/10/2026, cập nhật từ `main` tại `5f1b086`. Ở thời điểm chạy các lệnh bên dưới, thay đổi chưa push và chưa triển khai. Ngày 23/10/2026, Project Owner cho phép push/PR/triển khai sau CI/review và đã review bốn commit triển khai; kết quả GitHub CI và tích hợp hiện hành xem tại [PR #84](https://github.com/zomboXx/bach-hoa-sim-tim/pull/84).

## Kết quả

| Lệnh/bộ kiểm tra | Kết quả |
|---|---|
| `pwsh -File scripts/verify.ps1` | Pass: policy, liên kết, lint, format, build; demo 51/51, API consumer 66/66, backend 210/210 |
| `services/api/mvnw.cmd verify` sau điều chỉnh lịch nền của test | Pass 210/210, không còn chờ cưỡng bức kết thúc JVM |
| `npm run verify` trong `apps/web` sau đồng bộ nhãn phương thức của máy thứ hai | Pass lint/format/build, demo 51/51 và API consumer 66/66 |
| `pwsh -File scripts/test-transfer-live.ps1` | Pass 1/1, PostgreSQL 17/API/web riêng và hai browser context desktop/mobile |
| `docker compose -f infra/compose.demo.yml config --quiet` và đọc YAML workflow | Pass với biến môi trường kiểm thử, không dùng tài khoản người nhận thật |

Máy chạy dùng JDK 25, Maven compile target Java 21; `JAVA_HOME` được đặt riêng cho lệnh vì biến trên máy đang trỏ JDK không còn tồn tại. Migration chạy từ schema trống gồm V18 ảnh sản phẩm và V19 chuyển khoản.

Trong backend có 8 ca `ManualTransferE2eTest` và 2 ca `VietQrTest`: giữ hàng không tạo doanh thu/movement, khóa cả checkout CASH, retry tạo QR, xác nhận đồng thời thu ngân/quản lý, chống chứng từ ngân hàng trùng, quyền chủ/cửa hàng/vai trò, hủy/hết hạn, sai giá, rollback khi phân bổ thứ hai thất bại và chặn kiểm kê thấp hơn số giữ. QR khớp vector công khai có CRC đã biết.

Hai ca consumer mới kiểm tra thao tác tạo QR chưa rõ kết quả được giữ qua reload/đăng nhập lại, retry đúng payload, hủy trả khóa và tài khoản chưa cấu hình không chặn CASH. Trong lúc thử đã sửa việc SVG bị thêm hai lần khi import bất đồng bộ và khóa giao diện còn giữ sau khi hủy/xác nhận.

Bài live đọc/ghi API thật; chỉ giả lập mất phản hồi HTTP sau khi xác nhận đã commit. Giải mã lại SVG QR, so nội dung với payload server; reload/đăng nhập lại trên điện thoại vẫn thấy cùng giao dịch và hai giỏ đều xóa sau khi hoàn tất.

SQL đối soát sau bài live:

- Một `transfer_order COMPLETED`, có người xác nhận và `bank_reference=LIVE-TRANSFER-001`.
- Tăng đúng một hóa đơn hoàn tất, một payment, một SALE movement; số phiếu nhận không đổi.
- Doanh thu tăng **22.500 VND** (hàng mẫu có khuyến mãi); không còn số lượng giữ và không lệch tồn với ledger.

Hình giao diện được lưu trong thư mục tạm do script in ra; job live `transfer` trong CI được thiết lập upload artifact `manual-transfer-browser`. Các lần đầu phát hiện dọn fixture thiếu giỏ và tài nguyên V18 cũ còn trong output build sau đổi tên; đã sửa thứ tự dọn dữ liệu test, clean output và chạy lại.

## Giới hạn và bước còn lại

Lượt GitHub CI đầu của [PR #84](https://github.com/zomboXx/bach-hoa-sim-tim/pull/84) gặp lỗi nạp named export của ZXing CommonJS trên Node 22/Linux, làm cả bốn suite live dừng trước khi chạy. Đã đổi riêng test loader sang `createRequire`; không đổi quy tắc hoặc API thanh toán. Project Owner đã review và duyệt bốn commit triển khai tại `17f8afc` ngày 23/10/2026; trạng thái CI sau sửa xem trực tiếp tại PR.

- Không gọi ngân hàng, không chuyển tiền thật. Tài khoản bài live ghi `TEST ONLY - DO NOT TRANSFER`; thống kê là giao dịch do ứng dụng ghi nhận thủ công.
- Đã kiểm tra tải lại/đăng nhập và dữ liệu PostgreSQL qua các request độc lập; chưa chạy ca riêng khởi động lại server giữa một giao dịch.
- Project Owner cho phép push/PR/triển khai và review bốn commit triển khai ngày 23/10/2026; trạng thái CI/triển khai theo PR. Sau triển khai, Project Owner quét QR bằng ứng dụng ngân hàng để kiểm tra người nhận, tiền và nội dung; có thể thoát trước bước chuyển tiền.
