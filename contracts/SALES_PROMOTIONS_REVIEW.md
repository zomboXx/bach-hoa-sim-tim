# PRO-01B — Sales promotions review

- Status: **Working Draft**, 01/10/2026. Owner API TV3 (Nguyễn Văn Thi); reviewer API & governance TV1 (Nguyễn Đức Phát).
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
   - Optional `store_id`: applies to a specific store when present, or all stores of the organization when null.
   - `sales.promotion_products`: empty product scope implies the promotion applies to all products.

4. **Migration sequencing**:
   - Retains self-contained `V5__sales_promotions.sql` on branch PR #25 to permit clean, independent verification from `main` (V1..V4).
   - Uses `CREATE SCHEMA IF NOT EXISTS sales;` to avoid DDL collision.
   - When merged to `main` following SAL-01, this migration sequences as V7 and introduces the foreign key from `sales.invoice_lines(applied_promotion_id)` to `sales.promotions(id)`.

## Verification evidence — 01/10/2026

- 15 automated integration tests (`PromotionApiTest`) executed against clean PostgreSQL 17 Testcontainers with `demo` profile:
  - CRUD operations with permissions check (MANAGER/ADMIN allowed, SALES/STOCK denied write).
  - Applicable promotion calculation matching store, product and active time window.
  - Validation: endsAt before startsAt (422), negative discount (422), percent > 100 (422), duplicate code (409).
  - Wire alias `/api/v1/sales/promotions` verified.
- Suite result: 15/15 tests passing.
