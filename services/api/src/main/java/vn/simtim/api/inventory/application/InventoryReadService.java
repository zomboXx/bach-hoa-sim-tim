package vn.simtim.api.inventory.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service cho ba truy vấn tồn hiện tại của INV-02. */
@Service
@Transactional(readOnly = true)
public class InventoryReadService {

    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> PRODUCT_STATUSES = Set.of("ACTIVE", "INACTIVE");
    private static final Set<String> BATCH_STATUSES = Set.of("AVAILABLE", "BLOCKED", "EXHAUSTED");
    private static final Set<String> MOVEMENT_TYPES = Set.of("RECEIPT", "SALE", "ADJUSTMENT");

    private final InventoryReadRepository repository;
    private final Clock clock;

    public InventoryReadService(InventoryReadRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public InventoryPage<InventoryProductView> products(
            UUID organizationId, UUID storeId, UUID productId, String productStatus,
            int page, int size) {
        validatePage(page, size);
        return repository.findProducts(organizationId, storeId, productId,
                normalize(productStatus, PRODUCT_STATUSES, "status"), businessDate(), page, size);
    }

    public InventoryPage<InventoryBatchView> batches(
            UUID organizationId, UUID storeId, UUID productId, String batchStatus,
            String expiryStatus, int page, int size) {
        validatePage(page, size);
        ExpiryStatus parsedExpiry = parseExpiryStatus(expiryStatus);
        return repository.findBatches(organizationId, storeId, productId,
                normalize(batchStatus, BATCH_STATUSES, "status"), parsedExpiry,
                businessDate(), page, size);
    }

    public InventoryPage<InventoryMovementView> movements(
            UUID organizationId, UUID storeId, UUID productId, UUID batchId,
            String movementType, int page, int size) {
        validatePage(page, size);
        return repository.findMovements(organizationId, storeId, productId, batchId,
                normalize(movementType, MOVEMENT_TYPES, "type"), page, size);
    }

    LocalDate businessDate() {
        return LocalDate.now(clock.withZone(BUSINESS_ZONE));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InventoryRequestException("page phải >= 0 và size phải trong khoảng 1..100");
        }
    }

    private String normalize(String value, Set<String> allowed, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new InventoryRequestException(field + " không hợp lệ: " + value);
        }
        return normalized;
    }

    private ExpiryStatus parseExpiryStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ExpiryStatus.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InventoryRequestException("expiryStatus không hợp lệ: " + value);
        }
    }
}
