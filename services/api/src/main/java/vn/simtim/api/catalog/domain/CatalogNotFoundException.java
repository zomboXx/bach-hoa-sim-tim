package vn.simtim.api.catalog.domain;

public class CatalogNotFoundException extends RuntimeException {
    public CatalogNotFoundException(String message) {
        super(message);
    }
}
