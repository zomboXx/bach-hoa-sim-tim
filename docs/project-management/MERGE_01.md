# Điều kiện merge đầu tiên

Merge 01 tạo baseline để nhóm bắt đầu phát triển backend. Đây là prototype phía trình duyệt; không tuyên bố đã có REST server, PostgreSQL hoặc runtime Godot.

## Chức năng phải chạy

- Đăng nhập demo theo ba vai trò và ẩn chức năng không có quyền.
- Nhận một lô hàng, tăng tồn và ghi biến động.
- Bán hàng, áp dụng giá/khuyến mãi, xuất lô FEFO và tạo hóa đơn.
- Không bán lô hết hạn.
- Báo cáo và tồn phản ánh giao dịch vừa thực hiện.
- Phiếu kiểm kê giữ được khi reload offline; phát hiện xung đột khi tồn thay đổi.
- Một ca đào tạo 2D trên trình duyệt hoàn thành được và không sửa dữ liệu vận hành.

## Cổng chất lượng

```powershell
cd prototype-v2
npm ci
npm run build
npm run test:e2e
```

Kết quả yêu cầu: build thành công và 4/4 bài E2E đạt.

## Checklist PR

- [ ] Owner và reviewer đã được điền bằng tên thật.
- [ ] Nhóm đã đọc giới hạn trong `prototype-v2/README.md`.
- [ ] CI `build-and-test` đạt.
- [ ] Không có secret, dữ liệu thật, `node_modules`, `dist` hoặc test output.
- [ ] Một thành viên khác đã chạy bản demo trên máy của họ.
- [ ] Tài liệu phân tích và backlog được review.
- [ ] PR được ít nhất một thành viên khác chấp thuận.

