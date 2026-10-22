package vn.simtim.api.catalog.infrastructure;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Entity riêng chỉ dùng cho việc lưu trữ và đọc dữ liệu ảnh BLOB (image_data).
 * Tách biệt khỏi ProductJpa để các truy vấn danh mục thông thường (list, search, get)
 * không phải tải hàng MB dữ liệu ảnh nhị phân vào bộ nhớ heap.
 */
@Entity
@Table(schema = "catalog", name = "products")
class ProductImageJpa {

    @Id
    UUID id;

    @Column(name = "organization_id", nullable = false)
    UUID organizationId;

    @Column(name = "image_url")
    String imageUrl;

    @Column(name = "image_data")
    byte[] imageData;

    @Column(name = "image_content_type", length = 100)
    String imageContentType;

    ProductImageJpa() {}

    ProductImageJpa(UUID id, UUID organizationId, byte[] imageData, String imageContentType, String imageUrl) {
        this.id = id;
        this.organizationId = organizationId;
        this.imageData = imageData;
        this.imageContentType = imageContentType;
        this.imageUrl = imageUrl;
    }
}
