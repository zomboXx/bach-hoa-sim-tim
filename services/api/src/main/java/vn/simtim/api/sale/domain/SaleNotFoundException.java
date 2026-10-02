package vn.simtim.api.sale.domain;

/** Không tìm thấy hóa đơn, lô hoặc sản phẩm liên quan. → 404 */
public class SaleNotFoundException extends RuntimeException {
    public SaleNotFoundException(String message) { super(message); }
}
