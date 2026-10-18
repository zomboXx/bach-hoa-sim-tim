-- TRN-03: operational sales and inventory are deliberately outside this schema.
CREATE SCHEMA training;

CREATE TABLE training.scenarios (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    code varchar(80) NOT NULL,
    scenario_version integer NOT NULL CHECK (scenario_version > 0),
    name varchar(200) NOT NULL,
    summary text NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED')),
    created_at timestamptz NOT NULL,
    UNIQUE (organization_id, id),
    UNIQUE (organization_id, code, scenario_version)
);

CREATE TABLE training.scenario_steps (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    scenario_id uuid NOT NULL,
    step_order integer NOT NULL CHECK (step_order > 0),
    stage varchar(40) NOT NULL,
    action_type varchar(60) NOT NULL,
    target_key varchar(100) NOT NULL,
    instruction_text text NOT NULL,
    UNIQUE (organization_id, scenario_id, id),
    UNIQUE (scenario_id, step_order),
    FOREIGN KEY (organization_id, scenario_id) REFERENCES training.scenarios(organization_id, id)
);

CREATE TABLE training.scenario_choices (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    scenario_id uuid NOT NULL,
    scenario_step_id uuid NOT NULL,
    choice_key varchar(100) NOT NULL,
    is_correct boolean NOT NULL,
    feedback_text text NOT NULL,
    UNIQUE (scenario_step_id, choice_key),
    FOREIGN KEY (organization_id, scenario_id, scenario_step_id)
        REFERENCES training.scenario_steps(organization_id, scenario_id, id)
);
CREATE UNIQUE INDEX scenario_one_correct_choice ON training.scenario_choices(scenario_step_id) WHERE is_correct;

CREATE TABLE training.sessions (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL,
    learner_user_id uuid NOT NULL,
    scenario_id uuid NOT NULL,
    scenario_version integer NOT NULL CHECK (scenario_version > 0),
    attempt_number integer NOT NULL CHECK (attempt_number > 0),
    idempotency_key uuid NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
    step_cursor integer NOT NULL DEFAULT 0 CHECK (step_cursor >= 0),
    error_count integer NOT NULL DEFAULT 0 CHECK (error_count >= 0),
    steps_snapshot jsonb NOT NULL,
    started_at timestamptz NOT NULL,
    ended_at timestamptz,
    UNIQUE (organization_id, id),
    UNIQUE (organization_id, idempotency_key),
    UNIQUE (scenario_id, learner_user_id, attempt_number),
    FOREIGN KEY (organization_id, store_id) REFERENCES core.stores(organization_id, id),
    FOREIGN KEY (organization_id, learner_user_id) REFERENCES iam.users(organization_id, id),
    FOREIGN KEY (organization_id, scenario_id) REFERENCES training.scenarios(organization_id, id),
    CHECK ((status = 'IN_PROGRESS' AND ended_at IS NULL) OR
           (status <> 'IN_PROGRESS' AND ended_at IS NOT NULL))
);
CREATE INDEX training_learner_history ON training.sessions(organization_id, learner_user_id, started_at DESC);

CREATE TABLE training.session_actions (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    session_id uuid NOT NULL,
    action_sequence integer NOT NULL CHECK (action_sequence > 0),
    step_order integer NOT NULL CHECK (step_order > 0),
    action_type varchar(60) NOT NULL,
    target_key varchar(100) NOT NULL,
    choice_key varchar(100),
    evaluation_status varchar(20) NOT NULL CHECK (evaluation_status IN ('CORRECT', 'INCORRECT')),
    feedback_text text NOT NULL,
    response_data jsonb NOT NULL,
    occurred_at timestamptz NOT NULL,
    UNIQUE (session_id, action_sequence),
    FOREIGN KEY (organization_id, session_id) REFERENCES training.sessions(organization_id, id)
);

CREATE TABLE training.session_results (
    session_id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    total_score integer NOT NULL CHECK (total_score BETWEEN 0 AND 100),
    passed boolean NOT NULL,
    grade varchar(24) NOT NULL CHECK (grade IN ('EXCELLENT', 'SATISFACTORY', 'NEEDS_PRACTICE')),
    error_count integer NOT NULL CHECK (error_count >= 0),
    completed_at timestamptz NOT NULL,
    FOREIGN KEY (organization_id, session_id) REFERENCES training.sessions(organization_id, id)
);

INSERT INTO iam.permissions(id, code, description) VALUES
    ('40000000-0000-0000-0000-000000000001', 'training.results.read.store', 'Read training results of currently assigned store staff'),
    ('40000000-0000-0000-0000-000000000002', 'training.results.read.organization', 'Read training results within organization');
INSERT INTO iam.role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM iam.roles r CROSS JOIN iam.permissions p
WHERE (p.code = 'training.results.read.store' AND r.code = 'MANAGER')
   OR (p.code = 'training.results.read.organization' AND r.code = 'ADMIN')
ON CONFLICT DO NOTHING;
