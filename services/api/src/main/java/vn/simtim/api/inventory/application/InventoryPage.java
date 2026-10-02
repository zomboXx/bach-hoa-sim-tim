package vn.simtim.api.inventory.application;

import java.util.List;

/** Trang kết quả có thứ tự xác định. */
public record InventoryPage<T>(
        List<T> items,
        int page,
        int size,
        long totalElements) {

    public InventoryPage {
        items = List.copyOf(items);
    }

    public long totalPages() {
        return totalElements == 0 ? 0 : (totalElements + size - 1) / size;
    }
}
