# QA-02 — Ma trận backlog–contract–test–evidence

> **Kết quả chạy lại 20/10/2026:** sáu ngoại lệ bên dưới đã đạt trên mã tích hợp;
> hiện là **23/23 Pass**. Xem [bằng chứng đóng GAP](QA-02-closeout-2026-10-20.md).
> Bảng và số liệu ngày 07/10 phía dưới được giữ nguyên như ảnh chụp lịch sử.

- Trạng thái: **17 Pass, 3 Fail, 3 Blocked**
- Ngày cập nhật: 2026-10-07
- Source test sau independent review: `840a82a1101cbef5351407d498542c74b8ae237d`
- Evidence local: `E-QA02-API-2E6EB1B`, `E-QA02-ROOT-20261007`, `E-QA02-REVIEW-840A82A`; CI sau review: [`baseline-quality` #37590444421](https://github.com/zomboXx/bach-hoa-sim-tim/actions/runs/37590444421) `success`; chi tiết runtime và từng gate nằm trong test report

Các PR provider/consumer đã có trong lịch sử `origin/main` của source được test: [INV-01 PR #29](https://github.com/zomboXx/bach-hoa-sim-tim/pull/29) + [fix #32](https://github.com/zomboXx/bach-hoa-sim-tim/pull/32), [INV-02 #30](https://github.com/zomboXx/bach-hoa-sim-tim/pull/30), [FE-02 #31](https://github.com/zomboXx/bach-hoa-sim-tim/pull/31), [SAL-01 #26](https://github.com/zomboXx/bach-hoa-sim-tim/pull/26), [REP-01 #27](https://github.com/zomboXx/bach-hoa-sim-tim/pull/27), [PRO-01B #25](https://github.com/zomboXx/bach-hoa-sim-tim/pull/25). Đây là bằng chứng Git local; trạng thái review/CI trên GitHub vẫn phải được TV1 xác nhận trước đóng Issue.

> Follow-up ngày 11/10/2026: giữ nguyên kết luận 17 Pass, 3 Fail, 3 Blocked của
> source lịch sử phía dưới. Test tái hiện cho bốn GAP backend và hai GAP PWA,
> mapping sang S2-GAP-01…06 và điều kiện đưa vào root gate được ghi tại
> [QA-02-gap-checkpoint-2026-10-11.md](QA-02-gap-checkpoint-2026-10-11.md).
> Tất cả tiếp tục theo dõi tại [Issue #34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34),
> chưa có dòng nào được chuyển sang Pass.

> Follow-up ngày 13/10/2026 trên head PR #53: GAP-05 live 2/2, GAP-06 live 1/1,
> root gate 14 demo + 49 consumer + 165 backend đều đạt; DB oracle ghi receipt/invoice/payment
> cùng tăng một, revenue tăng 40.000 và balance khớp ledger. Xem
> [evidence GAP-06](QA-02-gap06-evidence-2026-10-13.md). Các trạng thái bên dưới
> vẫn là ảnh chụp 07/10 cho đến khi PR merge, QA chạy lại `main` và TV1 duyệt evidence.

> Follow-up review ngày 17/10/2026 bổ sung consumer/live browser test cho báo giá
> trước checkout và replay đúng `Idempotency-Key` sau khi server commit nhưng mất
> response. Oracle GAP-05 xác nhận ba request chỉ tạo hai operation bán: invoice,
> payment và SALE movement cùng tăng hai; ledger không lệch. Xem
> [evidence ngày 17/10](QA-02-sales-retry-evidence-2026-10-17.md).

> Follow-up review ngày 18/10/2026 bổ sung phân loại kết quả checkout chưa chắc
> chắn: HTTP 5xx giữ pending operation, HTTP 4xx mới xóa để sửa giỏ. Consumer và
> live browser test HTTP 502 replay cùng payload/key; oracle GAP-05 xác nhận ba
> request chỉ tạo hai invoice/payment/SALE movement, ledger không lệch. Xem
> [evidence ngày 18/10](QA-02-sales-502-evidence-2026-10-18.md). Trạng thái vẫn
> chờ merge, chạy lại trên `main` và TV1 duyệt.

| ID | Backlog/contract | Automated test / oracle | Actual | Status |
|---|---|---|---|---|
| QA02-REC-001 | INV-01 nhận đủ tạo receipt/lô/balance/movement nguyên tử | `Qa02E2eTest#receiptToCashInvoiceAndReportsReconcileWithIndependentSqlOracle`; query receipt/ledger | Một receipt, một lô và RECEIPT movement; ledger khớp balance | Pass |
| QA02-REC-002 | INV-01 từ chối toàn bộ chỉ lưu chứng từ | `#fullyRejectedReceiptPersistsTheDiscrepancyButReplayCreatesNoStockTwice` | Lưu line accepted=0/rejected=2; 0 batch/balance/movement | Pass |
| QA02-REC-003 | INV-01 replay cùng key/payload/actor không ghi lần hai | cùng test; query count độc lập | Cùng receipt ID; count receipt=1, không có stock | Pass |
| QA02-REC-004 | Replay khác actor/store/payload trả 409 | `InventoryReceiptApiTest#confirmReceipt_idempotency_otherActorOrStoreReturns409WithoutLeakingReceipt`; `#confirmReceipt_idempotency_sameActorDifferentPayloadReturns409` | 409, không lộ ID, count=1 | Pass |
| QA02-INV-001 | INV-02 hôm qua/hôm nay/+7/+8/null/BLOCKED | `InventoryReadApiTest#batches_classifyYesterdayTodayPlusSevenPlusEightNull_andBlocked` | Phân loại và available đúng biên | Pass |
| QA02-INV-002 | FEFO bỏ expired/blocked | `InventoryReadApiTest#salePort_plansFefoThenDeductsAndRecordsMovementsInCallerTransaction`; `SaleApiTest#checkout_fefoOrder_selectsBatchWithEarliestExpiry` | Phân bổ lô hợp lệ sớm nhất | Pass |
| QA02-QTY-001 | Quantity tối đa 3 số lẻ | `InventoryReceiptApiTest#confirmReceipt_quantityBeyondThreeDecimalsRejectsAllFieldsBeforeWriting`; `SaleApiTest#quoteAndCheckout_roundFractionalQuantityWithSameRule` | >3 số lẻ bị từ chối; KG làm tròn HALF_UP nhất quán | Pass |
| QA02-QTY-002 | Precision theo unit: EA=0, KG=3 | `Qa02GapReproduction#gap01_fractionalEaReceiptIsRejectedWithoutAnyWrite`; query receipt/line/batch/balance/movement | Receipt EA 0.500 trả 201 và ghi đủ stock records thay vì 422/rollback | Fail |
| QA02-SAL-001 | Quote → CASH checkout → invoice/payment/SALE movement | `Qa02E2eTest#receiptToCashInvoiceAndReportsReconcileWithIndependentSqlOracle` | Hóa đơn hai dòng 75.000 VND, payment một lần, change 5.000, trừ 1.250 KG và 1 EA | Pass |
| QA02-SAL-002 | Giá đổi giữa quote/checkout trả `PRICE_CHANGED` | `Qa02GapReproduction#gap02_priceChangedAfterQuoteRejectsCheckoutAndRollsBack`; query invoice/payment/SALE movement/balance | `expectedTotal` bị bỏ qua; checkout trả 201 theo giá mới và ghi/trừ tồn | Fail |
| QA02-SAL-003 | Thiếu tồn rollback toàn giỏ | `#insufficientSecondProductRollsBackTheWholeCart`; query invoice/payment/movement/balance | 409; 0 sales writes; cả hai balance giữ nguyên | Pass |
| QA02-SAL-004 | Hai checkout cạnh tranh lô cuối | `#concurrentCheckoutsSerializeAndOnlyOneCanConsumeTheLastUnit` | đúng một 201, một 409; một invoice/payment/movement; balance=0 | Pass |
| QA02-SAL-005 | Checkout replay/double-submit cùng key chỉ ghi một lần | `Qa02GapReproduction#gap04_checkoutReplayWritesOneInvoiceAndRejectsKeyReuseWithAnotherPayload`; query invoice/payment/movement/balance | Cùng key tạo invoice khác; payload khác vẫn 201; tổng 3 lần ghi/trừ tồn | Blocked |
| QA02-SAL-006 | Hóa đơn 0 VND vẫn completed và trừ tồn | `SaleApiTest#checkout_zeroVndTotal_createsInvoiceAndReducesStock`; PRO regression 100% | invoice/payment/tendered/change=0; balance giảm | Pass |
| QA02-SAL-007 | Tiền integer VND, HALF_UP theo line | `SaleApiTest#quoteAndCheckout_roundFractionalQuantityWithSameRule` | quote/checkout cùng subtotal/discount/grand total | Pass |
| QA02-RBAC-001 | 401/403 bốn role, store scope | `Qa02E2eTest#reportPermissionsAndSalesInventoryProjectionAreEnforcedByRealSecurityChain`; provider scope tests | reports chỉ MANAGER/ADMIN; receipt SALES 403; missing token 401 | Pass |
| QA02-RBAC-002 | SALES không thấy giá nhập | cùng test + `InventoryReadApiTest#movements_exposeReceiptSourceAndDelta_withoutCost` | inventory projection không có unit cost | Pass |
| QA02-RBAC-003 | SALES chỉ xem hóa đơn tự bán | `Qa02GapReproduction#gap03_salesCannotReadAnotherSellerInvoiceInTheSameStore`; DB xác nhận `sold_by` khác actor đọc | SALES B cùng store thấy invoice của A trong list; detail trả 200 thay vì 404 | Fail |
| QA02-REP-001 | Revenue SUM invoice completed, COUNT một lần | `Qa02E2eTest#receiptToCashInvoiceAndReportsReconcileWithIndependentSqlOracle`; hóa đơn hai dòng; SQL oracle đọc thẳng `sales.invoices` | API = SUM 75.000, COUNT 1; không thể pass nhầm nếu report join nhân bản theo hai invoice lines | Pass |
| QA02-REP-002 | Tồn báo cáo truy từ balance/batch và khớp ledger | cùng test; `balance = SUM(quantity_delta)` theo batch | API 1.250 KG; DB balance và ledger đều 1.250 | Pass |
| QA02-WEB-001 | PWA API mode nhận/tồn + report consumer; demo regression | `api-inventory.spec.ts`, `api-reports.spec.ts`, `workflows.spec.ts`; expected-red `qa02-sales-report.live.spec.ts` manager report | Có consumer mock và live spec; chưa chạy chuỗi nhận–bán–report cùng API/DB sạch | Blocked |
| QA02-WEB-002 | PWA API mode bán/checkout/hóa đơn | Expected-red `qa02-sales-report.live.spec.ts` tag `@qa02-gap05`, runner riêng `scripts/test-qa02-gap05-live.ps1`; inspection `routesFor`/`pay()` | API mode chưa cấp route sales/invoices; `pay()` chỉ chạy demo, chưa có sales adapter; ca cố ý đỏ không chạy trong live FE-02 mặc định | Blocked |
| QA02-PRO-001 | PRO-01B regression riêng, không chặn P0 | `SaleApiTest` promotion methods; `api-promotions.spec.ts` | Product/batch, tie-break, rounding, snapshot, 0 VND có regression | Pass |

## Khoảng trống phải xử lý trước nghiệm thu P0

1. SAL-01 bổ sung `expectedTotal`/`PRICE_CHANGED` và checkout idempotency/recovery.
2. Enforce unit precision theo EA/KG thay vì chỉ scale chung tối đa 3.
3. Tách `sales.read.own`/`sales.read.store` hoặc enforcement tương đương, thêm test hai SALES cùng store.
4. Nối PWA sales adapter và live browser E2E nhận → bán → báo cáo trên API thật.
