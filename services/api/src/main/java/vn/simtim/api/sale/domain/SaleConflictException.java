package vn.simtim.api.sale.domain;

/** Xung đột dữ liệu (tồn không đủ, số hóa đơn trùng, …). → 409 */
public class SaleConflictException extends RuntimeException {

    private final String code;

    /** Xung đột không có mã cụ thể – handler sẽ dùng "CONFLICT". */
    public SaleConflictException(String message) {
        super(message);
        this.code = null;
    }

    /** Xung đột có mã lỗi riêng (ví dụ: "PRICE_CHANGED"). */
    public SaleConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}
