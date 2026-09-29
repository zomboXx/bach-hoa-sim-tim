package vn.simtim.api.catalog.infrastructure;

import jakarta.persistence.*;
import java.util.UUID;
import vn.simtim.api.catalog.domain.Product;

@Entity
@Table(schema = "catalog", name = "products")
class ProductJpa {

    @Id
    UUID id;

    @Column(name = "organization_id", nullable = false)
    UUID organizationId;

    @Column(name = "category_id", nullable = false)
    UUID categoryId;

    @Column(name = "base_unit_id", nullable = false)
    UUID baseUnitId;

    @Column(nullable = false, length = 80)
    String sku;

    @Column(nullable = false, length = 200)
    String name;

    @Column(name = "tracks_expiry", nullable = false)
    boolean tracksExpiry;

    @Column(nullable = false, length = 16)
    String status;

    @Version
    @Column(nullable = false)
    long version;

    ProductJpa() {}

    ProductJpa(Product p) {
        this.id = p.id();
        this.organizationId = p.organizationId();
        this.categoryId = p.categoryId();
        this.baseUnitId = p.baseUnitId();
        this.sku = p.sku();
        this.name = p.name();
        this.tracksExpiry = p.tracksExpiry();
        this.status = p.status();
        this.version = p.version();
    }

    Product toDomain() {
        return new Product(id, organizationId, categoryId, baseUnitId, sku, name, tracksExpiry, status, version);
    }
}
