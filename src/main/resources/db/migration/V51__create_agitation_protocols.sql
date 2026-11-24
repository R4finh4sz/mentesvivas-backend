CREATE TABLE agitation_protocols (
    id           UUID PRIMARY KEY,
    severity     VARCHAR(10) NOT NULL CHECK (severity IN ('MILD', 'MODERATE', 'SEVERE')),
    instructions VARCHAR(4000) NOT NULL,
    approved_by  UUID NOT NULL REFERENCES users (id),
    approved_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_agitation_protocols_severity_date ON agitation_protocols (severity, approved_at);

ALTER TABLE occurrences ADD COLUMN agitation_protocol_id UUID REFERENCES agitation_protocols (id);
