CREATE TABLE termos_versoes (
    versao         INTEGER PRIMARY KEY,
    titulo         VARCHAR(200) NOT NULL,
    conteudo       TEXT         NOT NULL,
    publicado_por  UUID         NOT NULL REFERENCES usuarios (id),
    publicado_em   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE aceites_termos (
    id          UUID PRIMARY KEY,
    usuario_id  UUID    NOT NULL REFERENCES usuarios (id),
    versao      INTEGER NOT NULL REFERENCES termos_versoes (versao),
    aceito_em   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_aceites_termos_usuario_versao UNIQUE (usuario_id, versao)
);

ALTER TABLE usuarios ADD COLUMN dados_saude_consentido_em TIMESTAMP WITH TIME ZONE;
UPDATE usuarios SET dados_saude_consentido_em = termos_aceitos_em WHERE role IN ('PACIENTE', 'CUIDADOR');

ALTER TABLE usuarios DROP COLUMN termos_versao;
ALTER TABLE usuarios DROP COLUMN termos_aceitos_em;

ALTER TABLE usuarios ALTER COLUMN cpf DROP NOT NULL;
ALTER TABLE usuarios ALTER COLUMN telefone DROP NOT NULL;
