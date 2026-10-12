# Quyết định nền tảng đào tạo cho Sprint 4

**Trạng thái:** Quyết định của Project Owner được ghi lại ngày 13/10/2026; tài liệu và triển khai tiếp theo cần nhóm review qua PR. **Ngày quyết định trước đó:** không được ghi riêng trong repository.

## Quyết định

- Không đưa Godot vào phạm vi triển khai còn lại của đồ án. Giảng viên không yêu cầu công nghệ này; lựa chọn Godot trong tài liệu ban đầu là dự kiến của nhóm.
- Giữ mục tiêu **trải nghiệm nhập vai 2D giúp nhân viên học nghiệp vụ trước ca làm**. Dùng khu đào tạo Vue/SVG hiện có trong PWA làm nền, hoàn thiện một kịch bản và lưu kết quả theo các mục TRN-01/02/03 của [backlog](governance/BACKLOG.md#sprint-4--đào-tạo-ổn-định-và-báo-cáo).
- Phiên và kết quả đào tạo phải cách ly với bán hàng, hóa đơn và tồn kho vận hành. Chưa coi việc PWA hiện chạy được một ca demo là bằng chứng đã lưu kết quả qua API/PostgreSQL hoặc đã hoàn thành TRN-01/02/03.

## Thay thế và giới hạn

Dòng TRN-01 cũ “Khởi tạo Godot và scene cửa hàng” cùng các câu hứa tích hợp Godot trong tài liệu hiện hành được thay bằng hướng PWA 2D. [Biên bản 14/09](meetings/2026-09-14-tong-hop-du-an-hop-nhom.md), [ADR Sprint 1](../architecture/adr/0002-sprint-1-platform.md), hồ sơ QA cũ và [bối cảnh lưu trữ](../archive/PROJECT_CONTEXT.md) vẫn giữ nội dung tại thời điểm viết để truy vết; chúng không còn quyết định công nghệ cho Sprint 4.

Quyết định này không bổ sung tính năng đào tạo mới, không chốt contract API/schema cho TRN-03, không nghiệm thu Sprint 3 và không tự phân công sáu GAP P0 còn mở tại [#34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34) vào Sprint 4. Owner/reviewer sẽ review ranh giới dữ liệu, quyền và kiểm thử trước khi làm phần lưu kết quả.
