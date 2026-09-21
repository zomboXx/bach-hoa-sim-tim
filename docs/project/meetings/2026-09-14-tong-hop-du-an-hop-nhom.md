---
marp: true
title: Bách Hóa Sim Tím - Tổng hợp dự án
description: Nội dung họp nhóm ngày 14/09/2026
paginate: true
size: 16:9
---

# Bách Hóa Sim Tím

## Tổng hợp dự án đến ngày 14/09/2026

**Nội dung cuộc họp:** nghiệp vụ, dữ liệu, kiến trúc, tiến độ và cách phối hợp trong nhóm

<!--
Ghi chú thuyết trình:
- Bản này chỉ trình bày các thành phần thuộc đồ án hiện tại.
- Những nội dung ghi "cần chốt" là việc nhóm cần quyết định trong cuộc họp.
-->

---

# Mục tiêu sản phẩm

Xây dựng hệ thống quản lý bán hàng và tồn kho cho một cửa hàng bán lẻ, gồm:

- Quản lý tài khoản và quyền sử dụng.
- Quản lý danh mục, sản phẩm, mã vạch và nhà cung cấp.
- Nhận hàng, quản lý lô, hạn sử dụng và tồn kho.
- Bán hàng, thanh toán, hóa đơn và khuyến mãi.
- Kiểm kê bằng giao diện phù hợp với điện thoại.
- Báo cáo doanh thu, tồn kho và lịch sử biến động.
- Một môi trường 2D để nhân viên mới thực hành nghiệp vụ bằng dữ liệu cách ly.

**Tên đề tài làm việc:** Xây dựng hệ thống quản lý bán hàng và tồn kho cho cửa hàng bán lẻ

<!-- Nguồn đã chuyển vị trí: docs/archive/PROJECT_CONTEXT.md và docs/product/analysis/02-danh-sach-yeu-cau.md -->

---

# Vai trò và quyền sử dụng

| Vai trò | Nghiệp vụ chính |
|---|---|
| Nhân viên bán hàng | Tìm hoặc quét sản phẩm, lập hóa đơn, ghi nhận thanh toán, xem hóa đơn |
| Nhân viên kho/hàng hóa | Nhận hàng, theo dõi lô và hạn, tra cứu tồn, thực hiện kiểm kê |
| Quản lý cửa hàng | Quản lý danh mục, sản phẩm, nhà cung cấp, khuyến mãi, duyệt kiểm kê, xem báo cáo |
| Quản trị viên | Tạo hoặc khóa tài khoản, gán vai trò, kiểm tra nhật ký liên quan đến quyền |
| Nhân viên mới | Chọn bài đào tạo, thực hành, nhận phản hồi và xem kết quả |

- Mỗi người đăng nhập bằng tài khoản riêng.
- Backend kiểm tra quyền ở từng nghiệp vụ. Giao diện chỉ ẩn chức năng là chưa đủ.
- Các thao tác quan trọng phải ghi người thực hiện và thời điểm.

<!-- Nguồn đã chuyển vị trí: docs/product/analysis/03-actors-use-cases.md -->

---

# Nghiệp vụ danh mục và nhà cung cấp

## Danh mục và sản phẩm

- Tạo, xem, sửa và ngừng sử dụng loại sản phẩm.
- Tạo, xem, sửa và ngừng kinh doanh sản phẩm.
- Lưu mã nội bộ, tên, loại, đơn vị tính, mã vạch, giá bán và trạng thái.
- Tìm sản phẩm bằng mã, mã vạch hoặc tên.
- Không cho phép trùng mã sản phẩm hoặc mã vạch trong phạm vi cửa hàng.

## Nhà cung cấp

- Lưu mã, tên và thông tin liên hệ của nhà cung cấp.
- Chọn nhà cung cấp khi lập phiếu nhận hàng.
- Lưu nhà cung cấp trên chứng từ để truy vết nguồn hàng.

<!-- Nguồn đã chuyển vị trí: docs/product/analysis/02-danh-sach-yeu-cau.md -->

---

# Nghiệp vụ nhận hàng và tồn kho

## Tiếp nhận hàng

1. Nhân viên tạo phiếu và chọn nhà cung cấp.
2. Nhập sản phẩm, số lượng giao, số lượng chấp nhận và số lượng từ chối.
3. Nhập mã lô, ngày nhận và hạn sử dụng khi sản phẩm yêu cầu theo dõi hạn.
4. Ghi lý do cho hàng thiếu, thừa, hư hỏng hoặc bị từ chối.
5. Khi xác nhận, hệ thống tạo lô, tăng tồn và ghi biến động trong cùng transaction.

## Quản lý tồn

- Xem tồn theo sản phẩm và từng lô.
- Phân biệt lô còn hạn, cận hạn và hết hạn.
- Ưu tiên xuất lô còn hạn gần nhất theo FEFO.
- Không bán lô hết hạn, bị khóa hoặc không đủ số lượng.
- Mọi lần tăng hoặc giảm tồn phải có chứng từ nguồn.

<!-- Nguồn đã chuyển vị trí: docs/product/analysis/03-actors-use-cases.md và docs/architecture/database/01-thiet-ke-csdl-khai-niem.md -->

---

# Nghiệp vụ bán hàng và báo cáo

## Bán hàng

1. Thu ngân quét mã hoặc tìm sản phẩm rồi nhập số lượng.
2. Hệ thống kiểm tra trạng thái sản phẩm, tồn khả dụng, hạn dùng và khuyến mãi.
3. Backend chọn lô xuất theo FEFO và tính tổng tiền.
4. Thu ngân ghi nhận tiền mặt hoặc chuyển khoản mô phỏng.
5. Hệ thống lưu hóa đơn, chi tiết, thanh toán, giảm tồn và biến động trong cùng transaction.

## Khuyến mãi và hóa đơn

- Chỉ áp dụng khuyến mãi đúng sản phẩm hoặc lô, đúng thời gian và điều kiện.
- Chi tiết hóa đơn giữ tên, SKU, đơn giá và mức giảm tại thời điểm bán.
- Hóa đơn hoàn tất không bị xóa. Thao tác hủy phải lưu lý do và hoàn tồn đúng lô.

## Báo cáo

- Doanh thu và số hóa đơn theo khoảng thời gian.
- Tồn hiện tại, lô cận hạn, lô hết hạn và lịch sử biến động.

<!-- Nguồn đã chuyển vị trí: docs/product/analysis/02-danh-sach-yeu-cau.md và docs/architecture/database/01-thiet-ke-csdl-khai-niem.md -->

---

# Nghiệp vụ kiểm kê và đồng bộ

1. Nhân viên mở danh sách kiểm kê trên điện thoại.
2. Chọn hoặc quét sản phẩm/lô và nhập số lượng thực tế.
3. Hệ thống lưu số tồn tại thời điểm mở phiên để làm mốc so sánh.
4. Khi mất mạng, phiếu được giữ trong IndexedDB với trạng thái `PENDING`.
5. Khi có mạng, client gửi lại cùng `clientOperationId` để tránh xử lý hai lần.
6. Phiếu hợp lệ chuyển sang `REVIEW`. Dữ liệu đã thay đổi chuyển sang `CONFLICT`.
7. Quản lý duyệt phiếu `REVIEW`. Hệ thống điều chỉnh tồn và tạo biến động.

**Quy tắc chính**

- Phiếu đã duyệt không được duyệt lại.
- Xung đột yêu cầu kiểm tra lại, hệ thống không tự ghi đè số tồn mới.
- Lưu offline chỉ hoàn tất việc ghi nhận. Quyền điều chỉnh tồn vẫn thuộc quản lý.

<!-- Nguồn đã chuyển vị trí: apps/web/ARCHITECTURE.md và apps/web/README.md -->

---

# Nghiệp vụ đào tạo

**Kịch bản đang dùng:** nhận 20 hộp sữa, phát hiện 2 hộp hỏng, từ chối 2 và nhập 18 hộp đạt yêu cầu.

1. Người học đăng nhập và bắt đầu kịch bản.
2. Hệ thống tạo một phiên đào tạo cùng bộ dữ liệu riêng.
3. NPC hướng dẫn nêu mục tiêu và các bước cần thực hiện.
4. Người học kiểm tra hàng, nhập kết quả và xác nhận nghiệp vụ.
5. Hệ thống ghi hành động, phát hiện sai số lượng hoặc sai bước và phản hồi.
6. Khi hoàn thành, hệ thống lưu trạng thái, điểm và chi tiết lỗi.

- Bài học sử dụng cùng quy tắc nghiệp vụ nhận hàng.
- Phiên đào tạo không tạo hóa đơn, lô hoặc biến động trong dữ liệu vận hành.
- Có thể reset và thực hành lại từ dữ liệu ban đầu của kịch bản.

<!-- Nguồn đã chuyển vị trí: docs/product/analysis/03-actors-use-cases.md và docs/architecture/database/01-thiet-ke-csdl-khai-niem.md -->

---

# Kiến trúc hệ thống

```text
PWA responsive ────────┐
                      ├── REST API ── Backend nghiệp vụ ── PostgreSQL
Godot đào tạo 2D ──────┘                  ├── xác thực và phân quyền
                                         ├── danh mục, kho, bán hàng
                                         ├── kiểm kê và báo cáo
                                         └── phiên đào tạo cách ly
```

- PWA phục vụ nghiệp vụ quản lý trên máy tính và điện thoại.
- Godot phụ trách di chuyển, tương tác và hiển thị hướng dẫn trong bài đào tạo.
- Backend là nơi duy nhất cập nhật cơ sở dữ liệu và thực thi quy tắc nghiệp vụ.
- API dùng JSON. Thời gian sự kiện có múi giờ, hạn sử dụng dùng ngày theo cửa hàng.
- Frontend prototype hiện dùng Vue 3, TypeScript, Vite, Service Worker và IndexedDB.
- Backend, PostgreSQL và Godot cần được triển khai và tích hợp trong giai đoạn tiếp theo.

<!-- Nguồn đã chuyển vị trí: apps/web/ARCHITECTURE.md -->

---

# Phân tích và thiết kế dữ liệu

| Nhóm dữ liệu | Thành phần chính |
|---|---|
| `core` | Tổ chức và cửa hàng mẫu |
| `iam` | Người dùng, vai trò, quyền và quan hệ phân quyền |
| `catalog` | Danh mục, đơn vị, sản phẩm, mã vạch, nhà cung cấp, giá |
| `inventory` | Phiếu nhận, dòng nhận, lô, tồn, biến động, kiểm kê, xử lý hàng |
| `sales` | Khuyến mãi, hóa đơn, chi tiết hóa đơn, lô xuất và thanh toán |
| `sync` và `audit` | Chống xử lý lặp, kết quả đồng bộ và nhật ký thao tác |
| `training` | Kịch bản, bước, phiên học, hành động và kết quả |

**Các quan hệ quan trọng**

- Một sản phẩm có nhiều mã vạch, mức giá và lô hàng.
- Một phiếu nhận có nhiều dòng. Dòng được chấp nhận tạo hoặc gắn với lô.
- Một hóa đơn có nhiều dòng. Mỗi dòng có thể xuất từ nhiều lô.
- Tồn hiện tại nằm trong `inventory_balances`. Lịch sử nằm trong `stock_movements`.
- Phiên đào tạo tham chiếu người học nhưng không tham chiếu chứng từ vận hành.

<!-- Nguồn đã chuyển vị trí: docs/architecture/database/01-thiet-ke-csdl-khai-niem.md và ba ERD cùng thư mục -->

---

# Toàn vẹn dữ liệu và transaction

- Số lượng giao, chấp nhận và từ chối phải hợp lệ. Tổng nhận không vượt số giao nếu chưa có phê duyệt ngoại lệ.
- Sản phẩm theo dõi hạn phải có `expiry_date`. Hạn không được trước ngày nhận.
- Tồn vận hành không được âm.
- Tổng lượng xuất từ các lô phải bằng số lượng trên dòng hóa đơn.
- Thành tiền bằng số lượng nhân đơn giá trừ mức giảm và không được âm.
- Hóa đơn chỉ hoàn tất khi thanh toán hợp lệ.
- Mỗi `clientOperationId` chỉ được xử lý một lần.
- Dữ liệu thuộc cùng tổ chức và cửa hàng phải nhất quán ở mọi quan hệ.

**Ba transaction bắt buộc kiểm thử**

1. Xác nhận phiếu nhận: tạo lô, tăng tồn, ghi biến động.
2. Hoàn tất hóa đơn: lưu hóa đơn, thanh toán, giảm tồn, ghi biến động.
3. Duyệt kiểm kê: kiểm tra phiên bản, điều chỉnh tồn, ghi biến động.

<!-- Nguồn đã chuyển vị trí: docs/architecture/database/01-thiet-ke-csdl-khai-niem.md -->

---

# Tiến độ hiện tại

## Đã có trong prototype 02

- 11 màn hình responsive theo ba tài khoản demo.
- Luồng nhận hàng, tồn kho, bán hàng, hóa đơn và báo cáo dùng chung dữ liệu demo.
- Xuất lô FEFO, áp dụng khuyến mãi và chặn bán lô hết hạn.
- Kiểm kê theo lô, lưu IndexedDB, đồng bộ mô phỏng và duyệt chênh lệch.
- Một tình huống đào tạo có dữ liệu cách ly.
- Bộ tài liệu yêu cầu, use case, thiết kế CSDL và ba ERD logic.

## Kết quả kiểm tra ngày 14/09/2026

- Build TypeScript/Vite: **đạt**.
- Kiểm thử end-to-end: **3/3 đạt**.

## Phần cần triển khai tiếp

- Backend, xác thực thật, PostgreSQL và migration.
- Kết nối PWA với REST API và thay dữ liệu trình duyệt bằng dữ liệu tập trung.
- Runtime Godot và tích hợp phiên đào tạo với backend.

<!-- Nguồn đã chuyển vị trí: apps/web/README.md, CHANGELOG.md và kết quả kiểm tra cục bộ -->

---

# Kiểm soát module và người phụ trách

| Module | Owner đề xuất | Reviewer | Đầu ra cần kiểm soát |
|---|---|---|---|
| Yêu cầu, API, tích hợp | Trưởng nhóm | Thành viên 2 | Phạm vi, contract, bản tích hợp |
| CSDL, tài khoản, danh mục | Thành viên 2 | Trưởng nhóm | ERD, migration, API nền tảng |
| Nhận hàng và tồn kho | Thành viên 2 | Thành viên 4 | API, transaction, test nghiệp vụ |
| Bán hàng, khuyến mãi, báo cáo | Thành viên 3 | Trưởng nhóm | PWA, API và test xuyên suốt |
| Kiểm kê trên điện thoại | Thành viên 4 | Thành viên 2 | Giao diện, đồng bộ và xử lý xung đột |
| Đào tạo Godot | Thành viên 4 | Trưởng nhóm | Scene, tương tác, API phiên học |

**Quy tắc**

- Trước khi kết thúc cuộc họp, thay “Thành viên 2/3/4” bằng tên thật.
- Mỗi module có đúng một owner chính và ít nhất một reviewer.
- Owner chịu trách nhiệm tích hợp và trạng thái module. Người khác vẫn có thể hỗ trợ qua task riêng.
- Công việc dùng chung phải tách thành backlog item có người nhận cụ thể.

<!-- Khung phân công dựa trên docs/archive/PROJECT_CONTEXT.md và apps/web/ARCHITECTURE.md. Cần nhóm xác nhận. -->

---

# Cách quản lý backlog

**Công cụ:** GitHub Issues để mô tả việc. GitHub Projects để theo dõi trạng thái.

Mỗi backlog item phải có:

- Module, kết quả nghiệp vụ cần đạt và tiêu chí chấp nhận.
- Owner, reviewer, mức ưu tiên và mốc dự kiến.
- Phụ thuộc với task khác, file hoặc API dự kiến thay đổi.
- Bằng chứng hoàn thành: pull request, test, ảnh hoặc video demo khi phù hợp.

**Luồng trạng thái**

```text
Backlog → Ready → In Progress → Review → Testing → Done
```

- Chỉ đưa vào `Ready` khi yêu cầu và tiêu chí chấp nhận đã rõ.
- Mỗi người chỉ giữ số task `In Progress` phù hợp với khả năng thực tế.
- Chỉ chuyển `Done` khi thay đổi đã tích hợp, test đạt và tài liệu liên quan đã cập nhật.
- Nếu vướng phụ thuộc, ghi rõ blocker trên Issue thay vì để task đứng im không lý do.

<!-- Nguồn quy trình: docs/PROJECT_CONTEXT.md -->

---

# Quy tắc cập nhật contributor

`CONTRIBUTION_LOG.md` là bản tóm tắt đóng góp đã hoàn thành. Git commit, pull request và Issue là bằng chứng đối chiếu.

**Thời điểm cập nhật**

- Cập nhật sau khi task qua review, test và được tích hợp.
- Thêm mục mới ngay dưới dòng hướng dẫn trong file, mục mới nhất đặt trên cùng.
- Công việc chung phải ghi riêng phần của từng người. Không ghi toàn bộ module cho một người nếu nhiều người cùng làm.

**Mẫu ghi**

```text
Tên - YYYY-MM-DD HH:mm:ss

Tóm tắt kết quả đã hoàn thành.

- `đường/dẫn/file`: +số_dòng -số_dòng
- `Tên biên bản hoặc hoạt động không tạo diff`: N/A
```

- Ghi kết quả cụ thể, đường dẫn chính xác và số dòng thay đổi từ diff.
- Không dùng số dòng để tự suy ra tỷ lệ đóng góp hoặc chất lượng công việc.
- Đầu ra do công cụ AI tạo chỉ được tính cho thành viên sau khi người đó kiểm tra, chỉnh sửa và chịu trách nhiệm tích hợp.

<!-- Nguồn: CONTRIBUTION_LOG.md và CHANGELOG.md. Quy tắc nghiệm thu cần nhóm xác nhận. -->

---

# Các quyết định cần chốt trong cuộc họp

1. Tên đề tài dùng thống nhất trong báo cáo và repository.
2. Stack backend và phiên bản công cụ mà cả nhóm cùng dùng.
3. Xác nhận kịch bản nhận hàng 20/18 là bài đào tạo đầu tiên.
4. Điền tên owner và reviewer cho từng module.
5. Tạo backlog cho các luồng: danh mục, nhận hàng, tồn, bán hàng, kiểm kê, báo cáo và đào tạo.
6. Chốt tiêu chí `Done`, quy tắc cập nhật contributor và cách xử lý task bị trễ.
7. Chốt mốc tích hợp backend, PWA, CSDL và Godot.
8. Xác định nội dung khảo sát cần bổ sung cho báo cáo phân tích yêu cầu.

**Kết quả cần có sau cuộc họp:** mỗi thành viên biết rõ module mình chịu trách nhiệm, task đầu tiên phải làm và điều kiện để task được công nhận là hoàn thành.

<!-- Tổng hợp từ các điểm còn mở trong tài liệu dự án -->
