-- Sprint 1 subset of docs/architecture/database/DB-01_schema_proposal.sql.
-- V1 already created the core schema. Do not change V1 after release.
CREATE SCHEMA iam;

CREATE TABLE core.organizations (
    id uuid PRIMARY KEY,
    code varchar(40) NOT NULL UNIQUE,
    name varchar(200) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE core.stores (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    code varchar(40) NOT NULL,
    name varchar(200) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    UNIQUE (organization_id, id),
    UNIQUE (organization_id, code)
);

CREATE TABLE iam.users (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    username varchar(80) NOT NULL,
    password_hash varchar(255) NOT NULL,
    full_name varchar(200) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED')),
    training_enabled boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    UNIQUE (organization_id, id),
    UNIQUE (organization_id, username)
);
CREATE UNIQUE INDEX user_username_normalized ON iam.users(organization_id, lower(username));

CREATE TABLE iam.auth_sessions (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    user_id uuid NOT NULL,
    store_id uuid,
    token_hash bytea NOT NULL UNIQUE CHECK (octet_length(token_hash) = 32),
    issued_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    last_seen_at timestamptz,
    FOREIGN KEY (organization_id, user_id) REFERENCES iam.users(organization_id, id),
    FOREIGN KEY (organization_id, store_id) REFERENCES core.stores(organization_id, id),
    CHECK (expires_at > issued_at),
    CHECK (revoked_at IS NULL OR revoked_at >= issued_at)
);
CREATE INDEX auth_session_user_expiry ON iam.auth_sessions(user_id, expires_at);

CREATE TABLE iam.roles (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    code varchar(16) NOT NULL CHECK (code IN ('SALES', 'STOCK', 'MANAGER', 'ADMIN')),
    name varchar(100) NOT NULL,
    UNIQUE (organization_id, id),
    UNIQUE (organization_id, code)
);

CREATE TABLE iam.permissions (
    id uuid PRIMARY KEY,
    code varchar(100) NOT NULL UNIQUE,
    description varchar(255) NOT NULL
);

CREATE TABLE iam.user_roles (
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    user_id uuid NOT NULL,
    role_id uuid NOT NULL,
    store_id uuid NOT NULL,
    assigned_at timestamptz NOT NULL,
    assigned_by uuid NOT NULL,
    PRIMARY KEY (user_id, role_id, store_id),
    FOREIGN KEY (organization_id, user_id) REFERENCES iam.users(organization_id, id),
    FOREIGN KEY (organization_id, role_id) REFERENCES iam.roles(organization_id, id),
    FOREIGN KEY (organization_id, store_id) REFERENCES core.stores(organization_id, id),
    FOREIGN KEY (organization_id, assigned_by) REFERENCES iam.users(organization_id, id)
);

CREATE TABLE iam.role_permissions (
    role_id uuid NOT NULL REFERENCES iam.roles(id),
    permission_id uuid NOT NULL REFERENCES iam.permissions(id),
    PRIMARY KEY (role_id, permission_id)
);
