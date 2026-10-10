package vn.simtim.api.inventory.domain;

/** Exception gốc cho nghiệp vụ kiểm kê (SYN-02). */
public class StocktakeException extends RuntimeException {

    private final String code;

    public StocktakeException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}
