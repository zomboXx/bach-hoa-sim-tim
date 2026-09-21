# 3. Tác nhân và Use Case

## 3.1. Biên hệ thống

Hệ thống trong phạm vi gồm:

- Giao diện quản lý responsive trên máy tính và điện thoại.
- API nghiệp vụ và cơ sở dữ liệu trung tâm.
- Cơ chế lưu tạm/đồng bộ cho nghiệp vụ offline được chọn.
- Môi trường mô phỏng đào tạo 2D và dữ liệu phiên đào tạo.

Hệ thống không bao gồm tổ chức giao hàng của nhà cung cấp, ngân hàng/cổng thanh toán, hệ thống hóa đơn điện tử của cơ quan thuế hoặc hạ tầng của nhiều chi nhánh.

## 3.2. Nguyên tắc xác định actor

Actor là vai trò hoặc hệ thống bên ngoài trực tiếp trao đổi dữ liệu với hệ thống đang xây dựng. Vì vậy:

- **Khách hàng mua hàng** chưa phải actor trong MVP vì thu ngân thao tác thay khách hàng.
- **Nhà cung cấp** chưa phải actor vì họ không đăng nhập; nhân viên cửa hàng tự ghi nhận hàng giao.
- **Máy quét mã vạch, điện thoại, máy tính** là thiết bị/giao diện, không phải vai trò có mục tiêu riêng.
- **Cơ sở dữ liệu, API và cơ chế đồng bộ** nằm bên trong biên hệ thống.
- **Chị hướng dẫn tóc cam/NPC** là thành phần do phần mềm điều khiển, không phải actor.
- Một người thực tế có thể được gán nhiều vai trò; actor biểu diễn quyền và mục tiêu tương tác, không nhất thiết là những người khác nhau.

## 3.3. Danh sách actor

| ID | Actor | Mục tiêu | Quyền/chức năng tiêu biểu |
|---|---|---|---|
| ACT-00 | Người dùng hệ thống | Xác thực và sử dụng chức năng theo quyền | Đăng nhập, đăng xuất, đổi mật khẩu, xem trạng thái đồng bộ |
| ACT-01 | Nhân viên bán hàng | Hoàn thành giao dịch tại quầy nhanh và chính xác | Lập hóa đơn, quét mã, áp dụng thông tin thành viên, ghi nhận thanh toán, xem hóa đơn |
| ACT-02 | Nhân viên kho/hàng hóa | Ghi nhận hàng hóa tại nơi nhận, kho và kệ | Nhận hàng, tra cứu tồn/lô, kiểm hạn, kiểm kê, ghi nhận hàng lỗi/hết hạn |
| ACT-03 | Quản lý cửa hàng | Kiểm soát dữ liệu, ngoại lệ và kết quả vận hành | Quản lý danh mục, nhà cung cấp, khuyến mãi; duyệt điều chỉnh; xem báo cáo, audit và kết quả đào tạo |
| ACT-04 | Quản trị viên | Duy trì quyền truy cập của hệ thống | Tạo/khóa tài khoản, gán vai trò, xem nhật ký liên quan đến bảo mật |
| ACT-05 | Nhân viên mới/người học | Thực hành quy trình trong môi trường an toàn | Chọn kịch bản, nhận hướng dẫn, thực hiện thao tác, nhận phản hồi, xem kết quả |

`ACT-01` đến `ACT-05` đều là các vai trò chuyên biệt của `ACT-00`.

## 3.4. Danh mục Use Case tổng quát

### Nhóm tài khoản

| ID | Use Case | Actor chính | Kết quả |
|---|---|---|---|
| UC-AUTH-01 | Đăng nhập | Người dùng hệ thống | Phiên sử dụng được tạo với đúng vai trò |
| UC-AUTH-02 | Đăng xuất | Người dùng hệ thống | Phiên hiện tại kết thúc |
| UC-AUTH-03 | Đổi mật khẩu | Người dùng hệ thống | Thông tin xác thực được cập nhật |
| UC-ADM-01 | Quản lý tài khoản | Quản trị viên | Tài khoản được tạo, sửa trạng thái hoặc gán vai trò |

### Nhóm hàng hóa và nhận hàng

| ID | Use Case | Actor chính | Kết quả |
|---|---|---|---|
| UC-CAT-01 | Quản lý loại sản phẩm | Quản lý cửa hàng | Danh mục loại được cập nhật |
| UC-CAT-02 | Quản lý sản phẩm | Quản lý cửa hàng | Hồ sơ sản phẩm, mã vạch và giá được cập nhật |
| UC-SUP-01 | Quản lý nhà cung cấp | Quản lý cửa hàng | Hồ sơ nhà cung cấp được cập nhật |
| UC-REC-01 | Tiếp nhận hàng | Nhân viên kho | Phiếu nhận, lô, tồn và biến động được ghi nhận |
| UC-REC-02 | Ghi nhận sai lệch hàng giao | Nhân viên kho | Hàng thiếu/thừa/hư hỏng được lưu cùng lý do |

### Nhóm tồn kho

| ID | Use Case | Actor chính | Kết quả |
|---|---|---|---|
| UC-INV-01 | Tra cứu tồn kho và lô | Nhân viên kho, quản lý | Người dùng nhận dữ liệu tồn theo quyền |
| UC-INV-02 | Theo dõi hàng cận/hết hạn | Nhân viên kho, quản lý | Các lô cần xử lý được xác định |
| UC-INV-03 | Thực hiện kiểm kê | Nhân viên kho | Số lượng thực tế và chênh lệch được ghi nhận |
| UC-INV-04 | Duyệt điều chỉnh tồn | Quản lý cửa hàng | Tồn được điều chỉnh và có lịch sử |
| UC-INV-05 | Ghi nhận xử lý hàng hỏng/hết hạn | Nhân viên kho, quản lý | Hàng ngừng bán và biến động giảm tồn được tạo |

### Nhóm bán hàng và khuyến mãi

| ID | Use Case | Actor chính | Kết quả |
|---|---|---|---|
| UC-SAL-01 | Lập và thanh toán hóa đơn | Nhân viên bán hàng | Hóa đơn, thanh toán và giảm tồn được ghi nhận |
| UC-SAL-02 | Xem lại hóa đơn | Nhân viên bán hàng, quản lý | Hóa đơn được tra cứu/xuất trình |
| UC-SAL-03 | Hủy hóa đơn | Quản lý cửa hàng | Hóa đơn đổi trạng thái, tồn được hoàn tác và lý do được lưu |
| UC-PRO-01 | Quản lý chương trình khuyến mãi | Quản lý cửa hàng | Chương trình và phạm vi áp dụng được cập nhật |
| UC-CUS-01 | Xác định khách hàng thành viên | Nhân viên bán hàng | Giao dịch được gắn với khách hàng phù hợp |
| UC-CUS-02 | Ghi nhận điểm thành viên | — (xử lý nội bộ của UC-SAL-01) | Điểm và lịch sử thay đổi được lưu |

`UC-CUS-02` là xử lý tự động bên trong `UC-SAL-01`, không cần một actor “Hệ thống” riêng trên sơ đồ Use Case.

### Nhóm báo cáo và đồng bộ

| ID | Use Case | Actor chính | Kết quả |
|---|---|---|---|
| UC-REP-01 | Xem báo cáo doanh thu | Quản lý cửa hàng | Số liệu theo khoảng thời gian được hiển thị |
| UC-REP-02 | Xem báo cáo tồn và hạn sử dụng | Quản lý cửa hàng | Tồn và các lô cần xử lý được tổng hợp |
| UC-REP-03 | Xem lịch sử biến động/audit | Quản lý cửa hàng, quản trị viên | Nguồn gốc thay đổi được truy vết theo quyền |
| UC-SYNC-01 | Ghi nhận thao tác khi ngoại tuyến | Nhân viên được cấp quyền | Thao tác được lưu cục bộ ở trạng thái chờ |
| UC-SYNC-02 | Đồng bộ thao tác đang chờ | Người dùng hệ thống | Thao tác được máy chủ chấp nhận, từ chối hoặc báo xung đột |
| UC-SYNC-03 | Xử lý xung đột đồng bộ | Quản lý cửa hàng | Xung đột được xem và giải quyết có lưu lịch sử |

### Nhóm hỗ trợ đào tạo

| ID | Use Case | Actor chính | Kết quả |
|---|---|---|---|
| UC-TRN-01 | Chọn kịch bản đào tạo | Nhân viên mới | Kịch bản phù hợp được mở |
| UC-TRN-02 | Thực hiện kịch bản đào tạo | Nhân viên mới | Hành động được ghi trong phiên dữ liệu cách ly |
| UC-TRN-03 | Nhận hướng dẫn và phản hồi | Nhân viên mới | Người học biết mục tiêu, lỗi và cách sửa |
| UC-TRN-04 | Hoàn thành và xem kết quả | Nhân viên mới | Điểm/trạng thái và chi tiết lỗi được lưu, hiển thị |
| UC-TRN-05 | Xem kết quả đào tạo nhân viên | Quản lý cửa hàng | Quản lý theo dõi được các phiên đã hoàn thành |

## 3.5. Quan hệ include/extend quan trọng

| Use Case cơ sở | Quan hệ | Use Case liên quan | Giải thích |
|---|---|---|---|
| UC-REC-01 Tiếp nhận hàng | include | Kiểm tra dữ liệu lô | Luôn kiểm tra các trường bắt buộc theo loại sản phẩm |
| UC-REC-01 Tiếp nhận hàng | include | Cập nhật tồn và ghi biến động | Phiếu nhận chỉ hoàn tất khi tồn được cập nhật nhất quán |
| UC-REC-02 Ghi nhận sai lệch | extend | UC-REC-01 Tiếp nhận hàng | Chỉ phát sinh khi hàng giao không khớp/không đạt |
| UC-INV-04 Duyệt điều chỉnh | extend | UC-INV-03 Thực hiện kiểm kê | Chỉ phát sinh khi có chênh lệch cần điều chỉnh |
| UC-SAL-01 Lập hóa đơn | include | Nhận diện sản phẩm | Mỗi dòng hàng phải xác định được sản phẩm |
| UC-SAL-01 Lập hóa đơn | include | Kiểm tra tồn và hạn | Phải kiểm tra trước khi xác nhận bán |
| UC-SAL-01 Lập hóa đơn | include | Tính giá và khuyến mãi | Tổng tiền luôn được hệ thống tính |
| UC-SAL-01 Lập hóa đơn | include | Ghi thanh toán, hóa đơn và giảm tồn | Các dữ liệu phải được lưu nguyên tử |
| UC-CUS-01 Thành viên | extend | UC-SAL-01 Lập hóa đơn | Chỉ dùng khi khách cung cấp thông tin thành viên |
| UC-SYNC-01 Ghi offline | extend | UC-INV-03 hoặc UC-REC-01 | Chỉ dùng khi mất kết nối và nghiệp vụ được phép offline |
| UC-TRN-02 Thực hiện kịch bản | include | Khởi tạo dữ liệu phiên | Đảm bảo không tác động dữ liệu vận hành |
| UC-TRN-02 Thực hiện kịch bản | include | Ghi và đánh giá hành động | Cần để phản hồi và tính kết quả |

## 3.6. Đặc tả Use Case trọng tâm

### UC-REC-01 — Tiếp nhận hàng

| Thuộc tính | Nội dung |
|---|---|
| Actor chính | Nhân viên kho/hàng hóa |
| Actor phụ | Quản lý cửa hàng khi cần duyệt ngoại lệ |
| Tiền điều kiện | Người dùng đã đăng nhập, có quyền nhận hàng; sản phẩm và nhà cung cấp tồn tại |
| Kích hoạt | Xe/đơn vị giao hàng chuyển hàng đến cửa hàng |
| Luồng chính | 1. Tạo phiếu nhận. 2. Chọn nhà cung cấp. 3. Quét/chọn sản phẩm. 4. Nhập số lượng giao và số lượng chấp nhận. 5. Nhập lô, hạn dùng khi áp dụng. 6. Xác nhận phiếu. 7. Hệ thống lưu phiếu, tạo lô, tăng tồn và ghi biến động. |
| Luồng thay thế | Mã không tồn tại; thiếu dữ liệu lô; số lượng không hợp lệ; hàng thiếu/thừa/hỏng; người dùng hủy trước khi xác nhận; mất kết nối ở nghiệp vụ được phép offline |
| Hậu điều kiện thành công | Phiếu ở trạng thái xác nhận; tồn và lịch sử biến động nhất quán |
| Hậu điều kiện thất bại | Không có thay đổi dở dang trong CSDL trung tâm; bản nháp có thể được giữ theo chính sách |

### UC-SAL-01 — Lập và thanh toán hóa đơn

| Thuộc tính | Nội dung |
|---|---|
| Actor chính | Nhân viên bán hàng |
| Tiền điều kiện | Đã đăng nhập và có quyền bán hàng; dữ liệu sản phẩm/giá khả dụng |
| Kích hoạt | Khách hàng yêu cầu thanh toán |
| Luồng chính | 1. Tạo hóa đơn. 2. Quét mã từng sản phẩm. 3. Hệ thống kiểm tra tồn, hạn và khuyến mãi. 4. Thu ngân điều chỉnh số lượng nếu cần. 5. Tùy chọn xác định khách hàng thành viên. 6. Hệ thống tính tổng. 7. Thu ngân ghi nhận thanh toán. 8. Hệ thống lưu hóa đơn, chi tiết, thanh toán và giảm tồn. 9. Hiển thị hóa đơn hoàn tất. |
| Luồng thay thế | Không tìm thấy mã; sản phẩm ngừng bán; hết tồn; lô hết hạn; khuyến mãi không hợp lệ; thanh toán chưa đủ; lỗi lưu giao dịch |
| Hậu điều kiện thành công | Hóa đơn hoàn tất; tồn giảm đúng; lịch sử biến động và audit được tạo |
| Hậu điều kiện thất bại | Hóa đơn chưa hoàn tất và tồn không bị giảm dở dang |

### UC-INV-03 — Thực hiện kiểm kê

| Thuộc tính | Nội dung |
|---|---|
| Actor chính | Nhân viên kho/hàng hóa |
| Tiền điều kiện | Có phiên/danh sách kiểm kê và quyền truy cập |
| Kích hoạt | Đến lịch kiểm kê hoặc quản lý yêu cầu kiểm tra |
| Luồng chính | 1. Mở phiên trên điện thoại. 2. Quét/chọn sản phẩm hoặc lô. 3. Nhập số lượng thực tế. 4. Hệ thống lưu kết quả. 5. Kết thúc phiên. 6. Hệ thống tính chênh lệch và gửi quản lý xem. |
| Luồng thay thế | Quét sai mã; trùng dòng; mất mạng; thao tác chờ đồng bộ bị từ chối/xung đột |
| Hậu điều kiện | Kết quả kiểm kê được lưu; tồn chỉ thay đổi sau bước duyệt nếu có chênh lệch |

### UC-TRN-02 — Thực hiện kịch bản đào tạo

| Thuộc tính | Nội dung |
|---|---|
| Actor chính | Nhân viên mới/người học |
| Tiền điều kiện | Người học đã đăng nhập; kịch bản được mở; dữ liệu kịch bản hợp lệ |
| Kích hoạt | Người học chọn bắt đầu kịch bản |
| Luồng chính | 1. Hệ thống tạo phiên và dữ liệu cách ly. 2. NPC hướng dẫn nêu mục tiêu. 3. Người học tương tác với hàng hóa/thiết bị. 4. Người học thực hiện nghiệp vụ được yêu cầu. 5. Hệ thống ghi và đối chiếu hành động. 6. Hệ thống phản hồi. 7. Người học hoàn thành. 8. Kết quả được lưu. |
| Luồng thay thế | Làm sai thứ tự; chọn sai sản phẩm/lô; nhập sai số lượng; bỏ qua bước bắt buộc; rời bài giữa chừng |
| Hậu điều kiện | Kết quả đào tạo được lưu; dữ liệu vận hành không thay đổi |

## 3.7. Các quyết định còn cần xác nhận

1. Nhân viên bán hàng và nhân viên kho là hai chức danh tách biệt hay một nhân viên kiêm nhiệm?
2. Quản lý có trực tiếp bán hàng/nhận hàng bằng cùng tài khoản không?
3. Hệ thống quản lý một tồn chung hay tách tồn ở kho và trên kệ?
4. Ai có quyền tạo giảm giá cho hàng cận hạn?
5. Ai duyệt hủy hóa đơn, hủy hàng và điều chỉnh kiểm kê?
6. Khách hàng thành viên có thuộc MVP không?
7. Nghiệp vụ nào được phép làm ngoại tuyến trong bản nộp?
8. Kịch bản đào tạo bắt buộc đầu tiên là nhận hàng, kiểm hạn hay bán hàng?
