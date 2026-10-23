-- Existing completed transfers retain their bank receipt evidence.
ALTER TABLE sales.transfer_orders ADD COLUMN confirmation_method varchar(20)
    CHECK (confirmation_method IN ('BANK_RECEIPT','EMPLOYEE'));
UPDATE sales.transfer_orders SET confirmation_method='BANK_RECEIPT' WHERE status='COMPLETED';

-- V19 used an unnamed check; locate it by its bank_reference dependency.
DO $$
DECLARE existing_check record;
BEGIN
    FOR existing_check IN
        SELECT conname FROM pg_constraint
        WHERE conrelid='sales.transfer_orders'::regclass AND contype='c'
          AND pg_get_constraintdef(oid) LIKE '%bank_reference%'
    LOOP
        EXECUTE format('ALTER TABLE sales.transfer_orders DROP CONSTRAINT %I', existing_check.conname);
    END LOOP;
END $$;

-- Old deployments still write bank_reference without the new column during rollout.
CREATE FUNCTION sales.classify_legacy_transfer_confirmation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.status='COMPLETED' AND NEW.confirmation_method IS NULL AND NEW.bank_reference IS NOT NULL THEN
        NEW.confirmation_method := 'BANK_RECEIPT';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER classify_legacy_transfer_confirmation
    BEFORE INSERT OR UPDATE OF status,bank_reference ON sales.transfer_orders
    FOR EACH ROW EXECUTE FUNCTION sales.classify_legacy_transfer_confirmation();

ALTER TABLE sales.transfer_orders ADD CONSTRAINT transfer_confirmation_evidence CHECK (
    (status <> 'COMPLETED' AND confirmation_method IS NULL)
    OR (status='COMPLETED' AND resolved_by IS NOT NULL AND confirmation_method IS NOT NULL AND (
        (confirmation_method='BANK_RECEIPT' AND bank_reference IS NOT NULL)
        OR (confirmation_method='EMPLOYEE' AND bank_reference IS NULL)
    ))
);
