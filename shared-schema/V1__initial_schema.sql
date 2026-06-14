-- ============================================================
-- URL Shortener System - Initial Schema
-- File: V1__initial_schema.sql
-- Naming: Flyway migration convention (V{version}__{description}.sql)
-- ============================================================

-- ----------------------------------------------------------------
-- URL TABLE
-- Core table: maps Snowflake ID → shortKey → longUrl
--
-- id        = Snowflake ID (set by application, NOT DB sequence)
--             This ensures global uniqueness across distributed workers.
-- short_key = Base62(id), 6-7 chars
-- long_url  = original URL (up to 2048 chars covers most real-world URLs)
-- ----------------------------------------------------------------
CREATE TABLE url (
    id           BIGINT          PRIMARY KEY,    -- Snowflake ID from application
    short_key    VARCHAR(10)     NOT NULL UNIQUE,
    long_url     VARCHAR(2048)   NOT NULL,
    expires_at   TIMESTAMP,      NOT NULL
    created_at   TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_url_short_key  ON url(short_key);
CREATE INDEX idx_url_long_url   ON url(long_url);  -- for idempotency lookups
CREATE INDEX idx_url_expires_at ON url(expires_at) WHERE expires_at IS NOT NULL;

-- ----------------------------------------------------------------
-- Trigger: auto-update updated_at on row change
-- ----------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trg_url_updated_at
    BEFORE UPDATE ON url
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

