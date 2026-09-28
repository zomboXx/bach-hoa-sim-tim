package vn.simtim.api.catalog.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port cho persistence của danh mục sản phẩm. */
public interface CategoryRepository {
    List<Category> findByOrganization(UUID organizationId);
    Optional<Category> findById(UUID organizationId, UUID id);
    /** Trả về true nếu đã tồn tại mã khác biệt chữ hoa/thường trong cùng tổ chức. */
    boolean existsByCode(UUID organizationId, String code);
    boolean existsByCodeExcluding(UUID organizationId, String code, UUID excludeId);
    Category save(Category category);
    void deleteById(UUID organizationId, UUID id);
}
