CREATE TABLE addresses (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    cep         VARCHAR(8)   NOT NULL,
    street      VARCHAR(200) NOT NULL,
    number      VARCHAR(20)  NOT NULL,
    complement  VARCHAR(100),
    city        VARCHAR(100) NOT NULL,
    state       VARCHAR(2)   NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_addresses_user UNIQUE (user_id)
);
