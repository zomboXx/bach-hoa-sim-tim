package vn.simtim.api.catalog.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import vn.simtim.api.catalog.domain.Product;
import vn.simtim.api.catalog.domain.ProductRepository;

@Repository
class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpa;
    private final ProductBarcodeJpaRepository barcodeJpa;

    ProductRepositoryAdapter(ProductJpaRepository jpa, ProductBarcodeJpaRepository barcodeJpa) {
        this.jpa = jpa;
        this.barcodeJpa = barcodeJpa;
    }

    @Override
    public List<Product> findByOrganization(UUID organizationId) {
        return jpa.findByOrganizationId(organizationId).stream().map(ProductJpa::toDomain).toList();
    }

    @Override
    public Optional<Product> findById(UUID organizationId, UUID id) {
        return jpa.findByOrganizationIdAndId(organizationId, id).map(ProductJpa::toDomain);
    }

    @Override
    public Optional<Product> findBySku(UUID organizationId, String sku) {
        return jpa.findBySkuInsensitive(organizationId, sku).map(ProductJpa::toDomain);
    }

    @Override
    public Optional<Product> findByBarcode(UUID organizationId, String barcode) {
        return barcodeJpa.findByOrganizationIdAndBarcode(organizationId, barcode)
                .flatMap(b -> jpa.findByOrganizationIdAndId(organizationId, b.productId))
                .map(ProductJpa::toDomain);
    }

    @Override
    public List<Product> searchByName(UUID organizationId, String keyword) {
        return jpa.searchByName(organizationId, keyword).stream().map(ProductJpa::toDomain).toList();
    }

    @Override
    public boolean existsBySku(UUID organizationId, String sku) {
        return jpa.existsBySkuInsensitive(organizationId, sku);
    }

    @Override
    public boolean existsBySkuExcluding(UUID organizationId, String sku, UUID excludeId) {
        return jpa.existsBySkuInsensitiveExcluding(organizationId, sku, excludeId);
    }

    @Override
    public Product save(Product product) {
        return jpa.save(new ProductJpa(product)).toDomain();
    }

    @Override
    public void deleteById(UUID organizationId, UUID id) {
        jpa.findByOrganizationIdAndId(organizationId, id)
                .ifPresent(e -> jpa.deleteById(e.id));
    }
}
