-- NULL = the user has not clicked the link in the verification email yet (cannot log in)
ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMPTZ;

-- accounts created before email verification existed stay usable
UPDATE users SET email_verified_at = created_at;

-- one-time tokens sent by email; only a SHA-256 hash is stored, like refresh tokens
CREATE TABLE email_verification_tokens (
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT email_verification_tokens_token_hash_unique UNIQUE (token_hash)
);

CREATE INDEX email_verification_tokens_user_id_idx ON email_verification_tokens (user_id);
