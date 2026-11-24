CREATE TABLE family_members (
    id          UUID PRIMARY KEY,
    patient_id  UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    kinship     VARCHAR(50)  NOT NULL,
    photo_url   VARCHAR(500) NOT NULL,
    audio_url   VARCHAR(500),
    created_by  UUID         NOT NULL REFERENCES users (id),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_family_members_patient ON family_members (patient_id);
