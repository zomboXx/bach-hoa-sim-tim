# Hướng dẫn đóng góp

## Chuẩn bị môi trường

1. Cài Git, PowerShell 7 và Node.js 22.
2. Clone repository, tạo nhánh từ `main` và chạy `pwsh -File scripts/setup.ps1`.
3. Đọc [`docs/README.md`](docs/README.md), backlog item và `AGENTS.md` gần phần mã sẽ sửa.

## Quy trình công việc

1. Mỗi thay đổi phải có Issue hoặc backlog item với kết quả, owner, reviewer và tiêu chí chấp nhận.
2. GitHub Project là nguồn trạng thái vận hành; backlog Markdown giữ phạm vi và thứ tự phụ thuộc, không cập nhật trạng thái hằng ngày ở hai nơi.
3. Thành viên dùng `feature/<issue>-<ket-qua>`, `fix/<issue>-<van-de>` hoặc `docs/<issue>-<noi-dung>`. Codex dùng tiền tố `codex/`.
4. Mỗi người chỉ giữ tối đa một Issue ở `In Progress`; mở Draft PR sớm khi cần phản hồi.
5. Giữ PR nhỏ và không trộn refactor không liên quan với thay đổi hành vi.
6. Thay đổi API/CSDL phải có reviewer của cả phía cung cấp và phía sử dụng.
7. Chỉ squash merge khi CI đạt, reviewer chấp thuận, hội thoại đã giải quyết và tài liệu liên quan đã cập nhật.

Khuyến nghị Conventional Commits: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:` và `ci:`.

## Cổng chất lượng baseline

```powershell
pwsh -File scripts/verify.ps1
```

Nếu không chạy được một kiểm tra, ghi rõ lệnh, nguyên nhân và rủi ro trong PR; không ghi chung chung là “chưa test”.

## Tài liệu và nhật ký

- Thay đổi hành vi, cấu trúc hoặc quy trình phải cập nhật [`CHANGELOG.md`](CHANGELOG.md) trong mục `Unreleased`.
- Chỉ thêm [`CONTRIBUTION_LOG.md`](CONTRIBUTION_LOG.md) sau khi người chịu trách nhiệm đã review và nhận phần việc. AI không được tự quy đổi thành đóng góp của thành viên.
- Output build, secret, database thật, ảnh test và cache công cụ không được commit.
- `.agents/`, `.codex/` và bản Word đang soạn là dữ liệu local. Chỉ commit DOCX/PDF bàn giao khi Project Owner chỉ định rõ bản cuối.

## File được quản lý bởi Git

- Commit source code, test, migration, lockfile, Maven Wrapper, CI, tài liệu Markdown và `.env.example` không chứa secret.
- Không commit `.env`, IDE settings, dependency/build output, database dump, test artifact hoặc cấu hình riêng của công cụ.
- Không dùng `git add -f` cho tài liệu binary nếu chưa có quyết định bàn giao của Project Owner.

## Git an toàn

- Không force-push nhánh dùng chung.
- Không sửa hoặc xóa thay đổi chưa commit của người khác.
- Không commit trực tiếp lên `main`, trừ thao tác tích hợp đã được Project Owner chỉ định rõ.
- Trước khi xóa nhánh, xác nhận commit cần giữ đã có trên `main` hoặc một nhánh lưu trữ có chủ đích.
