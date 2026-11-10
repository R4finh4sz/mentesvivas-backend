ALTER TABLE doctor_patient_links ADD COLUMN doctor_confirmed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE doctor_patient_links ADD COLUMN patient_authorized BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE doctor_patient_links ADD COLUMN initiated_by UUID REFERENCES users (id);
ALTER TABLE doctor_patient_links ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;
UPDATE doctor_patient_links SET updated_at = created_at;
ALTER TABLE doctor_patient_links ALTER COLUMN updated_at SET NOT NULL;
UPDATE doctor_patient_links SET doctor_confirmed = TRUE, patient_authorized = TRUE WHERE status = 'ACTIVE';

CREATE TABLE caregiver_patient_links (
    id            UUID PRIMARY KEY,
    caregiver_id  UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    patient_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status        VARCHAR(10) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_caregiver_patient_links UNIQUE (caregiver_id, patient_id)
);

CREATE TABLE pending_invitations (
    id          UUID PRIMARY KEY,
    email       VARCHAR(254) NOT NULL,
    inviter_id  UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    kind        VARCHAR(30)  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_pending_invitations UNIQUE (email, inviter_id, kind)
);

CREATE TABLE notifications (
    id            UUID PRIMARY KEY,
    recipient_id  UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type          VARCHAR(30)  NOT NULL,
    reference_id  UUID,
    message       VARCHAR(300) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at       TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notifications_recipient ON notifications (recipient_id, created_at);
