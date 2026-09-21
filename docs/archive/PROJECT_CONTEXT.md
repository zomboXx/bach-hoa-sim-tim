# Ngữ cảnh dự án — Bách Hóa Sim Tím

Ngày tiếp nhận: 09/09/2026.

Nguồn: [cuộc trò chuyện “Đề xuất game quản lý”](https://chatgpt.com/share/6aa0505c-9784-83ec-af65-e370101328be), được đối chiếu và đọc đầy đủ qua lịch sử trò chuyện trong ứng dụng: 9 lượt trao đổi cùng 2 hình tham khảo. ID cuộc trò chuyện gốc: `6a9a6ad9-2934-83ec-a2ff-1ca9c519342a`.

Tài liệu này ghi nhận ngữ cảnh để tiếp tục công việc tại phiên trò chuyện mới. Đây chưa phải đặc tả yêu cầu đã được cả nhóm và giảng viên phê duyệt. Các đề xuất của trợ lý cũ được phân biệt với mong muốn và quyết định của người dùng.

## 1. Mục tiêu và bối cảnh

- Đồ án cuối kỳ môn Nhập môn Công nghệ phần mềm; trọng tâm là vận dụng phân tích yêu cầu, thiết kế, triển khai, kiểm thử và làm việc nhóm.
- Người dùng là trưởng nhóm, nhóm có 4 thành viên. Tại thời điểm trao đổi cũ, thời gian còn khoảng hơn một tháng và cả nhóm còn nhiều deadline khác. Chưa có ngày nộp cụ thể.
- Giảng viên không giới hạn tech stack theo thông tin người dùng cung cấp.
- Người dùng muốn có sản phẩm khác biệt, có khả năng thu hút người sử dụng, đồng thời phần quản lý bán hàng và kho phải hoạt động thực chất, dùng được độc lập với môi trường 2D.
- Đồng đội lo về phạm vi, công nghệ mới, khả năng thực hiện và cấu hình máy. Giảng viên đã cảnh báo tên đề tài hứa hẹn quá nhiều so với khả năng hoàn thành; chưa có bằng chứng tên mới đã được giảng viên duyệt.
- Khi phối hợp cần đưa ra phạm vi, phân công và bằng chứng khả thi cụ thể; không mặc định cả nhóm có thể bù rủi ro bằng việc làm quá sức.
- Máy người dùng được mô tả có i5-11400H, RAM 16 GB và GPU dòng 1650. Có đồng đội RAM 8 GB, máy cũ hoặc không có GPU rời. Chưa có kết quả chạy thử trên các máy đó.

## 2. Ý định rõ ràng và tên đề tài tạm duyệt

Người dùng muốn môi trường cửa hàng 2D pixel: nhân vật tương tác với máy móc, hàng hóa, công cụ; có thể dùng máy tính trong cửa hàng để mở ứng dụng quản lý bán hàng và kho. Trải nghiệm đi từ được đào tạo đến thạo việc.

Tên được đề xuất ngay trước khi người dùng nói “Xem như chúng ta tạm duyệt tên đề tài nhé”:

> Xây dựng ứng dụng quản lý bán hàng và kho hàng kết hợp mô phỏng đào tạo nghiệp vụ tại cửa hàng bán lẻ.

Tên này được hiểu là đã được người dùng tạm duyệt trong trao đổi, chưa phải xác nhận chính thức của giảng viên hoặc cả nhóm.

Tên thư mục hiện tại là “Bách Hóa Sim Tím”. “BeHind the Store” là tên sáng tạo xuất phát từ ý tưởng ban đầu. Người dùng đã nói rõ không thích việc trợ lý quá bám vào chữ “BHS”; không dùng chữ viết tắt này như yêu cầu bắt buộc cho mọi màn hình, module hay tên học thuật.

Điểm khác biệt được hướng tới là mô phỏng đào tạo gắn với nghiệp vụ: hướng dẫn, thực hành, kiểm tra thao tác, phản hồi, lưu tiến độ và đánh giá. Việc khác công nghệ giao diện với website quản lý bán lẻ của nhóm khác chưa đủ để chứng minh khác đề tài. Quyết định có trùng hay không vẫn thuộc giảng viên.

## 3. Sự thay đổi phạm vi qua các lượt trao đổi

| Giai đoạn | Nội dung | Cách sử dụng khi tiếp tục |
|---|---|---|
| Ý tưởng ban đầu của người dùng | Game kinh dị khoa học viễn tưởng 2D pixel, vận hành cửa hàng, khám phá bí mật | Giữ làm tầm nhìn sáng tạo dài hạn |
| Đề xuất mở rộng của trợ lý | Kho trung tâm, hai chi nhánh, quản trị chuỗi, hồ sơ con người và phân hệ bí mật | Đã được các lượt sau thu hẹp; không dùng làm phạm vi mặc định |
| Điều chỉnh theo phản hồi nhóm và giảng viên | Một cửa hàng; phần quản lý chạy độc lập; giảm cam kết game | Cơ sở giới hạn quy mô |
| Hướng gần nhất của người dùng | Quản lý bán hàng–kho kết hợp môi trường mô phỏng đào tạo đến thạo việc | Cơ sở tiếp tục phân tích và thiết kế |
| Kế hoạch triển khai cuối của trợ lý | Làm quản lý trước, tối thiểu một bài đào tạo 2D hoàn chỉnh, mở rộng nếu đủ thời gian | Bản đề xuất làm việc gần nhất, chưa phải mọi chi tiết đều đã được duyệt |

Không gộp tất cả danh sách chức năng ở các giai đoạn thành một backlog bắt buộc.

## 4. Phạm vi trong đề xuất triển khai gần nhất

Quy mô dự kiến: ứng dụng desktop Windows, hoạt động cục bộ tại một cửa hàng, chưa triển khai nhiều máy đồng thời.

### Phần bắt buộc được đề xuất

- Đăng nhập và phân quyền cơ bản.
- Danh mục, sản phẩm và nhà cung cấp.
- Lô hàng, ngày nhập, hạn sử dụng.
- Nhập hàng, theo dõi tồn kho và lịch sử biến động.
- Bán hàng, hóa đơn, ghi nhận thanh toán và tự động trừ tồn.
- Khuyến mãi hoặc giảm giá hàng cận hạn.
- Báo cáo doanh thu và tồn kho.
- Ít nhất một bài mô phỏng đào tạo 2D hoàn chỉnh.
- Lưu tiến độ và kết quả đào tạo.

### Phần mở rộng được đề xuất

- Kiểm kê, điều chỉnh tồn, trả hàng và bàn giao ca.
- Hoàn thành đủ ba bài: nhập hàng, bán hàng, kiểm tra/bàn giao cuối ca.
- Chấm điểm và phản hồi lỗi chi tiết hơn.

### Ngoài phạm vi kỳ này theo đề xuất gần nhất

- Chuỗi nhiều chi nhánh, nhiều máy vận hành đồng thời, multiplayer.
- Bán hàng trực tuyến, giao hàng, tính lương, kế toán đầy đủ.
- AI khách hàng, thành phố lớn, toàn bộ cốt truyện, nhiều ending.
- Các phân hệ bí mật hoặc “quản lý nhân loại” phức tạp.

## 5. Hai cách truy cập, một lõi nghiệp vụ

- Chế độ quản lý mở trực tiếp từ menu để sử dụng và kiểm tra chức năng.
- Môi trường 2D cho người học thao tác trong không gian và mở cùng giao diện quản lý thông qua máy tính trong cửa hàng.
- Cả hai sử dụng chung các service và mô hình dữ liệu; không viết hai bộ logic bán hàng hoặc tồn kho độc lập.
- Scene chịu trách nhiệm tương tác và hiển thị; service xử lý nghiệp vụ; repository truy xuất SQLite.
- Hệ thống đào tạo theo dõi thao tác, đối chiếu tiêu chí và lưu kết quả.

Kiến trúc trong kế hoạch cũ:

```text
Giao diện quản lý ─┐
                  ├─> Tầng nghiệp vụ ─> Repository ─> SQLite
Mô phỏng 2D ───────┘          │
                            └─> Theo dõi và đánh giá đào tạo
```

Các service được đề xuất: `AuthService`, `ProductService`, `InventoryService`, `PurchaseService`, `SalesService`, `PromotionService`, `ReportService`, `TrainingService`.

Ví dụ đào tạo trong trao đổi cũ: nhận 20 hộp sữa, kiểm tra thấy 18 hộp đạt và 2 hộp hỏng; người học phải nhập 18 hộp đạt yêu cầu và ghi nhận số bị từ chối. Nếu nhập 20 hộp đạt, hệ thống cần phát hiện và giải thích sai lệch. Phần 2D vì vậy tạo tình huống để xử lý nghiệp vụ và đánh giá kết quả.

Phong cách pixel áp dụng cho thế giới 2D và nhận diện; biểu mẫu, bảng dữ liệu, giá tiền, số lượng và hạn dùng vẫn cần dễ đọc.

## 6. Công nghệ và công cụ trong kế hoạch cũ

| Thành phần | Đề xuất gần nhất |
|---|---|
| Engine | Godot Standard, nhánh 4.x; bản cụ thể đã được trợ lý cũ nêu là 4.6.3 |
| Renderer | Compatibility |
| Ngôn ngữ | Typed GDScript |
| Lưu trữ | SQLite qua addon Godot-SQLite |
| Kiểm thử nghiệp vụ | GUT hoặc script test GDScript |
| Mã nguồn và công việc | Git, GitHub, Issues, Projects |
| UML | Enterprise Architect |
| ERD | draw.io, SQLDBM hoặc công cụ phù hợp với nhóm |
| Wireframe | Figma |
| Pixel art | LibreSprite hoặc Piskel |
| Báo cáo | Word hoặc Google Docs |
| Bản phát hành dự kiến | Windows x86-64 |

Đây là ghi nhận đề xuất lịch sử, không phải xác minh phiên bản hiện hành hay xác nhận phần mềm đã cài trên máy. Cặp phiên bản engine/addon và việc export phải được kiểm tra trước khi khóa phiên bản.

Kế hoạch ưu tiên một ngôn ngữ cho MVP, chưa bổ sung Java, React, Spring Boot, C# hoặc backend mạng. Database cần ghi khi chạy được đề xuất đặt tại `user://`; schema và dữ liệu mẫu được quản lý bằng mã nguồn. Chi tiết đóng gói addon vẫn cần kiểm thử thực tế.

## 7. Dữ liệu và actor đã được bàn tới

Danh sách gần nhất gồm 20 bảng dự kiến:

```text
users, roles, user_roles
categories, products, suppliers, product_batches
goods_receipts, goods_receipt_details
stock_movements, inventory_balances
invoices, invoice_details, payments
promotions, promotion_products
training_scenarios, training_steps, training_sessions, training_results
```

Các nguyên tắc đã xuất hiện trong trao đổi:

- Hạn sử dụng gắn với từng lô, không đặt một hạn dùng chung cho sản phẩm.
- Mỗi thay đổi tồn có lịch sử biến động tương ứng.
- Chi tiết hóa đơn giữ giá bán và mức giảm tại thời điểm giao dịch.
- Kiểm tra bán vượt tồn, lô hết hạn, quyền thao tác, tổng tiền và xử lý giao dịch bị hủy.
- Tiến độ đào tạo, lần thực hành và kết quả phải lưu được.

Actor trong hướng đào tạo gần nhất: người học, người hướng dẫn, nhân viên bán hàng, nhân viên kho, quản lý cửa hàng và quản trị viên. Có thể gộp vai trò trong MVP; chưa có ma trận quyền được duyệt. Khách hàng và nhà cung cấp chỉ là actor phần mềm nếu thật sự tương tác với hệ thống.

## 8. Kế hoạch tổ chức được đề xuất

- Khởi đầu bằng 3–4 ngày chốt phạm vi, actor/use case, ERD đầu tiên, wireframe và chứng minh Godot/SQLite/export chạy được trên máy yếu nhất.
- Hoàn thiện luồng tạo sản phẩm → nhập lô → tăng tồn → bán → hóa đơn → giảm tồn → báo cáo.
- Xây giao diện theo từng module khi hợp đồng dữ liệu đã rõ.
- Sau đó hoàn thiện một bài đào tạo nhập hàng, rồi mới mở rộng bài bán hàng và cuối ca.
- Dành tuần cuối cho kiểm thử, sửa lỗi, báo cáo, dữ liệu demo, video và đóng gói.

Phân công trong đề xuất cuối, chưa gắn tên người hoặc xác nhận năng lực:

| Vai trò | Phần việc dự kiến |
|---|---|
| Trưởng nhóm | Phạm vi, kiến trúc, tích hợp, TrainingService, duyệt merge |
| Thành viên 2 | CSDL, repository, sản phẩm, lô hàng, tồn kho |
| Thành viên 3 | Bán hàng, hóa đơn, khuyến mãi, giao diện quản lý |
| Thành viên 4 | Môi trường 2D, tương tác, kiểm thử và điều phối tài liệu |

Mỗi thành viên vẫn viết tài liệu và test case của phần mình. Mỗi module có người phụ trách và người review. Mỗi scene nên có một người phụ trách để giảm xung đột `.tscn`.

Quy trình công việc đề xuất: Backlog → Ready → In Progress → Review → Testing → Done. Việc hoàn thành cần được tích hợp, kiểm tra và cập nhật tài liệu; không chỉ dừng ở code trên máy cá nhân.

Lộ trình cũ là khung khoảng 4–5 tuần, chưa phải lịch theo ngày. Số giờ của từng người và phân công thực tế còn thiếu.

## 9. Tầm nhìn sáng tạo cần giữ lại

Ý tưởng ban đầu của người dùng: sinh viên đến thành phố mới, thường mua thực phẩm giảm giá rồi trở thành nhân viên cửa hàng; được một chị tóc cam đeo kính hướng dẫn. Các nghiệp vụ thường ngày dần hé lộ sản phẩm nhân tạo, tổ chức phi nhân loại và âm mưu liên quan đến Mặt Trời. Việc tiêu thụ sản phẩm và làm việc tích cực dẫn đến quá trình đồng hóa nhân vật, cuối cùng trở thành một lãnh đạo của tổ chức.

Ý tưởng người dùng bổ sung: chỉ một lập trình viên con người đứng sau hệ thống, có thể xuất hiện ở ending hoặc after-credit.

Các diễn giải khác của trợ lý cũ — thân phận chị tóc cam, nhân vật chính thay thế lập trình viên, tên ending, thời điểm 3:17, dashboard bí mật — là gợi ý sáng tác, chưa phải nội dung đã được người dùng chốt.

Hai hình gốc là tham khảo hình nhân màu tím và hình dáng đối chiếu; chưa có bộ asset sản xuất hoặc nhận diện hoàn chỉnh trong workspace. Người dùng muốn nhận diện riêng, tông tím, tránh lấy nguyên hình ảnh và tên của Bách Hóa Xanh.

Cốt truyện được lưu để phát triển về sau; không biến thành yêu cầu bắt buộc của bản đào tạo hiện tại.

## 10. Những điểm còn mở khi triển khai

Các ghi chú sau là kết quả đối chiếu ở phiên tiếp nhận này, không phải quyết định đã thống nhất:

- Bản đề xuất ngay trước lần tạm duyệt nói ba quy trình đào tạo; bản triển khai sau đó hạ mức bắt buộc xuống một bài. Cần chốt mức cam kết trước khi ghi vào đề cương nộp.
- Lời mô tả “15–18 bảng” trong kế hoạch cuối không khớp danh sách thực tế 20 bảng. ERD còn là bản nháp, số bảng không phải yêu cầu sản phẩm.
- Có đoạn yêu cầu luồng nhập–bán hoàn chỉnh cuối tuần 1, nhưng lịch chi tiết để bán hàng sang tuần 2. Cần thống nhất tiêu chí prototype và mốc nghiệm thu, không giữ cả hai như cam kết tương đương.
- Cần làm rõ giảm giá theo sản phẩm hay theo từng lô cận hạn, cách chọn lô khi bán và quy tắc hủy/hoàn tác.
- Cần thiết kế tính nguyên tử của giao dịch: hóa đơn, tồn kho và biến động phải nhất quán khi có lỗi.
- Chưa có quyết định cách khởi tạo/reset bài học, tách dữ liệu giữa các lần thực hành hoặc với dữ liệu vận hành. Dùng chung lõi nghiệp vụ không tự giải quyết vấn đề này.
- Các bất thường phục vụ cốt truyện về sau cần được mô hình hóa có chủ đích; không coi tồn âm hay dữ liệu hỏng là hành vi đúng của nghiệp vụ quản lý.
- Còn thiếu deadline chính xác, rubric/yêu cầu nộp, xác nhận của giảng viên, kỹ năng và thời gian của từng thành viên, máy yếu nhất, phiên bản công cụ và repository chung.

## 11. Trạng thái thực tế khi chuyển phiên

Tại lần kiểm tra ban đầu ngày 09/09/2026, thư mục dự án trống: chưa có mã nguồn, `project.godot`, `.git`, schema, asset hay tài liệu. File này được tạo để lưu ngữ cảnh đã đọc. Chưa cài công cụ, khởi tạo ứng dụng hoặc xác nhận prototype nào đã chạy.

Điểm bắt đầu phù hợp theo kế hoạch đã bàn: chuyển hướng mới nhất thành phạm vi MVP, use case và tiêu chí nghiệm thu ngắn gọn; sau đó dựng prototype nghiệp vụ xuyên suốt và kiểm tra bản export trên máy yếu nhất.

## 13. Cập nhật kiến trúc và phạm vi ngày 14/09/2026

- Tên định hướng: **Xây dựng hệ thống quản lý bán hàng và tồn kho cho cửa hàng bán lẻ**.
- Kiến trúc được mô tả là **hệ thống client–server triển khai tập trung, có khả năng phát triển theo mô hình SaaS**; chưa tuyên bố là SaaS hoàn chỉnh.
- Giao diện quản lý là PWA responsive trên máy tính và điện thoại. Backend xử lý nghiệp vụ, xác thực và phân quyền; PostgreSQL là CSDL quan hệ tập trung.
- PWA và Godot gọi chung backend REST API. Godot chỉ phụ trách môi trường mô phỏng; dữ liệu đào tạo tách khỏi dữ liệu vận hành.
- Phương án stack mặc định đang được cân nhắc: Vue 3, Vite, TypeScript, Spring Boot, Spring Security, Spring Data JPA, PostgreSQL, Flyway và Godot 4.x. Backend có thể đổi sang NestJS nếu năng lực JavaScript của nhóm phù hợp hơn; stack chưa khóa hoàn toàn.
- Must have: đăng nhập/phân quyền; danh mục, sản phẩm, mã vạch; nhà cung cấp; lô và hạn; nhận hàng; tồn và biến động; bán hàng/hóa đơn; khuyến mãi hoặc giảm giá cận hạn; báo cáo; responsive; một kịch bản Godot hoàn chỉnh.
- Should have: kiểm kê trên điện thoại; lưu kiểm kê offline và đồng bộ; quét camera; lưu kết quả đào tạo; kịch bản thứ hai.
- Ngoài phạm vi: native app riêng, bán hàng offline hoàn chỉnh, thiết bị POS phức tạp, SaaS đa doanh nghiệp hoàn chỉnh, subscription, nhiều chi nhánh đồng thời, toàn bộ cốt truyện và AI khách hàng.
- Thiết kế CSDL bắt đầu tại `docs/database/`, với một tổ chức và một cửa hàng mẫu nhưng có khóa phạm vi để mở rộng về sau.

## 12. Cập nhật yêu cầu ngày 10/09/2026

- Tên nhóm đã đăng ký tạm thời trong danh sách lớp là **“Xây dựng website quản lý cửa hàng bán lẻ”**. Tên chưa được coi là đã chốt cuối.
- Tên làm việc đề xuất hiện tại là **“Xây dựng hệ thống quản lý bán hàng và tồn kho cho cửa hàng bán lẻ”**; cần nhóm và giảng viên xác nhận.
- Người dùng làm rõ hai bối cảnh sử dụng chính: nhân viên bán hàng dùng máy tính/máy quét tại quầy; nhân viên hàng hóa dùng điện thoại khi nhận hàng, kiểm hàng và bổ sung hàng tại kệ.
- Mỗi nhân viên dự kiến có tài khoản riêng. Giao diện cần dùng được trên máy tính và điện thoại, truy cập dữ liệu chung và có khả năng tiếp tục một số thao tác khi kết nối gián đoạn.
- “Làm việc ngoại tuyến và đồng bộ” được hiểu là lưu thao tác được phép trên thiết bị rồi đồng bộ khi có mạng trở lại; thiết bị không thể đồng bộ với máy chủ trong lúc hoàn toàn mất kết nối.
- Các yêu cầu mới làm phương án Godot + SQLite cục bộ không còn phù hợp cho toàn hệ thống. Kiến trúc ứng viên là giao diện web/PWA responsive, API và CSDL quan hệ trung tâm; Godot chỉ phụ trách môi trường đào tạo 2D và gọi chung API/quy tắc nghiệp vụ.
- Offline toàn bộ, đặc biệt bán hàng trên nhiều máy cùng lúc, là bài toán xung đột dữ liệu lớn. Bản MVP chỉ nên chọn một quy trình offline, ưu tiên kiểm kê hoặc nhận hàng.
- Bộ tài liệu phân tích bản `0.1-draft` nằm tại `docs/analysis/`, gồm khảo sát hiện trạng, danh sách yêu cầu, actor/Use Case, lược đồ chức năng và mã nguồn sơ đồ PlantUML.
