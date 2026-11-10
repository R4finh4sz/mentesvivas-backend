CREATE TABLE admin_actions (
    id              UUID PRIMARY KEY,
    admin_id        UUID         NOT NULL REFERENCES users (id),
    target_user_id  UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    action          VARCHAR(30)  NOT NULL,
    performed_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_admin_actions_target ON admin_actions (target_user_id, performed_at);
