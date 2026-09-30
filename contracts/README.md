# Shared contracts

Thư mục này dành cho hợp đồng giữa web và API. Provider auth/session đã được tích hợp qua PR #14; consumer FE-01 chờ tích hợp PR #12.

Sprint 1 bắt đầu bằng contract nhỏ cho health, session và catalog. Mỗi contract phải ghi rõ `Draft` hoặc `Accepted`, có owner API, reviewer web và test provider/consumer tương ứng. Chỉ mô tả endpoint là “implemented” sau khi code và kiểm thử đã nhập `main`.

- [BE-02 session/RBAC](AUTH_SESSION_REVIEW.md): Accepted, ma trận quyền và bằng chứng provider/consumer review.
- [Auth/session OpenAPI 3.1](auth-session.openapi.yaml): Accepted wire contract cho BE-02/FE-01.
- [Sprint 2 boundary](SPRINT_2_BOUNDARY_DRAFT.md): Draft tên route, field và public port cần provider/consumer review trước triển khai.
