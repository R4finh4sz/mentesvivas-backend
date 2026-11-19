CREATE TABLE mfa_backup_codes (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code_hash   VARCHAR(64)  NOT NULL,
    used_at     TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_mfa_backup_codes UNIQUE (user_id, code_hash)
);

CREATE INDEX idx_mfa_backup_codes_user ON mfa_backup_codes (user_id);
