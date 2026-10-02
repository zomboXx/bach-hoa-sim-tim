package vn.simtim.api.inventory.application;

import java.util.UUID;

/** Port để kiểm tra sản phẩm tồn tại và ACTIVE trong tổ chức. */
public interface ProductExistenceChecker {
    boolean isActive(UUID organizationId, UUID productId);
}
