# Quầy bán hàng: nhập số lượng và xác nhận tiền về — 23/10/2026

Owner: TV1 — Nguyễn Đức Phát. Quyết định của Project Owner thay phần biểu mẫu xác nhận trong PR #84; chứng từ và bằng chứng kiểm thử cũ giữ nguyên để truy vết.

## Phạm vi

- Lịch sử chuyển khoản chuyển sang Hóa đơn, không hiển thị/tải lịch sử trong giỏ đang bán.
- Ô số lượng chặn chữ/dấu âm/ký hiệu mũ, nhận số lẻ theo đơn vị tối đa 3 chữ số. Enter hoặc rời ô để lưu; 0 bỏ dòng.
- Đặt số lượng tuyệt đối kiểm generation/version và giữ operation ID qua retry; không ghi đè thao tác mới từ thiết bị khác.
- Nhân viên xác nhận nhận đủ tiền bằng một nút; server lấy tổng đã khóa và lưu người/thời gian/`confirmation_method=EMPLOYEE`. Không tự tạo mã ngân hàng.
- V20 giữ chứng từ cũ ở chế độ BANK_RECEIPT. Trigger tương thích phân loại chứng từ khi bản API cũ hoàn tất đơn trong thời gian rollout. Endpoint nhập chứng từ cũ vẫn được giữ.

## Bằng chứng

- Bộ consumer bán/chuyển khoản đầu: 20/20 đạt, gồm nhập số lẻ và xung đột thiết bị. Bổ sung thêm ca đơn vị đếm chặn số lẻ ở gate đầy đủ.
- `ManualTransferE2eTest`: xác nhận nhân viên đồng thời/retry chỉ ghi một payment/movement, giữ actor, bank reference NULL; kiểm quyền, false confirmation, đặt số lượng idempotent/version/precision.
- `TransferMigrationTest`: nâng từ V19 có giao dịch đã hoàn tất, giữ mã chứng từ/tiền; kiểm API cũ vẫn cập nhật chứng từ sau V20.
- Gate `pwsh -File scripts/verify.ps1 -SkipInstall`: demo 51/51, consumer 73/73, backend PostgreSQL 216/216 đạt. Kết quả này trước khi cập nhật main có PR #83; CI kiểm lại mã tích hợp.

- `pwsh -File scripts/test-transfer-live.ps1`: 1/1 đạt trên PostgreSQL sạch, hai trình duyệt sửa số lượng và nhận cập nhật, F5 giữ phiên/QR, mất phản hồi xác nhận vẫn chỉ bán một lần. Đối soát DB: +1 hóa đơn, +1 payment, +1 SALE movement, +22.500 VND; không giữ hàng còn sót hoặc lệch ledger; EMPLOYEE và bank reference NULL. Logout/F5 không phục hồi phiên đã thu hồi, phiên thiết bị khác vẫn hoạt động. Lượt chạy trước bị dừng ở khởi tạo do Docker chưa mở; chạy lại sau khi Docker sẵn sàng đạt.

## Giới hạn

Xác nhận nhân viên không có kết nối ngân hàng. Không có mã chứng từ nên không chống được việc con người gán cùng một khoản chuyển khoản thật cho hai đơn khác nhau; ứng dụng chống hoàn tất trùng từng đơn. Kiểm thử dùng CSDL/tài khoản nhận giả, không chuyển tiền thật. Trạng thái CI/triển khai theo PR.
