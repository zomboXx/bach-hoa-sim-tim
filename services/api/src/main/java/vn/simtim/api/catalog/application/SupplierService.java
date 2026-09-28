package vn.simtim.api.catalog.application;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.catalog.domain.*;

/** Use-case service cho nhà cung cấp. */
@Service
@Transactional
public class SupplierService {

    private final SupplierRepository repo;

    public SupplierService(SupplierRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Supplier> listByOrg(UUID organizationId) {
        return repo.findByOrganization(organizationId);
    }

    @Transactional(readOnly = true)
    public Supplier getById(UUID organizationId, UUID id) {
        return repo.findById(organizationId, id)
                .orElseThrow(() -> new CatalogNotFoundException("Nhà cung cấp không tồn tại: " + id));
    }

    public Supplier create(UUID organizationId, String code, String name, String phone,
                           String email, String status) {
        if (repo.existsByCode(organizationId, code)) {
            throw new CatalogConflictException("Mã nhà cung cấp đã tồn tại: " + code);
        }
        var supplier = new Supplier(UUID.randomUUID(), organizationId, code, name, phone, email, status);
        return repo.save(supplier);
    }

    public Supplier update(UUID organizationId, UUID id, String code, String name, String phone,
                           String email, String status) {
        repo.findById(organizationId, id)
                .orElseThrow(() -> new CatalogNotFoundException("Nhà cung cấp không tồn tại: " + id));
        if (repo.existsByCodeExcluding(organizationId, code, id)) {
            throw new CatalogConflictException("Mã nhà cung cấp đã tồn tại: " + code);
        }
        var updated = new Supplier(id, organizationId, code, name, phone, email, status);
        return repo.save(updated);
    }

    public void delete(UUID organizationId, UUID id) {
        repo.findById(organizationId, id)
                .orElseThrow(() -> new CatalogNotFoundException("Nhà cung cấp không tồn tại: " + id));
        repo.deleteById(organizationId, id);
    }
}
