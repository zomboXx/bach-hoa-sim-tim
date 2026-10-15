# QA-03 — Kế hoạch test desktop/mobile/offline

**Owner:** TV4 — Lê Văn Chiến (`@VanChien11-02`)  
**Reviewer:** TV3 — Nguyễn Văn Thi (`@thinguyen135`)  
**Phụ thuộc:** INV-03 ([#46](https://github.com/zomboXx/bach-hoa-sim-tim/issues/46))  
**Issue:** QA-03 ([#47](https://github.com/zomboXx/bach-hoa-sim-tim/issues/47))

## Mục tiêu và ranh giới

Xác nhận luồng kiểm kê chạy xuyên PWA, API và PostgreSQL sạch: nhân viên nhập theo lô trên mobile, mất mạng, reload, đăng nhập lại sau reconnect, retry giữ nguyên `clientOperationId`, quản lý duyệt bằng quyền server, rồi đối soát tồn và movement. Kiểm riêng xung đột trước sync, xung đột trước duyệt, RBAC/store scope và precision EA/KG.

QA-03 không đổi tiêu chí backlog, không gộp sáu ngoại lệ QA-02 tại [#34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34), và không coi test demo là bằng chứng provider PostgreSQL. Dữ liệu đào tạo vẫn được kiểm bằng regression PWA vì API lưu phiên đào tạo thuộc Sprint 4, chưa phải dependency của QA-03.

## Môi trường và dữ liệu

- PWA production build qua Vite preview, Chrome hệ thống trên Windows.
- PostgreSQL `17-alpine` disposable; API profile `demo`, Flyway chạy từ database sạch.
- Tài khoản server `stock`, `manager`, `sales`; password ngẫu nhiên cho từng lần chạy.
- Viewport live: mobile `390 × 844`, desktop `1366 × 768`.
- Regression viewport đã công bố: `360 × 800`, `768 × 1024`, `1366 × 768`.
- Ảnh chọn lọc lưu tại [`evidence/qa-03`](evidence/qa-03/); trace Playwright chỉ giữ tạm khi lỗi, không commit output test.

## Bộ test

| Nhóm | Test | Oracle |
|---|---|---|
| Live browser/API/DB | `qa03-live-tests/stocktake-live.spec.ts` | Không overflow; queue còn qua reload; response-loss retry cùng operation; duyệt tạo đúng một adjustment; balance/movement khớp |
| Consumer contract | `api-stocktake.spec.ts`, `stocktake-offline-queue.spec.ts` | `baseVersion`, `unitCode`, payload, actor/org/store guard, conflict dừng retry |
| Provider sync | `StocktakeSyncApiTest` | permission, idempotency, conflict, scope và không đổi tồn khi submit |
| Provider approval | `StocktakeApprovalApiTest` | tăng/giảm/không đổi, duyệt lặp, conflict trước duyệt, rollback, precision và ledger |
| Viewport/demo isolation | `stocktake-responsive.spec.ts`, `workflows.spec.ts` | mobile/tablet/desktop không cuộn ngang; game không đổi tồn/hóa đơn vận hành demo |

## Lệnh và điều kiện kết luận

```powershell
pwsh -NoProfile -File scripts/test-qa03-live.ps1
pwsh -File scripts/verify.ps1 -SkipInstall
```

Chỉ ghi `Pass` khi lệnh đã chạy trên SHA xác định. Lỗi assertion ghi `Fail`; không khởi tạo được Docker/browser/API ghi `Error`; dependency chưa có ghi `Blocked`. CI/reviewer sau push là cổng review riêng, không được suy thành Pass từ kết quả local.
