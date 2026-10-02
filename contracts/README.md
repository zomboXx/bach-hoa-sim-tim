# Shared contracts

Thư mục này dành cho hợp đồng giữa web và API. Provider auth/session đã tích hợp qua PR #14; consumer FE-01 đã tích hợp qua PR #12.

Sprint 1 bắt đầu bằng contract nhỏ cho health, session và catalog. Mỗi contract phải ghi rõ `Draft` hoặc `Accepted`, có owner API, reviewer web và test provider/consumer tương ứng. Chỉ mô tả endpoint là “implemented” sau khi code và kiểm thử đã nhập `main`.

- [BE-02 session/RBAC](AUTH_SESSION_REVIEW.md): Accepted, ma trận quyền và bằng chứng provider/consumer review.
- [Auth/session OpenAPI 3.1](auth-session.openapi.yaml): Accepted wire contract cho BE-02/FE-01.
- [Sprint 2 boundary](SPRINT_2_BOUNDARY_DRAFT.md): Draft tên route, field và public port cần provider/consumer review trước triển khai.
- [INV-02 inventory read OpenAPI](inventory.openapi.yaml): Working Draft cho ba route tồn hiện tại; provider tests nằm trong API module, chờ TV4 review wire contract.
- [SAL-01 sales/invoices](SALES_INVOICE_REVIEW.md): Working Draft cho Issue #21 / PR #26, wire `/api/v1/sales/...`, ma trận quyền `sales.*` và public `InventoryPort`.
- [Sales/invoices OpenAPI 3.1](sales-invoices.openapi.yaml): Working Draft wire contract cho SAL-01.
