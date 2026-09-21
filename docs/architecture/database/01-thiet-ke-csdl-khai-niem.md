# Thiết kế cơ sở dữ liệu khái niệm và logic

## 1. Mục tiêu thiết kế

Cơ sở dữ liệu phục vụ một cửa hàng bán lẻ mẫu trên kiến trúc client–server triển khai tập trung. PWA và Godot sử dụng chung backend API; chỉ backend được phép cập nhật cơ sở dữ liệu. Mô hình phải bảo đảm tính nhất quán giữa nhận hàng, lô hàng, tồn kho, hóa đơn và báo cáo, đồng thời chừa đường mở rộng sang nhiều tổ chức hoặc nhiều cửa hàng mà không làm tăng phạm vi triển khai cuối kỳ.

Thiết kế ưu tiên năm thuộc tính:

1. Mỗi thay đổi tồn kho có nguồn gốc và có thể truy vết.
2. Hạn sử dụng thuộc về lô hàng.
3. Giá và mức giảm trên hóa đơn được lưu dưới dạng ảnh chụp tại thời điểm bán.
4. Thao tác gửi lại do đồng bộ không được xử lý hai lần.
5. Dữ liệu đào tạo không làm thay đổi dữ liệu vận hành.

## 2. Quyết định và giả định hiện tại

| Mã | Quyết định thiết kế | Lý do |
|---|---|---|
| DB-01 | Có bảng `organizations` và `stores`, dù MVP chỉ tạo một bản ghi cho mỗi bảng. | Chuẩn bị khóa phạm vi dữ liệu cho hướng phát triển SaaS mà chưa triển khai quản lý tenant hoàn chỉnh. |
| DB-02 | Mọi bảng nghiệp vụ mang `organization_id`; bảng phát sinh tại cửa hàng có thêm `store_id`. | Hỗ trợ lọc dữ liệu theo tenant và cửa hàng ngay từ API. |
| DB-03 | Tồn hiện tại được lưu trong `inventory_balances`; lịch sử bất biến được lưu trong `stock_movements`. Hai bảng được cập nhật trong cùng transaction. | Vừa tra cứu nhanh, vừa có lịch sử để đối chiếu và dựng lại số tồn. |
| DB-04 | Mỗi lượng hàng nhập tạo một `product_batch`. Với sản phẩm không yêu cầu người dùng nhập lô, backend sinh mã lô nội bộ và ẩn chi tiết này trên giao diện thông thường. | Mọi đơn vị tồn đều có nguồn nhận hàng, thuận lợi cho FEFO, hủy hóa đơn và truy vết. |
| DB-05 | Một tồn chung được quản lý cho toàn cửa hàng; chưa tách vị trí kho và kệ. | Chức năng bổ sung kệ hiện không thuộc phạm vi cuối kỳ đã khóa. |
| DB-06 | Bản offline đầu tiên áp dụng cho kiểm kê. IndexedDB giữ bản nháp phía PWA; PostgreSQL chỉ lưu yêu cầu đã gửi và kết quả xử lý. | Giới hạn bài toán xung đột và phù hợp danh sách Should have. |
| DB-07 | Khách hàng thành viên và điểm thưởng chưa nằm trong mô hình MVP. | Phạm vi mới không liệt kê chức năng này trong Must have hoặc Should have. |
| DB-08 | Dữ liệu đào tạo đặt trong PostgreSQL schema `training`; dữ liệu vận hành đặt trong các schema còn lại. | Tách dữ liệu rõ ràng nhưng vẫn dùng chung kết nối, API và tài khoản người học. |
| DB-09 | Bảng đã phát sinh giao dịch không bị xóa vật lý; dùng trạng thái và nghiệp vụ đảo/hủy. | Giữ tính truy vết của hóa đơn, phiếu nhận và biến động tồn. |
| DB-10 | Khóa chính dùng UUID; thời gian dùng `timestamptz`; tiền dùng `numeric(14,2)`; số lượng dùng `numeric(14,3)`. | Phù hợp môi trường phân tán, tránh sai số tiền và hỗ trợ hàng có số lượng lẻ. |

## 3. Phân chia schema

| Schema | Trách nhiệm | Bảng chính |
|---|---|---|
| `core` | Tổ chức và cửa hàng | `organizations`, `stores` |
| `iam` | Tài khoản và phân quyền | `users`, `roles`, `permissions`, `user_roles`, `role_permissions` |
| `catalog` | Danh mục, sản phẩm, mã vạch, nhà cung cấp và giá | `categories`, `units`, `products`, `product_barcodes`, `suppliers`, `product_prices` |
| `inventory` | Nhận hàng, lô, tồn, biến động, kiểm kê và loại bỏ hàng | `goods_receipts`, `goods_receipt_lines`, `product_batches`, `inventory_balances`, `stock_movements`, `stocktakes`, `stocktake_lines`, `stock_disposals`, `stock_disposal_lines` |
| `sales` | Khuyến mãi, hóa đơn và thanh toán | `promotions`, `promotion_products`, `promotion_batches`, `invoices`, `invoice_lines`, `invoice_line_batches`, `payments` |
| `sync` | Chống xử lý lặp và ghi kết quả đồng bộ | `processed_operations` |
| `audit` | Nhật ký thao tác nhạy cảm | `audit_logs` |
| `training` | Kịch bản, phiên, hành động và kết quả đào tạo | `scenarios`, `scenario_steps`, `sessions`, `session_actions`, `session_results` |

Việc chia schema là ranh giới logic trong cùng một PostgreSQL database, không phải microservice hoặc database độc lập.

## 4. Mô hình dữ liệu vận hành

### 4.1. Tổ chức, cửa hàng và quyền truy cập

`organizations` là tenant tiềm năng. `stores` thuộc một tổ chức. `users` thuộc một tổ chức, còn `user_roles` xác định vai trò của người dùng tại cửa hàng. `permissions` là danh mục quyền kỹ thuật; `role_permissions` ánh xạ quyền vào vai trò.

Các khóa duy nhất quan trọng:

- `organizations(code)`.
- `stores(organization_id, code)`.
- `users(organization_id, username)`.
- `roles(organization_id, code)`.
- `user_roles(user_id, role_id, store_id)`.

### 4.2. Danh mục và giá

`products` biểu diễn một mặt hàng bán được, có SKU, đơn vị cơ sở, trạng thái và cờ yêu cầu theo dõi hạn sử dụng. Một sản phẩm có thể có nhiều mã trong `product_barcodes`. Giá không ghi đè trực tiếp trên sản phẩm mà được phiên bản hóa trong `product_prices` theo cửa hàng và thời gian hiệu lực.

Các khóa duy nhất quan trọng:

- `products(organization_id, sku)`.
- `product_barcodes(organization_id, barcode)`.
- `suppliers(organization_id, code)`.
- Mỗi sản phẩm tại một cửa hàng chỉ có một khoảng giá hiệu lực tại cùng thời điểm; backend kiểm tra khoảng thời gian không chồng lấn.

### 4.3. Nhận hàng, lô và tồn kho

`goods_receipts` là chứng từ nhận hàng. Mỗi `goods_receipt_line` tương ứng một sản phẩm và một lô giao; dòng đã được chấp nhận tạo `product_batch`. `inventory_balances` lưu số tồn hiện tại của từng lô tại cửa hàng. Mỗi lần tăng hoặc giảm tồn sinh một `stock_movement` mang loại biến động, số lượng có dấu và chứng từ nguồn.

Quy trình xác nhận phiếu nhận phải chạy trong một transaction:

1. Khóa phiếu nhận và kiểm tra trạng thái `DRAFT`.
2. Kiểm tra số lượng, sản phẩm, lô và hạn sử dụng.
3. Tạo hoặc xác định `product_batch`.
4. Tăng `inventory_balances.quantity_on_hand`.
5. Thêm `stock_movements` loại `RECEIPT`.
6. Chuyển phiếu sang `CONFIRMED` và ghi người xác nhận.

`stock_movements` không được cập nhật hoặc xóa sau khi ghi. Nếu cần sửa sai, hệ thống tạo biến động ngược có tham chiếu đến biến động gốc.

### 4.4. Kiểm kê và offline

`stocktakes` quản lý vòng đời một đợt kiểm kê: `DRAFT`, `IN_PROGRESS`, `SUBMITTED`, `APPROVED`, `REJECTED` hoặc `CANCELLED`. `stocktake_lines` lưu số hệ thống tại thời điểm mở phiên, số thực tế và chênh lệch theo lô.

PWA lưu bản nháp kiểm kê trong IndexedDB. Mỗi lần gửi lên API mang `client_operation_id`. Bảng `sync.processed_operations` có khóa duy nhất `(organization_id, client_operation_id)` để trả lại cùng kết quả nếu thiết bị gửi lại. Khi dữ liệu máy chủ đã thay đổi so với `base_version`, yêu cầu được ghi `CONFLICT` thay vì tự ghi đè.

Khi quản lý duyệt chênh lệch, backend tạo `stock_movements` loại `STOCKTAKE_ADJUSTMENT` và cập nhật `inventory_balances` trong cùng transaction.

### 4.5. Bán hàng, khuyến mãi và hóa đơn

`invoices` lưu chứng từ bán hàng. `invoice_lines` giữ tên, SKU, đơn giá và mức giảm tại thời điểm bán để lịch sử không thay đổi khi danh mục hoặc giá được sửa. `invoice_line_batches` ghi số lượng được xuất từ từng lô; backend ưu tiên lô còn hạn gần nhất theo FEFO khi người dùng không chọn lô thủ công.

Khuyến mãi được tách thành `promotions`, `promotion_products` và `promotion_batches`. Nhờ đó chương trình thông thường có thể áp dụng theo sản phẩm, còn giảm giá hàng cận hạn có thể giới hạn chính xác theo lô mà không dùng khóa ngoại đa hình.

Quy trình hoàn tất hóa đơn phải chạy trong một transaction:

1. Khóa các dòng `inventory_balances` được chọn.
2. Kiểm tra lô còn hiệu lực và đủ số lượng.
3. Chụp giá, khuyến mãi và tổng tiền vào `invoice_lines`.
4. Ghi `payments` ở trạng thái phù hợp.
5. Giảm tồn và tạo `stock_movements` loại `SALE`.
6. Chuyển hóa đơn sang `COMPLETED`.

Hủy hóa đơn đã hoàn tất không xóa dữ liệu. Hệ thống đổi trạng thái, lưu lý do và tạo biến động `SALE_VOID` để hoàn tồn theo đúng các lô đã xuất.

### 4.6. Báo cáo và audit

Báo cáo doanh thu được tổng hợp từ hóa đơn `COMPLETED` sau khi xét nghiệp vụ hủy. Báo cáo tồn dùng `inventory_balances`; báo cáo biến động dùng `stock_movements`. Chưa cần bảng báo cáo hoặc materialized view trong MVP.

`audit_logs` dùng cho thao tác nhạy cảm như thay đổi quyền, xác nhận phiếu nhận, duyệt kiểm kê, hủy hóa đơn và thay đổi khuyến mãi. Audit log không thay thế stock movement: audit trả lời ai đã thao tác trên bản ghi, còn stock movement là sổ cái số lượng hàng.

## 5. Mô hình dữ liệu đào tạo

`training.scenarios` lưu định nghĩa và phiên bản kịch bản. `scenario_steps` lưu thứ tự, hành động mong đợi, mục tiêu trong cảnh, điểm và quy tắc đánh giá. `sessions` là một lần thực hành của người học. Mọi hành động cần đánh giá được ghi nối tiếp vào `session_actions`; kết quả tổng hợp lưu tại `session_results`.

Các bảng đào tạo không tham chiếu đến hóa đơn, phiếu nhận, lô hoặc tồn vận hành. Sản phẩm và trạng thái dùng trong một kịch bản được mô tả bằng `fixture_data` dạng JSONB thuộc phiên bản kịch bản; khi bắt đầu, backend sao chép dữ liệu đó thành `state_data` của phiên. Cách này cho phép reset và chạy lại mà không tạo bản ghi giả trong các schema vận hành.

`training.sessions` được phép tham chiếu `iam.users` để biết người học, nhưng không được gọi transaction cập nhật tồn kho hoặc doanh thu.

## 6. Ràng buộc toàn vẹn cốt lõi

| Mã | Ràng buộc |
|---|---|
| C-01 | `accepted_quantity >= 0`, `rejected_quantity >= 0` và `accepted_quantity + rejected_quantity <= delivered_quantity`, trừ trường hợp được phê duyệt riêng. |
| C-02 | Lô có theo dõi hạn phải có `expiry_date`; `expiry_date` không trước `received_date`. |
| C-03 | `inventory_balances.quantity_on_hand >= 0` đối với dữ liệu vận hành bình thường. |
| C-04 | Tổng `invoice_line_batches.quantity` bằng `invoice_lines.quantity` khi hóa đơn hoàn tất. |
| C-05 | `invoice_lines.line_total = quantity * unit_price - discount_amount`, với kết quả không âm. |
| C-06 | Hóa đơn chỉ được `COMPLETED` khi tổng thanh toán hợp lệ bằng số phải trả trong phạm vi làm tròn. |
| C-07 | Chứng từ `CONFIRMED`, `COMPLETED` hoặc `APPROVED` không bị xóa vật lý. |
| C-08 | Mỗi `client_operation_id` chỉ được xử lý một lần trong phạm vi tổ chức. |
| C-09 | Bản ghi tham chiếu `organization_id` và `store_id` phải thuộc cùng một tenant. |
| C-10 | Dữ liệu `training` không được tạo khóa ngoại đến bảng giao dịch vận hành. |

Các ràng buộc liên quan nhiều bảng được thực thi trong service transaction và kiểm thử tích hợp; các điều kiện cục bộ được đặt bằng `NOT NULL`, `CHECK`, `UNIQUE` và `FOREIGN KEY` trong PostgreSQL.

## 7. Chỉ mục dự kiến

- `products(organization_id, normalized_name)` và chỉ mục duy nhất cho SKU.
- `product_barcodes(organization_id, barcode)`.
- `product_batches(store_id, product_id, expiry_date, status)` để chọn FEFO và lọc cận hạn.
- `inventory_balances(store_id, product_batch_id)`.
- `stock_movements(store_id, product_batch_id, occurred_at desc)`.
- `goods_receipts(store_id, received_at desc, status)`.
- `invoices(store_id, sold_at desc, status)` và chỉ mục duy nhất cho số hóa đơn.
- `invoice_lines(invoice_id)`; `invoice_line_batches(product_batch_id)`.
- `stocktakes(store_id, status, started_at desc)`.
- `audit_logs(organization_id, occurred_at desc)`, `(entity_type, entity_id)` và `(actor_user_id, occurred_at desc)`.
- `training.sessions(learner_user_id, started_at desc)`.

## 8. Thành phần hoãn khỏi mô hình MVP

Các bảng sau chỉ bổ sung khi phạm vi được mở lại: khách hàng thành viên, lịch sử điểm, quy đổi điểm, vị trí kho/kệ, điều chuyển giữa vị trí, trả hàng, nhiều chi nhánh đồng thời, subscription, tenant onboarding và cấu hình gói dịch vụ.

## 9. Trình tự chuyển sang thiết kế vật lý

1. Duyệt các giả định DB-05 đến DB-08.
2. Chốt thuộc tính và cardinality trên ba ERD logic.
3. Chuyển bảng sang PostgreSQL DDL với khóa, check constraint và partial index.
4. Chia Flyway migration theo `V1__core_and_iam`, `V2__catalog`, `V3__inventory`, `V4__sales`, `V5__sync_audit` và `V6__training`.
5. Tạo dữ liệu mẫu cho một tổ chức, một cửa hàng và bốn vai trò.
6. Kiểm thử transaction xuyên suốt: nhận hàng → tăng tồn → bán hàng → giảm tồn → báo cáo.

