package vn.simtim.api.sale.api;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.simtim.api.sale.domain.SaleConflictException;
import vn.simtim.api.sale.domain.SaleNotFoundException;
import vn.simtim.api.sale.domain.SaleValidationException;

/** Dịch domain exceptions sang HTTP status codes cho module sale. */
@RestControllerAdvice(basePackages = "vn.simtim.api.sale")
public class SaleExceptionHandler {

    @ExceptionHandler(SaleNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(SaleNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of("code", "NOT_FOUND", "message", ex.getMessage()));
    }

    @ExceptionHandler(SaleConflictException.class)
    public ResponseEntity<Map<String, String>> conflict(SaleConflictException ex) {
        return ResponseEntity.status(409).body(Map.of("code", "CONFLICT", "message", ex.getMessage()));
    }

    @ExceptionHandler(SaleValidationException.class)
    public ResponseEntity<Map<String, String>> validation(SaleValidationException ex) {
        return ResponseEntity.status(422).body(Map.of("code", "VALIDATION_ERROR", "message", ex.getMessage()));
    }
}
