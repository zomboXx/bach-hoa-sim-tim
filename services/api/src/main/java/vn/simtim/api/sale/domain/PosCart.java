package vn.simtim.api.sale.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PosCart(UUID id, long version, long generation, List<Line> items) {
    public record Scope(UUID organizationId, UUID storeId, UUID ownerId) {}
    public record Line(UUID productId, String productName, BigDecimal quantity) {}
    public record Operation(String payloadHash, UUID invoiceId) {}
}
