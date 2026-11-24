# Regras de Negócio Implementadas no Código

Mapa das regras de [regras-de-negocio.md](regras-de-negocio.md) que já têm implementação no backend. Atualizar a cada nova entrega.

Situação: **Completa** (regra atendida no escopo do backend), **Parcial** (atendida em parte; a lacuna está descrita) ou **Pendente (Definir)** quando depende de decisão em aberto.

## Resumo

| Situação | RNs |
|---|---|
| Completa | RN01, RN02, RN04, RN05, RN06, RN07, RN08, RN09, RN10, RN11, RN12, RN13, RN14, RN15, RN16, RN17, RN18, RN19, RN20, RN21, RN22, RN23, RN24, RN25, RN26, RN27, RN28, RN30, RN31, RN32, RN33, RN34, RN36, RN37, RN38, RN39, RN40, RN41, RN42, RN43, RN44, RN45, RN46, RN47, RN48 |
| Removida | RN03 |
| Parcial | — |

## 1. Acesso e autenticação

| RN | Situação | O que está implementado | Onde |
|---|---|---|---|
| RN01 | Completa | Login por e-mail e senha para todos os perfis. E-mail normalizado (minúsculo, NFKC) na entrada e no banco (constraint `ck_users_email_lowercase`). Resposta única para e-mail inexistente ou senha errada, com hash fictício para tempo constante. "Ver senha" é responsabilidade do front. | `LoginService`, `LoginRequest`, `Normalization`, V3 |
| RN02 | Completa | Mínimo de 8 e máximo de 72 caracteres (limite do BCrypt), com ao menos 1 letra maiúscula, 1 minúscula e 1 número (decisão do time), no cadastro e na redefinição de senha. A senha do administrador inicial segue regra própria (12 caracteres). | `StrongPassword`, `RegistrationRequest`, `DoctorRegistrationRequest`, `ResetPasswordRequest` |
| RN03 | Removida | Regra removida das regras de negócio em 10/11/2026 (sem "manter conectado" nem PIN/biometria). | — |
| RN04 | Completa | Médico faz login com e-mail e senha e, em seguida, informa um código de seis dígitos. Dois métodos (escolha do usuário): **e-mail** (Resend, padrão) ou **aplicativo autenticador** (TOTP RFC 6238, compatível com Microsoft Authenticator). Sem o código nenhum token é emitido (`POST /api/auth/login` responde 202 com o desafio e o `method`; `POST /api/auth/2fa/verify` conclui). O código só é aceito após a senha correta (desafio pendente), é de uso único, tem limite de tentativas (e de envios no e-mail) e é distinto do código de recuperação de senha. Configuração em `/api/users/me/mfa`: `setup` devolve segredo e link `otpauth://`, `confirm` ativa com um código válido, `disable` volta ao e-mail exigindo código válido. O segredo TOTP fica criptografado (AES-GCM, chave `MFA_ENCRYPTION_KEY`) e o mesmo código não é aceito duas vezes. **Pendente (Definir):** validade definitiva do código por e-mail (provisório: 10 min), limites e recuperação do segundo fator se o aplicativo for perdido. Outros profissionais com acesso clínico entram em `TwoFactorRequirement` quando existirem (RN29, V2). | `TwoFactorService`, `SecondFactor`, `EmailSecondFactor`, `AuthenticatorSecondFactor`, `AuthenticatorEnrollmentService`, `TotpService`, `AesGcmSecretCipher`, `MfaController`, V8 |
| RN05 | Completa | Após 5 tentativas inválidas consecutivas o login é bloqueado temporariamente para qualquer e-mail (existente ou não), com resposta 429, `Retry-After` e orientação de recuperar a senha. Login correto zera a contagem; a redefinição de senha libera o bloqueio. Duração e janela de contagem são configuráveis (`LOGIN_LOCKOUT_DURATION`, `LOGIN_ATTEMPTS_WINDOW`). **Pendente (Definir):** valores definitivos (provisório: 15 min e 15 min). | `LoginLockoutService`, `LoginService`, `LoginLockoutProperties`, `AccountLockedException` |
| RN06 | Completa | Sessão de 1 hora para todos os perfis (decisão do time): o refresh token expira em 1 h e é rotativo (uso único); o access token dura 30 min e é renovado pelo refresh. Reutilização de token revogado revoga todas as sessões do usuário, e a redefinição de senha encerra todas. A reautenticação para assinar receita (RN92) e exportar dados (RN75) será feita nessas funcionalidades. | `RefreshTokenService`, `JwtProperties`, `PasswordRecoveryService`, `.env.example` |
| RN07 | Completa | Senhas guardadas só como hash (delegating encoder, BCrypt) e refresh tokens só como hash SHA-256. Contadores de tentativas e códigos temporários ficam no Redis com expiração automática; chaves e códigos são armazenados como hash, nunca em texto aberto. | `SecurityConfig`, `TokenHasher`, `RedisTemporaryStorage`, `TemporaryCounter`, `TemporaryValue` |
| RN08 | Completa | `POST /api/auth/logout` revoga o refresh token (idempotente). A confirmação de saída é do front. Não haverá token de push do aparelho (decisão de 10/11/2026). | `LoginController`, `TokenService`, `RefreshTokenService` |
| RN09 | Completa | `POST /api/auth/password/forgot` recebe o e-mail e devolve sempre a mesma resposta (202), exista ou não a conta; o código só é gerado e enviado se a conta existir. | `PasswordRecoveryController`, `PasswordRecoveryService` |
| RN10 | Completa | Código de seis dígitos gerado com `SecureRandom`, enviado por e-mail via Resend, de uso único, com limite de tentativas (5) e de reenvios (3 por hora). A resposta informa a validade em segundos para a tela exibir o tempo restante. **Pendente (Definir):** validade definitiva (provisório: 30 min, proposta do rascunho) e limites. | `RecoveryCodeService`, `PasswordRecoveryEmails`, `ResendEmailSender`, `PasswordRecoveryProperties` |
| RN11 | Completa | `POST /api/auth/password/reset` exige nova senha e confirmação (RN02) e só grava a senha após validar o código. Encerra as demais sessões e envia aviso por e-mail sem dados de saúde. | `PasswordRecoveryService`, `ResetPasswordRequest` |

Não implementadas: nenhuma regra do tópico 1 sem alguma entrega; as lacunas estão descritas nas linhas acima (RN04, RN05 e RN10).

## 2. Cadastro, conta e perfil

| RN | Situação | O que está implementado | Onde |
|---|---|---|---|
| RN12 | Completa | No app, cadastro apenas como PACIENTE ou CUIDADOR (`/api/auth/register`). Médico se cadastra em rota própria (`/api/auth/register/doctor`). Administrador não tem autocadastro: é criado na subida pelas variáveis `BOOTSTRAP_ADMIN_*`, com senha de no mínimo 12 caracteres. | `RegistrationController`, `RegistrationRequest`, `AdminBootstrap` |
| RN13 | Completa | Uma única rota de cadastro no aplicativo (`POST /api/auth/register`); o tipo (PACIENTE ou CUIDADOR) vai no corpo (`role`). O cadastro não aceita cuidador, responsável nem representante, e cada pessoa cria a própria conta. O vínculo vem depois, por convite (RN40 e RN41). | `RegistrationController`, `RegistrationRequest` |
| RN16 | Completa | CPF validado (11 dígitos, dígitos verificadores) e único, nome, data de nascimento (passada), telefone (10 ou 11 dígitos) e e-mail único, confirmado conforme a RN30. Entradas normalizadas. A foto é um link `https://` (até 500 caracteres) opcional no cadastro e editável em `PATCH /api/users/me` (texto vazio remove); o sistema não armazena o arquivo. | `RegistrationRequest`, `DoctorRegistrationRequest`, `UpdateProfileRequest`, `PhotoUrl`, `UserUniquenessValidator`, `Normalization`, V8 |
| RN24 | Completa | Senha e confirmação precisam ser iguais; do contrário o cadastro é recusado. | `RegistrationRequest`, `DoctorRegistrationRequest` |
| RN25 | Completa | Documentos publicados pelo administrador por **tipo** (`TERMS_OF_USE`, `PRIVACY_POLICY`, `SPECIFIC_CONSENT`) e **público** (`ALL` ou `SPECIFIC` com a lista de perfis). A versão numérica é única e cada tipo tem sua versão vigente por perfil. O cadastro exige o aceite da versão vigente de todo documento aplicável ao perfil (`termsVersion`, `privacyPolicyVersion`, `specificConsentVersion`) e, para paciente/cuidador, o consentimento de dados de saúde. Nova publicação faz o login indicar `termsAccepted = false` até o novo aceite (`POST /api/terms/accept`); versões antigas ou de outro perfil são recusadas. `GET /api/terms?type=&role=` e `GET /api/terms/signup?role=` listam o que o front deve exibir. | `TermsAcceptanceService`, `TermsService`, `TermsVersion`, `TermsController`, V2, V8 |
| RN27 | Completa | Médico informa nome, CPF, telefone, e-mail, CRM (só dígitos, até 10), UF válida, senha com confirmação e aceita os Termos. | `DoctorRegistrationRequest`, `RegistrationService` |
| RN28 | Completa | Formato do CRM e da UF verificado. Conta nasce com `crmVerified = false`. O administrador lista os médicos pendentes (`GET /api/admin/doctors/pending-crm`) e confere o registro manualmente (`POST /api/admin/doctors/{id}/verify-crm`, idempotente); a ação fica registrada com autor e hora (RN37, RN39). **Falta:** bloqueio de emissão de receita para conta não verificada (módulo de prescrição). **Pendente (Definir):** manter a conferência manual ou consultar a base do conselho. | `DoctorCrmVerificationService`, `DoctorCrmController`, `AdminAction`, V7 |
| RN30 | Completa | O cadastro envia um código de seis dígitos para o e-mail (Resend). `POST /api/auth/email/confirm` confirma e `POST /api/auth/email/resend` reenvia com resposta idêntica para qualquer e-mail, sem criar outra conta. Se o envio falhar, o cadastro continua e o usuário pede novo código. A troca de e-mail volta a exigir confirmação. `emailConfirmed` aparece no perfil. Validade e limites configuráveis (provisório: 30 min). | `EmailConfirmationService`, `EmailConfirmationController`, `OneTimeCodeService` |
| RN31 | Completa | `GET` e `PATCH /api/users/me`: nome, data de nascimento, telefone e e-mail editáveis, com resposta de confirmação; CPF e CRM não são editáveis pelo usuário; a troca de e-mail exige nova confirmação e valida unicidade. Alterações de alergias, estado das alergias, doença declarada e contatos de emergência ficam registradas com autor, data e hora. CPF e CRM não são corrigíveis por ninguém (decisão). | `ProfileController`, `ProfileService`, `UpdateProfileRequest` |
| RN17 | Completa | `PUT/GET /api/users/me/address` guarda CEP, endereço, número, complemento, cidade e UF. `GET /api/addresses/cep/{cep}` consulta o ViaCEP e, se a busca falhar ou o CEP não existir, responde com mensagem pedindo o preenchimento manual (que sempre é permitido). **Falta:** ligar o endereço ao fluxo de cadastro no app. | `AddressService`, `ViaCepLookup`, `AddressController`, V5 |
| RN18 | Completa | Perfil de saúde do paciente com tipo sanguíneo (lista A+, A−, B+, B−, AB+, AB−, O+, O−), doença declarada, alergias, peso e altura e contatos de emergência (`/api/patients/me/...`, restrito ao perfil PATIENT). **Falta:** medicamentos em uso (RN83) e médico/cuidador (RN42), que dependem dos vínculos. | `PatientProfileController`, `PatientProfileService`, V6 |
| RN19 | Completa | Alergias por categoria (medicamentos, alimentos, contraste iodado, látex) com texto livre e gravidade (leve ou grave). Cada inclusão registra autor, data e hora (`GET /api/patients/me/changes`). **Pendência dos módulos futuros:** destaque da alergia grave no resumo e no SOS. O paciente só inclui alergias (cadastro, RN18); apenas o médico com vínculo ativo e CRM conferido inclui, edita e remove alergias e define o estado (`/api/doctors/me/patients/{id}/allergies`), com a alteração registrada em nome do médico. Ao remover a última alergia o estado volta a "não informada". | `AllergyService`, `DoctorClinicalService`, `DoctorPatientAccess`, `DoctorClinicalController`, `SensitiveChangeRecorder` |
| RN20 | Completa | Três estados: informada, nenhuma conhecida e não informada (padrão). "Não informada" nunca vira ausência de alergia; "nenhuma conhecida" só é aceita sem alergias cadastradas. **Pendência do módulo de prescrição:** aviso na emissão de receita (RN90). | `AllergyService`, `AllergyStatus` |
| RN21 | Completa | `PUT /api/patients/me/emergency-contacts` exige de 2 a 10 contatos com nome, relação, telefone e ordem de acionamento única; a substituição é registrada. A falta de contatos aparece na completude do perfil. **Falta:** exigir os contatos dentro do fluxo de cadastro no app. | `EmergencyContactService`, `ReplaceEmergencyContactsRequest` |
| RN22 | Completa | Peso e altura com histórico (data e autor). Dois níveis: fora do esperado, mas possível, responde 409 com `requiresConfirmation` e só salva com `confirmOutOfRange`; impossível responde 400. Faixas configuráveis (`WEIGHT_*` e `HEIGHT_*`). **Pendente (Definir):** faixas "esperada" (rascunho: 30–300 kg e 1,20–2,30 m) e "impossível" (provisório: 10–500 kg e 0,50–2,80 m), validadas com médicos. O perfil devolve o IMC (`bmi`, peso ÷ altura², uma casa decimal) a partir das últimas medidas; sem peso ou altura vem vazio. A classificação do IMC (faixas OPAS) fica fora até validação médica. **Falta:** meta hídrica e alerta de perda de peso (RN61 e RN73). | `MeasurementService`, `MeasurementValidator`, `MeasurementProperties`, `BodyMassIndex` |
| RN23 | Completa | A doença é registrada como declarada, com `declaredDiseaseValidated = false`. O paciente só a informa uma vez; depois, só o médico vinculado a altera (`PUT /api/doctors/me/patients/{id}/declared-disease`), o que a marca como validada e é registrado em nome dele (o paciente recebe 409 ao tentar alterar). **Pendência dos módulos futuros:** código CID-10 (RN71) e uso da condição declarada nas travas clínicas (RN61). | `PatientProfileService`, `PatientProfile` |
| RN26 | Completa | `GET /api/patients/me/profile` devolve `completeness` com `modulesUnlocked` (falso sem estado de alergia, peso ou altura) e a lista do que falta, incluindo os contatos de emergência. **Pendência dos módulos futuros:** medicação, hidratação e IMC consultarem esse indicador quando existirem. | `ProfileCompletenessService` |

Não implementadas: RN29 (V2). RN13 está atendida pela rota única de cadastro; RN14 e RN15 estão descritas no tópico 4.

## 3. Privacidade, segurança e auditoria

| RN | Situação | O que está implementado | Onde |
|---|---|---|---|
| RN32 | Completa | Autorização por perfil via JWT com claim de papel; rotas protegidas por padrão. Só o administrador publica Termos; `/api/doctors/**` é restrito ao médico, que só age sobre paciente com vínculo ativo e CRM conferido. Alergias e doenças já registradas só o médico altera ou remove (RN19, RN23). **Falta:** a matriz completa (Definir). | `SecurityConfig`, `RoleJwtAuthenticationConverter` |
| RN33 | Completa | Cadastro de médico não concede outras funções. O acesso a dados do paciente exige vínculo ativo, criado pelo fluxo de convite e aceite (RN40 a RN45) e checado por `DoctorPatientAccess`. | `RegistrationService`, `DoctorPatientAccess` |
| RN34 | Completa | Consentimento específico de dados de saúde exigido no cadastro de paciente e cuidador, com data (`dados_saude_consentido_em`). Revogação e consentimentos de geolocalização, fotos e áudios ainda não existem. | `RegistrationRequest`, `User` |
| RN37 | Completa | Termos publicados são imutáveis (nova publicação gera nova versão, com autor e data) e aceites são únicos por usuário e versão. **Falta:** trilha de auditoria dos demais eventos. | `TermsService`, `TermsAcceptance`, V2 |
| RN38 | Completa | E-mails do sistema (código de recuperação e aviso de senha alterada) carregam apenas código e texto genérico, sem dados de saúde. Sem `RESEND_API_KEY` o envio fica desabilitado. **Pendente (Definir com o jurídico):** transferência internacional de dados. | `PasswordRecoveryEmails`, `EmailSender`, `ResendEmailSender` |
| RN39 | Completa | Perfil administrador com permissões próprias (`/api/admin/**` restrito ao papel; publicar Termos; conferir CRM de médicos), com ações registradas em `admin_actions`. **Falta:** gestão de medicamentos e de solicitações de titulares. | `SecurityConfig`, `TermsController`, `DoctorCrmController` |

Não implementadas: RN35 e RN36.

## 4. Vínculos entre paciente, cuidador e médico

| RN | Situação | O que está implementado | Onde |
|---|---|---|---|
| RN40 | Completa | O cuidador convida o paciente pelo e-mail (`POST /api/caregiver-links/invitations`, resposta 202 igual para qualquer e-mail). O sistema envia e-mail só com texto genérico (RN38) e cria a notificação do paciente. | `CaregiverLinkService`, `LinkEmails`, `NotificationService` |
| RN41 | Completa | O paciente aceita ou recusa pelo aplicativo, a partir da lista de notificações (`GET /api/notifications`, `POST /api/caregiver-links/{id}/accept` ou `decline`). O convite nasce `PENDING` e o envio não conclui o vínculo. | `CaregiverLinkController`, `NotificationController` |
| RN42 | Completa | Paciente ou cuidador autorizado procura médico com CRM conferido (`GET /api/doctor-directory?q=`, mínimo de 3 caracteres, nome ou CRM) e pede o vínculo (`POST /api/doctor-links/requests`). O pedido fica pendente e escolher o médico não libera prontuário. | `DoctorDirectoryService`, `DoctorLinkService` |
| RN43 | Completa | O médico com CRM conferido convida o paciente pelo e-mail (`POST /api/doctor-links/invitations`). O paciente aceita ou recusa sem acesso imediato. | `DoctorLinkService` |
| RN44 | Completa | Convite para e-mail sem conta guarda o convite e orienta o cadastro por e-mail. Quando o paciente cria a conta e **confirma o e-mail**, o convite vira vínculo pendente, sem duplicar registros. | `PendingInvitationAttacher`, `EmailConfirmationService` |
| RN45 | Completa | O vínculo médico–paciente só fica `ACTIVE` com a confirmação do médico e a autorização do paciente. Pendente, recusado ou encerrado não libera acesso (`DoctorPatientAccess`). A situação é visível às partes em `GET /api/doctor-links`; o médico vê só os próprios vínculos. | `DoctorLinkService`, `DoctorPatientAccess` |
| RN46 | Completa | Pedido feito pelo cuidador exige vínculo ativo com o paciente e ainda precisa da autorização do paciente; o cuidador não aceita nem recusa pelo paciente. **Falta:** permissões por função dentro do vínculo (matriz em Definir, RN32) e representante autorizado (RN14). | `DoctorLinkService`, `CaregiverLinkService` |
| RN47 | Completa | O paciente é a referência comum: cada vínculo é uma linha própria, o médico não alcança outros pacientes e a troca de cuidador não herda nada. **Falta:** limitar o que o cuidador vê por permissão (matriz em Definir). | `CaregiverPatientLink`, `DoctorPatientLink` |
| RN48 | Completa | Médico e paciente encerram o vínculo médico–paciente; cuidador e paciente encerram o vínculo de cuidado (`POST .../{id}/end`). O acesso para na hora, o histórico é preservado e a outra parte é notificada. Decisão: depois do encerramento a parte perde o acesso ao histórico do paciente; a política de retenção (RN35) trata só da guarda dos registros. | `DoctorLinkService`, `CaregiverLinkService` |
| RN15 | Completa | Implementada a proposta (a confirmar): um cuidador acompanha vários pacientes e um paciente tem vários cuidadores; cada par é um vínculo independente. **Falta:** permissões próprias por vínculo (Definir). | `CaregiverPatientLink` |

Não implementada: RN14 (representante do paciente, registrado fora do cadastro).

## 5 a 12. Demais módulos

Esta parte do mapa ainda está incompleta. As entregas documentadas de acompanhamento de saúde e jogos aparecem nas seções ao final. Entre as lacunas conferidas no backend estão a exportação do resumo clínico (RN75), a cerca virtual (RN80), os quatro jogos de seleção e arraste (RN109) e a respiração guiada (RN111).

## Nomes em inglês

Código, JSON da API, roles (PATIENT, CAREGIVER, DOCTOR, ADMINISTRATOR) e esquema do banco (migration V4) estão em inglês; apenas as mensagens devolvidas ao usuário ficam em português.

## Endpoints atuais

| Método e rota | Acesso | RNs |
|---|---|---|
| `POST /api/auth/register` | Público | RN12, RN16, RN24, RN25, RN34 |
| `POST /api/auth/register/doctor` | Público | RN12, RN24, RN25, RN27, RN28 |
| `POST /api/auth/login` | Público | RN01, RN04, RN05 |
| `POST /api/auth/2fa/verify` | Público | RN04 |
| `POST /api/auth/email/confirm` | Público | RN30 |
| `POST /api/auth/email/resend` | Público | RN30 |
| `GET /api/users/me` | Autenticado | RN31 |
| `PATCH /api/users/me` | Autenticado | RN31 |
| `GET/PUT /api/users/me/address` | Autenticado | RN17 |
| `GET /api/addresses/cep/{cep}` | Autenticado | RN17 |
| `GET/PUT /api/patients/me/profile` | Paciente | RN18, RN23, RN26 |
| `GET/POST /api/patients/me/allergies` | Paciente | RN19, RN20 |
| `PUT /api/patients/me/allergy-status` | Paciente | RN20 |
| `GET/POST /api/patients/me/measurements` | Paciente | RN22 |
| `GET/PUT /api/patients/me/emergency-contacts` | Paciente | RN21 |
| `GET /api/patients/me/changes` | Paciente | RN19, RN31, RN37 |
| `POST /api/auth/refresh` | Público | RN06 |
| `POST /api/auth/logout` | Público | RN08 |
| `POST /api/auth/password/forgot` | Público | RN09, RN10, RN38 |
| `POST /api/auth/password/reset` | Público | RN02, RN06, RN10, RN11, RN38 |
| `GET /api/terms` (`?type=&role=`) | Público | RN25 |
| `GET /api/terms/signup?role=` | Público | RN25 |
| `GET /api/users/me/mfa` | Autenticado | RN04 |
| `POST /api/users/me/mfa/authenticator/setup`, `/confirm`, `/disable` | Autenticado | RN04 |
| `POST/PUT/DELETE /api/doctors/me/patients/{id}/allergies[/{allergyId}]` | Médico | RN19, RN32, RN33 |
| `PUT /api/doctors/me/patients/{id}/allergy-status` | Médico | RN19, RN20, RN32 |
| `PUT /api/doctors/me/patients/{id}/declared-disease` | Médico | RN23, RN32 |
| `POST /api/caregiver-links/invitations` | Cuidador | RN40, RN44 |
| `GET /api/caregiver-links` | Paciente, Cuidador | RN45, RN47 |
| `POST /api/caregiver-links/{id}/accept`, `/decline` | Paciente | RN41 |
| `POST /api/caregiver-links/{id}/end` | Paciente, Cuidador | RN48 |
| `GET /api/doctor-directory?q=` | Paciente, Cuidador | RN42 |
| `POST /api/doctor-links/requests` | Paciente, Cuidador | RN42, RN46 |
| `POST /api/doctor-links/invitations` | Médico | RN43, RN44 |
| `GET /api/doctor-links` | Médico, Paciente, Cuidador | RN45, RN51 |
| `POST /api/doctor-links/{id}/accept`, `/decline`, `/end` | Médico, Paciente | RN41, RN45, RN48 |
| `GET /api/notifications`, `POST /api/notifications/{id}/read` | Autenticado | RN41 |
| `GET /api/terms/history` | Autenticado | RN25 |
| `GET /api/terms/accepted` | Autenticado | RN25 |
| `POST /api/terms/accept` | Autenticado | RN25 |
| `POST /api/terms` | Administrador | RN25, RN37, RN39 |
| `GET /api/admin/doctors/pending-crm` | Administrador | RN28, RN39 |
| `POST /api/admin/doctors/{id}/verify-crm` | Administrador | RN28, RN37, RN39 |
| `GET /api/care/patients/{patientId}/vital-signs` | Paciente, cuidador representante, médico vinculado | RN36, RN58 |
| `POST /api/care/patients/{patientId}/vital-signs` | Paciente, cuidador representante | RN56, RN66 |
| `GET /api/care/patients/{patientId}/routine` (`?type=`) | Paciente, cuidador representante, médico vinculado | RN36, RN59, RN70 |
| `POST /api/care/patients/{patientId}/routine` | Paciente, cuidador representante | RN59, RN66 |

## Pendências de (Definir) que afetam o código atual

- RN04: validade e limites do código do segundo fator por e-mail (provisório 10 min) e recuperação do segundo fator se o aplicativo autenticador for perdido. Configurar `MFA_ENCRYPTION_KEY` (Base64, 32 bytes) para habilitar o autenticador.
- RN30: validade e limites do código de confirmação de e-mail (provisório 30 min).
- RN05 e RN10: duração e contagem do bloqueio (provisório 15 min) e validade/limites do código de recuperação (provisório 30 min, 5 tentativas, 3 envios por hora), todos configuráveis no `.env`.
- RN38: transferência internacional de dados (Resend e hospedagem).
- RN22: faixas de peso e altura (esperada e impossível), a validar com médicos.
- RN23: versão do CID adotada.
- RN32: matriz de permissões (alergias: quem edita e remove).
- RN28: forma de conferência do CRM.
- RN15, RN46, RN47: permissões próprias por vínculo (o que o cuidador pode ver e fazer) dependem da matriz de permissões (RN32).
- RN14: representante autorizado do paciente (documentação exigida) ainda não modelado; hoje só o próprio paciente autoriza.

## Entregas da rodada de 19/11/2026

- RN04: códigos de backup do autenticador (uso único, só hash), volta ao e-mail com e-mail e senha (aviso por e-mail), 3 envios por janela de 15 min com bloqueio (429 com `Retry-After`), 2º fator obrigatório e não desativável também para o administrador. Rotas: `POST /api/auth/2fa/recover`, `POST /api/users/me/mfa/backup-codes/regenerate`.
- RN05 e RN10: valores definidos (15 min de bloqueio e janela; 30 min, 5 tentativas e 3 reenvios por hora), configuráveis por env.
- RN22: limites de peso e altura fixos no código (`MeasurementLimits`), sem env.
- RN17 e RN21: endereço obrigatório (paciente e cuidador) e dois contatos de emergência (só paciente) no corpo de `POST /api/auth/register`.
- RN18: `GET /api/patients/me/profile` traz `careTeam` (médicos e cuidadores com vínculo ativo).
- RN28: conferência atrás da interface `CrmConfirmation` (hoje `ManualCrmConfirmation`, decisão do administrador); trocar pelo webservice do CFM depois.
- RN36: tabela imutável `access_events`, registro dos acessos do médico e consulta paginada com filtro de período pelo paciente (`GET /api/patients/me/access-log`) e pelo representante (`GET /api/caregivers/me/patients/{id}/access-log`).
- RN37: tabela imutável `audit_events` (só IDs, valores antes/depois, `on_behalf_of_user_id`), registro em autenticação, 2º fator, vínculos, termos, alergias, doença, CRM e ações do administrador; errata justificada em `POST /api/admin/audit-events/{id}/errata`. Imutabilidade garantida por trigger e `REVOKE` no PostgreSQL (no H2 de teste vale a entidade `@Immutable` e o repositório sem delete/update).
- RN14, RN32, RN46, RN47: o cuidador com vínculo ativo é representante; aceita/recusa convites de cuidador e vínculos com médicos e aceita termos em nome do paciente (`POST /api/caregivers/me/patients/{id}/terms/accept`). Encerrar o vínculo encerra a representação. Alterar alergias e doenças registradas segue só com o médico.
- RN12 e RN39: administrador único (índice único parcial no PostgreSQL), listagem de usuários por perfil só com dados de conta, inativação/reativação com justificativa (conta inativa não entra, sessões revogadas), métricas agregadas (`GET /api/admin/users`, `POST /api/admin/users/{id}/deactivate|reactivate`, `GET /api/admin/metrics`). O administrador não se inativa e não há rota de exclusão.
- RN16: idade máxima de 100 anos na data de nascimento (cadastro e edição de perfil).

Pendências restantes: gestão de medicamentos e de solicitações de titulares (RN39), destaque da alergia grave no SOS e aviso na receita (RN19/RN20), medicamentos em uso (RN18), CID e trava hídrica (RN23), transferência internacional de dados e documentação do representante (jurídico), retenção e eliminação (RN35).

## Entregas da rodada de 23/11/2026

- RN56: aferições de pressão (sistólica e diastólica juntas), frequência cardíaca, glicemia (com tipo jejum ou pós-prandial), temperatura, SpO₂ e observações; valores fora do espectro humano são bloqueados. **Pendente (Definir):** os limites são configuráveis em `app.vital-signs.*` e os padrões atuais são provisórios, a validar por médicos.
- RN58: histórico de aferições, do mais novo ao mais antigo, com data, hora e autor; consultas de cuidador e médico entram no registro de acessos (RN36).
- RN66 (parcial): cada aferição guarda a origem (digitada, ditada ou OCR) e as de OCR saem marcadas com `checkAgainstOriginal`.
- RN57 segue pendente: faixas do semáforo dependem de validação médica.

- RN59: registro de rotina e fisiologia (`routine_records`, V16) com humor (alegria, calma, agitação, tristeza, agressividade), sono (qualidade e horas), alimentação por período (aceitou ou recusou), hidratação (ml), banho (cooperação), urina (frequência e aspecto) e fezes (frequência e escala de Bristol de 1 a 7). Cada tipo tem sua regra de validação (`RoutineRule`), então um novo tipo entra sem mexer nas demais. O horário do banho e dos demais eventos vai em `occurredAt` (padrão: agora, nunca no futuro); o momento do registro é automático. **Pendente (Definir):** limites (sono 0–24 h, até 5000 ml por registro, frequências de 0 a 30) e os aspectos da urina (transparente, amarela, escura, turva, com sangue) são provisórios, a validar por médicos.
- RN66 (parcial): a origem (digitada, ditada ou OCR) também é guardada na rotina, e as de OCR saem com `checkAgainstOriginal`.
- RN36: a leitura da rotina por cuidador e médico entra no registro de acessos (área `ROUTINE`).
- RN61 segue pendente: o registro de hidratação existe, mas a meta hídrica e as travas (condição renal ou cardíaca) ainda não. RN60 (escala de dor) e RN62 a RN65 (ocorrências) seguem para a próxima etapa.
- O acesso por papel (paciente e cuidador representante gravam; médico vinculado só lê) foi extraído para `PatientCareAccess`, compartilhado com as aferições.


## Jogo de reconhecimento de familiares

- RN105: cadastro de familiares e liberação da partida com pelo menos quatro pessoas.
- RN106 e RN107: partida com uma pergunta por familiar, quatro opções, nova tentativa após erro, áudio opcional após o acerto e mensagem de conclusão. Após a primeira partida concluída, o jogo prioriza opções com o mesmo grau de parentesco quando a galeria permite.
- RN108 (parcial): erros e tempo de resposta são registrados por pergunta e ficam fora das respostas das partidas.
- Rotas para o paciente e o cuidador com vínculo ativo: `POST /api/care/patients/{patientId}/games/family-recognition/sessions`; sob a mesma base, `GET /{sessionId}/question` e `POST /{sessionId}/answers` (corpo com `position` e `memberId`).

## Relatório médico de desempenho nos jogos

- RN72 e RN108: `GET /api/doctors/me/patients/{patientId}/game-performance` exige médico com CRM conferido e vínculo ativo. Retorna, por jogo, o total de partidas concluídas, questões respondidas, erros e tempo médio de resposta em milissegundos. O relatório abrange Reconhecimento de Familiares e Bingo Musical e inclui aviso de que o desempenho não é diagnóstico e não substitui a avaliação neuropsicológica.
- RN36: a leitura do relatório entra no registro de acessos na área `GAME_PERFORMANCE`.
- RN108 segue parcial para os quatro jogos da RN109 e para a apresentação no app.
