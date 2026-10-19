package vn.simtim.api.sale.domain;

/** Dữ liệu đầu vào không hợp lệ (đơn hàng rỗng, số tiền âm, …). → 422 */
public class SaleValidationException extends RuntimeException {
    private final String code;

    public SaleValidationException(String message) {
        super(message);
        this.code = "VALIDATION_ERROR";
    }

    public SaleValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
