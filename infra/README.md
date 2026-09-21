# Local infrastructure

Baseline không cần dịch vụ hạ tầng. Khi `BE-01` thêm PostgreSQL, Pull Request đó phải thêm Compose, `.env.example`, health check, volume đặt tên và hướng dẫn reset an toàn; không commit secret hoặc dữ liệu thật.
