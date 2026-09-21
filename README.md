# Bách Hóa Sim Tím — Sprint 1

Hệ thống quản lý bán hàng và tồn kho cho một cửa hàng bán lẻ, kết hợp mô phỏng đào tạo nghiệp vụ. Bản Sprint 1 tích hợp Vue PWA, Spring Boot API, PostgreSQL/Flyway và Godot Chapter 0; trạng thái này đang chờ hardening và review trước khi nhập `main`.

## Chạy môi trường Sprint 1

```powershell
docker compose up -d
mvn -f backend/pom.xml spring-boot:run
pwsh -File scripts/export-training.ps1 -Godot "duong-dan-godot-console.exe"
cd prototype-v2
npm ci
npm run dev
```

Mở `http://127.0.0.1:5174`. Tài khoản bán hàng mẫu là `NV001` / `demo123`.

## Tài liệu chính

- [Hướng dẫn và phạm vi Sprint 1](docs/SPRINT_1.md)
- [Mô hình dữ liệu và REST contract](docs/DATA_MODEL.md)
- [Vai trò và Chapter 0](docs/ROLES_AND_CHAPTER_0.md)
- [Bản đồ tài liệu](docs/README.md)
- [Quy trình đóng góp](CONTRIBUTING.md)

Sprint 0 đã được chốt trên `main`. Prototype đời đầu được lưu tại `archive/prototype-v1/`; không phát triển tính năng mới trong thư mục lưu trữ.
