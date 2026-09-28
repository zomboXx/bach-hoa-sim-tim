# Local infrastructure

`BE-01` cung cấp PostgreSQL 17 cho phát triển local. Cần Docker Desktop hoặc Docker Engine.

1. Sao chép `.env.example` thành `.env` trong thư mục này và đổi `POSTGRES_PASSWORD` thành mật khẩu local riêng. `.env` không được commit.
2. Chạy `docker compose --env-file infra/.env -f infra/compose.yml up -d --wait` từ root repository.
3. Đặt `DB_PASSWORD` bằng đúng `POSTGRES_PASSWORD`, rồi chạy API theo [hướng dẫn API](../services/api/README.md).

Compose giữ dữ liệu trong named volume. `docker compose --env-file infra/.env -f infra/compose.yml down` chỉ dừng container và giữ volume. Thêm `-v` vào lệnh `down` sẽ xóa vĩnh viễn database; chỉ dùng khi đã xác nhận đây là dữ liệu thử nghiệm và có bản sao cần thiết. Không chạy lệnh reset trên dữ liệu thật.
