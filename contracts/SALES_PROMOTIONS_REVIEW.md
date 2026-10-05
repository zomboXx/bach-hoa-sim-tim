# PRO-01B — Sales promotions review

- Status: **Working Draft**, cập nhật ngày 06/10/2026 (Component PR: API quản lý khuyến mãi theo sản phẩm; giữ Issue #23 mở cho phần tiếp theo). Owner API TV3 (Nguyễn Văn Thi); reviewer API & governance TV1 (Nguyễn Đức Phát).
- Issue: [PRO-01B #23](https://github.com/zomboXx/bach-hoa-sim-tim/issues/23).
- Provider PR: [PR #25](https://github.com/zomboXx/bach-hoa-sim-tim/pull/25).
- Wire contract: [sales-promotions.openapi.yaml](sales-promotions.openapi.yaml).

## Permission matrix

| Server role | GET /api/v1/sales/promotions/** | POST/PUT/DELETE /api/v1/sales/promotions/** |
|---|---|---|
| SALES | promotions.read | Denied (403) |
| STOCK | promotions.read | Denied (403) |
| MANAGER | promotions.read | promotions.write |
| ADMIN | promotions.read | promotions.write |

- **Read promotions** (`GET /api/v1/sales/promotions`, `/applicable`, `/{id}`): All authenticated roles holding `promotions.read` can inspect promotion list, details, and active applicable discounts at point of sale.
- **Manage promotions** (`POST/PUT/DELETE`): Restricted to `MANAGER` and `ADMIN` with `promotions.write`.

## Key architectural decisions

1. **Wire path alignment**:
   - Primary route standardized to `/api/v1/sales/promotions`, aligning with Issue #23 and the unified `/api/v1/sales/...` contract scope.
   - Preserves `/api/v1/promotions` as an alias for compatibility.

2. **Discount types and precision**:
   - `AMOUNT`: Fixed discount in integer VND.
   - `PERCENT`: Rate percentage between 0 and 100.

3. **Effective window and scope**:
   - Must satisfy `ends_at > starts_at`.
   - Store scope enforced from `SessionPrincipal`: Mọi thao tác CRUD và tra cứu `findApplicable` đều được ràng buộc tự động theo `principal.storeId()` từ session JWT, loại bỏ `storeId` tự do trong request body để ngăn truy cập trái phép xuyên cửa hàng.
   - `sales.promotion_products`: Phạm vi sản phẩm (product scope). Danh sách rỗng biểu thị khuyến mãi áp dụng cho toàn bộ sản phẩm.
   - **Target BATCH & Tích hợp bán hàng**: Bổ sung `sales.promotion_batches` (target BATCH, không trộn với PRODUCT); chọn mức giảm tối ưu sau phân bổ FEFO trong `quote` và `checkout`; lưu snapshot khuyến mãi bất biến trên `sales.invoice_lines` (kể cả trường hợp hóa đơn 0 VND khi giảm 100%).
   - **Giao diện PWA & Consumer E2E**: Hoãn lại cho issue tiếp theo; Issue #23 tiếp tục mở.

4. **Migration sequencing**:
   - Nhánh `PRO-01B` gồm hai migration:
     - `V10__sales_promotions.sql`: Tạo schema `sales`, bảng `promotions`, `promotion_products`, các indexes và quyền `promotions.read` / `promotions.write`.
     - `V11__promotion_batch_targets_and_invoice_snapshots.sql`: Tạo bảng `sales.promotion_batches` và bổ sung snapshot khuyến mãi bất biến cùng check constraint trên `sales.invoice_lines`.

## Verification evidence — 06/10/2026

- Automated integration tests (`PromotionApiTest`) executed against clean PostgreSQL 17 Testcontainers with `demo` profile:
  - CRUD operations with permissions check (MANAGER/ADMIN allowed, SALES/STOCK denied write).
  - Applicable promotion calculation matching store, product and active time window.
  - Store-scoped isolation: kiểm tra không đọc/sửa/xóa khuyến mãi của store khác cùng tổ chức.
  - Validation: endsAt before startsAt (422), negative discount (422), percent > 100 (422), percent > 2 decimals (422), duplicate code precheck toàn organization (409).
  - Wire alias `/api/v1/sales/promotions` verified.
- Suite result: All promotion tests passing. CI GitHub Actions trên HEAD xanh.
