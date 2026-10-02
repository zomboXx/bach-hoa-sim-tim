package vn.simtim.api.inventory.application;

/** Lỗi filter/phân trang của read API. */
public class InventoryRequestException extends RuntimeException {
    public InventoryRequestException(String message) {
        super(message);
    }
}
