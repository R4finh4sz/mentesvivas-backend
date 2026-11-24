CREATE TABLE bingo_sounds (
    id          UUID PRIMARY KEY,
    instrument  VARCHAR(60)  NOT NULL,
    search_key  VARCHAR(60)  NOT NULL UNIQUE,
    audio_url   VARCHAR(500) NOT NULL,
    active      BOOLEAN      NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE bingo_settings (
    patient_id        UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    narration_enabled BOOLEAN NOT NULL,
    updated_by        UUID    NOT NULL REFERENCES users (id),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE bingo_sessions (
    id               UUID PRIMARY KEY,
    patient_id       UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    started_by       UUID NOT NULL REFERENCES users (id),
    difficulty_level INT  NOT NULL,
    total_rounds     INT  NOT NULL,
    error_count      INT  NOT NULL,
    started_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_bingo_sessions_patient ON bingo_sessions (patient_id, started_at);

CREATE TABLE bingo_rounds (
    id          UUID PRIMARY KEY,
    session_id  UUID         NOT NULL REFERENCES bingo_sessions (id) ON DELETE CASCADE,
    position    INT          NOT NULL,
    sound_id    UUID         NOT NULL,
    option_ids  VARCHAR(200) NOT NULL,
    attempts    INT          NOT NULL,
    shown_at    TIMESTAMP WITH TIME ZONE,
    answered_at TIMESTAMP WITH TIME ZONE,
    response_ms BIGINT
);

CREATE INDEX idx_bingo_rounds_session ON bingo_rounds (session_id, position);
