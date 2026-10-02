package vn.simtim.api.sale.domain;

/** Dữ liệu đầu vào không hợp lệ (đơn hàng rỗng, số tiền âm, …). → 422 */
public class SaleValidationException extends RuntimeException {
    public SaleValidationException(String message) { super(message); }
}
