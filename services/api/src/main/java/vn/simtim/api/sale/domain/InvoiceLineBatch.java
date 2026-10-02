package vn.simtim.api.sale.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Phân bổ lô hàng cho một dòng hóa đơn (FEFO). */
public record InvoiceLineBatch(
        UUID organizationId,
        UUID storeId,
        UUID invoiceLineId,
        UUID productId,
        UUID productBatchId,
        BigDecimal quantity) {}
