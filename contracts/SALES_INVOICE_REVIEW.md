# SAL-01 — Sales invoices and checkout review

- Status: **Working Draft**, 01/10/2026. Owner API TV3 (Nguyễn Văn Thi); reviewer API TV2 (Nguyễn Văn Trung) for inventory boundary, reviewer API & governance TV1 (Nguyễn Đức Phát) for wire routes & RBAC.
- Issue: [SAL-01 #21](https://github.com/zomboXx/bach-hoa-sim-tim/issues/21).
- Provider PR: [PR #26](https://github.com/zomboXx/bach-hoa-sim-tim/pull/26).
- Wire contract: [sales-invoices.openapi.yaml](sales-invoices.openapi.yaml).

## Permission matrix

| Server role | POST /api/v1/sales/quote | GET /api/v1/sales/invoices/** | POST /api/v1/sales/invoices (checkout) |
|---|---|---|---|
| SALES | sales.read | sales.read | sales.write |
| STOCK | sales.read | sales.read | Denied (403) |
| MANAGER | sales.read | sales.read | sales.write |
| ADMIN | sales.read | sales.read | sales.write |

- **Quote preview** (`POST /api/v1/sales/quote`): Read-only computation that calculates totals based on `catalog.product_prices`. Permitted for all authenticated roles holding `sales.read`.
- **Checkout CASH** (`POST /api/v1/sales/invoices`): Creates invoice, executes FEFO batch deduction, records stock movements and cash payment. Requires `sales.write` (SALES, MANAGER, ADMIN). Denied for STOCK.
- **Invoice query** (`GET /api/v1/sales/invoices`, `GET /api/v1/sales/invoices/{id}`): Requires `sales.read` within store scope.

## Key architectural decisions

1. **Wire path & permissions alignment**:
   - Standardized route to `/api/v1/sales/...` (with alias `/api/v1/sales/invoices/quote`), aligning with Issue #21 and the unified Sprint 2 wire contract convention.
   - Permissions standardized to `sales.read` and `sales.write` in IAM migrations (V6) and `AuthSecurity`.

2. **Public Inventory boundary port**:
   - `SaleService` no longer owns or accesses inventory tables directly.
   - Cross-module interaction uses `vn.simtim.api.inventory.application.InventoryPort`:
     - `findAvailableBatchesFEFO(orgId, storeId, productId)`
     - `deductBalance(orgId, storeId, batchId, quantity)`
     - `recordSaleMovement(id, orgId, storeId, batchId, quantityDelta, invoiceLineId, actorUserId, occurredAt)`
   - Runs in the same Spring `@Transactional` transaction context with sales writes, ensuring atomicity and ACID compliance.

3. **Concurrency and stock integrity**:
   - Uses `SELECT ... FOR UPDATE` (PESSIMISTIC_WRITE) on `inventory.inventory_balances` when allocating and deducting.
   - FEFO batch order: `expiry_date ASC NULLS LAST, received_date ASC`. Expired batches are excluded.

4. **Monetary precision**:
   - All monetary values are integer VND (`bigint` in PostgreSQL, `long` in Java and OpenAPI).
   - Quantity allows up to 3 decimal places (`numeric(14,3)`).

## Verification evidence — 01/10/2026

- 16 automated integration tests (`SaleApiTest`) executed against clean PostgreSQL 17.11 Testcontainers:
  - Quote calculation and validation (positive, unknown product 422/404, empty items 4xx).
  - Quote access by `STOCK` role succeeds (200 OK) with `sales.read`.
  - Cash checkout with FEFO allocation, balance deduction and `inventory.stock_movements` creation (201 Created).
  - Multiple products, insufficient stock conflict (409 Conflict), cash less than total validation (422).
  - Authorization enforcement: `STOCK` checkout attempt denied (403 Forbidden); unauthenticated request denied (401 Unauthorized).
  - Store invoice listing and detail retrieval (200 OK), non-existent invoice (404 Not Found).
- Total API test suite: 47/47 passing tests.
