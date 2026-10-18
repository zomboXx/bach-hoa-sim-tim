package vn.simtim.api.training.api;

import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import vn.simtim.api.training.domain.TrainingFailure;

@RestControllerAdvice(assignableTypes = TrainingController.class)
public class TrainingErrors {
    public record Error(String code, String message) {}

    @ExceptionHandler(TrainingFailure.class)
    ResponseEntity<Error> failure(TrainingFailure error) {
        return ResponseEntity.status(error.status()).header("Cache-Control", "no-store")
                .body(new Error(error.code(), error.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<Error> invalidRequest(Exception error) {
        return ResponseEntity.badRequest().header("Cache-Control", "no-store")
                .body(new Error("INVALID_REQUEST", "Invalid training request"));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Error> dataFailure(DataAccessException error) {
        return ResponseEntity.internalServerError().header("Cache-Control", "no-store")
                .body(new Error("TRAINING_STORAGE_ERROR", "Training request could not be saved"));
    }
}
