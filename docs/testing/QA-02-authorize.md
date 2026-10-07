# QA-02 — Ma trận quyền cần đối chiếu

- Ngày cập nhật: 2026-10-07
- Nguồn: quyết định QA-02 hiện hành và [Sprint 2 boundary](../../contracts/SPRINT_2_BOUNDARY_DRAFT.md)
- Trạng thái: **Đối chiếu một phần; invoice privacy chưa đạt**

| Operation | SALES | STOCK | MANAGER | ADMIN | Scope/error mong đợi |
|---|---|---|---|---|---|
| Xem tồn/lô/movement | Có, không giá nhập | Có | Có | Có | chỉ store session; thiếu token `401` |
| Xác nhận/xem receipt | Không (`403`) | Có | Có | Có | ID ngoài store `404`; replay không lộ chứng từ |
| Quote/checkout CASH | Có | Không (`403`) | Có | Có | server lấy store/user từ session |
| Xem hóa đơn | chỉ hóa đơn tự bán | theo contract được duyệt | cả store | cả store | hóa đơn ngoài quyền `404` |
| Báo cáo | Không (`403`) | Không (`403`) | Có | Có | chỉ store session |
| Promotion P1 | đọc | đọc | đọc/ghi | đọc/ghi | regression riêng, không đổi snapshot cũ |

Các ca `Qa02E2eTest#reportPermissionsAndSalesInventoryProjectionAreEnforcedByRealSecurityChain`, `InventoryReceiptApiTest` và `InventoryReadApiTest` chứng minh 401/403, scope và không lộ giá nhập. SAL-01 hiện dùng quyền `sales.read` chung và list toàn store cho SALES; vì vậy tiêu chí “SALES không thấy hóa đơn người khác” đang **Fail**, không được suy ra là Pass từ việc route có Bearer.
