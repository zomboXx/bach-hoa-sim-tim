# QA-04 — Ma trận phân quyền và bảo mật

- Ngày cập nhật: 2026-10-15
- Chủ trì: TV2 — Nguyễn Văn Trung (`@nguyentrung01ute-ui`)
- Reviewer kiểm bằng chứng: TV4 — Lê Văn Chiến (`@VanChien11-02`)
- Reviewer xác nhận: TV1 — Nguyễn Đức Phát (`@zomboXx`)
- Nguồn: [BE-02 Session/RBAC Accepted](../../contracts/AUTH_SESSION_REVIEW.md), [Sprint 2 Boundary](../../contracts/SPRINT_2_BOUNDARY_DRAFT.md), [SYN-02 Contract](../../contracts/SYN-02_SYNC_API_CONTRACT.md), [INV-03 Contract](../../contracts/INV-03_STOCKTAKE_APPROVAL_CONTRACT.md), [ADR 0004](../architecture/adr/0004-training-2d-web.md)
- Trạng thái: **Đã kiểm chứng tự động qua chuỗi bảo mật thật; invoice privacy chưa đạt theo S2-GAP-03**

---

## 1. Ma trận quyền hạn theo vai trò (RBAC Matrix)

Hệ thống áp dụng kiến trúc kiểm soát truy cập dựa trên vai trò (Role-Based Access Control) với 4 vai trò cố định, được gán quyền chi tiết thông qua các bảng `iam.roles`, `iam.permissions` và `iam.role_permissions`. Mọi yêu cầu được thẩm tra qua Bearer token opaque lưu dưới dạng băm SHA-256.

| Nhóm nghiệp vụ | Thao tác (Endpoint / Operation) | Quyền yêu cầu | SALES | STOCK | MANAGER | ADMIN | Quy tắc phạm vi và xử lý lỗi |
|---|---|---|:---:|:---:|:---:|:---:|---|
| **Xác thực** | `POST /api/v1/auth/login` | Công khai | Có | Có | Có | Có | Kiểm tra mật khẩu BCrypt, giới hạn 20 req/phút |
| **Xác thực** | `GET /api/v1/auth/session` | Đã xác thực | Có | Có | Có | Có | Trả thông tin actor, store, role, permission, trainingEnabled; thiếu token trả `401` |
| **Xác thực** | `POST /api/v1/auth/logout` | Đã xác thực | Có | Có | Có | Có | Thu hồi token ngay lập tức |
| **Danh mục** | `GET /api/v1/categories`, `/products`, `/units`, `/suppliers` | `catalog.read` | Có | Có | Có | Có | Cho phép xem danh mục dùng chung |
| **Danh mục** | `POST/PUT/DELETE /api/v1/...` | `catalog.write` | Không (`403`) | Có | Có | Có | Chỉ nhân viên kho hoặc quản lý được chỉnh sửa danh mục |
| **Nhận hàng** | `GET /api/v1/inventory/receipts` | `receipts.read` | Không (`403`) | Có | Có | Có | Xem danh sách phiếu nhận trong cửa hàng |
| **Nhận hàng** | `POST /api/v1/inventory/receipts` | `receipts.write` | Không (`403`) | Có | Có | Có | Xác nhận phiếu nhận, tạo lô và tăng tồn kho |
| **Tồn & Lô** | `GET /api/v1/inventory/products`, `/batches`, `/movements` | `inventory.read` | Có (*) | Có | Có | Có | (*) SALES xem tồn nhưng **không thấy giá vốn/giá mua** (`unitCost`) |
| **Kiểm kê** | `POST /api/v1/inventory/stocktakes` | `stocktakes.write` | Không (`403`) | Có | Có | Không (`403`) | Mở hoặc lấy phiên kiểm kê đang mở |
| **Kiểm kê** | `GET /api/v1/inventory/stocktakes/{id}` | `stocktakes.read` | Không (`403`) | Có | Có | Có | Xem chi tiết phiên kiểm kê trong phạm vi cửa hàng |
| **Kiểm kê** | `POST /api/v1/inventory/stocktakes/{id}/counts` | `stocktakes.write` | Không (`403`) | Có | Có | Không (`403`) | Gửi số đếm thực tế; idempotent theo `clientOperationId` |
| **Kiểm kê** | `POST /api/v1/inventory/stocktakes/{id}/approve` | `stocktakes.approve` | Không (`403`) | Không (`403`) | Có | Có | Duyệt điều chỉnh tồn kho, ghi nhận `STOCKTAKE_ADJUSTMENT` |
| **Bán hàng** | `POST /api/v1/sales/quote` | `sales.read` | Có | Không (`403`) | Có | Có | Xem trước giá và chiết khấu, không ghi CSDL |
| **Bán hàng** | `POST /api/v1/sales/checkout` | `sales.write` | Có | Không (`403`) | Có | Có | Thanh toán tiền mặt, tạo hóa đơn, trừ tồn FEFO |
| **Hóa đơn** | `GET /api/v1/sales/invoices` | `sales.read` | Có (**) | Có | Có | Có | (**) Xem chi tiết mục 3 về quyền riêng tư hóa đơn |
| **Hóa đơn** | `GET /api/v1/sales/invoices/{id}` | `sales.read` | Có (**) | Có | Có | Có | Chỉ xem hóa đơn thuộc phạm vi cửa hàng |
| **Khuyến mãi** | `GET /api/v1/promotions` | `promotions.read` | Có | Có | Có | Có | Xem danh sách chương trình khuyến mãi |
| **Khuyến mãi** | `POST/PUT/DELETE /api/v1/promotions` | `promotions.write` | Không (`403`) | Không (`403`) | Có | Có | Tạo, chỉnh sửa hoặc hủy kích hoạt khuyến mãi |
| **Báo cáo** | `GET /api/v1/reports/revenue` | `reports.read` | Không (`403`) | Không (`403`) | Có | Có | Báo cáo doanh thu theo khoảng thời gian |
| **Báo cáo** | `GET /api/v1/reports/inventory` | `reports.read` | Không (`403`) | Không (`403`) | Có | Có | Báo cáo trạng thái tồn kho theo thời gian thực |
| **Đào tạo** | `SessionPrincipal.trainingEnabled` | Boolean flag | Độc lập | Độc lập | Độc lập | Độc lập | Phân biệt người dùng đào tạo, không ghi CSDL vận hành |

---

## 2. Các nguyên tắc bảo mật đã kiểm chứng

### 2.1. Từ chối mặc định (Deny-by-default)
- Bất kỳ yêu cầu nào không có header `Authorization: Bearer <token>` hợp lệ tới các route được bảo vệ đều bị từ chối với HTTP `401 Unauthorized` và thân lỗi JSON `{"code":"UNAUTHENTICATED","message":"Valid bearer session required"}`.
- Bất kỳ yêu cầu nào có token hợp lệ nhưng vai trò không sở hữu authority theo cấu hình trong `AuthSecurity.java` đều bị chặn tại bộ lọc với HTTP `403 Forbidden` và thân lỗi JSON `{"code":"FORBIDDEN","message":"Permission denied"}`.
- Không tồn tại tài khoản siêu người dùng (super-admin bypass) có thể vượt qua kiểm tra quyền.

### 2.2. Che giấu dữ liệu giá vốn nhạy cảm (Data Masking)
- Nhân viên `SALES` được cấp quyền `inventory.read` để tra cứu tồn kho phục vụ bán hàng, nhưng phản hồi trả về từ `/api/v1/inventory/products` đã được che giấu:
  * Không chứa thuộc tính `unitCost`.
  * Không chứa thuộc tính `unit_cost`.
  * Không chứa các trường nhập hàng hoặc chi phí vốn (`purchase`).
- Quyền truy cập chi tiết phiếu nhận (`receipts.read`) và giá vốn mua hàng chỉ dành riêng cho `STOCK`, `MANAGER` và `ADMIN`.

### 2.3. Cô lập phạm vi cửa hàng (Store Scope Isolation)
- Quyền hạn của người dùng gắn chặt với `store_id` được xác định tại thời điểm đăng nhập.
- Khi một client gửi request bán hàng (quote/checkout) có kèm `storeId` khác với `principal.storeId()`, server từ chối xử lý bằng ngoại lệ `SaleValidationException` ("Cửa hàng không thuộc phiên làm việc") và trả về mã lỗi `422 Unprocessable Entity` (hoặc `403 Forbidden`), ngăn chặn hoàn toàn việc can thiệp chéo giữa các cửa hàng.

---

## 3. Khoảng trống cần lưu ý: Quyền riêng tư hóa đơn (S2-GAP-03)

- **Đặc tả ban đầu:** Nhân viên `SALES` chỉ được xem các hóa đơn do chính mình tạo ra (`soldBy = principal.userId()`), trong khi `MANAGER` và `ADMIN` được xem toàn bộ hóa đơn của cửa hàng.
- **Hành vi thực tế tại `main`:**
  * Endpoint `GET /api/v1/sales/invoices` trong `SaleController.java` chỉ lọc theo `effectiveStoreId`:
    ```java
    return service.listInvoices(orgId, effectiveStoreId).stream().map(InvoiceResponse::from).toList();
    ```
  * Ca kiểm thử tự động `Qa04RegressionSecurityTest#invoicePrivacy_documentsSalesVisibilityBehavior` xác nhận rằng một nhân viên Sales B trong cùng cửa hàng vẫn nhìn thấy hóa đơn do Sales A tạo ra.
- **Kết luận:** Khoảng trống **S2-GAP-03 vẫn ở trạng thái Fail**, đúng như đã ghi nhận tại biên bản nghiệm thu Sprint 2 ([Issue #34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34)). Báo cáo QA-04 ghi nhận trung thực hiện trạng này và không chuyển thành Pass khi chưa có mã xử lý.
