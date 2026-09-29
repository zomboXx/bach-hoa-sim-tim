package vn.simtim.api.catalog.domain;

public class CatalogConflictException extends RuntimeException {
    public CatalogConflictException(String message) {
        super(message);
    }
}
