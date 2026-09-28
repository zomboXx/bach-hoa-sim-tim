package vn.simtim.api.auth.api;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.simtim.api.auth.domain.AuthFailure;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthErrors {
    public record Error(String code, String message) {}
    @ExceptionHandler(AuthFailure.class)
    ResponseEntity<Error> failure(AuthFailure error) {
        int status = error.code().equals("INVALID_REQUEST") ? 400 : 401;
        var response = ResponseEntity.status(status).header("Cache-Control", "no-store");
        if (status == 401) response.header("WWW-Authenticate", "Bearer");
        return response.body(new Error(error.code(), error.getMessage()));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Error> unreadable() {
        return ResponseEntity.badRequest().header("Cache-Control", "no-store")
                .body(new Error("INVALID_REQUEST", "Invalid login request"));
    }
}
