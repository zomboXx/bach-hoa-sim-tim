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
- Khi chấm authoritative (TRN-03), server nhận đầu vào `EvaluateTrainingSessionInput { catalogVersion?, actions: TrainingActionEvent[] }` và đối chiếu với `TrainingCatalog` của phiên bản tương ứng.
- **Từ chối Key/Version không hợp lệ:** Ném lỗi `TrainingCatalogValidationError`:
  - Mã `UNKNOWN_CATALOG_VERSION`: nếu `catalogVersion` gửi lên không khớp với phiên bản catalog server hỗ trợ.
  - Mã `MISSING_CHOICE_KEY`: nếu sự kiện hành động thiếu trường `choiceKey`.
  - Mã `UNKNOWN_CHOICE_KEY`: nếu `choiceKey` không tồn tại trong catalog của server.
- **Tuyệt đối không fallback sang dữ liệu client:**
  - `resolveActionRight()` chỉ đọc `catalogChoice.isRight` từ catalog server, bỏ hoàn toàn việc đọc `action.isRight`.
  - `deriveCompletedStages()` chỉ suy diễn từ định nghĩa `catalogChoice.stage` và `catalogChoice.choiceKey` trong catalog server, bỏ hoàn toàn các trường `input.completedStages`, `action.stage`, `action.actionType`, hay `action.choiceText`.
- **Tách biệt đường legacy browser simulation:** Tách riêng hàm `evaluateLegacyBrowserSimulation` (alias `evaluateLegacyBrowserTrainingSession`) chỉ phục vụ UI mô phỏng phía client, không sử dụng trên đường chấm authoritative backend.

### 3.2. Cấu trúc bản ghi sự kiện và dữ liệu Catalog
```typescript
export const REQUIRED_CHECKOUT_SCAN_KEYS = ['scan_item_1', 'scan_item_2'] as const;
export const REQUIRED_CHECKOUT_TENDER_KEY = 'tender_3k_receipt' as const;

export class TrainingCatalogValidationError extends Error {
  constructor(
    public readonly code: 'UNKNOWN_CATALOG_VERSION' | 'MISSING_CHOICE_KEY' | 'UNKNOWN_CHOICE_KEY',
    message: string,
  ) {
    super(message);
    this.name = 'TrainingCatalogValidationError';
  }
}

export interface CatalogChoiceDefinition {
  choiceKey: string;
  stage: string;
  actionType?: string;
  isRight: boolean;
}

export interface TrainingCatalog {
  version: string;
  requiredStages: readonly string[];
  choicesByKey: ReadonlyMap<string, CatalogChoiceDefinition> | Record<string, CatalogChoiceDefinition>;
}

export interface TrainingActionEvent {
  choiceKey?: string;
  // Bị bỏ qua hoàn toàn trên đường authoritative:
  isRight?: boolean;
  stage?: string;
  actionType?: string;
  choiceText?: string;
}

export interface EvaluateTrainingSessionInput {
  catalogVersion?: string;
  actions: readonly TrainingActionEvent[];
  // Bị bỏ qua hoàn toàn trên đường authoritative:
  completedStages?: readonly string[];
}
```

### 3.3. Thuật toán xác định hoàn thành bước từ chuỗi sự kiện (Stage Completion Deduction Algorithm)

Oracle phân tích chuỗi `actions: readonly TrainingActionEvent[]` đối chiếu với `TrainingCatalog`:
1. **Phân giải đúng/sai độc lập (`resolveCatalogChoiceAuthoritative`):**
   - Từng sự kiện bắt buộc có `choiceKey` tồn tại trong catalog server.
   - Trả về định nghĩa lựa chọn chính thức; nếu `!choice.isRight`, hành động không được tính để hoàn thành bước.
2. **Suy diễn tiến trình hoàn thành tuần tự (`deriveCompletedStages`):**
   - Chỉ lựa chọn đúng của bước đang chờ mới đưa tiến trình sang bước kế tiếp. Lựa chọn của bước sau được gửi sớm không được tích lũy để hoàn thành sau đó; `hint_ack` không hoàn thành bước nào.
   - **Bước `checkout`:** Bắt buộc quét đủ **cả 2 mã khác nhau** `REQUIRED_CHECKOUT_SCAN_KEYS = ['scan_item_1', 'scan_item_2']` (lưu trữ bằng `Set<string>`, quét trùng 1 mã 2 lần không được công nhận) VÀ có hành động trả tiền đúng `REQUIRED_CHECKOUT_TENDER_KEY = 'tender_3k_receipt'`.
   - Hành động trả tiền đúng chỉ hoàn thành checkout nếu xảy ra **sau** hai lần quét hợp lệ; trả tiền trước rồi quét bù không được tính.
   - **Các bước khác (`greet`, `milk`, `cooler`, `quarantine`, `mentor`):** Được ghi nhận hoàn thành khi xuất hiện lựa chọn đúng thuộc `stage` tương ứng theo cấu hình catalog server.
   - Kết quả trả về danh sách các stage hoàn thành nằm trong `catalog.requiredStages`.

### 3.4. Công thức tính điểm và Xếp loại Authoritative
- **Tỷ lệ chính xác (Accuracy Ratio):** $\text{accuracyRatio} = \frac{\text{rightCount}}{\text{totalActions}}$
- Khi tính tỷ lệ, bỏ `hint_ack` và các lần lặp lại cùng một lựa chọn đúng; lựa chọn sai vẫn được tính mỗi lần. Cách này ngăn việc gửi lặp hành động đúng để pha loãng lỗi.
- **Tỷ lệ hoàn thành bước (Stage Ratio):** $\text{stageRatio} = \frac{\text{completedStages.length}}{\text{requiredStages.length}}$
- **Điểm số (Score):**
  - Nếu hoàn thành tất cả các bước: $\text{score} = \text{round}(\text{accuracyRatio} \times 100)$
  - Nếu chưa hoàn thành đủ các bước: $\text{score} = \min(\text{rawScore}, \text{round}(\text{stageRatio} \times 80))$
- **Trạng thái đạt (`passed`):** $\text{allStagesCompleted} \land (\text{score} \ge 80)$
- **Xếp loại (`rating`):**
  - **`EXCELLENT`:** $\text{passed} \land (\text{score} \ge 95)$
  - **`GOOD`:** $\text{passed} \land (\text{score} < 95)$
  - **`NEEDS_IMPROVEMENT`:** Không thỏa mãn điều kiện `passed`.

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
