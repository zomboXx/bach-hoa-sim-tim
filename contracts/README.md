# Shared contracts

Thư mục này dành cho hợp đồng giữa web và API. Baseline chưa công bố OpenAPI đã triển khai.

Sprint 1 bắt đầu bằng contract nhỏ cho health, session và catalog. Mỗi contract phải ghi rõ `Draft` hoặc `Accepted`, có owner API, reviewer web và test provider/consumer tương ứng. Chỉ mô tả endpoint là “implemented” sau khi code và kiểm thử đã nhập `main`.

- [BE-02 session/RBAC](AUTH_SESSION_REVIEW.md): Draft implementation proposal và ma trận quyền.
- [Auth/session OpenAPI 3.1](auth-session.openapi.yaml): Draft wire contract cho BE-02/FE-01.
