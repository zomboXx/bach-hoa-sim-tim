# ADR 0002 Sprint 1 platform

- Status: Accepted for Sprint 1 bootstrap
- Date: 2026-09-21
- Owner: Nguyễn Đức Phát

## Decision

Sprint 1 dùng Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, PostgreSQL 17, Flyway và OpenAPI 3.1. Web tiếp tục dùng Vue 3, TypeScript và Vite. Maven Wrapper và Docker Compose phải được commit cùng module API để thành viên không phụ thuộc cài Maven/PostgreSQL toàn cục.

Godot chưa được đưa vào baseline Sprint 1. Trò chơi Vue/SVG hiện tại tiếp tục làm mẫu hành vi tách dữ liệu cho đến backlog đào tạo được đưa vào Ready.

## Consequences

`BE-01` chịu trách nhiệm tạo module API tối thiểu và hạ tầng kiểm thử. `FE-01` chỉ thay adapter sau khi contract session/catalog được Accepted. Mọi thay đổi nền tảng khác cần ADR mới hoặc cập nhật trạng thái ADR này qua review.
