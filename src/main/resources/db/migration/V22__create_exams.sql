CREATE TABLE exams (
    id          UUID PRIMARY KEY,
    patient_id  UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category    VARCHAR(10)   NOT NULL,
    file_type   VARCHAR(10)   NOT NULL,
    title       VARCHAR(120)  NOT NULL,
    file_url    VARCHAR(500)  NOT NULL,
    exam_date   DATE          NOT NULL,
    notes       VARCHAR(500),
    uploaded_by UUID          NOT NULL REFERENCES users (id),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_exams_patient ON exams (patient_id, exam_date DESC);
