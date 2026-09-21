# Biên bản chấp nhận baseline Sprint 0

Đây là baseline Sprint 0 đã nghiệm thu và là điểm xuất phát để nhóm bắt đầu Sprint 1. Baseline chỉ có prototype phía trình duyệt; không tuyên bố đã có REST server, PostgreSQL hoặc runtime Godot.

## Trạng thái kỹ thuật ngày 21/09/2026

- Project Owner đã yêu cầu chốt Sprint 0 làm baseline trên `main`.
- `npm run verify` đạt production build và **4/4 Playwright E2E** trước lẫn sau khi chuẩn hóa repository.
- Tên và tài khoản GitHub của bốn thành viên đã được Project Owner cung cấp.
- GitHub Actions `baseline-quality` kiểm chứng repository policy và web baseline khi thay đổi được push.
- Việc demo trên máy thành viên khác vẫn cần ghi bằng chứng tại Sprint 1 kickoff; không được suy diễn từ kết quả local.

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
pwsh -File scripts/verify.ps1
```

Kết quả yêu cầu: build thành công và 4/4 bài E2E đạt.

## Checklist PR

- [x] Owner và reviewer đã được điền bằng tên thật và GitHub.
- [x] Project Owner đã chấp nhận giới hạn trong `apps/web/README.md`.
- [x] Local baseline gate đạt; CI `baseline-quality` là cổng bắt buộc cho các PR tiếp theo.
- [x] Không có secret, dữ liệu thật, `node_modules`, `dist` hoặc test output trong commit.
- [ ] Một thành viên khác đã chạy bản demo trên máy của họ.
- [x] Bản đồ tài liệu, backlog và kickoff Sprint 1 đã được cập nhật.
- [ ] Branch protection yêu cầu CODEOWNERS review đã được bật trên GitHub.

Baseline bootstrap được tích hợp theo quyết định trực tiếp của Project Owner. Ngoại lệ này không áp dụng cho code Sprint 1; các thay đổi tiếp theo phải qua Pull Request và reviewer khác owner.
