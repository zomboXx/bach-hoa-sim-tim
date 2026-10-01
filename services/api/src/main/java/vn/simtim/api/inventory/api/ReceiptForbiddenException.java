package vn.simtim.api.inventory.api;

/** HTTP 403 khi thiếu quyền nhận hàng — bắt bởi InventoryExceptionHandler. */
class ReceiptForbiddenException extends RuntimeException {
    ReceiptForbiddenException(String message) {
        super(message);
    }
}
