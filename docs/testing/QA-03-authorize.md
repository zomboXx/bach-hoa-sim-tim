# QA-03 — Ma trận quyền kiểm kê

Nguồn quyền là Bearer session và các contract Accepted [SYN-02](../../contracts/SYN-02_SYNC_API_CONTRACT.md), [INV-03](../../contracts/INV-03_STOCKTAKE_APPROVAL_CONTRACT.md). Header/body client không được mở rộng scope.

| Operation | SALES | STOCK | MANAGER | ADMIN | Bằng chứng |
|---|---:|---:|---:|---:|---|
| Đọc tồn/lô | Cho phép | Cho phép | Cho phép | Cho phép | `InventoryReadApiTest` và session permissions |
| Mở phiên/gửi số đếm | Từ chối `403` | Cho phép | Cho phép | Từ chối nếu không có `stocktakes.write` | `StocktakeSyncApiTest`, QA-03 live permission case |
| Đọc phiên trong đúng store | Từ chối | Cho phép | Cho phép | Cho phép | `StocktakeSyncApiTest#getSession_otherStore_returns404` |
| Duyệt điều chỉnh | Từ chối | Từ chối `403` | Cho phép | Cho phép | `StocktakeApprovalApiTest#approve_byStockUser_returns403Forbidden` và live approval |
| Duyệt phiên store khác | Không áp dụng | Không áp dụng | `404`, không lộ dữ liệu | `404`, không lộ dữ liệu | `StocktakeApprovalApiTest#approve_crossStore_returns404NotFound` |

Queue browser chỉ retry khi actor, organization, store và quyền hiện tại khớp thao tác đã lưu. Token/password không được ghi vào IndexedDB.
