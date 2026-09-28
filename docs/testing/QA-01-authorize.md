# QA-01 — Ma trận xác thực và phân quyền

- Trạng thái: **Draft — chờ contract/reviewer xác nhận**
- Ngày cập nhật: 2026-09-28
- Nguồn tạm thời: handoff BE-02 và quyết định bốn vai trò trong [backlog](../project/governance/BACKLOG.md)

Ma trận này mô tả policy cần test, không tự cấp quyền cho implementation. Khi OpenAPI/permission contract được Accepted, mọi ô phải được đối chiếu và ghi link/operation ID chính xác.

## 1. Quy ước

- `Allow`: request hợp lệ được phép đi qua security chain.
- `Deny`: server phải từ chối trước khi thay đổi dữ liệu.
- `N/A`: operation không yêu cầu session hoặc không áp dụng cho role.
- `TBD`: chưa có quyết định/contract đủ rõ.

`trainingEnabled` là trạng thái nhân viên, không phải role server thứ năm.

## 2. Session operations

| Operation | Chưa đăng nhập | `SALES` | `STOCK` | `MANAGER` | `ADMIN` | Nguồn/ghi chú |
|---|---:|---:|---:|---:|---:|---|
| Login | Allow | N/A | N/A | N/A | N/A | Public nhưng chịu rate limit |
| Đọc phiên hiện hành (`me`) | Deny | Allow | Allow | Allow | Allow | Cần Bearer session hợp lệ |
| Logout phiên hiện hành | Deny | Allow | Allow | Allow | Allow | Sau logout token phải bị revoke |
| API chưa được cấp policy | Deny | Deny | Deny | Deny | Deny | Deny-by-default theo handoff BE-02 |

## 3. Catalog permissions Draft

| Permission/operation | Chưa đăng nhập | `SALES` | `STOCK` | `MANAGER` | `ADMIN` | Nguồn/ghi chú |
|---|---:|---:|---:|---:|---:|---|
| `catalog.read` / GET catalog | Deny | Allow | Allow | Allow | Allow | Handoff BE-02; cần contract consumer review |
| `catalog.write` / POST catalog | Deny | Deny | Allow | Allow | Allow | Handoff BE-02; khác với ma trận QA ban đầu |
| `catalog.write` / PUT catalog | Deny | Deny | Allow | Allow | Allow | Handoff BE-02; BE-03 phải xác nhận endpoint |
| `catalog.write` / DELETE/deactivate catalog | Deny | Deny | Allow | Allow | Allow | Cách delete/deactivate còn TBD |

Controller catalog hiện được mô tả là test fixture của BE-02 để kiểm tra security chain. Các ô `Allow` không chứng minh CRUD catalog đã được BE-03 triển khai hoặc validation đúng.

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
| `stock-active` | Active | `STOCK`, `catalog.read/write` Draft | Positive read/write |
| `manager-active` | Active | `MANAGER`, `catalog.read/write` | Positive read/write |
| `admin-active` | Active | `ADMIN`, `catalog.read/write` Draft | Positive read/write, kiểm tra scope |
| `locked-user` | Locked | Bất kỳ | Negative login/session |
| `permission-revoked` | Active | Permission bị gỡ sau login | Kiểm tra reload authorization |

Tên trên chỉ là nhãn test; username/password thật do fixture tạo và không ghi vào repository.

## 6. Điểm cần xác nhận trước khi Accepted

1. `STOCK` có thực sự được tạo/sửa/xóa category, product và supplier hay `catalog.write` cần tách nhỏ?
2. `ADMIN` có cùng store scope với role vận hành hay có scope khác?
3. GET supplier có dùng `catalog.read` không?
4. DELETE là xóa vật lý hay chuyển trạng thái inactive?
5. Status/error code chuẩn cho missing token, invalid token, expired token, `403` và rate limit là gì?
6. Endpoint health nào public; endpoint nào bắt buộc session?

Các câu trả lời phải được ghi vào contract/decision nguồn. Sau đó TV4 cập nhật bảng này và test matrix, không chỉ sửa expected trong test code.
