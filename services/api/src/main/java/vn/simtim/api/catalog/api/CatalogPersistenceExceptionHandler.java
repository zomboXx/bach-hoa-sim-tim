package vn.simtim.api.catalog.api;

import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Keep database constraint failures inside the catalog API contract without exposing SQL. */
@RestControllerAdvice(assignableTypes = {
        ProductController.class, CategoryController.class, UnitController.class, SupplierController.class
})
public class CatalogPersistenceExceptionHandler {
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Không thể lưu hoặc xóa: dữ liệu đang được tham chiếu, bị trùng hoặc vi phạm ràng buộc danh mục.");
        detail.setProperty("code", "CATALOG_CONSTRAINT_CONFLICT");
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrentUpdate(OptimisticLockingFailureException ex) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Dữ liệu đã thay đổi. Hãy tải lại trước khi cập nhật.");
        detail.setProperty("code", "CATALOG_CONCURRENT_UPDATE");
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }
}
