CREATE TABLE recognition_sessions (
    id                UUID PRIMARY KEY,
    patient_id        UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    started_by        UUID NOT NULL REFERENCES users (id),
    started_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at       TIMESTAMP WITH TIME ZONE,
    question_index    INT NOT NULL,
    question_shown_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_recognition_sessions_patient_finished ON recognition_sessions (patient_id, finished_at);

CREATE TABLE recognition_questions (
    session_id  UUID NOT NULL REFERENCES recognition_sessions (id) ON DELETE CASCADE,
    position    INT NOT NULL,
    member_id   UUID NOT NULL,
    option_ids  VARCHAR(200) NOT NULL,
    errors      INT NOT NULL,
    response_ms BIGINT,
    PRIMARY KEY (session_id, position)
);
