package vn.simtim.api.catalog.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductBarcodeRepository {
    List<ProductBarcode> findByProductId(UUID organizationId, UUID productId);
    Optional<ProductBarcode> findById(UUID organizationId, UUID id);
    boolean existsByBarcode(UUID organizationId, String barcode);
    boolean existsByBarcodeExcluding(UUID organizationId, String barcode, UUID excludeId);
    ProductBarcode save(ProductBarcode barcode);
    void deleteById(UUID organizationId, UUID id);
}
