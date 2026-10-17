# QA-03 — Requirement → contract → test → evidence

| ID | Requirement | Contract/code | Test | Evidence | Kết quả |
|---|---|---|---|---|---|
| QA03-VIEW-001 | Backlog QA-03; MOB-01 không cuộn ngang | `StocktakeWorkspace.vue` | `stocktake-responsive.spec.ts`; live flow | [`mobile-offline-390x844.png`](evidence/qa-03/mobile-offline-390x844.png), [`desktop-approved-1366x768.png`](evidence/qa-03/desktop-approved-1366x768.png) | Pass |
| QA03-SYNC-001 | FR-SYNC-01..03; reload/reconnect | SYN-01 + SYN-02; IndexedDB queue; `ApiStocktakeAdapter` | live flow response-loss; queue regression | Live 2/2; operation giữ nguyên qua reload và retry | Pass |
| QA03-IDEM-001 | Retry không tạo bản ghi trùng | SYN-02 idempotency | live response-loss; `submitCount_idempotency_sameKeyAndScope_returnsSameLine` | Một line và một adjustment cho cùng `clientOperationId` | Pass |
| QA03-CONFLICT-001 | Tồn đổi trước sync không ghi đè | SYN-02 conflict detection | live stale snapshot; `submitCount_wrongVersion_returns409Conflict` | `409 CONFLICT`; không có `STOCKTAKE_ADJUSTMENT` | Pass |
| QA03-CONFLICT-002 | Tồn đổi trước duyệt rollback toàn bộ | INV-03 transaction | `approve_balanceChangedBeforeApproval_returns409ConflictAndRollsBack`; `approve_rollbackOnFailure_noPartialWrite` | Provider suite 31/31 | Pass |
| QA03-APPROVE-001 | Chỉ quản lý duyệt; tồn/movement khớp | INV-03 approval contract | live approval; approval provider suite | Balance `100 → 98`; đúng một delta `-2`; ledger/provider assertions đạt | Pass |
| QA03-RBAC-001 | Quyền và store scope | SYN-02/INV-03 permissions | live SALES `403`; sync/approval provider tests | [`QA-03-authorize.md`](QA-03-authorize.md) | Pass |
| QA03-PREC-001 | EA nguyên; KG tối đa 3 số lẻ | MOB-01/SYN-02/INV-03 | responsive consumer; provider precision tests | `unitCode` từ API; EA invalid rollback; KG hợp lệ | Pass |
| QA03-TRN-001 | Đào tạo không đổi dữ liệu vận hành | PWA training boundary; `world.ts` | `workflows.spec.ts` training isolation | Demo regression trong root gate; không suy thành API đào tạo đã triển khai | Pass |
| QA03-OFFLINE-001 | Không gửi nghiệp vụ khác offline | SYN-01 chỉ cho kiểm kê | queue tests; UI guard trong `change()` | Nhận/bán/thay đổi khác bị chặn khi offline | Pass |

Kết quả Pass trong bảng phải đọc cùng [test report](QA-03-test-report.md), nơi ghim source SHA và lệnh thực thi. QA-02 và [#34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34) không nằm trong ma trận này.
