package vn.simtim.api.sale.domain;

import java.util.UUID;

/** Snapshot SKU và tên sản phẩm tại thời điểm bán, dùng cho invoice_lines. */
public record ProductSnapshot(UUID productId, String sku, String name) {}
