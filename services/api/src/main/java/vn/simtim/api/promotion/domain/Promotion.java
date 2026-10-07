package vn.simtim.api.promotion.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Khuyến mãi cơ bản: có thời gian hiệu lực, phạm vi cửa hàng và loại giảm giá.
 * storeId null → áp dụng toàn bộ cửa hàng trong tổ chức.
 */
public record Promotion(
        UUID id,
        UUID organizationId,
        UUID storeId,
        String code,
        String name,
        String discountType,
        BigDecimal discountValue,
        Instant startsAt,
        Instant endsAt,
        String status) {

    public boolean isActive(Instant at) {
        return "ACTIVE".equals(status) && !at.isBefore(startsAt) && at.isBefore(endsAt);
    }

    public boolean appliesToStore(UUID targetStoreId) {
        return storeId == null || storeId.equals(targetStoreId);
    }
}
