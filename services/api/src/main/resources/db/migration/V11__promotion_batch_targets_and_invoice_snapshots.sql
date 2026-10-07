-- PRO-01B: batch targets and immutable promotion snapshots on sale lines.
CREATE TABLE sales.promotion_batches (
    organization_id uuid NOT NULL,
    promotion_id uuid NOT NULL,
    product_batch_id uuid NOT NULL,
    PRIMARY KEY (promotion_id, product_batch_id),
    FOREIGN KEY (organization_id, promotion_id) REFERENCES sales.promotions(organization_id, id),
    FOREIGN KEY (organization_id, product_batch_id) REFERENCES inventory.product_batches(organization_id, id)
);

ALTER TABLE sales.invoice_lines
    ADD COLUMN applied_promotion_code varchar(60),
    ADD COLUMN applied_promotion_name varchar(200),
    ADD COLUMN promotion_discount_type varchar(16),
    ADD COLUMN promotion_discount_value numeric(14,2),
    ADD CONSTRAINT invoice_lines_applied_promotion_fk
        FOREIGN KEY (organization_id, applied_promotion_id)
        REFERENCES sales.promotions(organization_id, id),
    ADD CONSTRAINT invoice_lines_promotion_snapshot_check CHECK (
        (applied_promotion_id IS NULL
            AND applied_promotion_code IS NULL
            AND applied_promotion_name IS NULL
            AND promotion_discount_type IS NULL
            AND promotion_discount_value IS NULL)
        OR
        (applied_promotion_id IS NOT NULL
            AND applied_promotion_code IS NOT NULL
            AND applied_promotion_name IS NOT NULL
            AND promotion_discount_type IN ('AMOUNT', 'PERCENT')
            AND promotion_discount_value > 0)
    );
