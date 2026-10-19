package vn.simtim.api.qa;

import org.junit.jupiter.api.Test;

/**
 * Explicit verification entry point for backend gaps tracked by Issue #34.
 *
 * <p>The resolved cases run in the default gate through {@link Qa02E2eTest}. This class keeps
 * named GAP entry points for an explicit {@code -Dtest=vn.simtim.api.qa.Qa02GapReproduction} run.
 *
 * <p>GAP-01 through GAP-04 have been promoted to the default provider/QA gate.
 */
class Qa02GapReproduction extends Qa02E2eTest {

    @Test
    void gap01_fractionalEaReceiptIsRejectedWithoutAnyWrite() {
        fractionalEaReceiptIsRejectedWithoutAnyWrite();
    }

    @Test
    void gap02_priceChangedAfterQuoteRejectsCheckoutAndRollsBack() {
        priceChangedAfterQuoteRejectsCheckoutAndRollsBack();
    }

    @Test
    void gap03_salesCannotReadAnotherSellerInvoiceInTheSameStore() {
        salesCannotReadAnotherSellerInvoiceInTheSameStore();
    }

    @Test
    void gap04_checkoutReplayWritesOneInvoiceAndRejectsKeyReuseWithAnotherPayload() {
        checkoutReplayWritesOneInvoiceAndRejectsKeyReuseWithAnotherPayload();
    }
}
