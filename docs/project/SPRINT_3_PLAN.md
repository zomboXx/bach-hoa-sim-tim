# Sprint 3 — Hoàn tất luồng bán hàng P0

**Kế hoạch điều hành ngày 08/10/2026.** Bản chính thức ở nhánh local để Project Owner review trước khi công bố qua PR và tạo Issue. Thời gian dự kiến **08–14/10/2026** theo nhịp sprint một tuần; mốc và phân công có hiệu lực trên GitHub sau khi Project Owner duyệt. Tài liệu này quy định phạm vi và cách nghiệm thu, không khẳng định sáu lỗi đã được sửa.

## Căn cứ và mục tiêu

[Sprint 2 đã được nghiệm thu có ngoại lệ](SPRINT_2_ACCEPTANCE_2026-10-07.md): [QA-02](../testing/QA-02-traceability.md) còn 3 Fail, 3 Blocked. [Issue #34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34) là đầu mối theo dõi. Sprint 3 ưu tiên đóng **cả sáu khoảng trống P0** trên code tích hợp: nhận số lượng đúng đơn vị, bán đúng giá và quyền, checkout có thể đối soát khi mất phản hồi, PWA bán hàng qua API thật, rồi chứng minh toàn luồng trên PostgreSQL thật. Không đánh dấu Pass dựa trên mock hoặc kiểm tra cục bộ đơn lẻ.

Kế hoạch này thay thế lịch Sprint 3 “hiện trường và kiểm kê” từng ghi trong [backlog](governance/BACKLOG.md). MOB-01, SYN-01, SYN-02, INV-03 và QA-03 vẫn là P1 trong hàng chờ sau Sprint 3, chưa có ngày/owner sprint mới. Lý do: không thể vừa khép sáu P0 của Sprint 2 vừa cam kết chuỗi offline phụ thuộc nhau trong một tuần. Không thay đổi phạm vi nghiệp vụ của các mục P1 hoặc hồ sơ QA-02 ngày 07/10.

## Phân công và thứ tự

| Việc / khoảng trống | Owner | Reviewer | Phụ thuộc để tích hợp | Đầu ra |
|---|---|---|---|---|
| S3-QTY-01 / GAP-01 | TV2 — Trung | TV3 — Thi | Nền INV-01, SAL-01 | EA nguyên, KG tối đa 3 số lẻ tại receipt và sale |
| S3-SAL-01 / GAP-02 | TV3 — Thi | TV2 — Trung | Quote/checkout hiện hành | `expectedTotal`, `PRICE_CHANGED`, không ghi khi giá đổi |
| S3-RBAC-01 / GAP-03 | TV3 — Thi | TV1 — Phát | Auth session hiện hành | SALES chỉ đọc hóa đơn tự bán; quyền khác rõ ràng |
| S3-SAL-02 / GAP-04 | TV2 — Trung | TV3 — Thi | S3-SAL-01 review contract và tích hợp | Checkout idempotency, tìm lại theo `clientOperationId` |
| S3-FE-01 / GAP-05 | TV4 — Chiến | TV3 — Thi | Contract S3-SAL-01/02, S3-RBAC-01 | PWA API mode quote, checkout, hóa đơn thật |
| S3-QA-01 / GAP-06 | TV4 — Chiến | TV1 — Phát | Năm việc trên tích hợp | Live browser E2E và QA checkpoint có bằng chứng |

TV1 điều phối, review và chốt nghiệm thu; không nhận thêm feature code. TV2 làm S3-QTY-01 rồi S3-SAL-02; TV3 làm S3-SAL-01 rồi S3-RBAC-01. TV4 chuẩn bị consumer test/adapter khi contract đang review, tích hợp S3-FE-01 sau provider, rồi làm S3-QA-01. Một owner không đồng thời sửa cùng vùng `SaleService` ở hai PR. Reviewer khác owner và kiểm tra cả contract lẫn test; việc đổi wire/API/schema phải được reviewer của provider và consumer chấp thuận trong PR trước khi merge.

### S3-QTY-01 — Precision theo đơn vị

**Nguồn:** S2-GAP-01, QA02-QTY-002. **Vùng dự kiến:** receipt service, sale quote/checkout, unit/catalog lookup, provider tests; cập nhật contract nếu response validation đổi.

- Đọc `units.precision_scale` của sản phẩm ở biên ghi; EA chỉ nhận số nguyên, KG nhận tối đa ba chữ số thập phân. Không tự làm tròn đầu vào sai precision.
- Receipt, quote và checkout trả lỗi validation có cấu trúc cho số lượng sai; transaction không tạo receipt, invoice, payment hoặc movement và không đổi balance.
- Test provider chứng minh EA `0.5` bị từ chối, KG `1.234` được chấp nhận, KG `1.2345` bị từ chối, đồng thời kiểm tra hậu điều kiện DB. Giữ test làm tròn tiền integer VND hiện hành.

### S3-SAL-01 — Khóa tổng tiền được khách xác nhận

**Nguồn:** S2-GAP-02, QA02-SAL-002. **Vùng dự kiến:** sales OpenAPI/DTO/service, provider tests, tài liệu ranh giới Sprint 2.

- Quote trả tổng tiền integer VND; checkout bắt buộc `expectedTotal` integer VND từ quote. Server tính lại giá/khuyến mãi và so tổng trước khi ghi.
- Khi tổng thay đổi, trả `409 PRICE_CHANGED` theo contract để PWA yêu cầu quote mới; không ghi hóa đơn, payment, stock movement hoặc trừ balance. Checkout cùng tổng vẫn giữ FEFO và snapshot giá hiện hành.
- Test provider sửa giá/khuyến mãi giữa quote và checkout, kiểm tra response và DB; test cả hóa đơn 0 VND, quote/checkout khớp và thiếu `expectedTotal`. Cập nhật OpenAPI, provider và consumer cùng PR hoặc chuỗi PR có thứ tự.

### S3-RBAC-01 — Quyền đọc hóa đơn

**Nguồn:** S2-GAP-03, QA02-RBAC-003. **Vùng dự kiến:** sale invoice list/detail, permission enforcement và provider tests.

- SALES list/detail chỉ thấy hóa đơn có `soldBy` là chính mình trong cửa hàng của session. MANAGER/ADMIN xem hóa đơn trong cửa hàng được cấp; STOCK không xem danh sách/chi tiết hóa đơn, nhưng quote read hiện hành vẫn dùng được nếu được cấp quyền.
- Không lấy `storeId` hoặc seller do client gửi làm nguồn quyền. Danh sách không lộ hóa đơn khác; detail của hóa đơn ngoài phạm vi trả 403/404 thống nhất theo contract, không lộ dữ liệu.
- Test hai SALES cùng store, MANAGER/ADMIN cùng store, STOCK, khác store và thiếu token trên list/detail. Reviewer xác nhận cách tách quyền hoặc enforcement tương đương trước khi sửa policy.

### S3-SAL-02 — Checkout có thể đối soát và retry an toàn

**Nguồn:** S2-GAP-04, QA02-SAL-005. **Vùng dự kiến:** checkout contract/controller/service, migration Flyway tiến lên, lookup invoice, provider tests. Bắt đầu merge sau S3-SAL-01 để tránh hai nhánh cùng sửa sales transaction.

- Checkout yêu cầu `Idempotency-Key` và `clientOperationId`; lưu khóa theo actor, tổ chức/cửa hàng và payload hash trong cùng transaction với invoice/payment/stock. Replay cùng key, actor và payload trả cùng invoice, không trừ tồn lần hai; đổi payload/actor hoặc scope trả 409, không lộ invoice ngoài quyền.
- GET tìm invoice theo `clientOperationId` trong phạm vi session để đối soát sau mất response. Kết quả GET rỗng **không chứng minh POST thất bại**; chỉ retry do người dùng chọn với cùng key/payload.
- Test timeout giả lập sau commit, replay, payload khác, actor/store khác, request đồng thời, 0 VND và rollback. Dùng migration mới, không sửa migration đã chạy; schema/API thay đổi cần review provider/consumer.

### S3-FE-01 — Bán hàng PWA qua API thật

**Nguồn:** S2-GAP-05, QA02-WEB-002. **Vùng dự kiến:** `apps/web` sale/invoice feature và adapter, consumer tests; đọc `apps/web/AGENTS.md` trước khi sửa.

- API mode dùng Bearer session để quote, checkout CASH và đọc hóa đơn đúng quyền; hiển thị tổng quote, tình trạng chờ/lỗi, `PRICE_CHANGED` để quote lại, 401/403 và hóa đơn 0 VND. Không fallback dữ liệu demo khi API lỗi; demo mode vẫn qua regression.
- Chặn double click; giữ `Idempotency-Key`/`clientOperationId` ổn định trong session tab khi chưa biết kết quả. Sau timeout/5xx chỉ GET đối soát; nếu chưa thấy invoice, hiển thị trạng thái chưa xác định và nút retry chủ động với đúng key/payload.
- Consumer tests bao phủ wire, quyền, lỗi mạng, mất response sau commit, retry, refresh/relogin và demo regression. Khi provider sẵn sàng, kiểm tra cùng luồng trên API thật.

### S3-QA-01 — Nghiệm thu P0 xuyên hệ thống

**Nguồn:** S2-GAP-06, QA02-WEB-001. **Vùng dự kiến:** Playwright live, test report/traceability mới hoặc checkpoint có liên kết, CI gate.

- Chạy browser với web/API/PostgreSQL sạch, đăng nhập theo quyền, nhận lô, xem tồn, quote, checkout CASH, xem hóa đơn, kiểm tra doanh thu và tồn khớp. Dữ liệu test cô lập, có oracle DB/API độc lập với UI; không dùng mock cho kết luận live.
- Bổ sung ca giá đổi, EA lẻ, hóa đơn người bán khác, timeout/retry không ghi trùng, 0 VND và rollback; mỗi ca liên kết mã GAP/requirement/contract/test/evidence.
- QA cập nhật trạng thái sáu GAP từ Fail/Blocked sang Pass **chỉ** sau khi PR tương ứng merge, root gate và CI đạt, có bằng chứng live. Giữ nguyên báo cáo QA-02 tại mốc 07/10; báo cáo Sprint 3 ghi kết quả mới và giới hạn còn lại.

## Nhịp tích hợp và điều kiện đóng

| Mốc dự kiến | Việc cần đạt |
|---|---|
| 08/10 | PO review kế hoạch; tạo milestone/Issue con liên kết #34 sau khi duyệt; TV2/TV3/TV4 review tên trường, lỗi, quyền và test oracle trước code thay contract. |
| 09–11/10 | Provider S3-QTY-01, S3-SAL-01, S3-RBAC-01 qua PR nhỏ; S3-SAL-02 bắt đầu sau contract S3-SAL-01; TV4 chuẩn bị consumer tests. |
| 12/10 | S3-SAL-02 và S3-FE-01 tích hợp theo thứ tự provider → consumer; chạy gate từng PR. |
| 13/10 | S3-QA-01 chạy live trên source đã tích hợp, ghi bằng chứng và sửa lỗi phát hiện. |
| 14/10 | Demo/review, kiểm tra traceability, báo cáo tuần và retrospective; việc chưa đạt chuyển backlog kèm blocker, không tự đánh dấu Done. |

Khi được duyệt để công bố, tạo milestone **Sprint 3** hạn 14/10/2026 và sáu Issue con gắn nhãn sprint/P0 hiện hành, assignee theo bảng trên, liên kết [#34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34). Trong thân Issue, đặt Owner và Reviewer trên hai dòng riêng; tiêu đề và nhãn đã có mã/ưu tiên nên không lặp metadata đó trong phần mở đầu.

Mỗi Issue con phải có owner, reviewer, phụ thuộc, vùng file/API, checklist test và liên kết S2-GAP tương ứng; Issue #34 giữ mở đến khi cả sáu GAP có bằng chứng Pass. `Done` cần PR merge, CI và reviewer theo [Scrum DoD](governance/SCRUM.md); riêng S3-QA-01 cần live evidence trên code tích hợp. Nếu provider contract không kịp review hoặc gate đỏ, hoãn consumer/live QA tương ứng và công bố rõ phần chưa nghiệm thu. Sau review Sprint 3 mới sắp lịch cho chuỗi kiểm kê offline P1 và các mục Sprint 4; không tự gán deadline cho chúng.
