# Sprint 2 — Contract tại ranh giới module

- **Draft để TV2, TV3, TV4 review, 30/09/2026.** Provider inventory TV2; provider sales/report TV3; consumer web TV3; QA TV4.
- [Kế hoạch review](../docs/project/SPRINT_2_REVIEW_PLAN.md) ghi phạm vi, owner và các quyết định đã xác nhận. Tài liệu này chỉ đặt tên phần hai nhánh cần dùng chung. Chữ ký Java và wire schema cuối cùng phải được provider/consumer xác nhận trước code.

## HTTP tối thiểu cần thống nhất

Mọi route dưới `/api/v1`, dùng Bearer session BE-02. `organizationId`, `storeId`, `userId` lấy từ session, không nhận từ body. Không nhận đơn giá do client tự gửi khi bán. ID ngoài phạm vi trả 404; không có phiên 401, thiếu quyền 403. Response lỗi có `code`, `message`, `fieldErrors` khi có validation. Web xử lý theo `code`, không phân tích câu chữ lỗi.

| Item | Route dự kiến | Quyền đề xuất | Ý nghĩa |
|---|---|---|---|
| INV-01 | `POST /inventory/receipts`; `GET /inventory/receipts`; `GET /inventory/receipts/{id}` | `receipts.write` / `receipts.read` | Xác nhận một lần và đọc phiếu; danh sách lọc được `clientOperationId` |
| INV-02 | `GET /inventory/products`; `GET /inventory/batches`; `GET /inventory/movements` | `inventory.read` | Tồn hiện tại, lô/hạn và biến động có nguồn |
| SAL-01 | `POST /sales/quote`; `POST /sales/checkout`; `GET /sales/invoices`; `GET /sales/invoices/{id}` | `sales.create`; `sales.read.own` hoặc `sales.read.store` | Quote không giữ tồn; checkout hoàn tất nguyên tử; đọc hóa đơn snapshot |
| REP-01 | `GET /reports/revenue`; `GET /reports/inventory` | `reports.read` | Khoảng ngày `[from,to)` theo giờ Việt Nam; chỉ dữ liệu đã commit |
| PRO-01B, P1 | `GET/POST /sales/promotions`; `PUT /sales/promotions/{id}` | `promotions.read/write` | Chỉ đưa vào contract Accepted/route thực tế cùng PR P1 |

Tên permission trên là **đề xuất**, chưa cấp grant. SALES chỉ đọc hóa đơn của mình; MANAGER/ADMIN đọc store hiện tại. API mode không fallback sang demo khi 401/403 hoặc lỗi mạng. Không bật route trước khi có kiểm thử quyền.

## Tên trường dùng chung

| Nhóm | Tên wire đề xuất | Quy tắc |
|---|---|---|
| Nhận hàng | `supplierId`, `receivedAt`, `lines[]` với `productId`, `expectedQuantity`, `deliveredQuantity`, `acceptedQuantity`, `rejectedQuantity`, `unitCost`, `supplierLotNumber`, `expiryDate`, `discrepancyReason` | `accepted + rejected = delivered`; sai lệch/từ chối cần lý do; accepted = 0 không tạo lô/movement |
| Tồn/lô | `productId`, `batchId`, `onHandQuantity`, `availableQuantity`, `expiryDate`, `businessDate` | `availableQuantity` là lượng bán được hôm nay; lô hết hạn hoặc BLOCKED không bán |
| Bán | `cart.lines[]` gồm `productId`, `quantity`; checkout thêm `expectedTotal`, `payment.method = CASH`, `payment.tenderedAmount` | Server tính lại giá, FEFO, giảm và tiền thừa; giá đổi trả conflict để xác nhận lại |
| Hóa đơn | `invoiceId`, `lines[]`, `allocations[]`, `subtotal`, `discountTotal`, `grandTotal`, `changeAmount` | Lưu SKU/tên/giá/lô tại lúc bán; tổng 0 vẫn có hóa đơn và movement |
| Timeout/retry | Header `Idempotency-Key` là UUID; chứng từ có `clientOperationId` | POST lại cùng key và cùng nội dung không ghi lần hai; GET theo key trong phạm vi quyền hiện tại |

Quantity trên wire là chuỗi decimal tối đa ba chữ số lẻ để tránh mất precision ở JavaScript; tiền là JSON integer VND. TV2/TV3 cần chốt giới hạn số nguyên, cách làm tròn và field lỗi trước khi đưa vào OpenAPI. `classificationDate` nếu thêm bộ lọc hạn `asOfDate` phải phân biệt với `businessDate` và không ngụ ý tồn lịch sử. Không đưa `unitCost` vào API tồn dành cho SALES.

## Public interface nội bộ đề xuất

| Tên | Module công bố | Consumer cần gì |
|---|---|---|
| `CatalogSaleView` | `catalog/application`, TV2 | Product ACTIVE, đơn vị/precision và giá hiệu lực tại thời điểm checkout; không đưa catalog JPA entity sang sales |
| `InventorySalePort` | `inventory/application`, TV2 | Trong transaction của sales: khóa product/balance cùng thứ tự với receipt, lập FEFO plan và thời điểm nghiệp vụ; sau khi sales tạo invoice/allocation, trừ balance và ghi SALE movement tham chiếu allocation |
| `FefoAllocation` | DTO công khai của `inventory/application`, TV2 | `productId`, `batchId`, `quantity`, `expiryDate`, `receivedDate`; không chứa JPA entity hoặc giá nhập |

TV3 gọi port, sở hữu quote/checkout, invoice/payment và transaction. Port không tự commit hoặc gọi HTTP. Tên method và record Java cụ thể do TV2/TV3 chốt bằng một đoạn interface biên dịch được trong PR đầu tiên cần dùng; không tạo bộ class rỗng trước khi biết consumer thật. Web giữ interface adapter theo feature; demo/API là hai implementation, cùng tên DTO công khai. Tên biến private và component con thuộc owner feature.

## Các bất biến để viết test

- Receipt tạo phiếu/dòng/lô/balance/RECEIPT movement nguyên tử; checkout tạo hóa đơn/dòng/phân bổ/payment/SALE movement và giảm balance nguyên tử. Lỗi bất kỳ bước nào rollback toàn bộ.
- FEFO chọn lô còn hạn theo `expiryDate`, ngày nhận, rồi `batchId`; giá/khuyến mãi không được đổi thứ tự lô. Quote chỉ là tính thử, không giữ tồn.
- `grandTotal = subtotal - discountTotal`; một payment CASH bằng `grandTotal`; với tổng 0, tiền nhận và tiền thừa đều 0. Replay không ghi thêm chứng từ/biến động/audit.
- Doanh thu cộng hóa đơn COMPLETED một lần; tồn đối chiếu được với tổng movement từng lô. Training không ghi vào các bảng vận hành.

Trước Accepted: TV2/TV3 xác nhận tên/shape và transaction boundary, TV4 xác nhận test oracle; provider PR thêm OpenAPI chính thức và provider/consumer tests cùng endpoint được triển khai. Thay đổi contract về sau đi cùng hai phía và test trong PR có reviewer liên quan.
