# INV-03 API Duyệt điều chỉnh tồn kiểm kê — Contract

**Status:** Accepted — Provider đã triển khai.

**Owner (provider):** TV2 — Nguyễn Văn Trung

**Reviewer:** TV1 — Nguyễn Đức Phát

**Phụ thuộc:** SYN-02 (#45), V14 migration

Tài liệu này ghi wire contract và quy tắc giao dịch chính thức cho Issue #46 (INV-03).

---

## Quyền và Vai trò

| Permission            | Vai trò được cấp  | Mô tả |
|-----------------------|-------------------|-------|
| `stocktakes.approve`  | MANAGER, ADMIN    | Duyệt điều chỉnh tồn kho của phiên kiểm kê trong store scope |

Scope (`organizationId`, `storeId`) luôn lấy từ Bearer session token; không tin bất kỳ field scope nào truyền từ client.
Tài khoản thiếu quyền `stocktakes.approve` (như `STOCK`, `SALES`) khi gọi endpoint duyệt sẽ nhận **403 Forbidden**.

---

## Routes

### POST `/api/v1/inventory/stocktakes/{id}/approve`

Quản lý duyệt điều chỉnh tồn kho cho toàn bộ các dòng của phiên kiểm kê `{id}`.

**Quyền:** `stocktakes.approve`

**Headers:**
- `Authorization: Bearer <token>`

**Request Body:** Trống (hoặc `{}`)

**Response 200 OK:**
Trả về phiên kiểm kê với trạng thái `APPROVED`, `submittedAt` và danh sách các dòng mang trạng thái `APPROVED`:

```json
{
  "id": "<uuid>",
  "organizationId": "<uuid>",
  "storeId": "<uuid>",
  "actorId": "<uuid>",
  "status": "APPROVED",
  "openedAt": "2026-10-10T02:00:00Z",
  "submittedAt": "2026-10-10T03:00:00Z",
  "lines": [
    {
      "id": "<uuid>",
      "stocktakeId": "<uuid>",
      "productId": "<uuid>",
      "batchId": "<uuid>",
      "clientOperationId": "<uuid>",
      "expectedQuantity": 100.000,
      "actualQuantity": 105.000,
      "baseVersion": 1,
      "note": "Kiểm kê kho",
      "status": "APPROVED",
      "conflictReason": null,
      "countedAt": "2026-10-10T02:30:00Z"
    }
  ]
}
```

---

## Quy tắc giao dịch & Tính nguyên tử (AC / DoD)

Tất cả các bước sau được thực thi trong **một database transaction duy nhất**:

1. **Kiểm tra phiên & Scope:**
   - Phiên phải tồn tại trong đúng `organizationId` và `storeId` của token Bearer (nếu khác store -> trả **404 NOT_FOUND**, không làm lộ dữ liệu store khác).
   - Phiên không được duyệt hai lần: nếu `status == 'APPROVED'` -> từ chối với **409 CONFLICT** (`code: ALREADY_APPROVED`).
   - Phiên không thể duyệt nếu bị hủy: nếu `status == 'CANCELLED'` -> trả **422 UNPROCESSABLE_ENTITY** (`code: SESSION_CANCELLED`).
   - Phiên không có dòng kiểm kê nào -> trả **422 UNPROCESSABLE_ENTITY** (`code: EMPTY_STOCKTAKE`).
   - Nếu phiên có dòng đang mang trạng thái `CONFLICT` -> từ chối duyệt với **409 CONFLICT** (`code: UNRESOLVED_CONFLICT`).

2. **Khóa và kiểm tra phiên bản tồn kho (Optimistic & Pessimistic Lock):**
   - Đặt khóa `PESSIMISTIC_WRITE` trên từng `inventory_balances` của lô.
   - Kiểm tra `balance.version == line.baseVersion`.
   - Nếu `balance.version != line.baseVersion`: tồn kho đã thay đổi kể từ lúc lấy snapshot kiểm kê (bán hàng/nhập hàng diễn ra trước khi duyệt).
     -> Từ chối duyệt với **409 CONFLICT** (`code: BALANCE_VERSION_MISMATCH`).
     -> **Rollback toàn bộ transaction**, không ghi một phần bất kỳ lô nào.

3. **Cập nhật Balance & Ledger (Biến động kho):**
   - `inventory_balances.on_hand_quantity = line.actualQuantity`.
   - Tồn kho không được âm: `actualQuantity >= 0` (nếu âm trả **422 NEGATIVE_BALANCE**).
   - Tính chênh lệch: `delta = line.actualQuantity - balance.onHandQuantity`.
   - Nếu `delta != 0`:
     - Tạo 1 bản ghi vào `inventory.stock_movements`:
       - `movement_type = 'STOCKTAKE_ADJUSTMENT'`
       - `quantity_delta = delta`
       - `reference_id = stocktakeId`
       - `reference_type = 'STOCKTAKE'`
       - `recorded_by = managerUserId`
   - Nếu `delta == 0`: không tạo movement mới vì không phát sinh biến động điều chỉnh kho.
   - **Đối soát ledger độc lập:** `balance.on_hand_quantity == SUM(quantity_delta)` của tất cả movements của lô đó.

4. **Cập nhật trạng thái:**
   - Cập nhật dòng `stocktake_lines.status = 'APPROVED'`.
   - Cập nhật phiên `stocktakes.status = 'APPROVED'` và `submitted_at = now()`.
   - Ghi nhật ký kiểm toán vào `audit.audit_logs`.

---

## Mã lỗi HTTP

| HTTP Code | Error Code | Mô tả |
|-----------|------------|-------|
| 401 | `UNAUTHENTICATED` | Chưa đăng nhập hoặc token không hợp lệ |
| 403 | `FORBIDDEN` | Tài khoản thiếu quyền `stocktakes.approve` |
| 404 | `NOT_FOUND` | Không tìm thấy phiên kiểm kê trong store hiện tại |
| 409 | `ALREADY_APPROVED` | Phiên đã được duyệt trước đó (từ chối duyệt hai lần) |
| 409 | `BALANCE_VERSION_MISMATCH` | Tồn kho của lô đã đổi trước khi duyệt |
| 409 | `UNRESOLVED_CONFLICT` | Phiên có dòng kiểm kê đang bị CONFLICT chưa xử lý |
| 422 | `EMPTY_STOCKTAKE` | Phiên kiểm kê không có dòng kiểm nào |
| 422 | `SESSION_CANCELLED` | Phiên kiểm kê đã bị hủy |
| 422 | `NEGATIVE_BALANCE` | Số lượng kiểm kê thực tế âm |
