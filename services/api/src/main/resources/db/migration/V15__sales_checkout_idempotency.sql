-- S2-GAP-04: Checkout idempotency key on sales.invoices.
-- A UUID supplied in the Idempotency-Key request header is stored here so that
-- a duplicate POST /sales/checkout with the same key returns the already-created
-- invoice (200/201) instead of writing a second record.  Sending the same key
-- with a different payload yields 409 IDEMPOTENCY_KEY_REUSED.
-- The column is nullable so existing rows (created before this migration) are
-- not affected; the UNIQUE constraint prevents double-writes under the same key.

ALTER TABLE sales.invoices
    ADD COLUMN idempotency_key uuid;

CREATE UNIQUE INDEX uq_invoices_idempotency_key
    ON sales.invoices(organization_id, store_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
