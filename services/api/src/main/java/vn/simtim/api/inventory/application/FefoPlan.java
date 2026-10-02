package vn.simtim.api.inventory.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Kế hoạch được chụp sau khi product/balance đã bị khóa. */
public record FefoPlan(
        StoreScope scope,
        Instant capturedAt,
        LocalDate businessDate,
        List<FefoAllocation> allocations) {

    public FefoPlan {
        allocations = List.copyOf(allocations);
    }
}
