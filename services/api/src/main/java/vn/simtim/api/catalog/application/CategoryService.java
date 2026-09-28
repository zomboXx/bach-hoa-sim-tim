package vn.simtim.api.catalog.application;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.catalog.domain.*;

/**
 * Use-case service cho danh mục sản phẩm.
 * Mọi rule nghiệp vụ (trùng mã, tồn tại cha) được kiểm tra trước khi ghi.
 */
@Service
@Transactional
public class CategoryService {

    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Category> listByOrg(UUID organizationId) {
        return repo.findByOrganization(organizationId);
    }

    @Transactional(readOnly = true)
    public Category getById(UUID organizationId, UUID id) {
        return repo.findById(organizationId, id)
                .orElseThrow(() -> new CatalogNotFoundException("Danh mục không tồn tại: " + id));
    }

    public Category create(UUID organizationId, UUID parentId, String code, String name, String status) {
        if (repo.existsByCode(organizationId, code)) {
            throw new CatalogConflictException("Mã danh mục đã tồn tại: " + code);
        }
        // Kiểm tra cha hợp lệ nếu có
        if (parentId != null) {
            repo.findById(organizationId, parentId)
                    .orElseThrow(() -> new CatalogNotFoundException("Danh mục cha không tồn tại: " + parentId));
        }
        var category = new Category(UUID.randomUUID(), organizationId, parentId, code, name, status);
        return repo.save(category);
    }

    public Category update(UUID organizationId, UUID id, UUID parentId, String code, String name, String status) {
        repo.findById(organizationId, id)
                .orElseThrow(() -> new CatalogNotFoundException("Danh mục không tồn tại: " + id));
        if (repo.existsByCodeExcluding(organizationId, code, id)) {
            throw new CatalogConflictException("Mã danh mục đã tồn tại: " + code);
        }
        if (parentId != null) {
            if (parentId.equals(id)) {
                throw new CatalogConflictException("Danh mục không thể là cha của chính nó");
            }
            repo.findById(organizationId, parentId)
                    .orElseThrow(() -> new CatalogNotFoundException("Danh mục cha không tồn tại: " + parentId));
        }
        var updated = new Category(id, organizationId, parentId, code, name, status);
        return repo.save(updated);
    }

    public void delete(UUID organizationId, UUID id) {
        repo.findById(organizationId, id)
                .orElseThrow(() -> new CatalogNotFoundException("Danh mục không tồn tại: " + id));
        repo.deleteById(organizationId, id);
    }
}
