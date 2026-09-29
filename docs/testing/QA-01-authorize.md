# QA-01 — Ma trận xác thực và phân quyền

- Trạng thái: **Accepted contract — QA integration đã xác nhận**
- Ngày cập nhật: 2026-09-29
- Nguồn: [auth/session OpenAPI](../../contracts/auth-session.openapi.yaml), [session/RBAC review](../../contracts/AUTH_SESSION_REVIEW.md), `AuthSecurity` trong source `5f0b14c` và quyết định bốn vai trò trong [backlog](../project/governance/BACKLOG.md)

Ma trận này mô tả policy đã Accepted và được QA đối chiếu trên source tích hợp. Nó không tự cấp quyền cho implementation; thay đổi policy phải cập nhật contract, test và traceability cùng nhau.

## 1. Quy ước

- `Allow`: request hợp lệ được phép đi qua security chain.
- `Deny`: server phải từ chối trước khi thay đổi dữ liệu.
- `N/A`: operation không yêu cầu session hoặc không áp dụng cho role.
- `TBD`: chưa có quyết định/contract đủ rõ.

`trainingEnabled` là trạng thái nhân viên, không phải role server thứ năm.

## 2. Session operations

| Operation | Chưa đăng nhập | `SALES` | `STOCK` | `MANAGER` | `ADMIN` | Nguồn/ghi chú |
|---|---:|---:|---:|---:|---:|---|
| `POST /api/v1/auth/login` | Allow | N/A | N/A | N/A | N/A | `200`; invalid request `400`, sai thông tin `401`, rate limit `429` |
| `GET /api/v1/auth/session` | Deny | Allow | Allow | Allow | Allow | `200`; cần opaque Bearer session hợp lệ, trả organization/store scope |
| `POST /api/v1/auth/logout` | Deny | Allow | Allow | Allow | Allow | `204`; sau logout token phải bị revoke |
| API chưa được cấp policy | Deny | Deny | Deny | Deny | Deny | Deny-by-default theo handoff BE-02 |

## 3. Catalog permissions

| Permission/operation | Chưa đăng nhập | `SALES` | `STOCK` | `MANAGER` | `ADMIN` | Nguồn/ghi chú |
|---|---:|---:|---:|---:|---:|---|
| `catalog.read` / GET category, unit, product, supplier | Deny | Allow | Allow | Allow | Allow | Accepted; positive tests gửi Bearer thật |
| `catalog.write` / POST catalog | Deny | Deny | Allow | Allow | Allow | Accepted; SALES negative test trên controller thật |
| `catalog.write` / PUT catalog | Deny | Deny | Allow | Allow | Allow | Accepted; SALES negative test trên controller thật |
| `catalog.write` / DELETE catalog | Deny | Deny | Allow | Allow | Allow | Accepted; integration test kỳ vọng `204` |

Controller catalog trong `ApiBootstrapTest` là fixture của BE-02 tại `/api/v1/products/__be02_security_fixture`, chỉ kiểm tra security chain và không trùng controller thật. `CatalogApiTest` bổ sung bằng chứng trên CRUD thật với STOCK/SALES session. Kết quả tại `5f0b14c`: 31/31 automated tests đạt.

## 4. Hành vi động phải kiểm thử

| Tình huống | Expected |
|---|---|
| Token không tồn tại, malformed hoặc không có prefix Bearer hợp lệ | Deny; không chạy API nghiệp vụ |
| Token đã logout/revoked | Deny |
| Token hết hạn | Deny |
| Account bị khóa sau khi session được cấp | Deny ở request tiếp theo |
| Role bị gỡ sau khi session được cấp | Request tiếp theo dùng role mới nhất |
| Permission bị thu hồi sau khi session được cấp | Request tiếp theo bị từ chối nếu thiếu permission |
| Client gửi header tổ chức/cửa hàng giả mạo | Không vượt được scope do server nạp từ session/account |
| Permission chưa khai báo cho API mới | Deny-by-default |

## 5. Dữ liệu test tối thiểu

| Principal | Trạng thái | Role/permission | Mục đích |
|---|---|---|---|
| `sales-active` | Active | `SALES`, `catalog.read` | Positive read, negative write |
| `stock-active` | Active | `STOCK`, `catalog.read/write` | Positive read/write |
| `manager-active` | Active | `MANAGER`, `catalog.read/write` | Positive read/write |
| `admin-active` | Active | `ADMIN`, `catalog.read/write` | Positive read/write, kiểm tra scope |
| `locked-user` | Locked | Bất kỳ | Negative login/session |
| `permission-revoked` | Active | Permission bị gỡ sau login | Kiểm tra reload authorization |

Tên trên chỉ là nhãn test; username/password thật do fixture tạo và không ghi vào repository.

## 6. Quyết định và follow-up

- Provider/consumer reviewers đã Accepted request/response auth và permission matrix ngày 29/09/2026.
- Header organization/store tạm chỉ được chấp nhận khi khớp session; scope giả mạo trả `403`. Việc chuyển controller sang đọc `SessionPrincipal` trực tiếp là follow-up implementation.
- DELETE catalog hiện được integration test theo status `204`; mọi thay đổi semantics phải đi qua contract review.
- Validation catalog hiện dùng `ProblemDetail` với thuộc tính `errors` khi phù hợp.
- Fixture security đã được cô lập; negative catalog tests chứng minh missing token `401`, SALES write `403` và scope mismatch `403`.
- Evidence chi tiết nằm trong [ma trận truy vết](QA-01-traceability.md) và [test report](QA-01-test-report.md).
