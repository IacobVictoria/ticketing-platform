-- "users", not "user": USER is a reserved word in PostgreSQL
CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,

    -- the application stores emails trimmed + lower-case, so "Ana@X.ro" and "ana@x.ro" are one account
    CONSTRAINT users_email_normalized_check CHECK (email = lower(btrim(email)) AND email <> ''),
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_names_trimmed_check CHECK (
        first_name = btrim(first_name) AND first_name <> ''
        AND last_name = btrim(last_name) AND last_name <> ''
    ),
    CONSTRAINT users_role_check CHECK (role IN ('USER', 'ADMIN'))
);
