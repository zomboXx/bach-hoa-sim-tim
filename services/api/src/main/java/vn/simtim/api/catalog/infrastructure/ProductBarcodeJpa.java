package vn.simtim.api.catalog.infrastructure;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(schema = "catalog", name = "product_barcodes")
class ProductBarcodeJpa {

    @Id
    UUID id;

    @Column(name = "organization_id", nullable = false)
    UUID organizationId;

    @Column(name = "product_id", nullable = false)
    UUID productId;

    @Column(nullable = false, length = 100)
    String barcode;

    @Column(name = "is_primary", nullable = false)
    boolean isPrimary;

    ProductBarcodeJpa() {}
}
