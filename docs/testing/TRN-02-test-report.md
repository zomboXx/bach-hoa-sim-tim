# TRN-02 — Test report kịch bản đào tạo

- **Ngày lập:** 16/10/2026
- **Người kiểm thử:** TV4 — Lê Văn Chiến (`@VanChien11-02`)
- **Reviewer cần xác nhận:** TV1 — Nguyễn Đức Phát (`@zomboXx`) review luật chấm; TV2 — Nguyễn Văn Trung (`@nguyentrung01ute-ui`) review contract
- **Branch:** `test/trn-02-script`
- **Source under test:** `<điền commit SHA từ git rev-parse HEAD>`
- **Kết luận local:** Chưa kết luận — cần hoàn thành metadata, M02–M07 và bằng chứng tương ứng; M08 chờ TRN-03

Tài liệu này ghi kết quả thực thi theo [kịch bản kiểm thử TRN-02](TRN-02-manual-test-script.md). Chỉ đổi kết luận thành `Pass` khi kết quả gắn với một commit cụ thể, các ca bắt buộc đã đạt và không còn lỗi chưa xử lý trong phạm vi TRN-02.

## 1. Môi trường kiểm thử

| Trường       | Giá trị cần ghi           |
| ------------ | ------------------------- |
| Commit SHA   | `<git rev-parse HEAD>`    |
| Hệ điều hành | `Windows <phiên bản>`     |
| Node.js      | `<node --version>`        |
| npm          | `<npm --version>`         |
| Trình duyệt  | `Chrome/Edge <phiên bản>` |
| Desktop      | `1280x720`                |
| Mobile       | `390x844`                 |
| Chế độ       | `Demo PWA`                |

## 2. Kết quả test tự động

Chạy từ `apps/web` và chép đúng tổng số test, không chỉ ghi “đã chạy”:

```powershell
npm ci
npm run build
npm run test:e2e:demo -- --grep "TRN-02"
```

| Lệnh              | Kết quả thực tế            | Trạng thái   |
| ----------------- | -------------------------- | ------------ |
| `npm ci`          | `<exit code hoặc lỗi>`     | Not recorded |
| `npm run build`   | `<BUILD SUCCESS hoặc lỗi>` | Not recorded |
| Playwright TRN-02 | `<ví dụ: 5 passed (1.0m)>` | Not recorded |

Nếu phải sửa quyền `node_modules`/`dist` trước khi chạy, ghi đó là sự cố môi trường và không tính là lỗi sản phẩm.

## 3. Kết quả test thủ công

| Test case                         | Kết quả        | Expected/Actual tóm tắt                                                               | Bằng chứng hoặc Issue                                    |
| --------------------------------- | -------------- | ------------------------------------------------------------------------------------- | -------------------------------------------------------- |
| TRN02-M01 — Mục tiêu/hướng dẫn    | Chưa chuẩn hóa | Đã ghi nhận chạy desktop/mobile; cần đổi thành `Pass` hoặc `Fail` và bổ sung metadata | [`image.png`](image.png) — cần đổi tên mô tả rõ viewport |
| TRN02-M02 — Luồng đúng            | Not run        | Chờ ghi điểm 100, xếp loại và network                                                 | —                                                        |
| TRN02-M03 — Sai rồi sửa           | Not run        | Chờ ghi phản hồi, 40 điểm và 6 lỗi                                                    | —                                                        |
| TRN02-M04 — Không đảo bước        | Not run        | Chờ ghi stage không tiến và checkout bị khóa                                          | —                                                        |
| TRN02-M05 — Bỏ dở                 | Not run        | Chờ ghi giữ tiến độ/hủy phiên                                                         | —                                                        |
| TRN02-M06 — Chơi lại              | Not run        | Chờ ghi lần hai reset về 100 điểm                                                     | —                                                        |
| TRN02-M07 — Cách ly dữ liệu       | Not run        | Chờ đối chiếu hóa đơn, tồn, movement và Network trước/sau                             | —                                                        |
| TRN02-M08 — Không tin điểm client | Blocked        | Chờ TRN-03 có API lưu phiên/kết quả                                                   | TRN-03 #56                                               |

Quy ước:

- `Pass`: actual khớp toàn bộ expected trên đúng SHA.
- `Fail`: có ít nhất một actual khác expected; phải mở Issue hoặc ghi liên kết lỗi.
- `Blocked`: dependency/môi trường chưa cho phép đi tới assertion.
- `Not run`: chưa thực thi.

## 4. Lỗi phát hiện

Nếu không có lỗi sản phẩm, ghi: `Không phát hiện lỗi sản phẩm trong các ca đã thực thi.`

Nếu có lỗi, mỗi lỗi trình bày theo mẫu:

```text
Mã: TRN02-BUG-01
Mức độ: P0/P1/P2
Môi trường và SHA:
Bước tái hiện:
Expected:
Actual:
Bằng chứng:
Issue:
```

Không gộp lỗi quyền file `EPERM` của môi trường cài đặt thành lỗi PWA.

## 5. Kiểm tra cách ly dữ liệu

Ghi số liệu trước và sau TRN02-M07:

| Oracle vận hành      | Trước ca |   Sau ca | Kết quả |
| -------------------- | -------: | -------: | ------- |
| Số hóa đơn           | `<điền>` | `<điền>` | Not run |
| Tồn lô `LO01`        | `<điền>` | `<điền>` | Not run |
| Số movement          | `<điền>` | `<điền>` | Not run |
| Số phiếu kiểm kê     | `<điền>` | `<điền>` | Not run |
| Request ghi vận hành |      `0` |      `0` | Not run |

## 6. Kết luận và việc còn chờ

Chọn đúng một kết luận:

- **Pass local:** M01–M07 đạt trên cùng SHA; test tự động đạt; không có lỗi mở trong phạm vi.
- **Fail:** có ca sai expected; liệt kê Issue chặn.
- **Incomplete/Blocked:** còn ca chưa chạy, thiếu evidence hoặc dependency chưa sẵn sàng.

Ngay cả khi `Pass local`, TRN-02 vẫn chưa được chuyển Done cho tới khi PR merge, CI đạt và TV1/TV2 xác nhận phần review được giao.

## 7. Mẫu comment cho Issue #55

Sao chép đoạn dưới vào Issue và thay các phần trong `<...>`:

```text
TV4 verification — TRN-02

- Branch/SHA: test/trn-02-script @ <SHA>
- Môi trường: Windows <version>, Node <version>, <browser version>
- Automated: build <Pass/Fail>; Playwright TRN-02 <x/x Pass/Fail>
- Manual: M01 <status>, M02 <status>, M03 <status>, M04 <status>, M05 <status>, M06 <status>, M07 <status>; M08 Blocked bởi TRN-03
- Data isolation: hóa đơn <trước→sau>, LO01 <trước→sau>, movement <trước→sau>; operational write requests = <số>
- Defect: <Không phát hiện / danh sách Issue>
- Evidence: <link tới report và ảnh>

Kết luận: <Pass local / Fail / Incomplete>. Chưa đề nghị Done cho tới khi PR merge, CI đạt, TV1 review luật chấm và TV2 review contract.
```
