package vn.simtim.api.catalog.application;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.catalog.domain.*;

/** Use-case service cho sản phẩm: CRUD và tìm kiếm theo SKU/barcode/tên. */
@Service
@Transactional
public class ProductService {

    private final ProductRepository repo;
    private final CategoryRepository categoryRepo;
    private final UnitRepository unitRepo;

    public ProductService(ProductRepository repo,
                          CategoryRepository categoryRepo,
                          UnitRepository unitRepo) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
        this.unitRepo = unitRepo;
    }

    @Transactional(readOnly = true)
    public List<Product> listByOrg(UUID organizationId) {
        return repo.findByOrganization(organizationId);
    }

    @Transactional(readOnly = true)
    public Product getById(UUID organizationId, UUID id) {
        return repo.findById(organizationId, id)
                .orElseThrow(() -> new CatalogNotFoundException("Sản phẩm không tồn tại: " + id));
    }

    /** Tìm sản phẩm theo SKU, mã vạch hoặc tên (ít nhất một tham số). */
    @Transactional(readOnly = true)
    public List<Product> search(UUID organizationId, String sku, String barcode, String name) {
        if (sku != null && !sku.isBlank()) {
            return repo.findBySku(organizationId, sku).map(List::of).orElse(List.of());
        }
        if (barcode != null && !barcode.isBlank()) {
            return repo.findByBarcode(organizationId, barcode).map(List::of).orElse(List.of());
        }
        if (name != null && !name.isBlank()) {
            return repo.searchByName(organizationId, name);
        }
        return repo.findByOrganization(organizationId);
    }

    public Product create(UUID organizationId, UUID categoryId, UUID baseUnitId,
                          String sku, String name, boolean tracksExpiry, String status) {
        if (repo.existsBySku(organizationId, sku)) {
            throw new CatalogConflictException("SKU đã tồn tại: " + sku);
        }
        categoryRepo.findById(organizationId, categoryId)
                .orElseThrow(() -> new CatalogNotFoundException("Danh mục không tồn tại: " + categoryId));
        unitRepo.findById(organizationId, baseUnitId)
                .orElseThrow(() -> new CatalogNotFoundException("Đơn vị không tồn tại: " + baseUnitId));
        var product = new Product(UUID.randomUUID(), organizationId, categoryId, baseUnitId,
                sku, name, tracksExpiry, status, 0L);
        return repo.save(product);
    }

    public Product update(UUID organizationId, UUID id, UUID categoryId, UUID baseUnitId,
                          String sku, String name, boolean tracksExpiry, String status) {
        var existing = repo.findById(organizationId, id)
                .orElseThrow(() -> new CatalogNotFoundException("Sản phẩm không tồn tại: " + id));
        if (repo.existsBySkuExcluding(organizationId, sku, id)) {
            throw new CatalogConflictException("SKU đã tồn tại: " + sku);
        }
        categoryRepo.findById(organizationId, categoryId)
                .orElseThrow(() -> new CatalogNotFoundException("Danh mục không tồn tại: " + categoryId));
        unitRepo.findById(organizationId, baseUnitId)
                .orElseThrow(() -> new CatalogNotFoundException("Đơn vị không tồn tại: " + baseUnitId));
        var updated = new Product(id, organizationId, categoryId, baseUnitId,
                sku, name, tracksExpiry, status, existing.version());
        return repo.save(updated);
    }

    public void delete(UUID organizationId, UUID id) {
        repo.findById(organizationId, id)
                .orElseThrow(() -> new CatalogNotFoundException("Sản phẩm không tồn tại: " + id));
        repo.deleteById(organizationId, id);
    }
}
