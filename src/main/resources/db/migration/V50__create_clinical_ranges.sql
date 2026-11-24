CREATE TABLE clinical_range_policies (
    id          UUID PRIMARY KEY,
    approved_by UUID NOT NULL REFERENCES users (id),
    approved_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE clinical_ranges (
    policy_id   UUID NOT NULL REFERENCES clinical_range_policies (id),
    position    INT NOT NULL,
    metric      VARCHAR(24) NOT NULL,
    lower_bound NUMERIC(14,6),
    color       VARCHAR(6) NOT NULL,
    PRIMARY KEY (policy_id, position)
);

ALTER TABLE vital_signs ADD COLUMN clinical_color VARCHAR(6);
ALTER TABLE vital_signs ADD COLUMN clinical_policy_id UUID REFERENCES clinical_range_policies (id);
