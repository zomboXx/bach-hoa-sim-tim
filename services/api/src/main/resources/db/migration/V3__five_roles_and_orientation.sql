ALTER TABLE accounts DROP CONSTRAINT accounts_role_check;
ALTER TABLE accounts ADD CONSTRAINT accounts_role_check CHECK(role IN ('admin','accountant','manager','sales','stock'));
ALTER TABLE training_sessions ADD COLUMN orientation_passed boolean NOT NULL DEFAULT false;
-- Preserve historical completions; new sessions must pass the orientation questions.
UPDATE training_sessions SET orientation_passed=true WHERE status='COMPLETED';
