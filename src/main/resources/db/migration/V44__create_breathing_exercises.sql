CREATE TABLE breathing_exercises (
    id             UUID PRIMARY KEY,
    title          VARCHAR(100) NOT NULL,
    inhale_seconds INT NOT NULL CHECK (inhale_seconds > 0),
    hold_seconds   INT NOT NULL CHECK (hold_seconds >= 0),
    exhale_seconds INT NOT NULL CHECK (exhale_seconds > 0),
    cycles         INT NOT NULL CHECK (cycles > 0),
    validated_by   UUID NOT NULL REFERENCES users (id),
    validated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    published      BOOLEAN NOT NULL
);
