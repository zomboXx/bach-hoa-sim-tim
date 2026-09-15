# Bàn giao prototype 02

## Điều chỉnh trải nghiệm đào tạo theo phản hồi người dùng

- Thay màn minh họa nhận hàng + biểu mẫu bằng cửa hàng 2D chơi được, tách toàn màn hình khỏi dashboard.
- Người chơi điều khiển nhân viên bằng bàn phím hoặc cảm ứng; nhấp mục tiêu sẽ tìm đường, đi tới và tương tác khi đứng kề. Có va chạm với kệ/quầy/tường.
- Một ca liền mạch bao gồm giao tiếp với khách, chọn hàng, quét/thanh toán/trả tiền thừa, phát hiện hàng hết hạn, di chuyển hàng tới khu xử lý và báo cáo mentor.
- NPC phản hồi các lựa chọn sai; tiến độ và giỏ cầm tay thay đổi theo hành động. Có hướng dẫn, xác nhận thoát, tổng kết và chơi lại.
- Dữ liệu phiên game nằm riêng trong component; không ghi hóa đơn, tồn kho hoặc phiếu nhận thật. Game trình duyệt hiện dùng Vue/SVG; Godot vẫn là bước tích hợp sau.

## 2026-09-14 — Codex hỗ trợ triển khai theo yêu cầu người dùng

- Bản đầu đã lưu nguyên trạng tại `46ce071` trên main, không push.
- Bản mới ở branch `codex/prototype-centralized-pwa`, thư mục `prototype-v2/`; giữ bản cũ độc lập để so sánh.
- Thử Vue/TypeScript/Vite; 11 màn hình theo quyền demo, nhận hàng–tồn–bán hàng–hóa đơn–báo cáo nối dữ liệu, ưu đãi theo sản phẩm.
- Kiểm kê từng lô, lưu IndexedDB, cache PWA, đồng bộ mô phỏng, duyệt tồn và phát hiện chênh lệch snapshot.
- Một tình huống đào tạo cách ly; chưa triển khai engine Godot/backend.
- Đã bắt và sửa lỗi cache tài nguyên khi offline do response `Vary: Origin`; giới hạn cache cho static assets công khai.
- Có bộ kiểm thử trình duyệt trong `tests/workflows.spec.ts`, cùng build TypeScript/Vite.
- Tái sử dụng hình mentor hiện có; tài liệu từ các phiên khác không được đưa vào commit prototype.

Đây là nhật ký đầu ra AI hỗ trợ, không quy đổi thành điểm đóng góp cá nhân hoặc xác nhận nghiệm thu của nhóm.
