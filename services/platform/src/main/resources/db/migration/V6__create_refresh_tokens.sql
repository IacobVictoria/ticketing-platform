-- Long-lived tokens that let the browser get a new 15-minute access token without logging in again.
-- Only a SHA-256 hash is stored: a leaked table cannot be used to log in.
CREATE TABLE refresh_tokens (
    id            UUID        PRIMARY KEY,
    user_id       UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- all tokens rotated from the same login; revoked together if a stolen token is reused
    family_id     UUID        NOT NULL,
    token_hash    VARCHAR(64) NOT NULL,
    expires_at    TIMESTAMPTZ NOT NULL,
    revoked_at    TIMESTAMPTZ,
    revoke_reason VARCHAR(20),
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,

    CONSTRAINT refresh_tokens_token_hash_unique UNIQUE (token_hash),
    CONSTRAINT refresh_tokens_revoke_reason_check CHECK (revoke_reason IN ('ROTATED', 'LOGOUT', 'REUSE_DETECTED')),
    -- revoked_at and revoke_reason are set together or not at all
    CONSTRAINT refresh_tokens_revocation_check CHECK ((revoked_at IS NULL) = (revoke_reason IS NULL))
);

CREATE INDEX refresh_tokens_family_id_idx ON refresh_tokens (family_id);
CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens (user_id);
