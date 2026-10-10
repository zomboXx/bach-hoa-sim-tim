# SYN-02 API đồng bộ kiểm kê — Contract

**Status:** Accepted — Provider đã triển khai; consumer (SYN-01 queue) có thể nối sau khi review PR.

**Owner (provider):** TV2 — Nguyễn Văn Trung

**Reviewer:** TV4 — Lê Văn Chiến

**Phụ thuộc:** SYN-01 (#44), V12/V13 migration

Tài liệu này ghi wire contract chính thức cho Issue #45. Nó thay thế phần "Điểm SYN-02 phải chốt" trong `SYN-01_OFFLINE_QUEUE_REVIEW.md`.

---

## Quyền

| Permission        | Vai trò được cấp       |
|-------------------|------------------------|
| `stocktakes.read` | STOCK, MANAGER, ADMIN  |
| `stocktakes.write`| STOCK, MANAGER         |

Scope (organizationId, storeId) luôn lấy từ Bearer session; không tin bất kỳ field scope nào trong body.

---

## Routes

### POST `/api/v1/inventory/stocktakes`

Mở phiên OPEN mới cho actor+store hiện tại, hoặc trả lại phiên đang mở (idempotent theo actor+store).

**Quyền:** `stocktakes.write`

**Response 201** (phiên mới) hoặc **200** (phiên đang mở):

```json
{
  "id": "<uuid>",
  "organizationId": "<uuid>",
  "storeId": "<uuid>",
  "actorId": "<uuid>",
  "status": "OPEN",
  "openedAt": "<ISO-8601>",
  "submittedAt": null,
  "lines": []
}
```

---

### GET `/api/v1/inventory/stocktakes/{id}`

Đọc phiên và tất cả dòng trong phạm vi org/store của session.

**Quyền:** `stocktakes.read`

**Response 200:**

```json
{
  "id": "<uuid>",
  "status": "OPEN",
  "lines": [
    {
      "id": "<uuid>",
      "stocktakeId": "<uuid>",
      "productId": "<uuid>",
      "batchId": "<uuid>",
      "clientOperationId": "<uuid>",
      "expectedQuantity": 100.000,
      "actualQuantity": 98.000,
      "baseVersion": 3,
      "note": "",
      "status": "PENDING",
      "conflictReason": null,
      "countedAt": "<ISO-8601>"
    }
  ]
}
```

**Response 404** nếu sessionId không tồn tại trong phạm vi store của session Bearer.

---

### POST `/api/v1/inventory/stocktakes/{sessionId}/counts`

Gửi số đếm thực tế cho một lô. **Idempotent** theo `clientOperationId` trong phạm vi org/store.

**Quyền:** `stocktakes.write`

**Request body:**

```json
{
  "clientOperationId": "<uuid>",
  "batchId": "<uuid>",
  "actualQuantity": 98.000,
  "baseVersion": 3,
  "note": "Kiểm kho sáng 10/10",
  "countedAt": "2026-10-10T02:30:00Z"
}
```

| Field              | Bắt buộc | Mô tả |
|--------------------|----------|-------|
| `clientOperationId`| ✓        | UUID ổn định của thao tác này; tạo một lần trên client |
| `batchId`          | ✓        | UUID lô hàng đang đếm |
| `actualQuantity`   | ✓        | Số đếm thực tế (>= 0, tối đa 3 chữ số thập phân) |
| `baseVersion`      | ✓        | `version` của balance khi client lấy snapshot |
| `note`             |          | Ghi chú tự do (tối đa 500 ký tự) |
| `countedAt`        |          | Thời điểm đếm trên thiết bị; nếu null server dùng `now()` |

**Response 200 — PENDING (thành công):**

```json
{
  "id": "<uuid>",
  "stocktakeId": "<uuid>",
  "clientOperationId": "<uuid>",
  "batchId": "<uuid>",
  "status": "PENDING",
  "expectedQuantity": 100.000,
  "actualQuantity": 98.000,
  "baseVersion": 3,
  "conflictReason": null
}
```

**Response 409 — CONFLICT (version lệch):**

```json
{
  "code": "CONFLICT",
  "message": "Tồn kho lô <id> đã thay đổi (baseVersion=3, currentVersion=5). Hãy lấy snapshot mới và kiểm lại.",
  "clientOperationId": "<uuid>",
  "batchId": "<uuid>",
  "baseVersion": 3,
  "currentStatus": "CONFLICT"
}
```

> Khi nhận CONFLICT, dòng **đã được lưu** với status=CONFLICT. Client KHÔNG tự retry; hiển thị lý do và yêu cầu người dùng đếm lại.

**Response 409 — IDEMPOTENCY_KEY_REUSED:**

```json
{
  "code": "IDEMPOTENCY_KEY_REUSED",
  "message": "clientOperationId đã dùng trong phiên khác."
}
```

---

## Idempotency

| Tình huống | Server trả |
|---|---|
| Cùng `clientOperationId` + cùng sessionId + bất kỳ lần gọi nào | 200, dòng cũ |
| Cùng `clientOperationId` nhưng khác sessionId (cùng org/store) | 409 IDEMPOTENCY_KEY_REUSED |

Server **không** kiểm tra hash payload; chỉ dựa vào `(clientOperationId, organizationId, storeId)` là khóa duy nhất (`UNIQUE (organization_id, client_operation_id)` trên bảng `stocktake_lines`).

---

## Conflict detection

Server so sánh `cmd.baseVersion` với `inventory_balances.version` của lô tại thời điểm nhận request:

- `baseVersion == currentVersion` → **PENDING** (không cập nhật tồn — INV-03 làm việc đó)
- `baseVersion != currentVersion` → **CONFLICT** (lưu dòng với status=CONFLICT, trả 409)

Không có side-effect nào lên `inventory_balances` hoặc `stock_movements` khi chỉ gửi count.

---

## Bằng chứng provider

`StocktakeSyncApiTest.java` chạy trên PostgreSQL 17 sạch qua Testcontainers, kiểm tra:

- `openSession_stock_returns201WithOpenStatus`
- `openSession_calledTwice_returnsSameSession`
- `openSession_sales_returns403`
- `submitCount_correctVersion_returnsPending200`
- `submitCount_wrongVersion_returns409Conflict` — và xác nhận balance version không đổi
- `submitCount_idempotency_sameKeyAndScope_returnsSameLine`
- `submitCount_sales_returns403`
- `submitCount_unknownBatch_returns404`
- `getSession_stock_returnsSessionWithLines`
- `getSession_otherStore_returns404`
