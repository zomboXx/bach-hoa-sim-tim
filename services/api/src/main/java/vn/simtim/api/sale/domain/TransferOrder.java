package vn.simtim.api.sale.domain;

import java.time.Instant;
import java.util.UUID;

public record TransferOrder(UUID id, UUID organizationId, UUID storeId, UUID ownerId, UUID cartId,
        UUID invoiceId, UUID operationId, long cartVersion, String reference, long amount, String status,
        String bankBin, String accountNumber, String accountName, Instant createdAt, Instant expiresAt,
        Instant resolvedAt, UUID resolvedBy, String bankReference, String resolutionNote, String confirmationMethod) {
    public boolean pending() { return "PENDING".equals(status); }
    public PosCart.Scope scope() { return new PosCart.Scope(organizationId, storeId, ownerId); }
}
