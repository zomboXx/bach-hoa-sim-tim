# TRN-03 — Contract lưu phiên và kết quả đào tạo

**Trạng thái hiện hành 20/10/2026:** provider, Flyway V15, demo seed và PWA adapter đã tích hợp vào `main` qua [PR #69](https://github.com/zomboXx/bach-hoa-sim-tim/pull/69). `TrainingApiTest` có 7 ca PostgreSQL kiểm chấm điểm server, quyền xem lịch sử theo gán cửa hàng hiện hành, retry/concurrency, rollback và cách ly bảng vận hành; browser consumer test và lượt Compose thật nằm trong [báo cáo QA-04](../docs/testing/QA-04-test-report.md). [CI `main` run 39365624618](https://github.com/zomboXx/bach-hoa-sim-tim/actions/runs/39365624618) đạt 5/5 job. Chưa có bằng chứng TV2 đã gửi review kỹ thuật; câu trạng thái ngày 18/10 bên dưới là ảnh chụp trước khi tích hợp, không còn mô tả vị trí code hiện tại.

**Status:** Project Owner duyệt phạm vi ngày 18/10/2026 tại [PR #61](https://github.com/zomboXx/bach-hoa-sim-tim/pull/61#issuecomment-6249679679); còn chờ TV2 duyệt kỹ thuật head mới và TV3/TV4 chốt event/choice map ở TRN-02. Provider/migration đang triển khai trên nhánh riêng, chưa tích hợp vào `main`.

**Owner:** TV1 — Nguyễn Đức Phát. **Reviewer:** TV2 — Nguyễn Văn Trung; TV3/TV4 kiểm consumer và luật kịch bản.

**Quyết định chấm điểm 19/10/2026:** Project Owner chọn luật oracle mới: điểm = làm tròn `100 × số lựa chọn đúng / (số lựa chọn đúng + số lựa chọn sai)`; chỉ có kết quả khi hoàn thành đủ bước, đạt từ **80** điểm, Xuất sắc từ **95** điểm. Lựa chọn đúng lặp lại không cộng điểm. Provider tính từ các action đã lưu và catalog của phiên, không nhận điểm do PWA gửi. Bản chơi thử trên trình duyệt vẫn là điểm xem trước theo luật cũ cho tới khi TRN-02 đồng bộ UI; kết quả lưu trên server là nguồn chính thức. Quyết định này thay thế dải 70–90 điểm ghi trong mô tả cũ của PR #66.

**Nguồn:** [TRN-03 #56](https://github.com/zomboXx/bach-hoa-sim-tim/issues/56), [TRN-02 #55](https://github.com/zomboXx/bach-hoa-sim-tim/issues/55), [FR-TRN-01..08 và NFR-TRN-01](../docs/product/analysis/02-danh-sach-yeu-cau.md), [ADR 0004](../docs/architecture/adr/0004-training-2d-web.md). Thiết kế đích [DB-01](../docs/architecture/database/DB-01_PHYSICAL_SCHEMA_DRAFT.md) không phải migration đã chạy.

## Ranh giới dữ liệu và quyền

- Đây là API cho **một ca đào tạo 2D trong PWA**. Không gọi service bán/nhận/điều chỉnh tồn, không có FK sang `sales` hoặc `inventory`, không dùng số tiền/hàng hóa vận hành để chấm điểm. Dữ liệu mô phỏng của ca học được định nghĩa bằng phiên bản kịch bản riêng.
- `userId`, `organizationId`, `storeId`, `trainingEnabled` và vai trò lấy từ `SessionPrincipal` đã được server xác thực. Mọi scope hoặc điểm số trong body/header client đều bị bỏ qua hoặc từ chối; không tạo vai trò server thứ năm.
- Người có `trainingEnabled=true` và session hợp lệ được xem kịch bản ACTIVE, bắt đầu/gửi hành động mới của **phiên mình**. `AuthService.login` và `authenticate` hiện đã từ chối principal không còn vai trò ở store của session; TRN-03 không tạo cách đăng nhập thứ hai. Khi cờ đào tạo tắt, không cho bắt đầu/gửi hành động **mới**; người học vẫn được đọc kết quả cũ và nhận lại response của action đã ghi khi retry đúng payload, không phát sinh ghi mới. Khi toàn bộ vai trò ở store bị thu hồi, session cũ không còn hợp lệ theo BE-02.
- Kết quả hoàn thành là lịch sử năng lực của **người học**, được giữ khi chuyển cửa hàng và qua các phiên bản kịch bản. Người học đọc toàn bộ lịch sử của mình. MANAGER có permission `training.results.read.store` chỉ xem lịch sử của nhân viên **hiện được gán** vào cửa hàng thuộc session quản lý, kể cả lần học tại cửa hàng trước. Khi nhân viên chuyển đi và quyền gán cửa hàng cũ bị thu hồi, quản lý cũ không còn xem được. ADMIN có `training.results.read.organization` xem toàn bộ lịch sử trong tổ chức. SALES/STOCK không được xem người khác; ngoài tổ chức/phạm vi trả `404`, thiếu quyền theo vai trò trả `403`.
- Phiên lưu `store_id` tại lúc bắt đầu như dấu vết nơi học, **không** dùng riêng trường đó để cấp quyền đọc lịch sử. Quyền MANAGER dựa vào gán cửa hàng hiện hành trong IAM tại thời điểm đọc; chuyển cửa hàng phải thu hồi dòng gán cũ trong `iam.user_roles`. Không cho client chọn `storeId` ở request. TV2 cần xác nhận `iam.user_roles` đủ biểu diễn quan hệ nhân viên–cửa hàng trước khi chốt quyền và migration.

## Wire contract đề xuất

Các route dưới `/api/v1/training`, dùng Bearer session và `Cache-Control: no-store` cho response chứa phiên/kết quả. UUID là định danh opaque. `occurredAt`, điểm, trạng thái hoàn thành và lời phản hồi đều do server tạo.

| Route | Body / query từ client | Kết quả | Quyền và điều kiện |
|---|---|---|---|
| `GET /scenarios` | Không có scope client | `200`: `[{id, code, version, name, summary}]` ACTIVE | Session BE-02 hợp lệ và `trainingEnabled` |
| `POST /sessions` | Header `Idempotency-Key: <UUID>` và `{scenarioId}` | `201`: `{id, scenarioId, scenarioVersion, attemptNumber, status, nextActionSequence, startedAt}` | Session hợp lệ, `trainingEnabled`; server chụp actor/org/store và tạo attempt tiếp theo |
| `POST /sessions/{id}/actions` | `{actionSequence, actionType, targetKey, choiceKey?}` | `200`: `{actionSequence, evaluationStatus, feedback, status, nextActionSequence, result?}` | Chủ phiên; action mới cần `trainingEnabled` và store session khớp store lúc bắt đầu; retry đúng action đã ghi chỉ đọc response cũ |
| `POST /sessions/{id}/abandon` | Không có body hoặc điểm từ client | `200`: `{id, status: "ABANDONED", endedAt}` | Chủ phiên; đóng phiên dở dang, retry trả cùng kết quả; được phép dù cờ đào tạo đã tắt hoặc người học chuyển store |
| `GET /sessions/{id}` | Không có scope client | `200`: trạng thái, tiến độ, hành động đã ghi, kết quả nếu hoàn thành | Chỉ chủ phiên; quản lý dùng báo cáo kết quả, không đọc chi tiết hành động |
| `GET /results` | `scope=SELF|STORE|ORGANIZATION` (mặc định `SELF`), `learnerId?`, `page` (từ 0), `size` (1–100) | `200`: `{items, page, size, total}` chứa kết quả tóm tắt | `SELF` của chính mình; `STORE` cho MANAGER theo gán nhân viên hiện hành; `ORGANIZATION` chỉ ADMIN cùng tổ chức |

Không nhận `score`, `passed`, `status`, `organizationId`, `storeId`, `learnerId` trong POST. `scenarioId` phải thuộc đúng tổ chức, ACTIVE ở lúc bắt đầu. Phiên giữ `scenarioId` và `scenarioVersion`; RETIRED sau đó không làm đổi kết quả cũ. `GET /results` trả tóm tắt tối thiểu `{sessionId, scenarioCode, scenarioVersion, learnerId, storeId, attemptNumber, totalScore, passed, completedAt}`; chỉ chủ phiên dùng `GET /sessions/{id}` để xem action/feedback chi tiết. Với `scope=SELF`, `learnerId` khác actor bị từ chối; với `scope=STORE`, query đối chiếu gán cửa hàng **hiện hành** của learner với store session của MANAGER; `scope=ORGANIZATION` giới hạn theo organization của ADMIN.

Provider lưu bản chụp các bước/lựa chọn của version vào phiên ngay khi bắt đầu. Mỗi lựa chọn có `choiceKey` ổn định, cờ đúng/sai và phản hồi ở phía server; action không có lựa chọn hiển thị dùng bản ghi `_default` nội bộ. Bản seed demo `SCN-RETAIL-01` và adapter PWA trên nhánh cục bộ dùng tám bước/choiceKey từ head PR #66; vẫn cần review và tích hợp PR #66 trước khi coi đây là baseline.

### Gửi hành động và retry

1. Tạo phiên yêu cầu `Idempotency-Key` ổn định. Cùng actor/org/store/key và cùng `scenarioId` trả lại phiên cũ; dùng lại key với payload khác trả `409 START_REPLAY_MISMATCH`. Phân bổ `attemptNumber` dưới khóa giao dịch/unique constraint để hai lần bắt đầu khác key không cùng số lần chơi.
2. Khi gửi action, trong transaction khóa phiên theo `(organization_id, id)` và kiểm owner. Tra action đã ghi với cùng `actionSequence` **trước** khi chặn phiên đã đóng, cờ đào tạo tắt hoặc store session không còn khớp store lúc bắt đầu: retry action cuối sau khi phiên chuyển `COMPLETED` vẫn phải nhận đúng response cũ. Chỉ action mới phải có `trainingEnabled`, store trùng lúc bắt đầu, trạng thái `IN_PROGRESS` và sequence kế tiếp hợp lệ.
3. Server dùng snapshot của `scenario_steps` theo version và trạng thái đã lưu để xét `actionType`, `targetKey`, `choiceKey`; không nhận kết quả chấm từ client. Danh mục sự kiện và điểm số cuối cùng do TRN-02 chốt. Chuyển trạng thái, action, score/result được commit cùng transaction.
4. Unique `(session_id, action_sequence)` và fingerprint của payload chuẩn hóa: gửi lại cùng sequence + cùng payload trả lại **cùng** đánh giá đã lưu, không cộng điểm lần hai. Cùng sequence nhưng payload khác trả `409 ACTION_REPLAY_MISMATCH`; nhảy qua sequence trả `409 ACTION_OUT_OF_ORDER`. Retry sau timeout phải đọc lại session trước khi quyết định gửi tiếp.
5. Không ghi token vào game state, IndexedDB hoặc hàng đợi offline. Mất mạng cho phép giữ tương tác cục bộ chưa gửi trong tab, nhưng không báo "đã lưu" hay tự gửi lại với sequence/key khác; consumer hiển thị trạng thái chưa đồng bộ.
6. `abandon` chuyển `IN_PROGRESS` sang `ABANDONED` một lần trong transaction; gửi lại trả cùng `endedAt`, không tạo điểm hoặc kết quả hoàn thành. Phiên `COMPLETED` không thể bỏ dở; sau khi chuyển store, người học chỉ được đọc/bỏ dở phiên cũ và bắt đầu attempt mới ở store hiện hành.

### Lỗi và tính nhất quán

| HTTP | Code | Trường hợp |
|---:|---|---|
| 400 | `INVALID_ACTION` | Body sai kiểu/thiếu field, action/target không nằm trong danh mục kịch bản |
| 401 | `UNAUTHENTICATED` | Thiếu/hết hạn Bearer session |
| 403 | `TRAINING_DISABLED` hoặc `FORBIDDEN` | Cờ đào tạo tắt khi bắt đầu/gửi action mới hoặc thiếu quyền xem người khác |
| 404 | `NOT_FOUND` | Scenario/session/learner ngoài tổ chức hoặc store scope, hoặc ID không tồn tại |
| 409 | `START_REPLAY_MISMATCH`, `ACTION_OUT_OF_ORDER`, `ACTION_REPLAY_MISMATCH`, `SESSION_CLOSED` | Key tạo phiên bị dùng với payload khác, sequence sai, payload action retry khác, hoặc phiên đã hoàn tất/bỏ dở |
| 422 | `ACTION_NOT_ALLOWED` | Sự kiện không thể đánh giá ở trạng thái hiện tại; lựa chọn sai nhưng thuộc ca học vẫn trả `200` với `INCORRECT` và phản hồi |

Lỗi trả `{code, message}` theo mẫu API hiện hành. Không dùng `500` cho input sai và không để lỗi ghi nửa phiên. Không sửa Flyway migration V1–V14; migration mới chỉ thêm schema/bảng/quyền cần cho một ca. CSDL đích DB-01 gồm `scenarios`, `scenario_steps`, `sessions`, `session_actions`, `session_results`; migration phải thêm `sessions.store_id` và dữ liệu đối chiếu replay nếu dùng cấu trúc này.

## Bằng chứng trước khi gọi Accepted

| Yêu cầu | Provider/consumer test bắt buộc |
|---|---|
| FR-TRN-01/02 | ACTIVE scenario đúng org; start lấy actor/store từ session; training disabled và khác org bị chặn; vai trò store bị thu hồi làm session BE-02 mất hiệu lực |
| FR-TRN-05/06/07 | Chuỗi đúng/sai, phản hồi server, complete, bỏ dở và play again tạo attempt riêng; scenario đổi version không sửa kết quả cũ |
| FR-TRN-08 | Learner A/B cùng/khác store; manager hiện tại xem lịch sử toàn bộ của nhân viên mới chuyển đến; manager cũ bị chặn sau thu hồi gán; ADMIN cùng org xem toàn bộ, khác org bị chặn; cờ training tắt vẫn đọc được lịch sử của chính mình |
| Retry/rollback | Tạo phiên lặp cùng/khác key/payload, action lặp cùng/khác payload (kể cả action hoàn tất sau khi session đã đóng hoặc cờ đào tạo tắt), sequence nhảy, hai request đồng thời, lỗi giữa ghi action/result; một kết quả duy nhất |
| NFR-TRN-01 | So sánh `inventory_balances`, `stock_movements`, `sales.invoices` và tổng doanh thu trước/sau ca, kể cả lỗi/rollback; kiểm không có dependency ghi chéo schema |
| Browser | Demo/API mode, hoàn thành/chơi lại, keyboard/touch, timeout, logout; API lỗi không fallback sang dữ liệu demo |

Chạy provider test trên PostgreSQL 17 sạch qua root gate/CI. Project Owner đã duyệt phạm vi để triển khai provider; chỉ đổi status kỹ thuật thành **Accepted** sau khi TV2 và consumer reviewer xác nhận cách hiện thực gán store trong IAM, vocabulary/luật chấm, DTO, quyền và test oracle. Adapter PWA và demo seed dùng key thực tế của PR #66 sau điểm review đó.
