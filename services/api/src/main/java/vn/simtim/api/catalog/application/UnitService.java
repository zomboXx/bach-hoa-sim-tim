package vn.simtim.api.catalog.application;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.catalog.domain.*;

@Service
@Transactional
public class UnitService {

    private final UnitRepository repo;

    public UnitService(UnitRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Unit> listByOrg(UUID organizationId) {
        return repo.findByOrganization(organizationId);
    }

    @Transactional(readOnly = true)
    public Unit getById(UUID organizationId, UUID id) {
        return repo.findById(organizationId, id)
                .orElseThrow(() -> new CatalogNotFoundException("Đơn vị tính không tồn tại: " + id));
    }

    public Unit create(UUID organizationId, String code, String name, short precisionScale) {
        if (repo.existsByCode(organizationId, code)) {
            throw new CatalogConflictException("Mã đơn vị tính đã tồn tại: " + code);
        }
        var unit = new Unit(UUID.randomUUID(), organizationId, code, name, precisionScale);
        return repo.save(unit);
    }

    public Unit update(UUID organizationId, UUID id, String code, String name, short precisionScale) {
        repo.findById(organizationId, id)
                .orElseThrow(() -> new CatalogNotFoundException("Đơn vị tính không tồn tại: " + id));
        if (repo.existsByCodeExcluding(organizationId, code, id)) {
            throw new CatalogConflictException("Mã đơn vị tính đã tồn tại: " + code);
        }
        var updated = new Unit(id, organizationId, code, name, precisionScale);
        return repo.save(updated);
    }

    public void delete(UUID organizationId, UUID id) {
        repo.findById(organizationId, id)
                .orElseThrow(() -> new CatalogNotFoundException("Đơn vị tính không tồn tại: " + id));
        repo.deleteById(organizationId, id);
    }
}
