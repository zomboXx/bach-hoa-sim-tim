package vn.simtim.api.inventory.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsReceiptLineJpaRepository extends JpaRepository<GoodsReceiptLineJpa, UUID> {
    List<GoodsReceiptLineJpa> findByReceiptId(UUID receiptId);
}
