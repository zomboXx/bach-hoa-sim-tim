# TRN-02 training scenario and evaluation contract

**Status:** Proposed — cần TV1 review luật chấm, TV2 review contract trong Pull Request trước khi TRN-03 dùng làm consumer contract

**Owner:** TV3 — Nguyễn Văn Thi (@thinguyen135) làm PWA; TV4 — Lê Văn Chiến (@VanChien11-02) làm kịch bản/kiểm thử

**Reviewer:** TV1 — Nguyễn Đức Phát (@zomboXx) review luật chấm; TV2 — Nguyễn Văn Trung (@nguyentrung01ute-ui) review contract

**Phụ thuộc:** TRN-01 (#54)

Tài liệu này xác lập kịch bản nghiệp vụ đào tạo, tập sự kiện hành động, phản hồi hướng dẫn sai, và thuật toán chấm điểm thuần nhất (deterministic evaluation oracle) cho Issue #55. Hợp đồng đảm bảo frontend và backend (TRN-03) có cùng một oracle kiểm chứng, không tin điểm client tự khai khi lưu kết quả và giữ cách ly hoàn toàn với dữ liệu vận hành.

---

## 1. Thông tin kịch bản đào tạo

- **Mã kịch bản:** `SCN-RETAIL-01`
- **Tên ca thực hành:** Ca đầu tiên ở Sim Tím (Ca thực hành 01)
- **Thời lượng ước tính:** 5 – 8 phút
- **Ba nhóm kỹ năng thực hành:**
  1. Giao tiếp & tư vấn khách hàng (chào hỏi, lắng nghe, xác nhận nhu cầu).
  2. Bán hàng & thanh toán tại quầy (chọn sản phẩm, quét mã, nhận tiền và trả tiền thừa).
  3. Xử lý & bảo quản hàng hóa (kiểm tra hạn dùng, tách hàng không đủ điều kiện bán, bàn giao quản lý).

---

## 2. Thứ tự hành động và bộ sự kiện nghiệp vụ (Event Vocabulary)

Mỗi ca đào tạo gồm chuỗi 6 bước nghiệp vụ bắt buộc theo đúng thứ tự:

| Thứ tự | Stage ID | Station | Hành động nghiệp vụ | Lựa chọn đúng | Lựa chọn sai & Phản hồi từ Mentor Mai |
|---|---|---|---|---|---|
| 1 | `greet` | `customer` (Khách Linh) | `greet_customer` | Xác nhận khách cần 2 hộp sữa ít đường và hỗ trợ lấy hàng. | Chọn giới thiệu giảm giá hoặc bảo khách tự tìm kệ -> Mai nhắc: cần lắng nghe và xác nhận đúng nhu cầu của khách trước. |
| 2 | `milk` | `milk` (Kệ sữa) | `pick_product` | Lấy đúng 2 hộp sữa ít đường, còn hạn vào giỏ cầm tay. | Chọn sữa có đường hoặc chọn thiếu số lượng (1 hộp) -> Mai nhắc: loại hàng hoặc số lượng chưa đúng yêu cầu khách đã xác nhận. |
| 3 | `checkout` | `checkout` (Quầy thu ngân) | `scan_item` & `tender_payment` | Quét đủ 2/2 sản phẩm, nhận 20.000đ, trả đúng 3.000đ tiền thừa và đưa hóa đơn kèm lời cảm ơn. | Quét thiếu hoặc trả sai tiền thừa (trả 2.000đ hoặc 5.000đ) -> Mai nhắc: tính lại `20.000 - 17.000 = 3.000đ`, đếm kỹ và giao kèm hóa đơn. |
| 4 | `cooler` | `cooler` (Tủ mát) | `inspect_cooler` | Phát hiện 1 hộp sữa chua hết hạn hôm qua, lấy khỏi tủ và mang tới khu xử lý. | Chọn giảm giá để bán nhanh hoặc đẩy vào trong tủ -> Mai nhắc: hàng hết hạn tuyệt đối không được tiếp tục bán, phải tách ngay khỏi khu bán hàng. |
| 5 | `quarantine` | `quarantine` (Khu xử lý) | `quarantine_product` | Đặt vào khay riêng, gắn nhãn “Hết hạn — không bán”. | Đặt chung vào thùng hàng mới nhận -> Mai nhắc: hàng không đủ điều kiện bán phải tách riêng để tránh nhân viên khác xếp nhầm lên kệ. |
| 6 | `mentor` | `mentor` (Mentor Mai) | `report_mentor` | Báo cáo đầy đủ: đã bán đúng hàng cho khách, tách và gắn nhãn 1 hộp sữa chua hết hạn ở khu xử lý. | Báo "không có gì cần báo" -> Mai nhắc: hàng đã cách ly phải được bàn giao cụ thể loại hàng, số lượng và vị trí cho người phụ trách. |

### 2.1. Bộ định danh ổn định (Vocabulary of Stable Identifiers)

Để frontend (PWA) và backend (TRN-03) thống nhất giao thức không phụ thuộc chuỗi văn bản tự do, hệ thống quy chuẩn 3 tập định danh ổn định:
1. **`actionType` (Hành động):** `greet_customer`, `pick_product`, `scan_item`, `tender_payment`, `inspect_cooler`, `quarantine_product`, `report_mentor`, `interact_hint`.
2. **`targetKey` (Đích tương tác):** `customer_linh`, `shelf_milk`, `pos_scanner`, `pos_cash`, `cooler_display`, `quarantine_bin`, `mentor_mai`, `any`.
3. **`choiceKey` (Lựa chọn ổn định):** Mã duy nhất gắn với từng lựa chọn (đúng hoặc sai) trong kịch bản.

### 2.2. Bảng ánh xạ lựa chọn theo phiên bản kịch bản (`SCN-RETAIL-01` v1.0.0)

Bảng ánh xạ tĩnh (Scenario Catalog) được lưu trữ độc lập ở cả client và server:

| `choiceKey` | Stage | `actionType` | `targetKey` | `isRight` | Nội dung lựa chọn (`textSnippet`) | Lời nhắc phản hồi (`feedback`) |
|---|---|---|---|:---:|---|---|
| `greet_discount` | `greet` | `greet_customer` | `customer_linh` | ✗ | Chị cứ lấy loại đang giảm giá đi ạ. | Mai: Khuyến mãi chưa chắc phù hợp. Hãy hỏi và xác nhận nhu cầu của khách trước nhé. |
| `greet_confirm_need` | `greet` | `greet_customer` | `customer_linh` | ✓ | Dạ, chị muốn 2 hộp sữa ít đường đúng không ạ? Em lấy giúp chị nhé. | Linh: Đúng rồi, cảm ơn em! Chị chờ ở quầy nhé. |
| `greet_self_service` | `greet` | `greet_customer` | `customer_linh` | ✗ | Sữa ở trên kệ đó chị tự tìm nhé. | Mai: Khách đang cần trợ giúp. Một lời chào và hướng dẫn cụ thể sẽ làm khách thoải mái hơn. |
| `milk_pick_2_low_sugar` | `milk` | `pick_product` | `shelf_milk` | ✓ | Lấy 2 hộp sữa ít đường · 8.500đ / hộp | Bạn đặt 2 hộp sữa ít đường vào giỏ cầm tay. Tiếp theo: mang tới quầy thanh toán. |
| `milk_pick_2_sweetened` | `milk` | `pick_product` | `shelf_milk` | ✗ | Lấy 2 hộp sữa có đường · 8.000đ / hộp | Mai: Loại này có đường, chưa đúng nhu cầu khách vừa xác nhận. |
| `milk_pick_1_low_sugar` | `milk` | `pick_product` | `shelf_milk` | ✗ | Lấy 1 hộp sữa ít đường | Mai: Khách cần hai hộp. Hãy kiểm tra cả loại hàng và số lượng nhé. |
| `scan_item_1` | `checkout` | `scan_item` | `pos_scanner` | ✓ | Quét hộp sữa 1 | Đã quét hộp sữa 1 |
| `scan_item_2` | `checkout` | `scan_item` | `pos_scanner` | ✓ | Quét hộp sữa 2 | Đã quét hộp sữa 2 |
| `tender_2k_change` | `checkout` | `tender_payment` | `pos_cash` | ✗ | Trả 2.000đ, cảm ơn khách. | Mai: Hãy tính lại: 20.000 − 17.000 = 3.000 đồng. Đếm lại trước khi đưa khách. |
| `tender_3k_receipt` | `checkout` | `tender_payment` | `pos_cash` | ✓ | “Em gửi chị 3.000đ tiền thừa và hóa đơn. Cảm ơn chị, hẹn gặp lại!” | Linh: Cảm ơn em nhé! Giao dịch đã hoàn tất, khách rất hài lòng. Mai nhờ bạn kiểm tra tủ mát trước khi kết thúc ca. |
| `tender_5k_no_receipt` | `checkout` | `tender_payment` | `pos_cash` | ✗ | Trả 5.000đ và bỏ qua hóa đơn. | Mai: Tiền thừa chưa đúng. Đối chiếu số tiền khách đưa và tổng hóa đơn, rồi giao hóa đơn cho khách. |
| `cooler_discount` | `cooler` | `inspect_cooler` | `cooler_display` | ✗ | Giảm giá để bán nhanh trong hôm nay. | Mai: Hàng đã hết hạn không được tiếp tục bán, kể cả giảm giá. Phải tách khỏi hàng còn bán được. |
| `cooler_quarantine_expired` | `cooler` | `inspect_cooler` | `cooler_display` | ✓ | Lấy hộp hết hạn khỏi tủ và mang tới khu hàng cần xử lý. | Bạn lấy hộp sữa chua hết hạn khỏi tủ. Mang tới khu xử lý để tránh lẫn vào hàng bán, rồi báo quản lý. |
| `cooler_push_inside` | `cooler` | `inspect_cooler` | `cooler_display` | ✗ | Đẩy hộp này vào phía trong tủ. | Mai: Để lại trong tủ vẫn có nguy cơ khách lấy nhầm. Hãy tách hẳn sản phẩm khỏi khu bán hàng. |
| `quarantine_label_expired` | `quarantine` | `quarantine_product` | `quarantine_bin` | ✓ | Đặt vào khay riêng, gắn nhãn “Hết hạn — không bán”. | Hàng đã được tách khỏi khu bán. Đến gặp Mai để báo lại và bàn giao tình trạng nhé. |
| `quarantine_mixed_with_new` | `quarantine` | `quarantine_product` | `quarantine_bin` | ✗ | Đặt chung vào thùng hàng mới nhận. | Mai: Hàng mới và hàng không đủ điều kiện bán phải tách riêng để tránh đưa nhầm trở lại kệ. |
| `mentor_full_report` | `mentor` | `report_mentor` | `mentor_mai` | ✓ | Em đã bán đúng hàng cho khách; có 1 hộp sữa chua hết hạn, đã tách và gắn nhãn ở khu xử lý. | Mai: Tốt lắm! Em vừa thực hành giao tiếp, bán hàng và chăm sóc hàng hóa trong cùng một ca. Chị sẽ tiếp nhận phần xử lý tiếp theo. |
| `mentor_report_nothing` | `mentor` | `report_mentor` | `mentor_mai` | ✗ | Không có gì cần báo chị ạ. | Mai: Hàng đã tách cần được bàn giao cho người phụ trách. Hãy nêu rõ loại hàng, số lượng và vị trí. |
| `hint_ack` | `greet` | `interact_hint` | `any` | ✓ | Mình hiểu rồi | Hướng dẫn mục tiêu hiện tại |

---

## 3. Quy tắc chấm và Điều kiện hoàn thành (Deterministic Scoring Oracle)

### 3.1. Nguyên tắc không tin điểm hoặc cờ trạng thái client (Zero-trust Client State)
- Khi hoàn tất ca học hoặc lưu kết quả (TRN-03), client chỉ gửi chuỗi sự kiện hành động `TrainingActionRecord[]` kèm định danh phiên và phiên bản kịch bản.
- Server tự tính lại điểm, số lỗi và suy diễn tiến độ hoàn thành các bước trực tiếp từ chuỗi sự kiện này.
- Tuyệt đối không chấp nhận các trường do client tự khai báo như `score`, `grade`, `passed`, `isRight` hoặc danh sách `completedStages`.

### 3.2. Cấu trúc bản ghi sự kiện hành động (`TrainingActionRecord`)
```typescript
interface TrainingActionRecord {
  sequence: number;           // Thứ tự hành động (bắt đầu từ 1, tăng đơn điệu)
  stage: TrainingStage;       // "greet" | "milk" | "checkout" | "cooler" | "quarantine" | "mentor"
  actionType: string;         // Định danh hành động nghiệp vụ (actionType)
  targetKey?: string;         // Định danh đích tương tác (targetKey)
  choiceKey?: string;         // Định danh ổn định của lựa chọn (choiceKey)
  choiceText: string;         // Nội dung lựa chọn đã thực hiện (phục vụ audit/UI)
  isRight: boolean;           // Cờ do client ghi nhận (chỉ để tham khảo/legacy fallback)
  feedback: string;           // Lời nhận xét / hướng dẫn tương ứng
  timestamp: number;          // Thời điểm thực hiện (epoch ms)
  scenarioVersion?: string;   // Phiên bản kịch bản (mặc định "1.0.0")
}
```

### 3.3. Thuật toán xác định hoàn thành bước từ chuỗi sự kiện (Stage Completion Deduction Algorithm)

Thay vì tin cờ `completedStages` do client gửi lên, Oracle phân tích chuỗi `actions: TrainingActionRecord[]` theo thuật toán đơn điệu sau:

1. **Phân giải đúng/sai độc lập (`resolveActionRight`):**
   - Với mỗi hành động `a`, Oracle lấy `a.choiceKey` tra vào bảng ánh xạ `SCENARIO_CATALOG[version]`.
   - Nếu `catalog[choiceKey].isRight === false`, ghi nhận là một lần phạm lỗi (`errors++`).
   - Mọi cờ `isRight: true` do client tự gán cho một `choiceKey` sai đều bị vô hiệu hóa.
2. **Suy diễn tiến trình hoàn thành tuần tự (`deriveCompletedStages`):**
   Tiến trình bắt buộc phải đi qua 6 bước theo đúng thứ tự:
   - **Bước 1 (`greet`):** Được công nhận hoàn thành khi chuỗi sự kiện xuất hiện hành động hợp lệ có `choiceKey === "greet_confirm_need"` (hoặc `actionType === "greet_customer"` đúng).
   - **Bước 2 (`milk`):** Yêu cầu bước 1 đã hoàn thành; được công nhận khi có hành động hợp lệ `choiceKey === "milk_pick_2_low_sugar"` (hoặc `actionType === "pick_product"` đúng).
   - **Bước 3 (`checkout`):** Yêu cầu bước 2 đã hoàn thành; được công nhận khi có **đủ ít nhất 2 sự kiện** `actionType === "scan_item"` (`scan_item_1` và `scan_item_2`) VÀ có hành động thanh toán đúng `choiceKey === "tender_3k_receipt"`.
   - **Bước 4 (`cooler`):** Yêu cầu bước 3 đã hoàn thành; được công nhận khi có hành động hợp lệ `choiceKey === "cooler_quarantine_expired"` (hoặc `actionType === "inspect_cooler"` đúng).
   - **Bước 5 (`quarantine`):** Yêu cầu bước 4 đã hoàn thành; được công nhận khi có hành động hợp lệ `choiceKey === "quarantine_label_expired"` (hoặc `actionType === "quarantine_product"` đúng).
   - **Bước 6 (`mentor`):** Yêu cầu bước 5 đã hoàn thành; được công nhận khi có hành động báo cáo đúng `choiceKey === "mentor_full_report"`.
3. **Ràng buộc hoàn thành ca học (`passed`):**
   $$\text{passed} = (\text{completedSteps} == 6)$$
   Nếu chuỗi sự kiện bị ngắt quãng, thiếu hành động đúng ở bất kỳ bước nào hoặc đi sai thứ tự, ca học bị đánh giá `passed = false`.

### 3.4. Công thức tính điểm và Xếp loại
- **Điểm cơ bản (Base Score):** 100 điểm.
- **Trừ điểm:** Mỗi thao tác sai hoặc lựa chọn sai bị trừ 10 điểm:
  $$\text{Score} = \max(0, 100 - \text{errorCount} \times 10)$$
- **Thang xếp loại (Grade):**
  - **`EXCELLENT` (Xuất sắc):** Đạt 100 điểm (0 lỗi, hoàn thành đủ 6/6 bước).
  - **`SATISFACTORY` (Đạt yêu cầu):** Đạt 70 – 90 điểm (1 – 3 lần được hướng dẫn lại, hoàn thành đủ 6/6 bước).
  - **`NEEDS_PRACTICE` (Cần luyện tập thêm):** Dưới 70 điểm (> 3 lỗi) hoặc chưa hoàn thành đủ 6 bước. Người học được khuyến nghị bấm "Chơi lại ca này" để nắm vững quy trình.

---

## 4. Vòng đời ca học: Hoàn thành, Bỏ dở và Chơi lại

1. **Hoàn thành ca (`complete`):**
   - Sau khi báo cáo Mentor Mai thành công, hệ thống mở màn hình tổng kết ca học (`shift-complete`).
   - Hiển thị đầy đủ: Điểm số đánh giá theo Oracle (`evaluation-score`), Xếp loại (`evaluation-grade`), Số bước chân và tổng số lần nhắc nhở (`evaluation-errors`), Nhật ký các hành động đã làm.
   - Cung cấp 2 lựa chọn: "Trở về cửa hàng →" hoặc "Chơi lại ca này".
2. **Bỏ dở ca giữa chừng (`leave` / `exitPrompt`):**
   - Bất kỳ lúc nào trong ca, người học có thể nhấn nút "← Về cửa hàng".
   - Hệ thống hiển thị hộp thoại xác nhận: *"Rời ca thực hành? Tiến độ của lần chơi này sẽ được đặt lại. Dữ liệu cửa hàng không bị ảnh hưởng."*
   - Nếu chọn *"Tiếp tục chơi"*: đóng hộp thoại, người học tiếp tục ca bình thường.
   - Nếu chọn *"Rời ca"*: hủy bỏ phiên hiện tại, thoát về không gian làm việc chính mà không ghi lại dữ liệu lỗi hay tiến độ dở dang.
3. **Chơi lại ca (`reset`):**
   - Nhấn "Chơi lại ca này" tại màn hình hoàn thành ca.
   - Toàn bộ trạng thái phiên được đặt lại sạch sẽ: vị trí nhân vật (10, 11), giỏ hàng rỗng, số bước = 0, số lỗi = 0, danh sách hành động = [], đưa về màn hình chào mừng ban đầu.

---

## 5. Ranh giới cách ly dữ liệu (Data Isolation Boundary)

- Toàn bộ tiến trình đào tạo chạy trong không gian mô phỏng (Sandbox).
- Các thao tác nhặt sữa, quét mã và tính tiền tại quầy thu ngân là mô phỏng, **tuyệt đối không phát sinh hóa đơn bán hàng (`sales`/`invoices`) và không trừ tồn kho (`inventory`)**.
- Thao tác lấy sữa chua hết hạn và gắn nhãn cách ly **tuyệt đối không gọi API kiểm kê (`stocktake`) hay điều chỉnh tồn kho**.
- Kiểm thử browser giám sát mạng và khẳng định 0 request HTTP (`POST`, `PUT`, `PATCH`, `DELETE`) gửi đến các endpoint vận hành.

---

## 6. Bằng chứng kiểm thử (Verification Evidence)

File kiểm thử Playwright: `apps/web/tests/trn-02-training-scenario.spec.ts`
- **Test 1:** Luồng hoàn thành đúng hoàn hảo (Happy path) -> Đạt 100 điểm, 0 lỗi, Xếp loại Xuất sắc, 0 network write request.
- **Test 2:** Luồng chọn sai, nhận phản hồi từ Mentor Mai, sửa sai và tiếp tục -> Đếm đúng số lỗi, trừ điểm theo đúng Oracle, xếp loại tương ứng.
- **Test 3:** Luồng bỏ dở giữa chừng -> Kiểm tra modal xác nhận, hủy bỏ phiên và quay về an toàn.
- **Test 4:** Luồng chơi lại ca học -> Reset toàn bộ dữ liệu phiên và bắt đầu lại ca mới thành công.
- **Test 5:** Kiểm thử trực tiếp hàm Oracle độc lập (`evaluateTrainingSession`) đảm bảo tính toán đồng nhất giữa client và server.
