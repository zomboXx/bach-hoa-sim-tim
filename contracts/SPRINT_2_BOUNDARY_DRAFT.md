# Sprint 2 — Contract tại ranh giới module

- **Working Draft, 30/09/2026.** Provider inventory/report TV2; provider sales TV3; consumer web nhận/tồn TV4; consumer web bán TV3; QA TV4. Đây là mặc định triển khai cho các Issue, chưa được nhóm đánh dấu Accepted.
- [Kickoff Sprint 2](../docs/project/SPRINT_2_KICKOFF.md) ghi phạm vi, owner và quyết định nghiệp vụ. Owner cập nhật contract/OpenAPI và test cùng PR khi cần đổi tên hoặc shape; reviewer của hai phía phải kiểm tra trước merge.

## HTTP tối thiểu cần thống nhất

Mọi route dưới `/api/v1`, dùng Bearer session BE-02. `organizationId`, `storeId`, `userId` lấy từ session, không nhận từ body. Không nhận đơn giá do client tự gửi khi bán. ID ngoài phạm vi trả 404; không có phiên 401, thiếu quyền 403. Response lỗi có `code`, `message`, `fieldErrors` khi có validation. Web xử lý theo `code`, không phân tích câu chữ lỗi.

| Item | Route dự kiến | Quyền đề xuất | Ý nghĩa |
|---|---|---|---|
| INV-01 | `POST /inventory/receipts`; `GET /inventory/receipts`; `GET /inventory/receipts/{id}` | `receipts.write` / `receipts.read` | Xác nhận một lần và đọc phiếu; danh sách lọc được `clientOperationId` |
| INV-02 | `GET /inventory/products`; `GET /inventory/batches`; `GET /inventory/movements` | `inventory.read` | Tồn hiện tại, lô/hạn và biến động có nguồn |
| SAL-01 | `POST /sales/quote`; `POST /sales/checkout`; `GET /sales/invoices`; `GET /sales/invoices/{id}` | `sales.create`; `sales.read.own` hoặc `sales.read.store` | Quote không giữ tồn; checkout hoàn tất nguyên tử; đọc hóa đơn snapshot |
| REP-01 | `GET /reports/revenue`; `GET /reports/inventory` | `reports.read` | Khoảng ngày `[from,to)` theo giờ Việt Nam; chỉ dữ liệu đã commit |
| PRO-01B, P1 | `GET/POST /sales/promotions`; `PUT /sales/promotions/{id}` | `promotions.read/write` | Chỉ đưa vào contract Accepted/route thực tế cùng PR P1 |

Tên permission trên là mặc định cho Issue, chưa cấp grant. SALES chỉ đọc hóa đơn của mình; MANAGER/ADMIN đọc store hiện tại. API mode không fallback sang demo khi 401/403 hoặc lỗi mạng. Không bật route trước khi có kiểm thử quyền.

| Permission | SALES | STOCK | MANAGER | ADMIN |
|---|---|---|---|---|
| `inventory.read` | Có | Có | Có | Có |
| `receipts.read`, `receipts.write` | Không | Có | Có | Có |
| `sales.create`, `sales.read.own` | Có | Không | Có | Có |
| `sales.read.store` | Không | Không | Có | Có |
| `reports.read`, `promotions.write` | Không | Không | Có | Có |
| `promotions.read` | Có | Có | Có | Có |

Quyền chỉ giới hạn trong `organizationId`/`storeId` của session; ADMIN không được xem xuyên store. `promotions.*` chỉ cấp cùng PR P1. Frontend có thể ẩn nút theo permission, server luôn kiểm quyền lại.

## Tên trường dùng chung

| Nhóm | Tên wire đề xuất | Quy tắc |
|---|---|---|
| Nhận hàng | `supplierId`, `receivedAt`, `lines[]` với `productId`, `expectedQuantity`, `deliveredQuantity`, `acceptedQuantity`, `rejectedQuantity`, `unitCost`, `supplierLotNumber`, `expiryDate`, `discrepancyReason` | `accepted + rejected = delivered`; sai lệch/từ chối cần lý do; accepted = 0 không tạo lô/movement |
| Tồn/lô | `productId`, `batchId`, `onHandQuantity`, `availableQuantity`, `expiryDate`, `businessDate` | `availableQuantity` là lượng bán được hôm nay; lô hết hạn hoặc BLOCKED không bán |
| Bán | `cart.lines[]` gồm `productId`, `quantity`; checkout thêm `expectedTotal`, `payment.method = CASH`, `payment.tenderedAmount` | Server tính lại giá, FEFO, giảm và tiền thừa; giá đổi trả conflict để xác nhận lại |
| Hóa đơn | `invoiceId`, `lines[]`, `allocations[]`, `subtotal`, `discountTotal`, `grandTotal`, `changeAmount` | Lưu SKU/tên/giá/lô tại lúc bán; tổng 0 vẫn có hóa đơn và movement |
| Timeout/retry | Header `Idempotency-Key` là UUID; chứng từ có `clientOperationId` | POST lại cùng key và cùng nội dung không ghi lần hai; GET theo key trong phạm vi quyền hiện tại |

Quantity trên wire là chuỗi decimal dương, tối đa 11 chữ số nguyên và 3 chữ số lẻ; Java dùng `BigDecimal`, DB dùng `numeric(14,3)`. Kiểm scale của `catalog.units.precision_scale` (EA = 0, KG = 3) **trước** khi DB có thể làm tròn. Tiền là JSON integer VND trong khoảng 0..9007199254740991; Java kiểm tràn trước khi đổi sang `long`. `unitCost` không xuất hiện trong API tồn dành cho SALES. Thời gian là RFC3339 có offset; ngày nghiệp vụ theo `Asia/Ho_Chi_Minh`.

## Quy tắc để provider và consumer triển khai giống nhau

1. **Nhận hàng:** `acceptedQuantity + rejectedQuantity = deliveredQuantity`; nếu rejected > 0 hoặc delivered khác expected thì `discrepancyReason` không trắng. accepted > 0 tạo một batch nội bộ cho từng receipt line, balance và RECEIPT movement cùng transaction; accepted = 0 chỉ lưu dòng phiếu. Product theo dõi hạn phải có `expiryDate` không trước ngày xác nhận ở Việt Nam. Phiếu đã CONFIRMED chỉ đọc.
2. **Tồn và hạn:** `expiryDate < businessDate` là EXPIRED; từ hôm nay đến hôm nay + 7 ngày là NEAR_EXPIRY; sau đó VALID; null là NO_EXPIRY. `availableQuantity` chỉ cộng batch AVAILABLE, còn hạn và balance > 0. Endpoint Sprint 2 đọc tồn **hiện tại**; không cung cấp `asOfDate`/tồn lịch sử.
3. **FEFO:** checkout chọn lô AVAILABLE còn hạn theo `expiryDate` tăng dần, null sau cùng, rồi ngày nhận và `batchId`. Receipt và checkout khóa product theo UUID tăng dần; checkout khóa balance theo UUID tăng dần trước khi chọn lô. Không dùng `SKIP LOCKED`; thiếu tồn ở một sản phẩm rollback cả giỏ.
4. **Quote/checkout:** quote không giữ hàng. Checkout gửi lại giỏ và `expectedTotal`; server chụp một thời điểm sau khóa, đọc giá đang hiệu lực trong `catalog.product_prices` khoảng `[effective_from,effective_to)`, tính lại tồn/giá và trả `PRICE_CHANGED` nếu tổng khác. Client không gửi `unitPrice`. CASH `tenderedAmount >= grandTotal`; một payment bằng grand total, tiền thừa do server tính. Nếu grand total = 0, payment/tendered/change đều 0, hóa đơn vẫn COMPLETED và trừ tồn.
5. **Làm tròn:** với mỗi invoice line, `gross = HALF_UP(quantity × unitPrice)` VND; `discount = min(gross, HALF_UP(quantity × discountPerUnit))`; `lineTotal = gross - discount`. Header `subtotal`, `discountTotal`, `grandTotal` là tổng các line tương ứng. Trước PRO-01B, discount = 0. Không làm tròn đơn giá giảm trước khi nhân quantity.
6. **Khuyến mãi P1:** chọn lô theo FEFO rồi chọn đúng một promotion ACTIVE đang hiệu lực cho mỗi phần lô; giảm thực lớn nhất thắng, hòa thì UUID nhỏ nhất. Target PRODUCT hoặc BATCH cùng store, không trộn; giảm PERCENT (0,100] tối đa hai chữ số lẻ hoặc AMOUNT VND/đơn vị, giới hạn không vượt gross. Lưu snapshot promotion/giá/giảm trên invoice; sửa promotion không sửa hóa đơn cũ.
7. **Retry online:** POST nhận/checkout bắt buộc UUID `Idempotency-Key`, lưu `clientOperationId` và hash payload đã chuẩn hóa trong chứng từ, unique theo loại chứng từ + organization/store. Cùng key/payload/actor trả chứng từ cũ, không ghi movement/payment/audit lần hai; cùng key với payload hoặc actor khác trả 409. Sau timeout, UI giữ key và nội dung, GET danh sách lọc theo `clientOperationId`; kết quả GET rỗng chưa chứng minh POST thất bại, nên chỉ thử lại cùng key. Không tự queue POST khi offline, không lưu Bearer token bền.
8. **Báo cáo:** doanh thu SUM `sales.invoices.grand_total` của hóa đơn COMPLETED, COUNT invoice một lần trong khoảng `[from,to)` theo giờ Việt Nam. Tồn báo cáo từ current balances/batch status, không suy ra từ response web demo; cần test đối chiếu ledger và hóa đơn độc lập.

Lỗi thống nhất `{code,message,fieldErrors?}`. Các code tối thiểu: `INVALID_REQUEST` (400), `UNAUTHENTICATED` (401), `FORBIDDEN` (403), `NOT_FOUND` (404), `INSUFFICIENT_STOCK`, `PRICE_CHANGED`, `IDEMPOTENCY_KEY_REUSED` (409), `QUANTITY_PRECISION`, `PRICE_UNAVAILABLE`, `INVALID_RECEIPT`, `INSUFFICIENT_CASH` (422), `TEMPORARILY_UNAVAILABLE` (503). Provider trả field path như `lines[0].acceptedQuantity`; không lộ SQL/token.

## Dữ liệu theo PR sở hữu

| Mốc | Bảng / thay đổi tối thiểu | Owner |
|---|---|---|
| INV-01 | `inventory.goods_receipts`, `goods_receipt_lines`, `product_batches`, `inventory_balances`, `stock_movements` (RECEIPT), `audit.audit_logs` | TV2, migration tiếp theo sau V4 |
| SAL-01 | `sales.invoices`, `invoice_lines`, `invoice_line_batches`, `payments`; mở ledger cho SALE và FK tới invoice allocation | TV3, TV2 review migration và khóa |
| PRO-01B | `sales.promotions`, `promotion_products`, `promotion_batches`; thêm promotion snapshot vào invoice line | TV3, migration riêng sau SAL-01 |
| REP-01 | Query/read model từ bảng đã commit, không tạo bảng tổng hợp | TV2; TV3 review web consumer |

Giữ FK cùng organization/store/product, chứng từ không cascade xóa, `balance` không âm và ledger có nguồn đúng loại. `balance`/header total là dữ liệu dẫn xuất được cập nhật nguyên tử; invoice SKU/tên/giá là snapshot lịch sử. Các ngoại lệ chuẩn hóa 3NF này giữ toàn vẹn chức năng và phải được QA đối chiếu. Migration đã merge chỉ sửa bằng migration mới; test database sạch và upgrade từ V4.

## Public interface nội bộ đề xuất

| Tên | Module công bố | Consumer cần gì |
|---|---|---|
| `CatalogSaleView` | `catalog/application`, TV2 | Product ACTIVE, đơn vị/precision và giá hiệu lực tại thời điểm checkout; không đưa catalog JPA entity sang sales |
| `InventorySalePort` | `inventory/application`, TV2 | Trong transaction của sales: khóa product/balance cùng thứ tự với receipt, lập FEFO plan và thời điểm nghiệp vụ; sau khi sales tạo invoice/allocation, trừ balance và ghi SALE movement tham chiếu allocation |
| `FefoAllocation` | DTO công khai của `inventory/application`, TV2 | `productId`, `batchId`, `quantity`, `expiryDate`, `receivedDate`; không chứa JPA entity hoặc giá nhập |

Tên method/kiểu đề xuất để TV2/TV3 bắt đầu cùng một hướng:

```java
interface CatalogSaleView {
    List<SaleProduct> findActivePricedProducts(StoreScope scope, List<UUID> productIds, Instant capturedAt);
}
interface InventorySalePort {
    FefoPlan planForCheckout(StoreScope scope, List<StockDemand> demands);
    void postSale(FefoPlan plan, List<SaleIssue> issuedAllocations);
}
```

`StoreScope` chỉ chứa organization/store từ session; `StockDemand` là product/quantity; `FefoPlan` giữ `capturedAt`, `businessDate` và allocations sau khi đã khóa; `SaleIssue` nối invoice line + batch + quantity để ghi movement. TV3 gọi port, sở hữu quote/checkout, invoice/payment và transaction. Port không tự commit hoặc gọi HTTP. TV2/TV3 chốt chữ ký Java biên dịch được trong PR đầu tiên dùng port; không tạo lớp rỗng trước consumer thật. Web giữ interface adapter theo feature; demo/API là hai implementation, cùng tên DTO công khai. Tên biến private và component con thuộc owner feature.

INV-02 triển khai chữ ký trên bằng các immutable DTO trong `inventory/application`: `StoreScope`, `StockDemand`, `FefoPlan`, `FefoAllocation` và `SaleIssue`. `SaleIssue` bổ sung `movementId`, `invoiceLineId` và `actorId` để movement có identity, chứng từ nguồn và người ghi nhận mà không truyền JPA entity. Adapter dùng `Propagation.MANDATORY`; compile-level consumer test tại `sale/InventorySalePortContractTest` chứng minh module sales gọi được boundary. TV3 vẫn phải review/xác nhận và đổi consumer SAL-01 cũ từ `InventoryPort` sang boundary này trước khi merge hai module.

## Các bất biến để viết test

- Receipt tạo phiếu/dòng/lô/balance/RECEIPT movement nguyên tử; checkout tạo hóa đơn/dòng/phân bổ/payment/SALE movement và giảm balance nguyên tử. Lỗi bất kỳ bước nào rollback toàn bộ.
- FEFO chọn lô còn hạn theo `expiryDate`, ngày nhận, rồi `batchId`; giá/khuyến mãi không được đổi thứ tự lô. Quote chỉ là tính thử, không giữ tồn.
- `grandTotal = subtotal - discountTotal`; một payment CASH bằng `grandTotal`; với tổng 0, tiền nhận và tiền thừa đều 0. Replay không ghi thêm chứng từ/biến động/audit.
- Doanh thu cộng hóa đơn COMPLETED một lần; tồn đối chiếu được với tổng movement từng lô. Training không ghi vào các bảng vận hành.

Các tên/shape trên là mặc định để bắt đầu. TV2/TV3 xác nhận chữ ký thật và transaction boundary trong PR đầu tiên dùng port; TV4 xác nhận test oracle. Provider PR thêm OpenAPI/provider tests và consumer PR thêm consumer tests cùng endpoint được triển khai. Thay đổi contract đi cùng hai phía và test trong PR có reviewer liên quan.
