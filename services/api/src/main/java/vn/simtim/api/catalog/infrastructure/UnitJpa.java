package vn.simtim.api.catalog.infrastructure;

import jakarta.persistence.*;
import java.util.UUID;
import vn.simtim.api.catalog.domain.Unit;

@Entity
@Table(schema = "catalog", name = "units")
class UnitJpa {

    @Id
    UUID id;

    @Column(name = "organization_id", nullable = false)
    UUID organizationId;

    @Column(nullable = false, length = 40)
    String code;

    @Column(nullable = false, length = 100)
    String name;

    @Column(name = "precision_scale", nullable = false)
    short precisionScale;

    UnitJpa() {}

    UnitJpa(Unit u) {
        this.id = u.id();
        this.organizationId = u.organizationId();
        this.code = u.code();
        this.name = u.name();
        this.precisionScale = u.precisionScale();
    }

    Unit toDomain() {
        return new Unit(id, organizationId, code, name, precisionScale);
    }
}
