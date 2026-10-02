package vn.simtim.api.inventory.application;

import java.util.List;

/**
 * Public boundary cho SAL-01. Caller phải gọi cả hai method trong transaction checkout.
 * Implementation không mở transaction mới và không commit độc lập.
 */
public interface InventorySalePort {
    FefoPlan planForCheckout(StoreScope scope, List<StockDemand> demands);

    void postSale(FefoPlan plan, List<SaleIssue> issuedAllocations);
}
