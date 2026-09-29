package vn.simtim.api.catalog.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    List<Product> findByOrganization(UUID organizationId);
    Optional<Product> findById(UUID organizationId, UUID id);
    Optional<Product> findBySku(UUID organizationId, String sku);
    Optional<Product> findByBarcode(UUID organizationId, String barcode);
    List<Product> searchByName(UUID organizationId, String keyword);
    boolean existsBySku(UUID organizationId, String sku);
    boolean existsBySkuExcluding(UUID organizationId, String sku, UUID excludeId);
    Product save(Product product);
    void deleteById(UUID organizationId, UUID id);
}
