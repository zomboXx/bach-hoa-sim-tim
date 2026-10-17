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

---

## 3. Quy tắc chấm và Điều kiện hoàn thành (Deterministic Scoring Oracle)

### 3.1. Nguyên tắc không tin điểm client
- Khi hoàn tất ca học hoặc lưu kết quả (TRN-03), client chỉ gửi chuỗi sự kiện hành động `TrainingActionRecord[]` kèm định danh phiên.
- Server tự tính lại điểm và xếp loại theo cùng một oracle, không chấp nhận trường `score` hoặc `grade` do client tự gửi.

### 3.2. Cấu trúc bản ghi sự kiện hành động (`TrainingActionRecord`)
```typescript
interface TrainingActionRecord {
  sequence: number;           // Thứ tự hành động (1, 2, 3...)
  stage: TrainingStage;       // "greet" | "milk" | "checkout" | "cooler" | "quarantine" | "mentor"
  actionType: string;         // Định danh hành động nghiệp vụ
  choiceText: string;         // Nội dung lựa chọn đã thực hiện
  isRight: boolean;           // Kết quả lựa chọn (đúng: true, sai: false)
  feedback: string;           // Lời nhận xét / hướng dẫn tương ứng
  timestamp: number;          // Thời điểm thực hiện (epoch ms)
}
```

### 3.3. Công thức tính điểm và Xếp loại
- **Điểm cơ bản (Base Score):** 100 điểm.
- **Trừ điểm:** Mỗi thao tác sai hoặc lựa chọn sai bị trừ 10 điểm:
  $$\text{Score} = \max(0, 100 - \text{errorCount} \times 10)$$
- **Điều kiện hoàn thành (Passed):** Phải hoàn thành đủ 6/6 bước nghiệp vụ bắt buộc (`greet`, `milk`, `checkout`, `cooler`, `quarantine`, `mentor`).
- **Thang xếp loại (Grade):**
  - **`EXCELLENT` (Xuất sắc):** Đạt 100 điểm (0 lỗi, hoàn thành đúng ngay từ lần đầu).
  - **`SATISFACTORY` (Đạt yêu cầu):** Đạt 70 – 90 điểm (1 – 3 lần được mentor hướng dẫn lại).
  - **`NEEDS_PRACTICE` (Cần luyện tập thêm):** Dưới 70 điểm (> 3 lỗi). Người học được khuyến nghị bấm "Chơi lại ca này" để nắm vững quy trình.

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
