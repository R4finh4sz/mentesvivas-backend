ALTER TABLE usuarios DROP CONSTRAINT ck_usuarios_role;

UPDATE usuarios SET role = 'PATIENT' WHERE role = 'PACIENTE';
UPDATE usuarios SET role = 'CAREGIVER' WHERE role = 'CUIDADOR';
UPDATE usuarios SET role = 'DOCTOR' WHERE role = 'MEDICO';
UPDATE usuarios SET role = 'HEALTH_PROFESSIONAL' WHERE role = 'PROFISSIONAL_SAUDE';
UPDATE usuarios SET role = 'ADMINISTRATOR' WHERE role = 'ADMINISTRADOR';

ALTER TABLE usuarios RENAME TO users;

ALTER TABLE users RENAME COLUMN nome_completo TO full_name;
ALTER TABLE users RENAME COLUMN data_nascimento TO birth_date;
ALTER TABLE users RENAME COLUMN telefone TO phone;
ALTER TABLE users RENAME COLUMN senha_hash TO password_hash;
ALTER TABLE users RENAME COLUMN email_confirmado TO email_confirmed;
ALTER TABLE users RENAME COLUMN crm_uf TO crm_state;
ALTER TABLE users RENAME COLUMN crm_verificado TO crm_verified;
ALTER TABLE users RENAME COLUMN dados_saude_consentido_em TO health_data_consented_at;
ALTER TABLE users RENAME COLUMN criado_em TO created_at;
ALTER TABLE users RENAME COLUMN atualizado_em TO updated_at;

ALTER TABLE users RENAME CONSTRAINT uk_usuarios_email TO uk_users_email;
ALTER TABLE users RENAME CONSTRAINT uk_usuarios_cpf TO uk_users_cpf;
ALTER TABLE users RENAME CONSTRAINT ck_usuarios_email_minusculo TO ck_users_email_lowercase;
ALTER TABLE users RENAME CONSTRAINT ck_usuarios_crm_uf_maiusculo TO ck_users_crm_state_uppercase;

ALTER TABLE users ADD CONSTRAINT ck_users_role
    CHECK (role IN ('PATIENT', 'CAREGIVER', 'DOCTOR', 'HEALTH_PROFESSIONAL', 'ADMINISTRATOR'));

ALTER TABLE refresh_tokens RENAME COLUMN usuario_id TO user_id;
ALTER TABLE refresh_tokens RENAME COLUMN expira_em TO expires_at;
ALTER TABLE refresh_tokens RENAME COLUMN revogado_em TO revoked_at;
ALTER TABLE refresh_tokens RENAME COLUMN criado_em TO created_at;
ALTER INDEX idx_refresh_tokens_usuario RENAME TO idx_refresh_tokens_user;

ALTER TABLE termos_versoes RENAME TO terms_versions;
ALTER TABLE terms_versions RENAME COLUMN versao TO version;
ALTER TABLE terms_versions RENAME COLUMN titulo TO title;
ALTER TABLE terms_versions RENAME COLUMN conteudo TO content;
ALTER TABLE terms_versions RENAME COLUMN publicado_por TO published_by;
ALTER TABLE terms_versions RENAME COLUMN publicado_em TO published_at;

ALTER TABLE aceites_termos RENAME TO terms_acceptances;
ALTER TABLE terms_acceptances RENAME COLUMN usuario_id TO user_id;
ALTER TABLE terms_acceptances RENAME COLUMN versao TO version;
ALTER TABLE terms_acceptances RENAME COLUMN aceito_em TO accepted_at;
ALTER TABLE terms_acceptances RENAME CONSTRAINT uk_aceites_termos_usuario_versao TO uk_terms_acceptances_user_version;
