package vn.simtim.api.catalog.domain;

import java.time.Instant;
import java.util.OptionalLong;
import java.util.UUID;

/** Store-scoped selling prices for catalog products. */
public interface ProductPriceRepository {

    OptionalLong findCurrent(UUID organizationId, UUID storeId, UUID productId, Instant at);

    void setCurrent(UUID organizationId, UUID storeId, UUID productId, long salePrice, Instant effectiveFrom);

    void deleteByProduct(UUID organizationId, UUID productId);
}
