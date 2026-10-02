package vn.simtim.api.sale.domain;

/** Xung đột dữ liệu (tồn không đủ, số hóa đơn trùng, …). → 409 */
public class SaleConflictException extends RuntimeException {
    public SaleConflictException(String message) { super(message); }
}
