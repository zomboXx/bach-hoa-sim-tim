# TRN-02 — Test report kịch bản đào tạo

- **Ngày lập:** 17/10/2026
- **Người kiểm thử:** TV4 — Lê Văn Chiến (`@VanChien11-02`)
- **Reviewer cần xác nhận:** TV1 — Nguyễn Đức Phát (`@zomboXx`) review luật chấm; TV2 — Nguyễn Văn Trung (`@nguyentrung01ute-ui`) review contract
- **Branch chạy test:** `test/trn-02-script`
- **Target bàn giao:** `origin/feature/TRN-02`
- **Source under test:** kết quả merge của test evidence `e855e74` với implementation `c56437c`
- **Kết luận local:** Pass cho M01–M07; M08 Blocked đúng dependency TRN-03; chưa đủ điều kiện Done vì CI/review chưa hoàn tất

Tài liệu này ghi kết quả thực thi theo [kịch bản kiểm thử TRN-02](TRN-02-manual-test-script.md). Chỉ đổi kết luận thành `Pass` khi kết quả gắn với một commit cụ thể, các ca bắt buộc đã đạt và không còn lỗi chưa xử lý trong phạm vi TRN-02.

## 1. Môi trường kiểm thử

| Trường       | Giá trị cần ghi                     |
| ------------ | ----------------------------------- |
| Commit SHA   | Kết quả merge `e855e74` + `c56437c` |
| Hệ điều hành | Windows 10 `10.0.19045.7807`        |
| Node.js      | `v24.16.0`                          |
| npm          | `11.13.0`                           |
| Trình duyệt  | Google Chrome `155.0.8059.80`       |
| Desktop      | `1280x720`                          |
| Mobile       | `390x844`                           |
| Chế độ       | `Demo PWA`                          |

## 2. Kết quả test tự động

Chạy từ `apps/web` và chép đúng tổng số test, không chỉ ghi “đã chạy”:

```powershell
npm ci
npm run build
npm run test:e2e:demo -- --grep "TRN-02"
```

| Lệnh                        | Kết quả thực tế                                    | Trạng thái |
| --------------------------- | -------------------------------------------------- | ---------- |
| `npm ci`                    | Cài sạch 149 package, exit code `0`                | Pass       |
| `npm run build`             | Typecheck đạt; Vite build 40 module, exit code `0` | Pass       |
| Playwright demo             | `25 passed (3.6m)`, exit code `0`                  | Pass       |
| Playwright API consumer     | `39 passed (2.0m)`, exit code `0`                  | Pass       |
| TRN-02 targeted + isolation | `5 passed (2.1m)`, exit code `0`                   | Pass       |

Nếu phải sửa quyền `node_modules`/`dist` trước khi chạy, ghi đó là sự cố môi trường và không tính là lỗi sản phẩm.

## 3. Kết quả acceptance trên browser

Các kết quả dưới đây được thực thi và đối chiếu bằng Playwright trên Chrome; ảnh `image.png` là bằng chứng giao diện bổ sung cho M01. Không ghi nhận đây là một lượt test tay độc lập.

| Test case                         | Kết quả | Expected/Actual tóm tắt                                                  | Bằng chứng hoặc Issue                                                |
| --------------------------------- | ------- | ------------------------------------------------------------------------ | -------------------------------------------------------------------- |
| TRN02-M01 — Mục tiêu/hướng dẫn    | Pass    | Desktop/mobile hiển thị mục tiêu, hướng dẫn, sandbox và không tràn ngang | [`image.png`](image.png); `trn-01-training-scene.spec.ts`            |
| TRN02-M02 — Luồng đúng            | Pass    | Hoàn thành đủ 6 stage, 100 điểm, Xuất sắc; mobile full flow cũng đạt     | `trn-02-training-scenario.spec.ts`; `workflows.spec.ts`              |
| TRN02-M03 — Sai rồi sửa           | Pass    | Sáu lựa chọn sai đều có phản hồi; sửa đúng hoàn thành 40 điểm            | `trn-02-training-scenario.spec.ts`; `trn-02-acceptance-gaps.spec.ts` |
| TRN02-M04 — Không đảo bước        | Pass    | Đi Mentor/quầy trước không đổi stage; nhận tiền khóa khi mới quét 1/2    | `trn-02-acceptance-gaps.spec.ts` desktop/mobile                      |
| TRN02-M05 — Bỏ dở                 | Pass    | Tiếp tục giữ tiến độ; xác nhận rời và vào lại tạo phiên sạch             | `trn-02-training-scenario.spec.ts`; `workflows.spec.ts`              |
| TRN02-M06 — Chơi lại              | Pass    | Phiên đầu 1 lỗi/90 điểm; chơi lại đúng đạt 100 điểm, không cộng dồn lỗi  | `trn-02-acceptance-gaps.spec.ts`; test replay gốc                    |
| TRN02-M07 — Cách ly dữ liệu       | Pass    | Snapshot trước/sau ca giống nhau; 0 request ghi endpoint vận hành        | `workflows.spec.ts`; happy-path network oracle                       |
| TRN02-M08 — Không tin điểm client | Blocked | Chờ TRN-03 có API lưu phiên/kết quả                                      | TRN-03 #56                                                           |

Quy ước:

- `Pass`: actual khớp toàn bộ expected trên đúng SHA.
- `Fail`: có ít nhất một actual khác expected; phải mở Issue hoặc ghi liên kết lỗi.
- `Blocked`: dependency/môi trường chưa cho phép đi tới assertion.
- `Not run`: chưa thực thi.

## 4. Lỗi phát hiện

Không phát hiện lỗi sản phẩm trong M01–M07.

Hai sự cố công cụ không được tính là lỗi PWA:

1. Lần chạy `npm run verify` trực tiếp trên Windows hoàn thành toàn bộ assertion demo tại thời điểm chạy nhưng kẹt khi Playwright dừng web server, nên phải ngắt trước suite API. Chạy lại với preview server quản lý riêng trả exit code `0` cho demo `25/25` và API `39/39`.
2. Bản đầu của test M04 dùng accessible name `exact` và không tính số thứ tự trên nút; locator được sửa sang regex theo nhãn người dùng, sau đó desktop/mobile đều Pass.

## 5. Kiểm tra cách ly dữ liệu

Ghi số liệu trước và sau TRN02-M07:

| Oracle vận hành                | Trước ca | Sau ca | Kết quả |
| ------------------------------ | -------: | -----: | ------- |
| Số hóa đơn                     |      `1` |    `1` | Pass    |
| Tồn lô `LO01`                  |     `46` |   `46` | Pass    |
| Số movement                    |      `3` |    `3` | Pass    |
| Số phiếu kiểm kê               |      `1` |    `1` | Pass    |
| Request ghi vận hành từ ca học |      `0` |    `0` | Pass    |

## 6. Kết luận và việc còn chờ

**Pass local cho phạm vi hiện có:** M01–M07 đạt trên working tree chứa implementation `c56437c`; test tự động đạt; không phát hiện lỗi sản phẩm. M08 giữ `Blocked` đúng kế hoạch vì TRN-03 chưa có API lưu phiên/kết quả để thử payload giả mạo, thứ tự action và retry.

Chưa chuyển TRN-02 sang Done: CI chưa chạy trên commit kết quả, TV1 chưa xác nhận luật chấm và TV2 chưa xác nhận contract.

## 7. Mẫu comment cho Issue #55

Sao chép đoạn dưới vào Issue và thay các phần trong `<...>`:

```text
TV4 verification — TRN-02

- Source: merge result of test/trn-02-script e855e74 + origin/feature/TRN-02 c56437c, pushed to origin/feature/TRN-02
- Môi trường: Windows 10.0.19045.7807, Node v24.16.0, npm 11.13.0, Chrome 155.0.8059.80
- Automated: npm ci Pass; lint/format/typecheck/build Pass; demo 25/25 Pass; API consumer 39/39 Pass
- Browser acceptance: M01–M07 Pass trên desktop 1280x720 và mobile 390x844; M08 Blocked bởi TRN-03
- Data isolation: hóa đơn 1→1, LO01 46→46, movement 3→3, phiếu kiểm kê 1→1; operational write requests = 0
- Defect: không phát hiện lỗi sản phẩm trong M01–M07
- Evidence: docs/testing/TRN-02-test-report.md, docs/testing/image.png và các Playwright specs liên kết trong report

Kết luận: Pass local cho phạm vi hiện có. Chưa đề nghị Done cho tới khi CI đạt, TV1 review luật chấm và TV2 review contract.
```
