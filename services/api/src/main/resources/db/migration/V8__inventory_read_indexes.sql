-- INV-02: deterministic scoped read paths and FEFO candidate lookup.
CREATE INDEX inventory_balances_store_product_batch
    ON inventory.inventory_balances(organization_id, store_id, product_id, batch_id);

CREATE INDEX product_batches_fefo
    ON inventory.product_batches(
        organization_id, store_id, product_id, status,
        expiry_date ASC NULLS LAST, received_date, id);

CREATE INDEX stock_movements_store_time
    ON inventory.stock_movements(organization_id, store_id, occurred_at DESC, id DESC);
