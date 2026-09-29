package vn.simtim.api.catalog.infrastructure;

import jakarta.persistence.*;
import java.util.UUID;
import vn.simtim.api.catalog.domain.Category;

@Entity
@Table(schema = "catalog", name = "categories")
class CategoryJpa {

    @Id
    UUID id;

    @Column(name = "organization_id", nullable = false)
    UUID organizationId;

    @Column(name = "parent_id")
    UUID parentId;

    @Column(nullable = false, length = 40)
    String code;

    @Column(nullable = false, length = 200)
    String name;

    @Column(nullable = false, length = 16)
    String status;

    CategoryJpa() {}

    CategoryJpa(Category c) {
        this.id = c.id();
        this.organizationId = c.organizationId();
        this.parentId = c.parentId();
        this.code = c.code();
        this.name = c.name();
        this.status = c.status();
    }

    Category toDomain() {
        return new Category(id, organizationId, parentId, code, name, status);
    }
}
