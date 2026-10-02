package vn.simtim.api.inventory.application;

public class InventorySaleException extends RuntimeException {
    private final String code;

    public InventorySaleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
