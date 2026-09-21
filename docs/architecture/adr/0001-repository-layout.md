# ADR 0001 Repository layout for Sprint 1

- Status: Accepted
- Date: 2026-09-21
- Owner: Nguyễn Đức Phát

## Context

Sprint 1 cần nhiều người làm web, API, dữ liệu và QA song song. Cấu trúc theo tên prototype và tài liệu trộn chung làm ownership, đường dẫn CI và trạng thái tài liệu khó hiểu.

## Decision

Sử dụng `apps/` cho client, `services/` cho tiến trình server, `contracts/` cho giao diện liên module, `infra/` cho hạ tầng local, `scripts/` cho lệnh root, `docs/` phân theo mục đích và `archive/` cho lịch sử.

Chỉ `apps/web/` có code sản phẩm trong baseline. Module mới phải được tạo bởi backlog item đã Ready và thêm owner, hướng dẫn, test cùng CI trong một Pull Request.

## Consequences

Đường dẫn cũ được thay thế trong tài liệu hiện hành. Biên bản lịch sử giữ nguyên nội dung quyết định, nhưng chú thích nguồn được cập nhật khi file nguồn chuyển vị trí. Code thử nghiệm Sprint 1 không tự động trở thành baseline.
