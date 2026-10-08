# Bản đồ tài liệu

Tài liệu được phân loại theo mục đích để tránh dùng nhầm đề xuất lịch sử làm yêu cầu hiện hành.

## Quy trình và phạm vi hiện hành

- [`project/README.md`](project/README.md): điểm vào tài liệu dự án, kickoff Sprint 3 ngày 08/10/2026 và ảnh chụp tiến độ các sprint trước.
- [`project/governance/README.md`](project/governance/README.md): điểm vào quy trình nhóm.
- [`project/governance/SCRUM.md`](project/governance/SCRUM.md): Ready, Done, nhánh và Pull Request.
- [`project/governance/BACKLOG.md`](project/governance/BACKLOG.md): phạm vi, ưu tiên và phụ thuộc của các sprint.
- [`project/governance/TEAM.md`](project/governance/TEAM.md): owner, GitHub và reviewer.
- [`project/governance/GITHUB_SETUP.md`](project/governance/GITHUB_SETUP.md): runbook thiết lập repository, ruleset và Project.
- [`project/SPRINT_2_KICKOFF.md`](project/SPRINT_2_KICKOFF.md): thứ tự Issue/PR, owner và quy tắc bắt đầu Sprint 2.
- [`project/SPRINT_3_KICKOFF.md`](project/SPRINT_3_KICKOFF.md): năm mục kiểm kê đã có trong backlog, phân công, phụ thuộc và điểm review; milestone không có ngày kết thúc.
- [`project/REQ-01_SCOPE_RECORD_2026-09-29.md`](project/REQ-01_SCOPE_RECORD_2026-09-29.md): quyết định phạm vi đã tích hợp qua PR #17, Issue #2 đã đóng; không suy ra mọi chức năng đã được triển khai.
- [`project/SPRINT_2_ACCEPTANCE_2026-10-07.md`](project/SPRINT_2_ACCEPTANCE_2026-10-07.md): quyết định nghiệm thu Sprint 2 có ngoại lệ và sáu điểm cần theo dõi.

## Phân tích và thiết kế

- [`product/analysis/README.md`](product/analysis/README.md): bộ yêu cầu bản nháp và sơ đồ Use Case.
- [`architecture/database/01-thiet-ke-csdl-khai-niem.md`](architecture/database/01-thiet-ke-csdl-khai-niem.md): thiết kế dữ liệu đề xuất, chưa phải schema vật lý.
- [`architecture/database/DB-01_PHYSICAL_SCHEMA_DRAFT.md`](architecture/database/DB-01_PHYSICAL_SCHEMA_DRAFT.md): DDL đích 39 bảng và đánh giá 3NF được nhóm đồng ý theo thông báo của Project Owner; 14 bảng Sprint 1 đã tách thành Flyway V2/V3, phần còn lại chờ sprint sở hữu.
- [`architecture/adr/`](architecture/adr/): quyết định kiến trúc đã đánh số và trạng thái.
- [`architecture/adr/0003-application-architecture.md`](architecture/adr/0003-application-architecture.md): kiến trúc code được chấp nhận cho Sprint 1.
- [`../contracts/AUTH_SESSION_REVIEW.md`](../contracts/AUTH_SESSION_REVIEW.md): Accepted contract session/RBAC và ma trận quyền của BE-02; FE-01 đã tích hợp qua PR #12.
- [Contract ranh giới Sprint 2](../contracts/SPRINT_2_BOUNDARY_DRAFT.md): Draft tên HTTP/DTO và public port để TV2/TV3/TV4 review.
- [`project/meetings/2026-09-14-tong-hop-du-an-hop-nhom.md`](project/meetings/2026-09-14-tong-hop-du-an-hop-nhom.md): biên bản tổng hợp cần nhóm xác nhận.

## Kiểm thử

- [`testing/README.md`](testing/README.md): điểm vào hồ sơ QA-01/QA-02, gồm test plan, ma trận quyền, truy vết và test report; QA-02 chưa đạt đầy đủ P0.

## Tài liệu lịch sử

- [`project/REQ-01_DECISION_DRAFT.md`](project/REQ-01_DECISION_DRAFT.md): phương án ngày 26/09/2026, đã được bản ghi ngày 29/09 thay thế.
- [`project/SPRINT_1_WORD_BACKLOG_DRAFT.md`](project/SPRINT_1_WORD_BACKLOG_DRAFT.md): đối chiếu Word ban đầu, không là backlog hay cam kết sprint hiện tại.
- [`project/SPRINT_1_KICKOFF.md`](project/SPRINT_1_KICKOFF.md): thứ tự tích hợp Sprint 1 đã kết thúc.
- [`archive/PROJECT_CONTEXT.md`](archive/PROJECT_CONTEXT.md): quá trình hình thành ý tưởng và thay đổi phạm vi. Dùng để truy vết, không dùng thay backlog hoặc quyết định mới hơn.
- [`../archive/prototype-v1/`](../archive/prototype-v1/README.md): prototype đầu tiên.

## Thứ tự xử lý khi tài liệu mâu thuẫn

1. Quyết định mới nhất của Project Owner được ghi trong Issue, PR hoặc biên bản.
2. Backlog và tài liệu sprint hiện hành.
3. Yêu cầu/thiết kế đã được nhóm xác nhận.
4. Code và test của baseline đã tích hợp.
5. Đề xuất, biên bản chưa xác nhận và tài liệu lịch sử.

Không sửa tài liệu lịch sử để làm như thể quyết định cũ chưa từng tồn tại. Hãy ghi quyết định mới, ngày áp dụng và cập nhật liên kết từ tài liệu hiện hành.
