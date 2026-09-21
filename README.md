# Bách Hóa Sim Tím — Sprint 1

Hệ thống quản lý bán hàng và tồn kho cho một cửa hàng bán lẻ, kết hợp mô phỏng đào tạo nghiệp vụ. Bản Sprint 1 tích hợp Vue PWA, Spring Boot API, PostgreSQL/Flyway và Godot Chapter 0; trạng thái này đang chờ hardening và review trước khi nhập `main`.

## Khởi động nhanh

```powershell
pwsh -File scripts/setup.ps1
pwsh -File scripts/dev.ps1
```

Mở `http://127.0.0.1:5174`. Tài khoản bán hàng mẫu là `NV001` / `demo123`.

Khi làm phần đào tạo, cài Godot 4.6.3 cùng Web export templates và chạy `pwsh -File scripts/export-training.ps1` trước khi khởi động web. Cổng đầy đủ trước PR là `pwsh -File scripts/verify.ps1`.

## Tài liệu chính

- [Hướng dẫn và phạm vi Sprint 1](docs/project/SPRINT_1.md)
- [Mô hình dữ liệu](docs/architecture/DATA_MODEL.md)
- [Hợp đồng OpenAPI](contracts/openapi.yaml)
- [Vai trò và Chapter 0](docs/product/ROLES_AND_CHAPTER_0.md)
- [Bản đồ tài liệu](docs/README.md)
- [Quy trình đóng góp](CONTRIBUTING.md)

Mã đang phát triển được chia thành `apps/web`, `services/api` và `apps/training-godot`; hạ tầng local nằm trong `infra/`, contract dùng chung nằm trong `contracts/`. Sprint 0 đã được chốt trên `main`; mọi prototype cũ chỉ còn để truy vết trong `archive/`.
