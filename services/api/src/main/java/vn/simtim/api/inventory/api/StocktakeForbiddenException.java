package vn.simtim.api.inventory.api;

/** Ném khi actor thiếu quyền stocktakes.read hoặc stocktakes.write. */
public class StocktakeForbiddenException extends RuntimeException {
    public StocktakeForbiddenException(String message) { super(message); }
}
