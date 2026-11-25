-- Handoff for AUTH-10-DB. Do not apply until the database issue approves the migration.
create schema if not exists auth;

create table auth.password_recovery_token (
    id uuid primary key,
    user_id uuid not null,
    token_hash varchar(64) not null unique,
    issued_at timestamptz not null,
    expires_at timestamptz not null,
    used_at timestamptz,
    revoked_at timestamptz,
    constraint ck_password_recovery_token_dates check (expires_at > issued_at),
    constraint ck_password_recovery_token_terminal_state check (used_at is null or revoked_at is null)
);

create index idx_password_recovery_token_user_issued
    on auth.password_recovery_token (user_id, issued_at);
