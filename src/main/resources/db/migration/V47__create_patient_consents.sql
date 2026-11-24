CREATE TABLE patient_consents (
    id         UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    scope      VARCHAR(20) NOT NULL CHECK (scope IN ('FAMILY_PHOTOS', 'FAMILY_AUDIO', 'GEOLOCATION')),
    granted    BOOLEAN NOT NULL,
    decided_by UUID NOT NULL REFERENCES users (id),
    decided_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (patient_id, scope)
);
