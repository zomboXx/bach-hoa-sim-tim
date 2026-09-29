package vn.simtim.api.catalog.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import vn.simtim.api.catalog.domain.Category;
import vn.simtim.api.catalog.domain.CategoryRepository;

@Repository
class CategoryRepositoryAdapter implements CategoryRepository {

    private final CategoryJpaRepository jpa;

    CategoryRepositoryAdapter(CategoryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Category> findByOrganization(UUID organizationId) {
        return jpa.findByOrganizationId(organizationId).stream()
                .map(CategoryJpa::toDomain).toList();
    }

    @Override
    public Optional<Category> findById(UUID organizationId, UUID id) {
        return jpa.findByOrganizationIdAndId(organizationId, id).map(CategoryJpa::toDomain);
    }

    @Override
    public boolean existsByCode(UUID organizationId, String code) {
        return jpa.existsByCodeInsensitive(organizationId, code);
    }

    @Override
    public boolean existsByCodeExcluding(UUID organizationId, String code, UUID excludeId) {
        return jpa.existsByCodeInsensitiveExcluding(organizationId, code, excludeId);
    }

    @Override
    public Category save(Category category) {
        return jpa.save(new CategoryJpa(category)).toDomain();
    }

    @Override
    public void deleteById(UUID organizationId, UUID id) {
        jpa.findByOrganizationIdAndId(organizationId, id)
                .ifPresent(e -> jpa.deleteById(e.id));
    }
}
