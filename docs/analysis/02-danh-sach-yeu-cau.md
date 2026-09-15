# 2. Danh sách yêu cầu

Phiên bản: `0.1-draft`. Tài liệu được lập từ mô hình hiện trạng dự kiến; các mục `Cần khảo sát` phải được xác nhận trước khi đóng phạm vi.

## 2.1. Mục tiêu hệ thống

| ID | Mục tiêu |
|---|---|
| OBJ-01 | Quản lý thống nhất sản phẩm, lô hàng và tồn kho của một cửa hàng bán lẻ. |
| OBJ-02 | Liên kết nghiệp vụ nhận hàng, bán hàng, xử lý hàng và kiểm kê với lịch sử biến động tồn. |
| OBJ-03 | Cho phép nhân viên sử dụng giao diện phù hợp trên máy tính tại quầy và điện thoại tại kho/kệ. |
| OBJ-04 | Kiểm soát quyền và truy vết thao tác theo từng tài khoản nhân viên. |
| OBJ-05 | Hỗ trợ một số hoạt động khi kết nối gián đoạn và đồng bộ sau khi có mạng, trong phạm vi được chốt. |
| OBJ-06 | Hỗ trợ nhân viên mới thực hành một quy trình bằng môi trường mô phỏng mà không ảnh hưởng dữ liệu vận hành. |

## 2.2. Quy ước ưu tiên

| Mức | Ý nghĩa |
|---|---|
| P1 | Phải có để hình thành sản phẩm quản lý có thể nghiệm thu. |
| P2 | Nên có, chỉ thực hiện sau khi luồng P1 chạy ổn định. |
| P3 | Hướng mở rộng hoặc nội dung trình diễn. |

Trạng thái `Đề xuất` nghĩa là nhóm chưa xác nhận với người dùng thực tế/giảng viên. `Cần khảo sát` nghĩa là yêu cầu phụ thuộc kết quả khảo sát.

## 2.3. Yêu cầu chức năng

### A. Tài khoản và phân quyền

| ID | Yêu cầu | Ưu tiên | Trạng thái |
|---|---|---:|---|
| FR-AUTH-01 | Người dùng đăng nhập bằng tài khoản cá nhân. | P1 | Đề xuất |
| FR-AUTH-02 | Người dùng đăng xuất; hệ thống kết thúc phiên làm việc trên thiết bị. | P1 | Đề xuất |
| FR-AUTH-03 | Hệ thống giới hạn chức năng theo vai trò nhân viên bán hàng, nhân viên kho, quản lý và quản trị viên. | P1 | Đề xuất |
| FR-AUTH-04 | Quản trị viên tạo, khóa, mở khóa và gán vai trò cho tài khoản. | P1 | Đề xuất |
| FR-AUTH-05 | Người dùng đổi mật khẩu của mình. | P2 | Đề xuất |

### B. Danh mục hàng hóa

| ID | Yêu cầu | Ưu tiên | Trạng thái |
|---|---|---:|---|
| FR-CAT-01 | Quản lý tạo, xem, sửa và ngừng sử dụng loại sản phẩm. | P1 | Đề xuất |
| FR-CAT-02 | Quản lý tạo, xem, sửa và ngừng kinh doanh sản phẩm. | P1 | Đề xuất |
| FR-CAT-03 | Mỗi sản phẩm có mã nội bộ, tên, loại, đơn vị tính, mã vạch, giá bán và trạng thái. | P1 | Đề xuất |
| FR-CAT-04 | Hệ thống cho phép tìm sản phẩm bằng mã, mã vạch hoặc tên. | P1 | Đề xuất |
| FR-CAT-05 | Hệ thống từ chối mã sản phẩm/mã vạch bị trùng trong phạm vi áp dụng. | P1 | Đề xuất |

### C. Nhà cung cấp và nhận hàng

| ID | Yêu cầu | Ưu tiên | Trạng thái |
|---|---|---:|---|
| FR-REC-01 | Quản lý lưu và cập nhật thông tin nhà cung cấp. | P1 | Đề xuất |
| FR-REC-02 | Nhân viên lập phiếu nhận hàng gồm nhà cung cấp, thời điểm nhận và các dòng hàng. | P1 | Đề xuất |
| FR-REC-03 | Mỗi dòng nhận hàng ghi sản phẩm, số lượng giao, số lượng chấp nhận, giá nhập, mã lô và hạn sử dụng khi áp dụng. | P1 | Đề xuất |
| FR-REC-04 | Nhân viên ghi nhận hàng thiếu, thừa, hư hỏng hoặc bị từ chối cùng lý do. | P2 | Cần khảo sát |
| FR-REC-05 | Khi xác nhận phiếu, hệ thống tạo/cập nhật lô, tăng tồn và ghi biến động kho trong cùng một giao dịch nghiệp vụ. | P1 | Đề xuất |
| FR-REC-06 | Phiếu đã xác nhận không được xóa trực tiếp; việc sửa sai phải qua thao tác hủy/điều chỉnh có lưu lịch sử. | P2 | Cần khảo sát |

### D. Tồn kho, lô và hạn sử dụng

| ID | Yêu cầu | Ưu tiên | Trạng thái |
|---|---|---:|---|
| FR-INV-01 | Người có quyền xem tồn hiện tại theo sản phẩm và theo lô. | P1 | Đề xuất |
| FR-INV-02 | Hệ thống lưu ngày nhận, hạn sử dụng và trạng thái của lô khi sản phẩm cần theo dõi lô. | P1 | Đề xuất |
| FR-INV-03 | Người dùng lọc các lô còn hạn, cận hạn và hết hạn tại một ngày xác định. | P1 | Đề xuất |
| FR-INV-04 | Hệ thống ghi mọi lần tăng/giảm tồn thành biến động có loại nghiệp vụ, số lượng, thời điểm, người thực hiện và chứng từ nguồn. | P1 | Đề xuất |
| FR-INV-05 | Hệ thống không cho bán lô hết hạn. | P1 | Đề xuất |
| FR-INV-06 | Hệ thống đề xuất hoặc tự chọn lô xuất theo quy tắc ưu tiên hết hạn sớm, còn khả dụng. | P2 | Cần khảo sát |
| FR-INV-07 | Nhân viên lập phiên kiểm kê và nhập số lượng thực tế. | P1 | Đề xuất |
| FR-INV-08 | Hệ thống tính chênh lệch giữa số thực tế và số hệ thống. | P1 | Đề xuất |
| FR-INV-09 | Quản lý duyệt điều chỉnh chênh lệch; hệ thống tạo biến động tương ứng. | P2 | Cần khảo sát |
| FR-INV-10 | Nhân viên ghi nhận việc loại bỏ hàng hết hạn/hư hỏng; quản lý phê duyệt khi quy định yêu cầu. | P2 | Cần khảo sát |
| FR-INV-11 | Hệ thống quản lý tồn theo vị trí “kho” và “kệ” cùng nghiệp vụ bổ sung kệ. | P3 | Cần khảo sát |

### E. Bán hàng, hóa đơn và thanh toán

| ID | Yêu cầu | Ưu tiên | Trạng thái |
|---|---|---:|---|
| FR-SAL-01 | Nhân viên bán hàng tạo một hóa đơn mới tại quầy. | P1 | Đề xuất |
| FR-SAL-02 | Nhân viên thêm sản phẩm bằng cách quét/nhập mã vạch hoặc tìm kiếm. | P1 | Đề xuất |
| FR-SAL-03 | Nhân viên thay đổi số lượng hoặc xóa dòng hàng trước khi xác nhận thanh toán. | P1 | Đề xuất |
| FR-SAL-04 | Hệ thống kiểm tra trạng thái sản phẩm, hạn sử dụng và lượng tồn khả dụng. | P1 | Đề xuất |
| FR-SAL-05 | Hệ thống tính tạm tính, giảm giá và tổng thanh toán. | P1 | Đề xuất |
| FR-SAL-06 | Nhân viên ghi nhận phương thức và số tiền thanh toán. | P1 | Đề xuất |
| FR-SAL-07 | Khi xác nhận thanh toán, hệ thống lưu hóa đơn, chi tiết, thanh toán và biến động giảm tồn trong cùng một giao dịch nghiệp vụ. | P1 | Đề xuất |
| FR-SAL-08 | Người dùng xem lại hoặc xuất bản trình bày hóa đơn. | P1 | Đề xuất |
| FR-SAL-09 | Quản lý hủy hóa đơn theo quyền; hệ thống hoàn tác tồn và lưu lý do, không xóa lịch sử. | P2 | Cần khảo sát |
| FR-SAL-10 | Hệ thống hỗ trợ trả hàng/hoàn tiền. | P3 | Cần khảo sát |

### F. Khuyến mãi và khách hàng thành viên

| ID | Yêu cầu | Ưu tiên | Trạng thái |
|---|---|---:|---|
| FR-PRO-01 | Quản lý tạo chương trình giảm giá với thời gian hiệu lực và phạm vi sản phẩm/lô. | P1 | Đề xuất |
| FR-PRO-02 | Hệ thống chỉ áp dụng khuyến mãi khi thỏa điều kiện và còn hiệu lực. | P1 | Đề xuất |
| FR-PRO-03 | Hệ thống lưu giá gốc, mức giảm và giá thực bán trên từng dòng hóa đơn. | P1 | Đề xuất |
| FR-CUS-01 | Nhân viên tìm hoặc đăng ký khách hàng thành viên bằng thông tin định danh được chọn. | P2 | Cần khảo sát |
| FR-CUS-02 | Hệ thống cộng điểm sau giao dịch và lưu lịch sử thay đổi điểm. | P2 | Cần khảo sát |
| FR-CUS-03 | Khách hàng sử dụng điểm theo quy tắc được cấu hình. | P3 | Cần khảo sát |

### G. Báo cáo và truy vết

| ID | Yêu cầu | Ưu tiên | Trạng thái |
|---|---|---:|---|
| FR-REP-01 | Quản lý xem doanh thu và số hóa đơn theo khoảng thời gian. | P1 | Đề xuất |
| FR-REP-02 | Quản lý xem tồn hiện tại theo sản phẩm/lô. | P1 | Đề xuất |
| FR-REP-03 | Quản lý xem danh sách lô cận hạn và hết hạn. | P1 | Đề xuất |
| FR-REP-04 | Quản lý xem lịch sử biến động của một sản phẩm/lô. | P1 | Đề xuất |
| FR-REP-05 | Quản lý xem chênh lệch kiểm kê. | P2 | Đề xuất |
| FR-AUD-01 | Hệ thống lưu nhật ký các thao tác quan trọng như đăng nhập, xác nhận phiếu, hủy hóa đơn, điều chỉnh tồn và thay đổi quyền. | P1 | Đề xuất |
| FR-AUD-02 | Người có quyền tra cứu nhật ký theo người dùng, thời gian và loại hành động. | P2 | Đề xuất |

### H. Đa thiết bị, trạng thái kết nối và đồng bộ

| ID | Yêu cầu | Ưu tiên | Trạng thái |
|---|---|---:|---|
| FR-DEV-01 | Giao diện quản lý dùng được trên màn hình máy tính và điện thoại đối với các chức năng được phân quyền. | P1 | Đề xuất |
| FR-DEV-02 | Người dùng trên nhiều thiết bị truy cập cùng dữ liệu trung tâm khi có kết nối. | P1 | Đề xuất |
| FR-SYNC-01 | Hệ thống hiển thị trạng thái trực tuyến, ngoại tuyến, đang chờ đồng bộ hoặc đồng bộ lỗi. | P2 | Đề xuất |
| FR-SYNC-02 | Với nghiệp vụ offline được chọn, thiết bị lưu thao tác cục bộ cùng mã thao tác duy nhất. | P2 | Cần khảo sát |
| FR-SYNC-03 | Khi có kết nối trở lại, hệ thống gửi thao tác đang chờ và không xử lý trùng cùng một mã thao tác. | P2 | Cần khảo sát |
| FR-SYNC-04 | Hệ thống thông báo các thao tác bị từ chối hoặc có xung đột để người có quyền xử lý. | P2 | Cần khảo sát |
| FR-SYNC-05 | Bán hàng ngoại tuyến hoàn chỉnh và giải quyết xung đột tồn giữa nhiều thiết bị. | P3 | Chưa cam kết |

### I. Hỗ trợ đào tạo nghiệp vụ

| ID | Yêu cầu | Ưu tiên | Trạng thái |
|---|---|---:|---|
| FR-TRN-01 | Người học xem danh sách và bắt đầu kịch bản đào tạo được mở. | P2 | Đề xuất |
| FR-TRN-02 | Hệ thống tạo phiên thực hành có dữ liệu riêng, không thay đổi dữ liệu vận hành. | P2 | Đề xuất |
| FR-TRN-03 | Nhân vật hướng dẫn trình bày mục tiêu và các chỉ dẫn của kịch bản. | P2 | Đề xuất |
| FR-TRN-04 | Người học tương tác với hàng hóa/thiết bị trong môi trường 2D và thực hiện nghiệp vụ liên quan. | P2 | Đề xuất |
| FR-TRN-05 | Hệ thống ghi thứ tự và kết quả các hành động cần đánh giá. | P2 | Đề xuất |
| FR-TRN-06 | Hệ thống phản hồi khi hành động không đáp ứng quy tắc của kịch bản. | P2 | Đề xuất |
| FR-TRN-07 | Hệ thống tính trạng thái hoàn thành và lưu kết quả phiên đào tạo. | P2 | Đề xuất |
| FR-TRN-08 | Người học xem lại kết quả; quản lý xem kết quả của nhân viên thuộc phạm vi quản lý. | P2 | Đề xuất |
| FR-TRN-09 | Kịch bản kinh dị, thực thể bí ẩn và jumpscare. | P3 | Nội dung mở rộng |

## 2.4. Yêu cầu phi chức năng

| ID | Nhóm | Yêu cầu đề xuất | Cách kiểm chứng dự kiến |
|---|---|---|---|
| NFR-SEC-01 | Bảo mật | Mật khẩu không được lưu dưới dạng văn bản thuần. | Kiểm tra schema và mã xử lý xác thực. |
| NFR-SEC-02 | Bảo mật | API kiểm tra quyền ở phía máy chủ cho mọi thao tác thay đổi dữ liệu. | Test gọi API bằng vai trò không đủ quyền. |
| NFR-SEC-03 | Bảo mật | Phiên đăng nhập hết hiệu lực theo chính sách được cấu hình. | Test token/session hết hạn. |
| NFR-DAT-01 | Toàn vẹn | Hóa đơn, thanh toán và biến động tồn của một giao dịch không được lưu dở dang. | Mô phỏng lỗi giữa giao dịch và kiểm tra rollback. |
| NFR-DAT-02 | Toàn vẹn | Mỗi yêu cầu đồng bộ có mã duy nhất để chống xử lý lặp. | Gửi lại cùng yêu cầu hai lần. |
| NFR-DAT-03 | Toàn vẹn | Máy chủ và CSDL trung tâm là nguồn dữ liệu vận hành có thẩm quyền. | Kiểm tra quy trình xử lý xung đột. |
| NFR-USA-01 | Khả dụng | Luồng bán hàng thường dùng được hoàn thành bằng thao tác ngắn, rõ và hỗ trợ nhập từ máy quét như bàn phím. | Kiểm thử tác vụ với người dùng. |
| NFR-USA-02 | Khả dụng | Giao diện điện thoại không yêu cầu cuộn ngang ở các màn hình nghiệp vụ chính. | Test trên kích thước màn hình đã chọn. |
| NFR-USA-03 | Khả dụng | Hệ thống hiển thị rõ thao tác đã thành công, đang chờ đồng bộ hay thất bại. | Test các trạng thái kết nối. |
| NFR-PER-01 | Hiệu năng | Các thao tác tra cứu thường dùng phản hồi trong mục tiêu 2 giây ở môi trường demo và dữ liệu mẫu. | Đo thời gian phản hồi với bộ dữ liệu kiểm thử. |
| NFR-REL-01 | Tin cậy | Dữ liệu đang nhập dở trong nghiệp vụ offline đã chọn không mất khi tải lại ứng dụng. | Tắt/mở ứng dụng trước khi đồng bộ. |
| NFR-COM-01 | Tương thích | Giao diện quản lý chạy trên các trình duyệt desktop/mobile được nhóm công bố trong báo cáo. | Lập ma trận trình duyệt và kiểm thử. |
| NFR-MAI-01 | Bảo trì | Tầng giao diện, nghiệp vụ/API và truy xuất dữ liệu được tách trách nhiệm. | Review kiến trúc và mã nguồn. |
| NFR-BAC-01 | Sao lưu | Có cách sao lưu và khôi phục CSDL cho môi trường demo. | Thực hiện phục hồi trên bản sao sạch. |
| NFR-TRN-01 | Cách ly | Dữ liệu phiên đào tạo không làm thay đổi tồn kho và báo cáo vận hành. | Chạy bài đào tạo và so sánh dữ liệu trước/sau. |

Các con số hiệu năng và danh sách trình duyệt là ngưỡng đề xuất, cần điều chỉnh sau khi biết môi trường triển khai và máy yếu nhất.

## 2.5. Quy tắc nghiệp vụ

| ID | Quy tắc |
|---|---|
| BR-01 | Mã sản phẩm và mã vạch phải tuân theo phạm vi duy nhất đã xác định. |
| BR-02 | Hạn sử dụng thuộc về lô hàng; cùng một sản phẩm có thể có nhiều lô và hạn khác nhau. |
| BR-03 | Số lượng nghiệp vụ phải lớn hơn 0; điều chỉnh tăng/giảm được biểu diễn bằng loại biến động. |
| BR-04 | Mọi thay đổi tồn phải liên kết với một nghiệp vụ nguồn hoặc lý do điều chỉnh. |
| BR-05 | Lô hết hạn, bị khóa hoặc không còn số lượng khả dụng không được chọn để bán. |
| BR-06 | Chi tiết hóa đơn lưu giá và mức giảm tại thời điểm bán, không phụ thuộc giá sản phẩm thay đổi về sau. |
| BR-07 | Khuyến mãi chỉ áp dụng trong thời gian hiệu lực, đúng đối tượng và đúng điều kiện. |
| BR-08 | Chứng từ đã xác nhận không được xóa vật lý trong nghiệp vụ thông thường. |
| BR-09 | Chỉ vai trò được cấp quyền mới được duyệt điều chỉnh tồn, hủy hóa đơn hoặc sửa quyền tài khoản. |
| BR-10 | Một mã thao tác offline chỉ được máy chủ xử lý tối đa một lần. |
| BR-11 | Thao tác offline có trạng thái `PENDING`, `SYNCED`, `REJECTED` hoặc `CONFLICT`. |
| BR-12 | Dữ liệu đào tạo được tạo trong phiên riêng và bị loại/reset theo chính sách của kịch bản. |

## 2.6. Phạm vi MVP đề xuất

MVP được nghiệm thu bằng hai luồng xuyên suốt:

### Luồng 1 — Nhận hàng đến bán hàng

```text
Tạo sản phẩm
→ nhận một lô có hạn sử dụng
→ tồn kho tăng và có lịch sử
→ quét mã tại quầy
→ áp dụng giá/khuyến mãi hợp lệ
→ thanh toán và tạo hóa đơn
→ tồn kho giảm
→ báo cáo cập nhật
```

### Luồng 2 — Kiểm kê trên điện thoại

```text
Đăng nhập trên điện thoại
→ lấy danh sách kiểm kê
→ nhập số lượng thực tế
→ xem chênh lệch
→ quản lý duyệt điều chỉnh
→ lịch sử biến động được tạo
```

Một kịch bản đào tạo tái sử dụng luồng nhận hàng hoặc xử lý hạn sử dụng được thực hiện sau khi Luồng 1 ổn định.

## 2.7. Ngoài phạm vi hiện tại

- Quản lý chuỗi nhiều cửa hàng và kho trung tâm.
- Đặt hàng trực tuyến, giao hàng và cổng thanh toán thật.
- Kế toán, thuế và tích hợp phát hành hóa đơn điện tử thật.
- Tính lương, tuyển dụng và quản trị nhân sự đầy đủ.
- Đồng bộ offline hoàn chỉnh cho mọi chức năng.
- iOS/Android native riêng biệt.
- AI khách hàng, multiplayer, hệ thống chiến đấu hoặc toàn bộ cốt truyện kinh dị.

## 2.8. Ma trận truy vết mục tiêu — pain point — yêu cầu

| Pain point | Mục tiêu | Yêu cầu chính |
|---|---|---|
| PP-01, PP-04 | OBJ-03 | FR-DEV-01, FR-DEV-02, FR-REC-02, FR-INV-07 |
| PP-02 | OBJ-01, OBJ-02 | FR-REC-05, FR-INV-04, FR-SAL-07, FR-REP-04 |
| PP-03 | OBJ-01 | FR-INV-02, FR-INV-03, FR-INV-05, FR-PRO-01 |
| PP-05 | OBJ-05 | FR-SYNC-01..04, NFR-DAT-02, NFR-REL-01 |
| PP-06 | OBJ-04 | FR-AUTH-01..04, FR-AUD-01..02 |
| PP-07 | OBJ-02 | FR-PRO-01..03, FR-SAL-05 |
| PP-08, PP-09 | OBJ-06 | FR-TRN-01..08, NFR-TRN-01 |

