package vn.simtim.api.qa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Executable expected-red specifications for the four backend gaps tracked by Issue #34.
 *
 * <p>The class deliberately has no {@code Test} suffix, so Maven's default root gate compiles it
 * but does not execute unresolved product gaps. Run it explicitly with
 * {@code -Dtest=vn.simtim.api.qa.Qa02GapReproduction}. Once a provider fix is merged, its matching
 * scenario must move into the default provider/QA gate before the traceability row can become
 * Pass.</p>
 */
class Qa02GapReproduction extends Qa02E2eTest {

    @Test
    void gap01_fractionalEaReceiptIsRejectedWithoutAnyWrite() {
        UUID operationId = UUID.randomUUID();
        Map<String, Object> request = Map.of(
                "supplierId", SUPPLIER_ID,
                "clientOperationId", operationId,
                "lines", List.of(Map.of(
                        "productId", RICE_ID,
                        "expectedQuantity", "0.500",
                        "deliveredQuantity", "0.500",
                        "acceptedQuantity", "0.500",
                        "rejectedQuantity", "0.000",
                        "unitCost", 10000,
                        "expiryDate", LocalDate.now().plusDays(8).toString())));
        var requestHeaders = headers(stockToken);
        requestHeaders.set("Idempotency-Key", UUID.randomUUID().toString());

        ResponseEntity<Object> response = http.exchange(
                "/api/v1/inventory/receipts", HttpMethod.POST,
                new HttpEntity<>(request, requestHeaders), Object.class);

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY),
                () -> assertThat(count(
                        "select count(*) from inventory.goods_receipts where client_operation_id=?",
                        operationId)).isZero(),
                () -> assertThat(count(
                        "select count(*) from inventory.goods_receipt_lines where organization_id=? and product_id=?",
                        ORG_ID, RICE_ID)).isZero(),
                () -> assertThat(count(
                        "select count(*) from inventory.product_batches where organization_id=? and product_id=?",
                        ORG_ID, RICE_ID)).isZero(),
                () -> assertThat(count(
                        "select count(*) from inventory.inventory_balances where organization_id=? and product_id=?",
                        ORG_ID, RICE_ID)).isZero(),
                () -> assertThat(count(
                        "select count(*) from inventory.stock_movements where organization_id=? and product_id=?",
                        ORG_ID, RICE_ID)).isZero());
    }

    @Test
    void gap02_priceChangedAfterQuoteRejectsCheckoutAndRollsBack() {
        receive(APPLE_ID, "2.000", "2.000", "0.000", null);
        List<Map<String, Object>> items = List.of(
                Map.of("productId", APPLE_ID, "quantity", "1.000"));
        Map<String, Object> quote = body(post(
                "/api/v1/sales/quote", Map.of("items", items), salesToken));
        long expectedTotal = ((Number) quote.get("grandTotal")).longValue();
        var balanceBefore = productBalance(APPLE_ID);

        try {
            jdbc.update("""
                    update catalog.product_prices set sale_price=41000
                    where organization_id=? and store_id=? and product_id=? and effective_to is null
                    """, ORG_ID, STORE_ID, APPLE_ID);
            ResponseEntity<Object> response = post(
                    "/api/v1/sales/checkout", Map.of(
                            "items", items,
                            "expectedTotal", expectedTotal,
                            "cashAmount", 100000), salesToken);

            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT),
                    () -> assertThat(body(response)).containsEntry("code", "PRICE_CHANGED"),
                    () -> assertThat(count(
                            "select count(*) from sales.invoices where organization_id=?", ORG_ID)).isZero(),
                    () -> assertThat(count(
                            "select count(*) from sales.payments where organization_id=?", ORG_ID)).isZero(),
                    () -> assertThat(count("""
                            select count(*) from inventory.stock_movements
                            where organization_id=? and movement_type='SALE'
                            """, ORG_ID)).isZero(),
                    () -> assertThat(productBalance(APPLE_ID)).isEqualByComparingTo(balanceBefore));
        } finally {
            jdbc.update("""
                    update catalog.product_prices set sale_price=40000
                    where organization_id=? and store_id=? and product_id=? and effective_to is null
                    """, ORG_ID, STORE_ID, APPLE_ID);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void gap03_salesCannotReadAnotherSellerInvoiceInTheSameStore() {
        receive(RICE_ID, "2.000", "2.000", "0.000", null);
        ResponseEntity<Object> checkout = post(
                "/api/v1/sales/checkout", Map.of(
                        "items", List.of(Map.of("productId", RICE_ID, "quantity", "1.000")),
                        "cashAmount", 50000), salesToken);
        assertThat(checkout.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID invoiceId = UUID.fromString(body(checkout).get("id").toString());
        String otherSalesToken = createActor("SALES");
        UUID otherSellerId = UUID.fromString(
                body(get("/api/v1/auth/session", otherSalesToken)).get("userId").toString());

        ResponseEntity<Object> listResponse = get("/api/v1/sales/invoices", otherSalesToken);
        ResponseEntity<Object> detailResponse = get(
                "/api/v1/sales/invoices/" + invoiceId, otherSalesToken);
        List<Map<String, Object>> visibleInvoices =
                (List<Map<String, Object>>) listResponse.getBody();

        assertAll(
                () -> assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(visibleInvoices)
                        .noneMatch(invoice -> invoiceId.toString().equals(invoice.get("id"))),
                () -> assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
                () -> assertThat(count(
                        "select count(*) from sales.invoices where id=? and sold_by<>?",
                        invoiceId, otherSellerId)).isEqualTo(1));
    }

    @Test
    void gap04_checkoutReplayWritesOneInvoiceAndRejectsKeyReuseWithAnotherPayload() {
        receive(APPLE_ID, "4.000", "4.000", "0.000", null);
        UUID idempotencyKey = UUID.randomUUID();
        Map<String, Object> request = Map.of(
                "items", List.of(Map.of("productId", APPLE_ID, "quantity", "1.000")),
                "cashAmount", 100000);
        ResponseEntity<Object> first = checkout(request, idempotencyKey);
        ResponseEntity<Object> replay = checkout(request, idempotencyKey);
        Map<String, Object> changedRequest = new LinkedHashMap<>(request);
        changedRequest.put("items", List.of(
                Map.of("productId", APPLE_ID, "quantity", "2.000")));
        ResponseEntity<Object> changed = checkout(changedRequest, idempotencyKey);

        assertAll(
                () -> assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(replay.getStatusCode().value()).isIn(200, 201),
                () -> assertThat(body(replay).get("id")).isEqualTo(body(first).get("id")),
                () -> assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.CONFLICT),
                () -> assertThat(body(changed)).containsEntry("code", "IDEMPOTENCY_KEY_REUSED"),
                () -> assertThat(count(
                        "select count(*) from sales.invoices where organization_id=?", ORG_ID)).isEqualTo(1),
                () -> assertThat(count(
                        "select count(*) from sales.payments where organization_id=?", ORG_ID)).isEqualTo(1),
                () -> assertThat(count("""
                        select count(*) from inventory.stock_movements
                        where organization_id=? and movement_type='SALE'
                        """, ORG_ID)).isEqualTo(1),
                () -> assertThat(productBalance(APPLE_ID)).isEqualByComparingTo("3.000"));
    }

    private ResponseEntity<Object> checkout(Map<String, Object> request, UUID idempotencyKey) {
        var requestHeaders = headers(salesToken);
        requestHeaders.set("Idempotency-Key", idempotencyKey.toString());
        return http.exchange(
                "/api/v1/sales/checkout", HttpMethod.POST,
                new HttpEntity<>(request, requestHeaders), Object.class);
    }
}
