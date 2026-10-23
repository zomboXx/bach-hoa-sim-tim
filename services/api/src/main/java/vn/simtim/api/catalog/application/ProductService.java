package vn.simtim.api.catalog.application;

import java.time.Clock;
import java.util.OptionalLong;
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
    private final ProductBarcodeRepository barcodeRepo;
    private final ProductPriceRepository priceRepo;
    private final Clock clock;

    public ProductService(ProductRepository repo,
                          CategoryRepository categoryRepo,
                          UnitRepository unitRepo,
                          ProductBarcodeRepository barcodeRepo,
                          ProductPriceRepository priceRepo,
                          Clock clock) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
        this.unitRepo = unitRepo;
        this.barcodeRepo = barcodeRepo;
        this.priceRepo = priceRepo;
        this.clock = clock;
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
                          UUID storeId, String sku, String name, long salePrice,
                          boolean tracksExpiry, String status) {
        if (repo.existsBySku(organizationId, sku)) {
            throw new CatalogConflictException("SKU đã tồn tại: " + sku);
        }
        categoryRepo.findById(organizationId, categoryId)
                .orElseThrow(() -> new CatalogNotFoundException("Danh mục không tồn tại: " + categoryId));
        unitRepo.findById(organizationId, baseUnitId)
                .orElseThrow(() -> new CatalogNotFoundException("Đơn vị không tồn tại: " + baseUnitId));
        var product = new Product(UUID.randomUUID(), organizationId, categoryId, baseUnitId,
                sku, name, tracksExpiry, status, 0L);
        Product saved = repo.save(product);
        priceRepo.setCurrent(organizationId, storeId, saved.id(), salePrice, clock.instant());
        return saved;
    }

    public Product update(UUID organizationId, UUID id, UUID categoryId, UUID baseUnitId,
                          UUID storeId, String sku, String name, long salePrice,
                          boolean tracksExpiry, String status) {
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
        Product saved = repo.save(updated);
        priceRepo.setCurrent(organizationId, storeId, saved.id(), salePrice, clock.instant());
        return saved;
    }

    @Transactional(readOnly = true)
    public OptionalLong currentSalePrice(UUID organizationId, UUID storeId, UUID productId) {
        return priceRepo.findCurrent(organizationId, storeId, productId, clock.instant());
    }

    public void delete(UUID organizationId, UUID id) {
        repo.findById(organizationId, id)
                .orElseThrow(() -> new CatalogNotFoundException("Sản phẩm không tồn tại: " + id));
        priceRepo.deleteByProduct(organizationId, id);
        repo.deleteById(organizationId, id);
    }

    public ProductBarcode addBarcode(UUID organizationId, UUID productId, String barcodeStr, boolean isPrimary) {
        repo.findById(organizationId, productId)
                .orElseThrow(() -> new CatalogNotFoundException("Sản phẩm không tồn tại: " + productId));
        if (barcodeRepo.existsByBarcode(organizationId, barcodeStr)) {
            throw new CatalogConflictException("Mã vạch đã tồn tại: " + barcodeStr);
        }
        var barcode = new ProductBarcode(UUID.randomUUID(), organizationId, productId, barcodeStr, isPrimary);
        return barcodeRepo.save(barcode);
    }

    @Transactional(readOnly = true)
    public List<ProductBarcode> listBarcodes(UUID organizationId, UUID productId) {
        repo.findById(organizationId, productId)
                .orElseThrow(() -> new CatalogNotFoundException("Sản phẩm không tồn tại: " + productId));
        return barcodeRepo.findByProductId(organizationId, productId);
    }

    @Transactional(readOnly = true)
    public ProductBarcode getBarcode(UUID organizationId, UUID productId, UUID barcodeId) {
        getById(organizationId, productId);
        var barcode = barcodeRepo.findById(organizationId, barcodeId)
                .orElseThrow(() -> new CatalogNotFoundException("Mã vạch không tồn tại: " + barcodeId));
        if (!barcode.productId().equals(productId)) {
            throw new CatalogConflictException("Mã vạch không thuộc sản phẩm này");
        }
        return barcode;
    }

    public ProductBarcode updateBarcode(UUID organizationId, UUID productId, UUID barcodeId,
                                        String barcode, boolean isPrimary) {
        getBarcode(organizationId, productId, barcodeId);
        if (barcodeRepo.existsByBarcodeExcluding(organizationId, barcode, barcodeId)) {
            throw new CatalogConflictException("Mã vạch đã tồn tại: " + barcode);
        }
        return barcodeRepo.save(new ProductBarcode(barcodeId, organizationId, productId, barcode, isPrimary));
    }

    public void deleteBarcode(UUID organizationId, UUID productId, UUID barcodeId) {
        getBarcode(organizationId, productId, barcodeId);
        barcodeRepo.deleteById(organizationId, barcodeId);
    }

    public Product uploadImage(UUID organizationId, UUID productId, byte[] data, String contentType) {
        getById(organizationId, productId);
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Tệp hình ảnh không được để trống");
        }
        if (data.length > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Dung lượng hình ảnh không được vượt quá 5MB");
        }
        String imageUrl = "/api/v1/products/" + productId + "/image";
        return repo.saveImage(organizationId, productId, data, contentType, imageUrl);
    }

    @Transactional(readOnly = true)
    public ProductImage getImage(UUID productId) {
        return repo.findImageById(productId)
                .orElseThrow(() -> new CatalogNotFoundException("Hình ảnh không tồn tại cho sản phẩm: " + productId));
    }

    public Product deleteImage(UUID organizationId, UUID productId) {
        getById(organizationId, productId);
        return repo.removeImage(organizationId, productId);
    }
}
