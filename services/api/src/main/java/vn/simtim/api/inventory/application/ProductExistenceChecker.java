package vn.simtim.api.inventory.application;

import java.util.UUID;

/** Port để kiểm tra sản phẩm tồn tại và ACTIVE trong tổ chức, và độ chính xác đơn vị tính. */
public interface ProductExistenceChecker {
    boolean isActive(UUID organizationId, UUID productId);

    /** Lấy precision_scale của đơn vị cơ sở sản phẩm (EA = 0, KG = 3). */
    int getPrecisionScale(UUID organizationId, UUID productId);
}
