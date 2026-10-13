# ADR 0004 — Game đào tạo 2D trong PWA

- Trạng thái: **Project Owner đã quyết định ngày 30/09/2026 và xác nhận lại ngày 09/10/2026; kế hoạch Sprint 4 vẫn cần nhóm review khi khởi động**.
- Người quyết định: **TV1 — Nguyễn Đức Phát**, Project Owner.
- Phạm vi thay thế: kế hoạch dùng Godot trong backlog TRN-01 và tài liệu kiến trúc trước ngày này; không thay yêu cầu chức năng đào tạo hoặc dữ liệu.

Cập nhật 13/10/2026: Project Owner khởi động [Sprint 4](../../project/SPRINT_4_KICKOFF.md) trên nền quyết định này; các contract API/schema vẫn cần owner và reviewer chốt trong Issue/PR tương ứng.

## Bối cảnh

Sprint 0 đã có một ca đào tạo 2D chơi được trong trình duyệt bằng Vue 3, TypeScript, SVG và CSS. `TrainingGame.vue`, `world.ts` và `PixelPerson.vue` thực hiện di chuyển, va chạm/tìm đường, tương tác, hội thoại, phản hồi sai và tổng kết một ca. Game chưa dùng runtime Godot; kết quả hiện chỉ tồn tại trong phiên chơi và chưa gắn với tài khoản server. Hai bản kế hoạch Word cũ trong thư mục làm việc còn ghi Godot là kế hoạch Sprint 4, không phải phần đã hiện thực.

Project Owner xác nhận ngày 30/09/2026 rằng đề bài và giảng viên **không yêu cầu Godot**. Chuyển game hiện có sang engine khác sẽ thêm thời gian học, viết lại scene/điều khiển và kiểm thử tích hợp, trong khi mục tiêu nghiệp vụ là một ca học 2D dùng được trước buổi làm. Đây là đánh giá về chi phí và phạm vi dự án, không đánh giá năng lực hay thái độ của thành viên.

## Quyết định

1. **Vue/SVG là công nghệ triển khai của game đào tạo 2D trong PWA.** Bỏ kế hoạch tích hợp Godot khỏi phạm vi dự án hiện hành; một thay đổi công nghệ trong tương lai cần quyết định mới.
2. Giữ một kịch bản nghiệp vụ có mục tiêu, hướng dẫn, phản hồi, kết quả, điều khiển bàn phím/chuột/chạm và hoạt động trên thiết bị yếu nhất của nhóm. TRN-01 tập trung hoàn thiện và kiểm thử game web hiện có; không tính nó Done chỉ vì prototype có thể chơi.
3. TRN-02 hoàn thiện nội dung và đánh giá ca học; TRN-03 lưu phiên, chuỗi hành động và kết quả qua backend theo quyền của nhân viên/quản lý. Phiên đào tạo dùng dữ liệu cách ly, không ghi tồn kho, hóa đơn hay báo cáo vận hành.
4. API mode chỉ mở đào tạo theo `trainingEnabled` của phiên server. Demo mode tiếp tục là môi trường thử giao diện, không là bằng chứng lưu kết quả nhân viên thật.
5. Không đổi vai trò server, DB-01 đích 39 bảng, yêu cầu SRS FR-TRN-01..08 hoặc NFR-TRN-01. Tên migration/API và cách tái sử dụng quy tắc nghiệp vụ sẽ được contract Sprint 4 chốt trước triển khai.

## Bằng chứng và tiêu chí review

- Code hiện tại: [`TrainingGame.vue`](../../../apps/web/src/training/TrainingGame.vue), [`world.ts`](../../../apps/web/src/training/world.ts), [`PixelPerson.vue`](../../../apps/web/src/training/PixelPerson.vue); không có project Godot hoặc bản export engine trong code active.
- [Backlog Sprint 4](../../project/governance/BACKLOG.md) giữ owner/reviewer và phụ thuộc; chỉ thay công nghệ bắt buộc của TRN-01. Quyết định tài liệu này không tự mở Issue, đổi milestone hoặc đánh dấu TRN-01 hoàn thành.
- Kiểm tra trước khi coi game hoàn thành: nhân viên mới chạy hết ca trên thiết bị tối thiểu, thao tác bàn phím và chạm, nhận phản hồi khi sai, xem lại kết quả; quản lý xem kết quả đúng phạm vi; thử lại không ghi vào dữ liệu vận hành. QA-04 phải có bằng chứng qua API/DB cho phần lưu kết quả.

## Quan hệ với quyết định cũ

[ADR 0002](0002-sprint-1-platform.md) chốt nền Sprint 1 và mô tả Vue/SVG là mẫu hành vi trước khi kế hoạch Godot được xét. [ADR 0003](0003-application-architecture.md) chốt ranh giới module đào tạo, không chọn engine. Hai quyết định đó vẫn đúng cho phạm vi ban đầu; **ADR 0004 thay thế riêng hướng công nghệ đào tạo kể từ 30/09/2026**. Kế hoạch Word ba tuần cũ và báo cáo đã nộp trước quyết định được giữ như ảnh chụp lịch sử ở ngoài bản `main` của repository. Tài liệu, kế hoạch và báo cáo biên soạn sau quyết định phải ghi game đào tạo Vue/SVG trong PWA, viện dẫn ADR này và không còn giao nhiệm vụ triển khai Godot.
