# Khởi động Sprint 1 từ baseline Sprint 0

## Mục tiêu

Đưa đăng nhập và dữ liệu danh mục của PWA từ mô phỏng cục bộ sang API/PostgreSQL thật mà không làm mất baseline demo đang chạy. Sprint 1 chưa triển khai giao dịch nhận hàng, bán hàng, kiểm kê đồng bộ hay Godot production.

## Thứ tự tích hợp

1. `REQ-01` và `DB-01`: chốt phạm vi, ERD MVP và data dictionary.
2. `BE-01`: tạo `services/api`, Maven Wrapper, PostgreSQL Compose, Flyway và health check.
3. `BE-02` và contract session: xác thực, năm role server và kiểm thử quyền.
4. `BE-03` và contract catalog: categories, products, suppliers cùng validation.
5. `FE-01`: thêm adapter API sau contract Accepted; giữ demo adapter để regression.
6. `QA-01`: provider test, consumer test, Playwright và traceability trước khi đóng sprint.

Không nhập một PR phụ thuộc khi contract hoặc migration nguồn chưa được merge vào `main`.

## Phân luồng bắt đầu

| Workstream | Owner | Reviewer | Mẫu nhánh sau khi có Issue | Kết quả đầu tiên |
|---|---|---|---|---|
| Phạm vi, contract và auth | Nguyễn Đức Phát `@zomboXx` | Nguyễn Văn Trung | `feature/<issue>-auth-contract` | Quyết định session/RBAC và OpenAPI draft nhỏ |
| Dữ liệu và API bootstrap | Nguyễn Văn Trung `@nguyentrung01ute-ui` | Nguyễn Đức Phát | `feature/<issue>-api-bootstrap` | Maven Wrapper, PostgreSQL/Flyway, health test |
| Web adapter | Nguyễn Văn Thi `@thinguyen135` | Nguyễn Đức Phát | `feature/<issue>-web-api-adapter` | Interface adapter và consumer test, chưa xóa demo mode |
| QA và traceability | Lê Văn Chiến `@VanChien11-02` | Nguyễn Văn Thi | `test/<issue>-quality-harness` | Test matrix, viewport và evidence convention |

## Definition of Ready cho PR đầu tiên

- Backlog item có owner, reviewer và acceptance criteria.
- Contract hoặc migration liên quan có trạng thái rõ ràng.
- Nêu file/module dự kiến thay đổi và phụ thuộc.
- Có lệnh kiểm thử tự động dự kiến thêm vào root gate/CI.
- Không chép nguyên WIP cũ mà không review lại theo baseline này.

## Cổng hoàn thành

Mọi PR phải chạy `pwsh -File scripts/verify.ps1` cùng kiểm thử module mới. `main` chỉ nhận code khi CI xanh, CODEOWNERS đã review, changelog/tài liệu liên quan cập nhật và không còn secret/build output.
