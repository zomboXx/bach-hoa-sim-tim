package vn.simtim.api.catalog.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import vn.simtim.api.catalog.domain.CatalogNotFoundException;
import vn.simtim.api.catalog.domain.Product;
import vn.simtim.api.catalog.domain.ProductImage;
import vn.simtim.api.catalog.domain.ProductRepository;

@Repository
class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpa;
    private final ProductBarcodeJpaRepository barcodeJpa;
    private final ProductImageJpaRepository imageJpa;

    ProductRepositoryAdapter(ProductJpaRepository jpa,
                             ProductBarcodeJpaRepository barcodeJpa,
                             ProductImageJpaRepository imageJpa) {
        this.jpa = jpa;
        this.barcodeJpa = barcodeJpa;
        this.imageJpa = imageJpa;
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
        var existing = jpa.findById(product.id());
        ProductJpa jpaEntity = new ProductJpa(product);
        if (existing.isPresent()) {
            ProductJpa old = existing.get();
            if (product.imageUrl() == null) {
                jpaEntity.imageUrl = old.imageUrl;
            }
        }
        return jpa.save(jpaEntity).toDomain();
    }

    @Override
    public void deleteById(UUID organizationId, UUID id) {
        jpa.findByOrganizationIdAndId(organizationId, id)
                .ifPresent(e -> jpa.deleteById(e.id));
    }

    @Override
    public Optional<ProductImage> findImageById(UUID id) {
        return imageJpa.findById(id)
                .filter(p -> p.imageData != null && p.imageData.length > 0)
                .map(p -> new ProductImage(p.imageData, p.imageContentType != null ? p.imageContentType : "image/jpeg"));
    }

    @Override
    public Product saveImage(UUID organizationId, UUID productId, byte[] data, String contentType, String imageUrl) {
        var entity = jpa.findByOrganizationIdAndId(organizationId, productId)
                .orElseThrow(() -> new CatalogNotFoundException("Sản phẩm không tồn tại: " + productId));
        var imageEntity = imageJpa.findById(productId)
                .orElseGet(() -> new ProductImageJpa(productId, organizationId, null, null, null));
        imageEntity.imageData = data;
        imageEntity.imageContentType = contentType;
        imageEntity.imageUrl = imageUrl;
        imageJpa.save(imageEntity);

        entity.imageUrl = imageUrl;
        return jpa.save(entity).toDomain();
    }

    @Override
    public Product removeImage(UUID organizationId, UUID productId) {
        var entity = jpa.findByOrganizationIdAndId(organizationId, productId)
                .orElseThrow(() -> new CatalogNotFoundException("Sản phẩm không tồn tại: " + productId));
        imageJpa.findById(productId).ifPresent(img -> {
            img.imageData = null;
            img.imageContentType = null;
            img.imageUrl = null;
            imageJpa.save(img);
        });
        entity.imageUrl = null;
        return jpa.save(entity).toDomain();
    }
}
