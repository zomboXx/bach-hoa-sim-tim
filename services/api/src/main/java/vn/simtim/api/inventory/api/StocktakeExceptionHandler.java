package vn.simtim.api.inventory.api;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import vn.simtim.api.inventory.application.StocktakeConflictException;
import vn.simtim.api.inventory.domain.StocktakeException;

/**
 * Exception handler cho StocktakeController (SYN-02).
 * Trả body {code, message} cùng cấu trúc với InventoryExceptionHandler.
 */
@RestControllerAdvice(assignableTypes = StocktakeController.class)
public class StocktakeExceptionHandler {

    /** 409 khi phát hiện xung đột version; kèm theo thông tin dòng đã lưu. */
    @ExceptionHandler(StocktakeConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(StocktakeConflictException ex) {
        var line = ex.getSavedLine();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "code",            "CONFLICT",
                        "message",         ex.getMessage(),
                        "clientOperationId", line.clientOperationId().toString(),
                        "batchId",         line.batchId().toString(),
                        "baseVersion",     line.baseVersion(),
                        "currentStatus",   line.status()
                ));
    }

    /** Các lỗi domain khác (NOT_FOUND, SESSION_CLOSED, SCOPE_MISMATCH, IDEMPOTENCY_KEY_REUSED). */
    @ExceptionHandler(StocktakeException.class)
    public ResponseEntity<Map<String, Object>> domain(StocktakeException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case "NOT_FOUND"                                          -> HttpStatus.NOT_FOUND;
            case "IDEMPOTENCY_KEY_REUSED", "ALREADY_APPROVED",
                 "BALANCE_VERSION_MISMATCH", "UNRESOLVED_CONFLICT"    -> HttpStatus.CONFLICT;
            case "SCOPE_MISMATCH"                                     -> HttpStatus.FORBIDDEN;
            case "SESSION_CLOSED", "SESSION_CANCELLED",
                 "EMPTY_STOCKTAKE", "NEGATIVE_BALANCE"                -> HttpStatus.UNPROCESSABLE_ENTITY;
            default                                                   -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(status)
                .body(Map.of("code", ex.getCode(), "message", ex.getMessage()));
    }

    @ExceptionHandler(StocktakeForbiddenException.class)
    public ResponseEntity<Map<String, Object>> forbidden(StocktakeForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("code", "FORBIDDEN", "message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(),
                        "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid"))
                .toList();
        return ResponseEntity.badRequest()
                .body(Map.of("code", "INVALID_REQUEST",
                             "message", "Dữ liệu không hợp lệ",
                             "fieldErrors", fieldErrors));
    }
}
