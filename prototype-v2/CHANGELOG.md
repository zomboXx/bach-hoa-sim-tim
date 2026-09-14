# Bàn giao prototype 02

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
