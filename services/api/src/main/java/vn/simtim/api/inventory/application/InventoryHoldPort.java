package vn.simtim.api.inventory.application;

import java.util.List;

/** Reservation lifecycle; implementations participate in the caller's transaction. */
public interface InventoryHoldPort {
    void reserve(FefoPlan plan);
    void release(StoreScope scope, List<SaleIssue> allocations);
    void complete(StoreScope scope, List<SaleIssue> allocations);
}
