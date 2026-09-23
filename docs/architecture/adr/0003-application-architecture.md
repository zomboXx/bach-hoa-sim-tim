# ADR 0003 Kiến trúc ứng dụng cho Sprint 1

- Status: Accepted
- Date: 2026-09-23
- Decision owner: Nguyễn Đức Phát
- Reviewers: Nguyễn Văn Trung, Nguyễn Văn Thi, Lê Văn Chiến

## Context

PWA Sprint 0 là prototype đã kiểm thử nhưng tập trung nhiều hành vi trong `App.vue` và `api.ts`. Backend chưa tồn tại. Sprint 1 cần nhiều người phát triển web, API, dữ liệu và kiểm thử song song mà không làm controller, persistence hoặc UI phụ thuộc chéo không kiểm soát.

MVC mô tả tốt lớp tiếp nhận HTTP của Spring nhưng không đủ để quy định biên nghiệp vụ, dependency và ownership cho toàn repository. Một cấu trúc chỉ chia ngang `controller`, `service`, `repository` cũng khiến một thay đổi nghiệp vụ chạm nhiều package dùng chung và khó giao trọn vẹn cho một owner.

## Decision

Xây dựng hệ thống dưới dạng modular monolith theo module nghiệp vụ. Mỗi module giữ dependency hướng vào domain/application và dùng ports/adapters tại biên HTTP, persistence hoặc dịch vụ ngoài.

### Web

Vue được tổ chức theo feature thay vì ép vào thư mục MVC:

```text
apps/web/src/
├── app/                         # bootstrap, router, providers, composition shell
├── modules/
│   └── <feature>/
│       ├── presentation/        # pages, components, view state
│       ├── application/         # use cases và orchestration
│       ├── domain/              # types và business rules thuần
│       └── infrastructure/      # demo/API/storage adapters
└── shared/                      # chỉ thành phần thực sự dùng chung
```

Module nhỏ không phải tạo thư mục rỗng. Khi tách code, dependency đi từ presentation/infrastructure qua application tới domain; module không import file private của module khác. Demo adapter được giữ cho regression cho tới khi API adapter và E2E thay thế hành vi tương ứng.

### API

Spring Boot được tổ chức package-by-feature:

```text
services/api/src/main/java/.../
├── auth/
│   ├── api/                     # controller và HTTP DTO
│   ├── application/             # use case, transaction boundary
│   ├── domain/                  # policy, entity/value và port
│   └── infrastructure/          # JPA, security và adapter
├── catalog/
├── inventory/
└── sales/
```

Spring MVC chỉ là inbound adapter. Controller không gọi repository trực tiếp. Application service điều phối transaction; domain giữ quy tắc giá, quyền và tồn kho không phụ thuộc HTTP. Infrastructure triển khai port và không làm rò rỉ JPA entity thành API contract.

### Cross-module boundaries

- OpenAPI và contract liên module nằm trong `contracts/`, có trạng thái `Draft` hoặc `Accepted`.
- Migration thuộc service sở hữu dữ liệu và được kiểm thử từ database sạch.
- Training là bounded context riêng; training state không được gọi đường ghi làm thay đổi bán hàng hoặc tồn kho vận hành.
- Mỗi module mới thêm test, hướng dẫn và CI/root verification trong cùng PR khởi tạo.

## Rollout

ADR này không cho phép refactor âm thầm baseline Sprint 0. Sprint 1 thực hiện theo các Issue riêng:

1. Chấp nhận ADR và contract/session boundary.
2. `BE-01` tạo skeleton API tối thiểu theo package-by-feature.
3. `FE-01` tách adapter và module cần cho đăng nhập trước; không chia toàn bộ prototype trong một PR.
4. Mỗi feature tiếp theo chỉ tách phần code nó sở hữu, có regression test trước và sau.

## Consequences

- Ownership và review bám theo nghiệp vụ thay vì tầng kỹ thuật dùng chung.
- Có thêm interface/adapter tại các biên cần thay thế hoặc kiểm thử; không tạo abstraction nếu chỉ có một hành vi nội bộ đơn giản.
- `App.vue` và `api.ts` hiện tại là nợ kỹ thuật có kế hoạch, không được tiếp tục mở rộng vô hạn.
- Nếu bằng chứng triển khai yêu cầu đổi hướng, ghi ADR thay thế và đánh dấu quan hệ supersede thay vì xóa quyết định này.
