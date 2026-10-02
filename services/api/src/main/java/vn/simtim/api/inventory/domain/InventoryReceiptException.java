package vn.simtim.api.inventory.domain;

/** Lỗi nghiệp vụ khi tạo phiếu nhận hàng không hợp lệ. */
public class InventoryReceiptException extends RuntimeException {
    private final String code;

    public InventoryReceiptException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
