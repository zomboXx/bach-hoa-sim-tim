package vn.simtim.api.inventory.api;

import java.util.List;
import java.util.function.Function;
import vn.simtim.api.inventory.application.InventoryPage;

/** Wire pagination chung cho ba read endpoint INV-02. */
public record InventoryPageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        long totalPages) {

    static <S, T> InventoryPageResponse<T> from(
            InventoryPage<S> source, Function<S, T> mapper) {
        return new InventoryPageResponse<>(
                source.items().stream().map(mapper).toList(),
                source.page(), source.size(), source.totalElements(), source.totalPages());
    }
}
