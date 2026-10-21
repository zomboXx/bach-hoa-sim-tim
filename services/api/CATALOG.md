# Catalog CRUD API

Trạng thái: Working trên nhánh `feature/catalog-crud-completion`, xuất phát từ `main` tại `0aeb514`. Backend và giao diện API mode cho CRUD danh mục được hoàn thiện trên nhánh này; chưa phải xác nhận nghiệm thu hay tích hợp `main`.

## Xác thực và phạm vi

Gửi `Authorization: Bearer <accessToken>` và `X-Organization-Id` khớp tổ chức trong phiên. Header `X-Store-Id`, nếu gửi, cũng phải khớp phiên. GET cần `catalog.read`; POST/PUT/DELETE cần `catalog.write`. Các vai trò demo STOCK/MANAGER/ADMIN được ghi, SALES chỉ đọc. Thiếu phiên trả `401`; thiếu quyền hoặc giả mạo phạm vi trả `403`.

## Tài nguyên

| Tài nguyên | URL gốc | Trường khi tạo/cập nhật |
|---|---|---|
| Danh mục | `/api/v1/categories` | `code`, `name`, `parentId` tùy chọn, `status` ACTIVE/INACTIVE |
| Đơn vị tính | `/api/v1/units` | `code`, `name`, `precisionScale` số nguyên 0–3 |
| Sản phẩm | `/api/v1/products` | `categoryId`, `baseUnitId`, `sku`, `name`, `tracksExpiry`, `status` ACTIVE/INACTIVE |
| Nhà cung cấp | `/api/v1/suppliers` | `code`, `name`, `phone`/`email` tùy chọn, `status` ACTIVE/INACTIVE |
| Barcode | `/api/v1/products/{productId}/barcodes` | `barcode` chuỗi tối đa 100 ký tự, `isPrimary` boolean |

Mỗi URL gốc hỗ trợ GET danh sách (`200`) và POST tạo (`201`, kèm `Location`). URL `/{id}` hỗ trợ GET chi tiết (`200`), PUT cập nhật (`200`) và DELETE (`204`). Với barcode, `{id}` là ID barcode thuộc đúng sản phẩm trên đường dẫn. GET/PUT chi tiết barcode là hai endpoint mới của nhánh này.

PUT gửi lại đầy đủ các trường bắt buộc, không phải cập nhật từng phần. `status` bỏ qua hoặc null mặc định ACTIVE như contract hiện có; muốn ngưng sử dụng phải gửi INACTIVE rõ ràng. Danh mục cha không được là chính nó hoặc hậu duệ của nó. Sản phẩm phải tham chiếu danh mục và đơn vị tính trong cùng tổ chức.

Sản phẩm còn hỗ trợ `GET /api/v1/products/search?sku=...`, `?barcode=...` hoặc `?name=...`. Barcode giữ nguyên dạng chuỗi, kể cả số 0 đầu. Một tổ chức không được có hai barcode trùng; mỗi sản phẩm có tối đa một barcode chính. Để đổi barcode chính, PUT barcode cũ với `isPrimary: false`, sau đó PUT barcode mới với `isPrimary: true`; đây là hai request riêng. Nếu đã có barcode chính khác, ghi thêm barcode chính trả `409` và rollback.

## Xóa và lỗi

DELETE xóa vật lý khi không còn tham chiếu. Khi còn danh mục con, sản phẩm, barcode, giá bán, phiếu nhận hoặc dữ liệu nghiệp vụ tham chiếu, API trả `409` và giữ nguyên dữ liệu; không cascade xóa lịch sử bán/tồn. Có thể cập nhật `status: INACTIVE` cho category/product/supplier khi cần lưu lại lịch sử. Đơn vị tính không có trường trạng thái trong schema hiện hành.

| HTTP | Ý nghĩa |
|---|---|
| 404 | Tài nguyên không tồn tại trong tổ chức hiện tại |
| 409 | Mã trùng, vòng lặp danh mục, barcode không thuộc sản phẩm, hoặc xung đột dữ liệu |
| 422 | Payload không hợp lệ, gồm `precisionScale` ngoài 0–3 |

Lỗi nghiệp vụ/validation dùng ProblemDetail với `status`, `detail`, `timestamp`; validation thêm `errors` theo tên trường. Lỗi ràng buộc database có `code: CATALOG_CONSTRAINT_CONFLICT`; lỗi optimistic locking có `code: CATALOG_CONCURRENT_UPDATE`. Không trả SQL hoặc tên bảng/ràng buộc nội bộ cho client.

Giá bán có lịch sử là phạm vi riêng PRC-01 trong backlog; ProductRequest không nhận giá. Không có thay đổi schema, quyền hoặc API giá trong nhánh này.

## Kiểm thử

Trong `services/api`, chạy `./mvnw -Dtest=CatalogApiTest test` để kiểm tra catalog; chạy `./mvnw verify` cho toàn backend. Windows dùng `mvnw.cmd`. Tests dùng PostgreSQL 17 Testcontainers disposable và đăng nhập Bearer thật.

`CatalogApiTest` bổ sung vòng đời CRUD unit/barcode, tìm kiếm sau đổi barcode, validation, mã trùng, barcode chính, quyền và scope, tài nguyên thiếu, bảo toàn dữ liệu khi xóa bị khóa ngoại chặn, vòng lặp danh mục. Các ca CRUD product/category/supplier hiện có tiếp tục chạy trong cùng suite. Báo cáo QA-01 cũ giữ nguyên mốc lịch sử, không thay bằng kết quả nhánh này.
