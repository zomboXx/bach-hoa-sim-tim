package vn.simtim.api.qa;

import org.junit.jupiter.api.Test;

/**
 * Executable expected-red specifications for the backend gaps tracked by Issue #34.
 *
 * <p>The class deliberately has no {@code Test} suffix, so Maven's default root gate compiles it
 * but does not execute unresolved product gaps. Run it explicitly with
 * {@code -Dtest=vn.simtim.api.qa.Qa02GapReproduction}. Once a provider fix is merged, its matching
 * scenario must move into the default provider/QA gate before the traceability row can become
 * Pass.
 *
 * <p>Graduated to default gate: GAP-01 (quantity precision), GAP-02 (price changed),
 * GAP-03 (invoice isolation), GAP-04 (idempotency replay).
 * Remaining backend gaps: none — all four have been resolved and promoted to {@link Qa02E2eTest}.
 */
class Qa02GapReproduction extends Qa02E2eTest {

    @Test
    void gap01_fractionalEaReceiptIsRejectedWithoutAnyWrite() {
        fractionalEaReceiptIsRejectedWithoutAnyWrite();
    }

    @Test
    void gap03_salesCannotReadAnotherSellerInvoiceInTheSameStore() {
        salesCannotReadAnotherSellerInvoiceInTheSameStore();
    }
}
