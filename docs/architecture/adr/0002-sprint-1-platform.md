# ADR 0002 Sprint 1 platform

- Status: Accepted as Sprint 1 implementation plan
- Date: 2026-09-21
- Owner: Nguyễn Đức Phát

## Decision

Sprint 1 dùng Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, PostgreSQL 17, Flyway và OpenAPI 3.1. Web tiếp tục dùng Vue 3, TypeScript và Vite. Maven Wrapper và Docker Compose phải được commit cùng module API để thành viên không phụ thuộc cài Maven/PostgreSQL toàn cục.

ADR này chốt kế hoạch triển khai, không xác nhận các thành phần server đã tồn tại trong baseline Sprint 0. Godot không thuộc baseline và chưa nằm trong phạm vi khởi tạo Sprint 1 hiện tại. Trò chơi Vue/SVG tiếp tục làm mẫu hành vi tách dữ liệu cho đến khi backlog đào tạo được đưa vào Ready.

## Consequences

`BE-01` chịu trách nhiệm tạo module API tối thiểu và hạ tầng kiểm thử. `FE-01` chỉ thay adapter sau khi contract session/catalog được Accepted. Mọi thay đổi nền tảng khác cần ADR mới hoặc cập nhật trạng thái ADR này qua review.
