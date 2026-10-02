package vn.simtim.api.sale;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.simtim.api.inventory.application.FefoAllocation;
import vn.simtim.api.inventory.application.FefoPlan;
import vn.simtim.api.inventory.application.InventorySalePort;
import vn.simtim.api.inventory.application.SaleIssue;
import vn.simtim.api.inventory.application.StockDemand;
import vn.simtim.api.inventory.application.StoreScope;

/** Compile-level consumer contract cho module SAL-01. */
class InventorySalePortContractTest {

    @Test
    void salesModuleCanCompileAgainstPublicInventoryBoundaryWithoutJpaTypes() {
        UUID organizationId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        LocalDate businessDate = LocalDate.of(2026, 10, 2);

        InventorySalePort port = new InventorySalePort() {
            @Override
            public FefoPlan planForCheckout(StoreScope scope, List<StockDemand> demands) {
                return new FefoPlan(scope, Instant.parse("2026-10-01T17:00:00Z"), businessDate,
                        List.of(new FefoAllocation(
                                productId, batchId, demands.getFirst().quantity(),
                                businessDate.plusDays(1), businessDate.minusDays(1))));
            }

            @Override
            public void postSale(FefoPlan plan, List<SaleIssue> issuedAllocations) {
                // Consumer only sees immutable application DTOs.
            }
        };

        FefoPlan plan = port.planForCheckout(
                new StoreScope(organizationId, storeId),
                List.of(new StockDemand(productId, new BigDecimal("2.000"))));
        port.postSale(plan, List.of(new SaleIssue(
                UUID.randomUUID(), UUID.randomUUID(), productId, batchId,
                new BigDecimal("2.000"), UUID.randomUUID())));

        assertThat(plan.allocations()).singleElement()
                .extracting(FefoAllocation::batchId)
                .isEqualTo(batchId);
    }
}
