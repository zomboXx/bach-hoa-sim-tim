# 1. Khảo sát hiện trạng

## 1.1. Mục đích khảo sát

Khảo sát nhằm trả lời bốn câu hỏi:

1. Những ai trực tiếp tham gia hoạt động bán hàng và quản lý hàng hóa?
2. Dữ liệu nào được tạo ra tại quầy thu ngân, kho và khu vực trưng bày?
3. Điểm nào trong quy trình hiện tại gây chậm, sai lệch hoặc khó truy vết?
4. Nhân viên mới đang được hướng dẫn và đánh giá khả năng làm việc như thế nào?

Kết quả khảo sát là căn cứ xác định phạm vi, yêu cầu, actor, Use Case và tiêu chí kiểm thử. Khảo sát không nhằm sao chép phần mềm nội bộ hoặc quy trình riêng của một thương hiệu cụ thể.

## 1.2. Đối tượng và phạm vi khảo sát

Mô hình tham chiếu là **một cửa hàng bán lẻ hàng tiêu dùng**, có:

- Quầy thanh toán với máy tính/POS và thiết bị đọc mã vạch.
- Khu vực trưng bày hàng hóa.
- Khu vực lưu hàng hoặc kho cửa hàng.
- Nhân viên bán hàng/thu ngân, nhân viên phụ trách hàng hóa và quản lý cửa hàng.
- Hàng hóa có thể được quản lý theo lô, ngày nhận và hạn sử dụng.
- Nhân viên sử dụng máy tính tại quầy và có thể sử dụng điện thoại khi kiểm hàng hoặc nhận hàng.

Phạm vi đồ án hiện không bao gồm quản lý nhiều chi nhánh, thương mại điện tử, giao hàng, tính lương, kế toán đầy đủ hoặc kết nối thật với cơ quan thuế.

## 1.3. Trạng thái bằng chứng

Tại thời điểm lập tài liệu, nhóm chưa cung cấp biên bản phỏng vấn, phiếu khảo sát, ảnh quan sát quy trình hoặc biểu mẫu nghiệp vụ từ một cửa hàng cụ thể. Vì vậy:

- Nội dung tại mục 1.4 là **mô hình hiện trạng dự kiến** dựa trên phạm vi người dùng mô tả và quy trình bán lẻ phổ biến.
- Các pain point tại mục 1.5 là **giả thuyết cần kiểm chứng**.
- Không được trình bày các giả thuyết này trong báo cáo cuối như kết quả khảo sát thực tế nếu chưa thu thập bằng chứng.

## 1.4. Quy trình hiện trạng dự kiến

### A. Bán hàng tại quầy

1. Thu ngân đăng nhập hoặc bắt đầu ca làm.
2. Thu ngân quét mã từng sản phẩm; nếu không đọc được mã thì tra cứu hoặc nhập mã thủ công.
3. Giá bán và khuyến mãi được xác định.
4. Khách hàng có thể cung cấp thông tin thành viên.
5. Thu ngân xác nhận thanh toán và phát hành hóa đơn/phiếu bán hàng.
6. Số lượng tồn được cập nhật theo giao dịch.
7. Khi có sai sót, giao dịch cần được hủy, điều chỉnh hoặc xử lý theo quyền hạn.

### B. Nhận hàng

1. Nhân viên nhận hàng và chứng từ giao hàng.
2. Nhân viên đối chiếu sản phẩm, số lượng và tình trạng thực tế.
3. Với hàng cần theo dõi hạn sử dụng, nhân viên ghi nhận lô và hạn dùng.
4. Hàng thiếu, thừa, hư hỏng hoặc không đạt được ghi nhận riêng.
5. Phiếu nhận hàng được xác nhận và tồn kho tăng theo số lượng chấp nhận.

### C. Kiểm hàng và bổ sung hàng lên kệ

1. Nhân viên kiểm tra hàng trên kệ và lượng hàng còn trong kho.
2. Nhân viên xác định sản phẩm cần bổ sung.
3. Khi chọn hàng, nhân viên ưu tiên lô phù hợp với quy định của cửa hàng.
4. Nhân viên kiểm tra tình trạng và hạn sử dụng trước khi đưa hàng lên kệ.
5. Các trường hợp bất thường được báo cho quản lý hoặc ghi nhận trên hệ thống.

Trong MVP, hệ thống quản lý **tồn kho của cửa hàng**; số lượng riêng trên kệ và trong kho chỉ được triển khai nếu khảo sát cho thấy cần quản lý hai vị trí tồn độc lập.

### D. Xử lý hàng cận hạn, hết hạn hoặc hư hỏng

1. Nhân viên tra cứu hoặc phát hiện lô sắp hết hạn.
2. Nhân viên kiểm tra điều kiện áp dụng giảm giá.
3. Người có quyền phê duyệt/tạo mức giảm cho lô hoặc sản phẩm.
4. Hàng hết hạn hoặc hư hỏng được ngừng bán và lập ghi nhận xử lý.
5. Tồn kho và lịch sử biến động được cập nhật.

### E. Kiểm kê

1. Nhân viên lấy danh sách hàng cần kiểm kê.
2. Nhân viên đếm thực tế và nhập số lượng bằng điện thoại hoặc máy tính.
3. Hệ thống so sánh số thực tế với số đang ghi nhận.
4. Chênh lệch được giải trình và gửi quản lý duyệt.
5. Sau khi duyệt, hệ thống tạo điều chỉnh tồn và lưu lịch sử.

### F. Hướng dẫn nhân viên mới

1. Người hướng dẫn giải thích quy trình và minh họa thao tác.
2. Nhân viên mới quan sát, làm thử và được sửa lỗi.
3. Mức độ thành thạo thường được nhận xét trực tiếp.
4. Việc đào tạo có thể phụ thuộc vào thời gian và cách hướng dẫn của từng người.

Phân hệ mô phỏng dự kiến chuẩn hóa một số bài thực hành; nó không thay thế hoàn toàn hoạt động hướng dẫn tại cửa hàng.

## 1.5. Pain point cần kiểm chứng

| ID | Giả thuyết pain point | Hệ quả dự kiến | Hướng giải quyết của hệ thống | Cần hỏi ai |
|---|---|---|---|---|
| PP-01 | Dữ liệu được ghi tại nhiều nơi hoặc nhập lại sau khi thao tác thực tế | Cập nhật chậm, nhập sai, khó đối chiếu | Ghi nhận tại điểm làm việc bằng máy tính/điện thoại | Nhân viên kho, quản lý |
| PP-02 | Bán hàng, nhận hàng và điều chỉnh tồn không được truy vết thống nhất | Tồn hệ thống khác tồn thực tế | Mọi thay đổi tồn tạo một giao dịch biến động có nguồn tham chiếu | Thu ngân, nhân viên kho |
| PP-03 | Khó theo dõi nhiều lô và hạn sử dụng của cùng sản phẩm | Bỏ sót hàng cận hạn, bán nhầm hàng hết hạn | Quản lý theo lô, cảnh báo hạn dùng, ngừng bán lô hết hạn | Nhân viên hàng hóa |
| PP-04 | Nhân viên phải quay lại máy tính cố định để tra cứu hoặc nhập dữ liệu | Tăng thao tác trung gian, dữ liệu không kịp thời | Giao diện responsive trên điện thoại | Nhân viên hàng hóa |
| PP-05 | Mạng không ổn định làm gián đoạn việc ghi nhận tại kho/kệ | Mất dữ liệu hoặc phải ghi tạm ngoài hệ thống | Lưu thao tác được phép ở trạng thái chờ và đồng bộ khi có mạng | Nhân viên, quản lý |
| PP-06 | Dùng chung tài khoản hoặc thiếu nhật ký thao tác | Khó xác định trách nhiệm khi sai lệch | Tài khoản cá nhân, phân quyền, audit log | Quản lý |
| PP-07 | Khuyến mãi hoặc giá bán được áp dụng không nhất quán | Sai tổng tiền, khó giải thích với khách | Tự kiểm tra thời gian, phạm vi và điều kiện áp dụng | Thu ngân, quản lý |
| PP-08 | Đào tạo nhân viên mới phụ thuộc nhiều vào mentor | Nội dung hướng dẫn không đồng đều, khó biết đã thành thạo chưa | Kịch bản mô phỏng, phản hồi và lưu kết quả | Nhân viên mới, mentor |
| PP-09 | Thực hành trực tiếp trên dữ liệu vận hành có thể tạo giao dịch sai | Ảnh hưởng tồn kho và báo cáo | Phiên đào tạo dùng dữ liệu tách biệt nhưng chung quy tắc nghiệp vụ | Mentor, quản lý |

## 1.6. Pain point được chọn cho MVP

Để tránh phạm vi quá lớn, bản đầu tập trung vào bốn vấn đề:

1. Liên kết bán hàng, nhận hàng và tồn kho để giảm sai lệch.
2. Theo dõi lô và hạn sử dụng.
3. Cho phép tra cứu/ghi nhận nghiệp vụ phù hợp trên máy tính và điện thoại.
4. Hỗ trợ một kịch bản đào tạo dùng cùng quy tắc nghiệp vụ.

Khả năng làm việc ngoại tuyến đầy đủ là bài toán đồng bộ phức tạp. MVP chỉ xem xét lưu tạm **một quy trình được chọn sau khảo sát**, ưu tiên kiểm kê hoặc nhận hàng; giao dịch thanh toán ngoại tuyến nằm ngoài cam kết ban đầu.

## 1.7. Kế hoạch khảo sát thực tế

### Người tham gia tối thiểu đề xuất

- 01 quản lý hoặc người từng phụ trách cửa hàng.
- 01 thu ngân/nhân viên bán hàng.
- 01 người từng nhận hàng, kiểm kê hoặc bổ sung hàng.
- 01 nhân viên mới hoặc người từng hướng dẫn nhân viên mới, nếu có thể tiếp cận.

Một người có thể đảm nhiệm nhiều vai trò. Nhóm cần ghi rõ số người và bối cảnh thực tế, không nhân số mẫu giả tạo.

### Phương pháp

- Phỏng vấn bán cấu trúc trong 15–25 phút.
- Quan sát một quy trình được cho phép: bán hàng, nhận hàng hoặc kiểm kê.
- Xin xem mẫu biểu đã loại thông tin nhạy cảm: hóa đơn, phiếu nhận, danh sách kiểm kê hoặc báo cáo cận hạn.
- Ghi nhận thiết bị sử dụng, điểm nhập dữ liệu, người phê duyệt và cách xử lý ngoại lệ.

### Câu hỏi phỏng vấn

1. Một ca làm bình thường của anh/chị gồm những công việc nào?
2. Khi bán hàng, hệ thống tự động làm những gì và nhân viên phải nhập những gì?
3. Khi mã vạch không đọc được hoặc giá hiển thị sai, anh/chị xử lý thế nào?
4. Khi nhận hàng, những thông tin nào phải đối chiếu?
5. Lô hàng và hạn sử dụng đang được ghi nhận ở đâu?
6. Làm sao biết sản phẩm nào cần bổ sung lên kệ?
7. Hàng cận hạn, hết hạn hoặc hư hỏng được xử lý và phê duyệt thế nào?
8. Kiểm kê được thực hiện bằng giấy, bảng tính, điện thoại hay thiết bị chuyên dụng?
9. Những sai lệch nào thường xảy ra giữa số liệu và hàng thực tế?
10. Có dùng chung tài khoản không? Quản lý truy được người thực hiện thao tác không?
11. Khi mất mạng hoặc thiết bị gặp lỗi, cửa hàng tiếp tục làm việc thế nào?
12. Dữ liệu nào cần sử dụng trên điện thoại ngay tại kệ hoặc kho?
13. Nhân viên mới được hướng dẫn trong bao lâu và bằng tài liệu nào?
14. Nghiệp vụ nào nhân viên mới thường làm sai nhất?
15. Quản lý dựa vào đâu để xác định nhân viên mới có thể tự làm việc?
16. Nếu chỉ cải thiện một điểm trong phần mềm hiện tại, anh/chị muốn cải thiện điều gì?

### Biểu mẫu ghi nhận kết quả

| Mã người tham gia | Vai trò | Ngày khảo sát | Quy trình quan sát | Vấn đề được xác nhận | Trích ý chính/ghi chú | Bằng chứng kèm theo |
|---|---|---|---|---|---|---|
| KS-01 | Chưa điền | Chưa điền | Chưa điền | Chưa điền | Chưa điền | Chưa điền |

## 1.8. Bối cảnh tiêu chuẩn và pháp lý tham khảo

- Mã vạch GS1/GTIN là cơ sở nhận diện sản phẩm phổ biến; tiêu chuẩn GS1 còn có thuộc tính lô và hạn sử dụng. Nguồn: [GS1 Barcodes](https://www.gs1.org/standards/barcodes), [GS1 Digital Link URI Syntax](https://ref.gs1.org/standards/digital-link/uri-syntax/1.7.0/).
- Nghị định 70/2025/NĐ-CP sửa đổi quy định về hóa đơn, trong đó có hóa đơn điện tử khởi tạo từ máy tính tiền và có hiệu lực từ 01/06/2025. Đồ án chỉ mô phỏng phát hành hóa đơn nội bộ, chưa cam kết tích hợp cơ quan thuế. Nguồn: [Cổng Thông tin điện tử Chính phủ](https://chinhphu.vn/?classid=1&docid=213179&pageid=27160&typegroupid=4).
- PWA có thể hỗ trợ trải nghiệm khi kết nối gián đoạn thông qua service worker; IndexedDB cung cấp lưu trữ dữ liệu có cấu trúc trên trình duyệt. Việc đồng bộ và giải quyết xung đột vẫn phải được thiết kế riêng. Nguồn: [MDN — Offline and background operation](https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Guides/Offline_and_background_operation), [MDN — IndexedDB](https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API).

## 1.9. Phát biểu bài toán dự kiến

> Nhân viên cửa hàng bán lẻ thực hiện nghiệp vụ tại quầy thanh toán, khu vực trưng bày và kho bằng các thiết bị khác nhau. Dữ liệu bán hàng, lô hàng và tồn kho cần được cập nhật thống nhất, truy vết theo tài khoản và có thể truy cập tại nơi phát sinh công việc. Hệ thống được xây dựng để hỗ trợ bán hàng, nhận hàng, theo dõi tồn và hạn sử dụng trên giao diện phù hợp với máy tính và điện thoại. Một môi trường mô phỏng sử dụng cùng quy tắc nghiệp vụ giúp nhân viên mới thực hành mà không tác động đến dữ liệu vận hành.

