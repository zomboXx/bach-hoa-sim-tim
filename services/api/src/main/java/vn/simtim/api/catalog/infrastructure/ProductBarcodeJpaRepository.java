package vn.simtim.api.catalog.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProductBarcodeJpaRepository extends JpaRepository<ProductBarcodeJpa, UUID> {

    @Query("select b from ProductBarcodeJpa b " +
           "where b.organizationId = :orgId and b.barcode = :barcode")
    Optional<ProductBarcodeJpa> findByOrganizationIdAndBarcode(
            @Param("orgId") UUID orgId, @Param("barcode") String barcode);

    List<ProductBarcodeJpa> findByOrganizationIdAndProductId(UUID organizationId, UUID productId);
    boolean existsByOrganizationIdAndBarcode(UUID organizationId, String barcode);
    void deleteByOrganizationIdAndId(UUID organizationId, UUID id);
}
