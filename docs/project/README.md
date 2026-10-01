# Tài liệu điều hành dự án

**Mốc kiểm tra: 30/09/2026.** [Backlog](governance/BACKLOG.md) ghi phạm vi, ưu tiên, phụ thuộc và phân công; [Issue Sprint 2](https://github.com/zomboXx/bach-hoa-sim-tim/milestone/2) và Pull Request ghi tiến độ thực tế. PR mở hoặc code trên nhánh chưa phải chức năng đã tích hợp vào `main`.

## Đọc tài liệu nào

| Nhu cầu | Tài liệu có hiệu lực | Ghi chú |
|---|---|---|
| Công việc Sprint 2 | [Backlog](governance/BACKLOG.md), [kickoff](SPRINT_2_KICKOFF.md), [contract ranh giới](../../contracts/SPRINT_2_BOUNDARY_DRAFT.md) | Contract vẫn là Working Draft; đổi wire/schema cần review trong PR. |
| Ai làm và ai review | [TEAM.md](governance/TEAM.md), từng [Issue Sprint 2](https://github.com/zomboXx/bach-hoa-sim-tim/milestone/2) | Bảng TEAM ghi trách nhiệm dài hạn; phân công riêng Sprint 2 nằm trong backlog và Issue. |
| Phạm vi yêu cầu | [Bản ghi REQ-01 ngày 29/09](REQ-01_SCOPE_RECORD_2026-09-29.md) | Đã tích hợp qua PR #17; Issue #2 đã đóng. |
| Quy trình và trạng thái | [SCRUM.md](governance/SCRUM.md), Issue/PR | Không dùng các bản nháp cũ để suy ra trạng thái hiện tại. |

## Tiến độ tại mốc kiểm tra

- [Sprint 1](https://github.com/zomboXx/bach-hoa-sim-tim/milestone/1) đã đóng, 7/7 Issue hoàn tất. Đây là nền `main` cho Sprint 2.
- [Sprint 2](https://github.com/zomboXx/bach-hoa-sim-tim/milestone/2) hạn 07/10/2026, hiện có 7/7 Issue mở. [PRO-01B PR #25](https://github.com/zomboXx/bach-hoa-sim-tim/pull/25), [SAL-01 PR #26](https://github.com/zomboXx/bach-hoa-sim-tim/pull/26) và [REP-01 PR #27](https://github.com/zomboXx/bach-hoa-sim-tim/pull/27) đang mở; chưa tính là Done. Xem PR/CI mới nhất trước khi báo cáo tiến độ.
- Tại mốc này INV-01/02 chưa có PR mở, trong khi SAL-01, PRO-01B và REP-01 đã có PR. [Thứ tự phụ thuộc](SPRINT_2_KICKOFF.md#phạm-vi-đầu-ra-và-thứ-tự) vẫn áp dụng khi review và merge; PR #27 tự ghi đang chờ schema sales/inventory và dùng schema giả lập trong test.
- TV2 — Trung: [INV-01 #18](https://github.com/zomboXx/bach-hoa-sim-tim/issues/18), [INV-02 #19](https://github.com/zomboXx/bach-hoa-sim-tim/issues/19), [REP-01 #22](https://github.com/zomboXx/bach-hoa-sim-tim/issues/22). TV3 — Thi: [SAL-01 #21](https://github.com/zomboXx/bach-hoa-sim-tim/issues/21), [PRO-01B #23](https://github.com/zomboXx/bach-hoa-sim-tim/issues/23) P1. TV4 — Chiến: [FE-02 #20](https://github.com/zomboXx/bach-hoa-sim-tim/issues/20), [QA-02 #24](https://github.com/zomboXx/bach-hoa-sim-tim/issues/24). TV1 — Phát điều phối và review.

## Bản lưu để truy vết

| Tài liệu | Trạng thái và cách dùng |
|---|---|
| [REQ-01 ngày 26/09](REQ-01_DECISION_DRAFT.md) | Phương án lịch sử, đã được bản ghi ngày 29/09 thay thế. |
| [Đối chiếu Word Sprint 1](SPRINT_1_WORD_BACKLOG_DRAFT.md) | Bản làm việc lịch sử; các ứng viên chưa được lên lịch vẫn cần refinement, không là cam kết sprint. |
| [Kickoff Sprint 1](SPRINT_1_KICKOFF.md), [biên bản baseline Sprint 0](governance/MERGE_01.md) | Ghi cách nhóm khởi động và nghiệm thu các sprint trước, không là hướng dẫn bắt đầu Sprint 2. |
| [Biên bản tổng hợp 14/09](meetings/2026-09-14-tong-hop-du-an-hop-nhom.md) | Bản ghi cũ, dùng để truy vết theo trạng thái ghi trong file. |

Giữ các bản cũ để biết vì sao phạm vi thay đổi. Khi thông tin khác nhau, áp dụng thứ tự nguồn trong [bản đồ tài liệu](../README.md) và quyết định mới nhất của Project Owner.
