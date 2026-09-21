# Hướng dẫn đóng góp

## Chuẩn bị môi trường

1. Cài Node.js 22 và Git.
2. Clone repository, tạo nhánh từ `main` và chạy `npm ci` trong `prototype-v2/`.
3. Đọc [`docs/README.md`](docs/README.md), backlog item và `AGENTS.md` gần phần mã sẽ sửa.

## Quy trình công việc

1. Mỗi thay đổi phải có Issue hoặc backlog item với kết quả, owner, reviewer và tiêu chí chấp nhận.
2. Thành viên dùng `feature/<module>-<ket-qua>` hoặc `fix/<van-de>`. Codex dùng tiền tố `codex/`.
3. Giữ PR nhỏ và không trộn refactor không liên quan với thay đổi hành vi.
4. Thay đổi API/CSDL phải có reviewer của cả phía cung cấp và phía sử dụng.
5. Chỉ merge khi CI đạt, reviewer chấp thuận và tài liệu liên quan đã cập nhật.

Khuyến nghị Conventional Commits: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:` và `ci:`.

## Cổng chất lượng Sprint 0

```powershell
cd prototype-v2
npm run verify
```

Nếu không chạy được một kiểm tra, ghi rõ lệnh, nguyên nhân và rủi ro trong PR; không ghi chung chung là “chưa test”.

## Tài liệu và nhật ký

- Thay đổi hành vi, cấu trúc hoặc quy trình phải cập nhật [`CHANGELOG.md`](CHANGELOG.md) trong mục `Unreleased`.
- Chỉ thêm [`CONTRIBUTION_LOG.md`](CONTRIBUTION_LOG.md) sau khi người chịu trách nhiệm đã review và nhận phần việc. AI không được tự quy đổi thành đóng góp của thành viên.
- Output build, secret, database thật, ảnh test và cache công cụ không được commit.

## Git an toàn

- Không force-push nhánh dùng chung.
- Không sửa hoặc xóa thay đổi chưa commit của người khác.
- Không commit trực tiếp lên `main`, trừ thao tác tích hợp đã được Project Owner chỉ định rõ.
- Trước khi xóa nhánh, xác nhận commit cần giữ đã có trên `main` hoặc một nhánh lưu trữ có chủ đích.
