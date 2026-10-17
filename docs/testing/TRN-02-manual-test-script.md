# TRN-02 — Kịch bản kiểm thử ca đào tạo

**Trạng thái:** Working — TV4 thực thi và điền bằng chứng; chưa phải test report hay xác nhận Done

**Owner kiểm thử:** TV4 — Lê Văn Chiến (`@VanChien11-02`)

**Reviewer:** TV1 — Nguyễn Đức Phát (`@zomboXx`) review luật chấm; TV2 — Nguyễn Văn Trung (`@nguyentrung01ute-ui`) review contract

**Phụ thuộc:** TRN-01 (#54), implementation TRN-02 (#55)

Tài liệu này là runbook để TV4 kiểm thử thủ công ca `SCN-RETAIL-01`. Nguồn oracle là [contract TRN-02](../../contracts/TRN-02_TRAINING_SCENARIO_CONTRACT.md) và implementation hiện tại trong [`scenario.ts`](../../apps/web/src/training/scenario.ts). Không đổi trạng thái ca thành `Pass` chỉ vì test tự động hoặc người khác báo đạt; người chạy phải ghi commit, môi trường, kết quả thực tế và bằng chứng.

## 1. Điều kiện trước khi chạy

1. Ghi SHA cần kiểm thử:

   ```powershell
   git rev-parse HEAD
   ```

2. Từ `apps/web`, cài đúng dependency và build trước khi chạy test Playwright. `test:e2e:demo` dùng `vite preview`, vì vậy nếu bỏ qua bước build thì có thể vô tình kiểm thử thư mục `dist` cũ.

   ```powershell
   npm ci
   npm run build
   npm run test:e2e:demo -- --grep "TRN-02"
   ```

3. Để kiểm thử thủ công, chạy:

   ```powershell
   npm run dev
   ```

   Mở `http://127.0.0.1:5173/training`. Kiểm cả desktop `1280x720` và mobile `390x844` bằng DevTools.

4. Bật DevTools > Network, giữ log và lọc `Fetch/XHR`. Trong toàn bộ ca học không được có request ghi (`POST`, `PUT`, `PATCH`, `DELETE`) tới `sales`, `invoices`, `inventory`, `receiving`, `stocktake` hoặc `catalog`.

## 2. Oracle dùng chung

- Chuỗi bắt buộc: `greet -> milk -> checkout -> cooler -> quarantine -> mentor`.
- Hoàn thành chỉ khi đạt đủ 6/6 stage theo đúng thứ tự.
- Điểm: `max(0, 100 - 10 * số lựa chọn sai)`.
- `EXCELLENT`: hoàn thành, 0 lỗi, 100 điểm.
- `SATISFACTORY`: hoàn thành, 1–3 lỗi, 70–90 điểm.
- `NEEDS_PRACTICE`: chưa hoàn thành hoặc có hơn 3 lỗi.
- Khi TRN-03 lưu kết quả, server phải tự xác thực thứ tự sự kiện, đáp án đúng/sai, số lỗi, trạng thái hoàn thành, điểm và xếp loại. Không tin `isRight`, `completedStages`, `passed`, `score` hoặc `grade` do client khai.

## 3. Bộ ca kiểm thử

### TRN02-M01 — Mục tiêu và hướng dẫn đầu ca

**Mục đích:** xác nhận người học biết mục tiêu, cách điều khiển và dữ liệu đang ở sandbox.

1. Mở `/training` ở desktop rồi mobile.
2. Quan sát màn hình chào mừng trước khi bấm **Bắt đầu ca thực hành**.
3. Bấm bắt đầu và quan sát nhật ký ca, mục tiêu tiếp theo, điều khiển và chỉ báo sandbox.

**Kỳ vọng:** có mục tiêu ba nhóm kỹ năng; có hướng dẫn WASD/phím mũi tên, `E`, chuột/chạm; mục tiêu đầu là gặp Khách Linh; không tràn ngang ở hai viewport; không có request ghi vận hành.

### TRN02-M02 — Luồng đúng 6 bước

1. Gặp **Khách Linh**; chọn xác nhận “2 hộp sữa ít đường”.
2. Đến **Kệ sữa & đồ uống**; lấy 2 hộp sữa ít đường giá 8.500đ/hộp.
3. Đến **Quầy thanh toán**; quét đủ 2 hộp, nhận 20.000đ, trả 3.000đ và giao hóa đơn.
4. Đến **Tủ mát**; lấy 1 hộp sữa chua hết hạn khỏi tủ.
5. Đến **Khu hàng cần xử lý**; đặt vào khay riêng và gắn nhãn “Hết hạn — không bán”.
6. Gặp **Mentor Mai**; báo đúng loại hàng, số lượng và vị trí bàn giao.

**Kỳ vọng:** mục tiêu chỉ tiến sau lựa chọn đúng; tổng kết hiện `100 / 100`, `Xuất sắc`, `Hoàn thành đúng ngay từ lần đầu`; có **Trở về cửa hàng** và **Chơi lại ca này**; Network không có request ghi vận hành.

### TRN02-M03 — Sai rồi sửa đúng

Tại mỗi stage, chọn một đáp án sai trước rồi chọn đáp án đúng:

1. `greet`: giới thiệu hàng giảm giá.
2. `milk`: lấy sữa có đường.
3. `checkout`: trả 2.000đ.
4. `cooler`: giảm giá hàng hết hạn.
5. `quarantine`: đặt chung với hàng mới nhận.
6. `mentor`: báo không có gì cần bàn giao.

**Kỳ vọng:** mỗi lần sai có phản hồi nêu lý do và cách sửa; stage không tiến khi còn sai; sau 6 lỗi và sửa đủ 6 bước, tổng kết là `40 / 100`, `Cần luyện tập thêm`, `6 lần được mentor hướng dẫn lại`.

### TRN02-M04 — Không được bỏ qua hoặc đảo thứ tự

1. Bắt đầu ca nhưng tương tác Mentor Mai, kệ sữa hoặc quầy trước Khách Linh.
2. Ở checkout chỉ quét 1/2 sản phẩm.
3. Thử nhận tiền khi chưa quét đủ.

**Kỳ vọng:** stage vẫn là `greet` khi đi sai trạm; hệ thống nhắc mục tiêu hiện tại; nút nhận tiền bị vô hiệu hóa khi mới quét 1/2; không thể mở màn tổng kết nếu chưa đủ chuỗi 6 bước.

### TRN02-M05 — Bỏ dở và tiếp tục

1. Hoàn thành bước `greet`, sau đó bấm **Về cửa hàng**.
2. Ở hộp thoại xác nhận, bấm **Tiếp tục chơi**.
3. Xác nhận đang ở stage `milk`, rồi bấm **Về cửa hàng** và chọn **Rời ca**.
4. Mở lại khu đào tạo và bắt đầu ca mới.

**Kỳ vọng:** hủy hộp thoại giữ nguyên tiến độ; xác nhận rời ca thoát an toàn; lần mở lại bắt đầu tại `greet`, vị trí ban đầu `(10, 11)`, giỏ rỗng, không mang lỗi/điểm của phiên cũ.

### TRN02-M06 — Chơi lại phải tạo phiên sạch

1. Hoàn thành một ca có ít nhất 1 lỗi.
2. Ghi điểm, xếp loại và số lỗi ở tổng kết.
3. Bấm **Chơi lại ca này**, bắt đầu lại và hoàn thành toàn bộ bằng đáp án đúng.

**Kỳ vọng:** intro xuất hiện lại; vị trí `(10, 11)`, stage `greet`, giỏ rỗng, số bước và action cũ được xóa; lần hai đạt `100 / 100`, 0 lỗi. Kết quả phiên trước không được cộng dồn vào phiên mới.

### TRN02-M07 — Cách ly hóa đơn và tồn kho

1. Đăng nhập demo bằng tài khoản quản lý `QL001` / `demo123`.
2. Ghi lại số hóa đơn, tồn lô `LO01`, danh sách biến động và phiếu kiểm kê hiện tại.
3. Vào **Đào tạo nghiệp vụ**, chạy hết ca đúng hoặc sai rồi trở về cửa hàng.
4. Đối chiếu lại bốn giá trị đã ghi và Network log.

**Kỳ vọng:** số hóa đơn, tồn `LO01`, biến động và phiếu kiểm kê không đổi; không có request ghi vận hành. Nếu khác, ghi `Fail` P0 và không tiếp tục đề nghị Done.

### TRN02-M08 — Contract không tin dữ liệu chấm từ client

Ca này dành cho lúc TRN-03 có API lưu phiên/kết quả.

1. Gửi một chuỗi sự kiện hợp lệ nhưng sửa `score=100`, `grade=EXCELLENT`, `isRight=true` cho một lựa chọn sai.
2. Gửi đủ tên 6 stage nhưng đảo thứ tự hoặc bỏ sự kiện bắt buộc.
3. Gửi lại cùng `sessionId + actionSequence` với payload khác.

**Kỳ vọng:** server bỏ qua hoặc từ chối các kết quả client tự khai; tự suy ra đúng/sai từ ID sự kiện/lựa chọn và phiên bản kịch bản; không đánh dấu hoàn thành cho chuỗi sai thứ tự; retry trùng phải idempotent, payload khác phải bị từ chối; không cộng điểm/lỗi hai lần.

## 4. Checklist review trước khi báo Pass

- TV1 xác nhận công thức điểm, ngưỡng xếp loại và quy tắc “chưa đủ 6/6 thì chưa hoàn thành”.
- TV2 xác nhận wire contract dùng ID ổn định cho sự kiện/lựa chọn; `choiceText` chỉ để hiển thị, không phải khóa chấm.
- TV2 xác nhận server không dùng trực tiếp `isRight` hoặc `completedStages` do browser gửi.
- Test tự động TRN-02 đạt trên đúng SHA sau `npm run build`.
- Test thủ công M01–M07 có kết quả và bằng chứng; M08 ghi `Blocked` cho tới khi TRN-03 có API, không được ghi `Pass` giả định.
- PR đã merge, CI đạt và reviewer xác nhận trước khi chuyển Issue sang Done.

## 5. Nhật ký thực thi

Nhật ký dưới đây là kết quả đối chiếu kịch bản bằng Playwright trên Chrome; chưa được ghi nhận là một lượt test tay độc lập.

| Trường                | Giá trị                                                     |
| --------------------- | ----------------------------------------------------------- |
| Ngày/giờ              | 16:05 17/10/2026                                            |
| Người chạy            | TV4 — Lê Văn Chiến                                          |
| Commit SHA            | Kết quả merge evidence `e855e74` + implementation `c56437c` |
| Trình duyệt/phiên bản | Google Chrome `155.0.8059.80`                               |
| Thiết bị/viewport     | Desktop `1280x720`; mobile `390x844`                        |
| Lệnh test tự động     | Demo `25/25 Pass`; API consumer `39/39 Pass`                |

| Test case | Desktop | Mobile  | Bằng chứng                                                           | Ghi chú/Issue                                    |
| --------- | ------- | ------- | -------------------------------------------------------------------- | ------------------------------------------------ |
| TRN02-M01 | Pass    | Pass    | [`image.png`](image.png); `trn-01-training-scene.spec.ts`            | Mục tiêu, hướng dẫn, sandbox và viewport đạt     |
| TRN02-M02 | Pass    | Pass    | `trn-02-training-scenario.spec.ts`; `workflows.spec.ts`              | 100 điểm; 0 request ghi vận hành                 |
| TRN02-M03 | Pass    | Pass    | `trn-02-training-scenario.spec.ts`; `trn-02-acceptance-gaps.spec.ts` | 6 phản hồi sai; 40 điểm                          |
| TRN02-M04 | Pass    | Pass    | `trn-02-acceptance-gaps.spec.ts`                                     | Không tiến stage; nhận tiền bị khóa khi quét 1/2 |
| TRN02-M05 | Pass    | Pass    | `trn-02-training-scenario.spec.ts`; `workflows.spec.ts`              | Hủy modal giữ tiến độ; rời ca reset phiên        |
| TRN02-M06 | Pass    | Pass    | `trn-02-training-scenario.spec.ts`; `trn-02-acceptance-gaps.spec.ts` | Phiên 90 điểm chơi lại thành phiên sạch 100 điểm |
| TRN02-M07 | Pass    | Pass    | `workflows.spec.ts`                                                  | Snapshot hóa đơn/tồn/movement/phiếu không đổi    |
| TRN02-M08 | Blocked | Blocked | —                                                                    | Chờ TRN-03 API                                   |

Giá trị hợp lệ: `Pass`, `Fail`, `Blocked`, `Not run`. Với `Fail`, ghi bước tái hiện, expected, actual, SHA, ảnh/trace và liên kết Issue.
