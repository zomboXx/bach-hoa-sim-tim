package vn.simtim.api.inventory.api;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import vn.simtim.api.inventory.application.InventoryRequestException;
import vn.simtim.api.inventory.domain.InventoryReceiptException;

/**
 * Exception handler cho inventory module.
 * Trả về {code, message} theo contract SPRINT_2_BOUNDARY_DRAFT.md.
 */
@RestControllerAdvice(assignableTypes = {ReceiptController.class, InventoryController.class})
public class InventoryExceptionHandler {

    @ExceptionHandler(InventoryRequestException.class)
    public ResponseEntity<Map<String, Object>> invalidInventoryQuery(InventoryRequestException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("code", "INVALID_REQUEST", "message", ex.getMessage()));
    }

    @ExceptionHandler(ReceiptForbiddenException.class)
    public ResponseEntity<Map<String, Object>> forbidden(ReceiptForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("code", "FORBIDDEN", "message", ex.getMessage()));
    }

    @ExceptionHandler(InventoryReceiptException.class)
    public ResponseEntity<Map<String, Object>> domainError(InventoryReceiptException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "IDEMPOTENCY_KEY_REUSED" -> HttpStatus.CONFLICT;
            case "FORBIDDEN" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(status)
                .body(Map.of("code", ex.getCode(), "message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message",
                        fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid"))
                .toList();
        return ResponseEntity.badRequest()
                .body(Map.of("code", "INVALID_REQUEST",
                             "message", "Dữ liệu không hợp lệ",
                             "fieldErrors", fieldErrors));
    }
}
