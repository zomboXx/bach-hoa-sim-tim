package vn.simtim.api.sale.domain;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PosCartRepository {
    PosCart read(PosCart.Scope scope, boolean exclusive);
    Optional<PosCart.Operation> operation(UUID cartId, UUID operationId);
    void setQuantity(UUID cartId, UUID productId, BigDecimal quantity);
    void advance(UUID cartId, boolean checkout);
    void record(UUID cartId, UUID operationId, String hash, UUID invoiceId);
}
