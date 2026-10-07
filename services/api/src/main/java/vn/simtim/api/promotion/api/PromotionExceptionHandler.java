package vn.simtim.api.promotion.api;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.simtim.api.promotion.domain.PromotionConflictException;
import vn.simtim.api.promotion.domain.PromotionNotFoundException;
import vn.simtim.api.promotion.domain.PromotionValidationException;

/**
 * Chuyển domain exceptions của promotion thành RFC 7807 ProblemDetail.
 * MethodArgumentNotValidException (Bean Validation) được xử lý bởi CatalogExceptionHandler toàn cục;
 * handler này chỉ bổ sung các exception riêng của module promotion.
 */
@RestControllerAdvice(basePackages = "vn.simtim.api.promotion")
public class PromotionExceptionHandler {

    @ExceptionHandler(PromotionNotFoundException.class)
    public ProblemDetail handleNotFound(PromotionNotFoundException ex) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(PromotionConflictException.class)
    public ProblemDetail handleConflict(PromotionConflictException ex) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(PromotionValidationException.class)
    public ProblemDetail handleValidation(PromotionValidationException ex) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }
}

