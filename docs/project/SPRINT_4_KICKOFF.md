# Sprint 4 kickoff — Đào tạo, ổn định và báo cáo

**Kickoff theo quyết định Project Owner ngày 13/10/2026.** Nguồn phạm vi là [sáu mã Sprint 4 trong backlog](governance/BACKLOG.md#sprint-4--đào-tạo-ổn-định-và-báo-cáo), [REQ-01 đã ghi nhận](REQ-01_SCOPE_RECORD_2026-09-29.md), [ADR 0004 đã có từ 30/09](../architecture/adr/0004-training-2d-web.md) và [Scrum DoR/DoD](governance/SCRUM.md). [Milestone Sprint 4](https://github.com/zomboXx/bach-hoa-sim-tim/milestone/4) không có ngày kết thúc. Việc mở Issue không xác nhận Sprint 3 đã nghiệm thu hoặc các mục Sprint 4 đã Done.

Mục tiêu là hoàn thiện **một ca đào tạo nghiệp vụ 2D trong PWA** với phiên/kết quả tách khỏi giao dịch vận hành, đồng thời kiểm tra lại sản phẩm, chuẩn bị hồ sơ và bản release candidate. [ADR 0002](../architecture/adr/0002-sprint-1-platform.md) chỉ chốt stack Sprint 1; [ADR 0003](../architecture/adr/0003-application-architecture.md) chốt ranh giới module. ADR 0004 chốt lựa chọn nền tảng đào tạo, không viết lại hai quyết định cũ.

## Hiện trạng và năng lực nhóm

- PWA đã có scene `TrainingGame.vue` với di chuyển, tương tác, mục tiêu và phản hồi trong bộ nhớ trình duyệt. Auth đã trả `trainingEnabled`; chưa có migration/API `training` hoặc kết quả lưu trong PostgreSQL. [DB-01 target](../architecture/database/DB-01_PHYSICAL_SCHEMA_DRAFT.md) mô tả năm bảng đào tạo nhưng là thiết kế đích, không phải schema đã chạy.
- Đến ngày 14/10, [QA-03 #47](https://github.com/zomboXx/bach-hoa-sim-tim/issues/47) và [sáu GAP P0 tại #34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34) còn mở. [PR #53](https://github.com/zomboXx/bach-hoa-sim-tim/pull/53) có bằng chứng QA trên head trước merge; CI đạt không thay cho nghiệm thu trên `main`. Giữ các việc này ở mã gốc và tính tải TV4 trước khi kéo việc Sprint 4 vào `In Progress`.
- TV1 là owner TRN-03 và đồng owner TRN-02; TV1 điều phối và review các điểm giao. Đổi người thực hiện code phải được ghi trong Issue/backlog, không ngầm chuyển trách nhiệm.

**Điều chỉnh phân công ngày 14/10/2026:** Sau khi TV4 báo tải việc Sprint 4 vượt khả năng hoàn thành, Project Owner xác nhận TV2 và TV3 đã đồng ý chia việc. Phân công khởi động ngày 13/10 vẫn được giữ trong lịch sử Git; bảng dưới đây thay thế phần owner/reviewer của TRN-01, TRN-02 và QA-04 từ ngày 14/10. Mã, ưu tiên, phạm vi và phụ thuộc của sáu Issue không đổi. [TRN-01 #54](https://github.com/zomboXx/bach-hoa-sim-tim/issues/54), [TRN-02 #55](https://github.com/zomboXx/bach-hoa-sim-tim/issues/55) và [QA-04 #57](https://github.com/zomboXx/bach-hoa-sim-tim/issues/57) là nơi ghi phân công thực thi tương ứng.

## Sáu đầu việc hiện hành

| Mã | Chủ trì; review | Đầu ra và bằng chứng cần có | Phụ thuộc backlog |
|---|---|---|---|
| [**TRN-01**](https://github.com/zomboXx/bach-hoa-sim-tim/issues/54) (P1) | TV3; TV1 review, TV4 kiểm mobile/QA | Hoàn thiện scene Vue/SVG hiện có cho bàn phím, chuột, cảm ứng, va chạm/tương tác, viewport desktop/mobile; đo trên máy yếu nhất nhóm có. Test browser xác nhận không ghi dữ liệu bán/tồn. | PRO-04 |
| [**TRN-02**](https://github.com/zomboXx/bach-hoa-sim-tim/issues/55) (P1) | TV3 làm PWA + TV1 chốt luật chấm; TV4 kiểm kịch bản, TV2 review contract | Một ca gồm mục tiêu, hướng dẫn, thứ tự hành động, phản hồi sai, hoàn thành và chơi lại. Ghi rõ luật đánh giá có thể kiểm thử; test đúng/sai và reset phiên. | TRN-01 |
| [**TRN-03**](https://github.com/zomboXx/bach-hoa-sim-tim/issues/56) (P1) | TV1; TV2 | Module/API và migration tiến lên cho kịch bản, phiên, hành động, kết quả; PWA lưu và xem lại kết quả của chính người học. Provider/consumer tests chứng minh kiểm quyền, retry, cách ly dữ liệu và không thay đổi số dư/hóa đơn. | BE-02, TRN-02 |
| [**QA-04**](https://github.com/zomboXx/bach-hoa-sim-tim/issues/57) (P0) | TV2; TV3 kiểm web, TV4 kiểm bằng chứng, TV1 xác nhận | Regression trên bản tích hợp, ma trận quyền, thử backup/restore PostgreSQL trên Compose sạch, test report với evidence và trạng thái thật của #34/QA-03. Không ghi Pass khi còn trường hợp Fail/Blocked. | Các sprint trước |
| [**DOC-01**](https://github.com/zomboXx/bach-hoa-sim-tim/issues/58) (P0) | TV1 điều phối; cả nhóm | Báo cáo và traceability nối FR-TRN-01..08/NFR-TRN-01 với quyết định, code, test và ảnh/demo; ghi ngoại lệ thực tế và đóng góp theo bằng chứng. | QA-04 |
| [**REL-01**](https://github.com/zomboXx/bach-hoa-sim-tim/issues/59) (P0) | TV1; cả nhóm | Bản release candidate chạy được từ Compose sạch, dữ liệu demo, hướng dẫn chạy/thử, kịch bản vấn đáp và video dự phòng; ghi phiên bản/commit đã kiểm thử. | DOC-01 |

Issue giữ mã, ưu tiên, owner/reviewer và tiêu chí chấp nhận trong backlog. Chi tiết kỹ thuật ở đây là điểm giao để review theo [DoR](governance/SCRUM.md#definition-of-ready), chưa phải wire contract Accepted. Không tạo thêm mã S4-* hoặc coi việc mở Issue là bằng chứng Done.

## Điểm giao TRN-01/02/03 cần chốt trước code tích hợp

1. **Trải nghiệm:** giữ một scene/kịch bản hiện có trong `apps/web/src/training/`, không đưa Godot hoặc kịch bản kinh dị vào release. Dữ liệu `bag`, `scans`, điểm và hành động trong game là mô phỏng; không gọi API bán, nhận hàng hay điều chỉnh tồn. Chốt bộ sự kiện có nghĩa nghiệp vụ và quy tắc chấm cho ca hiện có trước khi nối lưu kết quả.
2. **Ranh giới API:** TV1/TV2/TV4 review route, DTO, lỗi và ownership trong Issue/PR TRN-03 trước khi consumer gọi. Server tự lấy actor, organization và store scope từ session, kiểm `trainingEnabled` khi bắt đầu/gửi hành động và kiểm quyền/phạm vi riêng khi đọc kết quả; không tin điểm hoặc trạng thái hoàn thành do client tự khai. Retry cùng `sessionId` + `actionSequence` phải cho cùng kết quả hoặc từ chối payload khác; gửi lặp không cộng điểm hai lần. Không lưu token trong game/offline queue.
3. **Dữ liệu:** triển khai migration mới từ [DDL đề xuất DB-01](../architecture/database/DB-01_schema_proposal.sql), chỉ các bảng/cột cần cho một ca; không sửa migration đã chạy. `training.sessions` hiện chưa có `store_id`, trong khi FR-TRN-08 cho quản lý xem kết quả nhân viên thuộc phạm vi quản lý: phải chốt cách gắn store tại thời điểm làm bài và scope đọc trước khi duyệt schema/API. Giữ kết quả của lần chơi cũ khi kịch bản đổi phiên bản. Không có FK sang `sales`/`inventory` và không ghi các schema đó trong giao dịch đào tạo.
4. **Kiểm chứng:** test PostgreSQL sạch cho learner đúng/sai quyền, xem kết quả của chính mình, quản lý cùng/khác scope, retry và rollback; so sánh tồn, movement, hóa đơn và doanh thu trước/sau ca học. Browser test một ca hoàn thành/chơi lại ở demo mode và API mode, bàn phím/chạm, lỗi mạng và trạng thái đăng nhập hết hạn. Chỉ tuyên bố NFR-TRN-01 đạt khi test trên bản tích hợp.

## Thứ tự làm và cổng review

1. TV4 hoàn tất/ghi rõ phần QA-03 còn thiếu; các owner tiếp tục xử lý #34 theo Issue gốc. TV3 xử lý phần FE còn cần review ở PR #53 trước khi bắt đầu TRN-01; TV1/TV2 có thể phác thảo contract TRN-03. Mỗi người chỉ giữ một Issue `In Progress` tại một thời điểm.
2. TV3 thực hiện TRN-01 rồi phần PWA của TRN-02; TV1 chốt luật chấm và cùng TV2 review contract, TV4 kiểm thử kịch bản/mobile. TRN-03 chốt contract/schema với reviewer rồi mới triển khai API và nối PWA; có thể chuẩn bị migration/contract sớm nhưng không coi dependency TRN-02 đã đạt.
3. TV2 chủ trì QA-04 trên commit tích hợp sau các đầu việc đào tạo và các bản sửa P0 liên quan; TV3 kiểm web, TV4 đối chiếu hồ sơ QA và TV1 xác nhận. Nếu #34 còn ngoại lệ, report ghi đúng Fail/Blocked và Project Owner quyết định phạm vi release; QA-04 không tự đóng #34.
4. DOC-01 lấy kết quả QA-04 để cập nhật báo cáo; REL-01 lấy tài liệu và bản tích hợp đã kiểm thử để dựng release candidate. Mỗi PR qua `pwsh -File scripts/verify.ps1`, CI và reviewer; Done theo Scrum chỉ sau merge và evidence.

**Quyết định tại điểm giao:** TRN-03 phải chốt store gắn với phiên học và quyền quản lý khi nhân viên chuyển cửa hàng trước khi duyệt migration/contract. Nếu #34 còn GAP P0 khi chuẩn bị release, Project Owner quyết định phạm vi nghiệm thu trước REL-01. Milestone hiện không có ngày kết thúc; chỉ Project Owner mới bổ sung hạn.
