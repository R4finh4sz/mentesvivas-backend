UPDATE usuarios SET email = lower(trim(email));
UPDATE usuarios SET crm_uf = upper(trim(crm_uf)) WHERE crm_uf IS NOT NULL;

ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_email_minusculo CHECK (email = lower(email));
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_crm_uf_maiusculo CHECK (crm_uf IS NULL OR crm_uf = upper(crm_uf));
