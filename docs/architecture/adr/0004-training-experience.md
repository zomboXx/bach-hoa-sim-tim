# ADR 0004 — Trải nghiệm đào tạo 2D trong PWA

- Status: Accepted by Project Owner; team review pending
- Recorded: 2026-10-13 (ngày Project Owner nêu quyết định trước đó chưa được ghi riêng trong repository)
- Decision owner: Nguyễn Đức Phát
- Scope: nền tảng trải nghiệm đào tạo từ Sprint 4

## Context

[ADR 0002](0002-sprint-1-platform.md) chốt stack Sprint 1, chỉ ghi Godot chưa thuộc Sprint 1 và Vue/SVG đang làm mẫu hành vi. [ADR 0003](0003-application-architecture.md) chốt training là bounded context riêng, không chọn engine. Cả hai vẫn có hiệu lực cho phần đã chốt; chúng không quyết định nền tảng cuối cùng của khu đào tạo. [Biên bản 14/09](../../project/meetings/2026-09-14-tong-hop-du-an-hop-nhom.md) từng dự kiến Godot. Giảng viên không bắt buộc Godot và PWA hiện đã có một game nhập vai 2D chạy trên trình duyệt.

## Decision

- Dùng khu đào tạo Vue/SVG trong PWA để hoàn thiện **một kịch bản nhập vai 2D giúp nhân viên học nghiệp vụ trước ca làm**. Không đưa runtime Godot vào phạm vi triển khai còn lại của đồ án.
- Giữ bốn vai trò server hiện hành; quyền vào đào tạo dựa trên trạng thái nhân viên `trainingEnabled`. Phiên, hành động và kết quả đào tạo tách khỏi giao dịch bán hàng, hóa đơn và tồn kho vận hành.
- [Backlog Sprint 4](../../project/governance/BACKLOG.md#sprint-4--đào-tạo-ổn-định-và-báo-cáo) giữ TRN-01/02/03 để hoàn thiện trải nghiệm, một kịch bản và lưu kết quả. Kịch bản demo hiện tại không chứng minh TRN-03 đã có API/PostgreSQL.

## Consequences

Phương án chuyển scene sang Godot trong các mô tả cũ không còn là cam kết; biên bản, [bối cảnh lịch sử](../../archive/PROJECT_CONTEXT.md) và ADR 0002 vẫn giữ nguyên nội dung gốc để truy vết. ADR này bổ sung lựa chọn nền tảng đào tạo, **không thay thế** stack Sprint 1 của ADR 0002 hay ranh giới module của ADR 0003. TRN-03 cần contract, migration và test riêng trước code; tài liệu này không chốt wire/schema hoặc tự nghiệm thu Sprint 3. Sáu GAP P0 tại [#34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34) vẫn được theo dõi riêng.
