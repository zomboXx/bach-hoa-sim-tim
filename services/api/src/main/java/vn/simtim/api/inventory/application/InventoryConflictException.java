package vn.simtim.api.inventory.application;

/** Exception khi có xung đột tồn kho trong module inventory. */
public class InventoryConflictException extends RuntimeException {
    public InventoryConflictException(String message) {
        super(message);
    }
}
