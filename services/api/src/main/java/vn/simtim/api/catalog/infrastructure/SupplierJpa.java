package vn.simtim.api.catalog.infrastructure;

import jakarta.persistence.*;
import java.util.UUID;
import vn.simtim.api.catalog.domain.Supplier;

@Entity
@Table(schema = "catalog", name = "suppliers")
class SupplierJpa {

    @Id
    UUID id;

    @Column(name = "organization_id", nullable = false)
    UUID organizationId;

    @Column(nullable = false, length = 40)
    String code;

    @Column(nullable = false, length = 200)
    String name;

    @Column(length = 40)
    String phone;

    @Column(length = 255)
    String email;

    @Column(nullable = false, length = 16)
    String status;

    SupplierJpa() {}

    SupplierJpa(Supplier s) {
        this.id = s.id();
        this.organizationId = s.organizationId();
        this.code = s.code();
        this.name = s.name();
        this.phone = s.phone();
        this.email = s.email();
        this.status = s.status();
    }

    Supplier toDomain() {
        return new Supplier(id, organizationId, code, name, phone, email, status);
    }
}
