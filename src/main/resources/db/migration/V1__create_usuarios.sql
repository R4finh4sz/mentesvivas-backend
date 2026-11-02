CREATE TABLE usuarios (
    id                     UUID PRIMARY KEY,
    nome_completo          VARCHAR(150) NOT NULL,
    cpf                    VARCHAR(11)  NOT NULL,
    data_nascimento        DATE,
    telefone               VARCHAR(11)  NOT NULL,
    email                  VARCHAR(254) NOT NULL,
    senha_hash             VARCHAR(255) NOT NULL,
    role                   VARCHAR(30)  NOT NULL,
    email_confirmado       BOOLEAN      NOT NULL DEFAULT FALSE,
    crm                    VARCHAR(10),
    crm_uf                 VARCHAR(2),
    crm_verificado         BOOLEAN      NOT NULL DEFAULT FALSE,
    termos_versao          VARCHAR(20)  NOT NULL,
    termos_aceitos_em      TIMESTAMP WITH TIME ZONE NOT NULL,
    criado_em              TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT uk_usuarios_cpf UNIQUE (cpf),
    CONSTRAINT ck_usuarios_role CHECK (role IN ('PACIENTE', 'CUIDADOR', 'MEDICO', 'PROFISSIONAL_SAUDE', 'ADMINISTRADOR'))
);

CREATE TABLE refresh_tokens (
    id           UUID PRIMARY KEY,
    usuario_id   UUID        NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    token_hash   VARCHAR(64) NOT NULL,
    expira_em    TIMESTAMP WITH TIME ZONE NOT NULL,
    revogado_em  TIMESTAMP WITH TIME ZONE,
    criado_em    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_usuario ON refresh_tokens (usuario_id);
