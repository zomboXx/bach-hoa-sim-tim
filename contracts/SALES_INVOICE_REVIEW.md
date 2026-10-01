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
   - Standardized routes to `/api/v1/sales/...`:
     - Quote preview: `POST /api/v1/sales/quote` (alias: `/api/v1/sales/invoices/quote`).
     - Checkout CASH: `POST /api/v1/sales/checkout` (alias: `/api/v1/sales/invoices`).
     - Invoices query: `GET /api/v1/sales/invoices` and `GET /api/v1/sales/invoices/{id}`.
   - Permissions standardized to `sales.read` and `sales.write` in IAM migrations (V6) and `AuthSecurity`.
   - Store scope enforcement: operations strictly bounded to session store (`SessionPrincipal.storeId()`); cross-store access rejected with 422/404.

2. **Public Inventory boundary port**:
   - `SaleService` interacts with inventory through public boundary port `vn.simtim.api.inventory.application.InventoryPort` using clean domain transfer models (`BatchStock`, `InventoryConflictException`) owned by the inventory package.
   - Cross-module interaction methods:
     - `findAvailableBatchesFEFO(orgId, storeId, productId)`
     - `deductBalance(orgId, storeId, batchId, quantity)`
     - `recordSaleMovement(id, orgId, storeId, batchId, quantityDelta, invoiceLineId, actorUserId, occurredAt)`
   - Runs in the same Spring `@Transactional` transaction context with sales writes, ensuring atomicity and ACID compliance.

3. **Concurrency and stock integrity**:
   - Uses `SELECT ... FOR UPDATE` (PESSIMISTIC_WRITE) on `inventory.inventory_balances` when allocating and deducting.
   - FEFO batch order: `expiry_date ASC NULLS LAST, received_date ASC`. Expired batches are excluded.

4. **Monetary precision, changeAmount and 0 VND support**:
   - All monetary values are integer VND (`bigint` in PostgreSQL, `long` in Java and OpenAPI).
   - Quantity allows up to 3 decimal places (`numeric(14,3)`).
   - `Payment.amount` and `Invoice.paidTotal` record the exact `grandTotal` owed.
   - Customer change (`changeAmount = cashAmount - grandTotal`) is computed and returned on the invoice response and persisted in `sales.invoices`.
   - 0 VND checkout (100% discount / free item) supported with `cashAmount = 0`, payment of 0 VND, and full stock deduction as mandated by Project Owner decision on 30/09/2026.

## Verification evidence — 01/10/2026

- 19 automated integration tests (`SaleApiTest`) executed against clean PostgreSQL 17.11 Testcontainers:
  - Quote calculation and validation (positive, unknown product 422/404, empty items 4xx).
  - Quote access by `STOCK` role succeeds (200 OK) with `sales.read`.
  - Cash checkout with FEFO allocation, balance deduction, movement and payment creation (201 Created).
  - Checkout alias route `POST /api/v1/sales/checkout` works identically to `/invoices`.
  - Store scope violation returns 422 Unprocessable Entity.
  - 0 VND order with cashAmount = 0 completes successfully and reduces stock.
  - Correct `paidTotal`, `changeAmount`, and `Payment.amount` calculations.
  - Multiple products, insufficient stock conflict (409 Conflict), cash less than total validation (422).
  - Authorization enforcement: `STOCK` checkout attempt denied (403 Forbidden); unauthenticated request denied (401 Unauthorized).
  - Store invoice listing and detail retrieval (200 OK), non-existent invoice (404 Not Found).
- Total API test suite: 50/50 passing tests.
