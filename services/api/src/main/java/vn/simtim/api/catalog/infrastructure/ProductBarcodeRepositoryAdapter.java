package vn.simtim.api.catalog.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import vn.simtim.api.catalog.domain.ProductBarcode;
import vn.simtim.api.catalog.domain.ProductBarcodeRepository;

@Repository
class ProductBarcodeRepositoryAdapter implements ProductBarcodeRepository {

    private final ProductBarcodeJpaRepository jpa;

    ProductBarcodeRepositoryAdapter(ProductBarcodeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<ProductBarcode> findByProductId(UUID organizationId, UUID productId) {
        return jpa.findByOrganizationIdAndProductId(organizationId, productId).stream()
                .map(ProductBarcodeJpa::toDomain).toList();
    }

    @Override
    public Optional<ProductBarcode> findById(UUID organizationId, UUID id) {
        return jpa.findById(id).filter(b -> organizationId.equals(b.organizationId)).map(ProductBarcodeJpa::toDomain);
    }

    @Override
    public boolean existsByBarcode(UUID organizationId, String barcode) {
        return jpa.existsByOrganizationIdAndBarcode(organizationId, barcode);
    }

    @Override
    public ProductBarcode save(ProductBarcode barcode) {
        return jpa.save(new ProductBarcodeJpa(barcode)).toDomain();
    }

    @Override
    public void deleteById(UUID organizationId, UUID id) {
        jpa.deleteByOrganizationIdAndId(organizationId, id);
    }
}
