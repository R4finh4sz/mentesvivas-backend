# Testes manuais (Postman)

## Importar
1. No Postman (ou na extensao do VS Code), clique em **Import** e selecione os dois arquivos desta pasta:
   - `mentesvivas.postman_collection.json` (a colecao **MentesVivas API**)
   - `mentesvivas.postman_environment.json` (o ambiente **MentesVivas Local**)
2. Selecione o ambiente **MentesVivas Local**.
3. Preencha `adminEmail` e `adminPassword` no ambiente com os mesmos valores de `BOOTSTRAP_ADMIN_EMAIL` e `BOOTSTRAP_ADMIN_PASSWORD` do `.env`.

O `baseUrl` ja inclui o prefixo `/api` (`http://localhost:8080/api`); as rotas da colecao comecam depois dele.

## Pastas
- **1 - Publica**: rotas sem token. Termos por tipo, registro (paciente, cuidador, medico), confirmacao de e-mail, login (o medico tem dois passos), refresh, logout, recuperacao de senha e erros esperados.
- **2 - Admin**: login do admin, publicacao de termos por tipo e publico, conferencia de CRM dos medicos e erros 401/403.
- **3 - Autenticado**: termos aceitos, perfil e foto (link), endereco e CEP, segundo fator (e-mail ou aplicativo autenticador) e notificacoes.
- **4 - Paciente**: perfil de saude (IMC, alergias, peso e altura, contatos de emergencia, historico de alteracoes).
- **5 - Medico**: alergias e doenca do paciente vinculado (so o medico altera o que ja foi registrado).
- **6 - Vinculos**: cuidador e paciente, medico e paciente, convites, aceite, recusa e encerramento.

## Tokens e variaveis
Cada perfil tem seu token no ambiente: `adminToken`, `patientToken`, `caregiverToken` e `doctorToken`. Os requests de login preenchem os tokens, e o cadastro preenche `patientId`, `caregiverId` e `doctorId`. Outros ids (`allergyId`, `doctorLinkId`, `caregiverLinkId`, `notificationId`) sao preenchidos pelas listagens e criacoes.

Preencha a mao, no ambiente, os codigos que chegam por e-mail ou pelo aplicativo autenticador:

| Variavel | Quando usar |
|---|---|
| `emailCode` | Confirmar e-mail (1 - Publica > Confirmar e-mail) |
| `recoveryCode` | Redefinir senha |
| `twoFactorCode` | Login do medico, passo 2 |
| `mfaCode` | Confirmar ou desativar o aplicativo autenticador |

## Primeira execucao
O cadastro exige termos publicados, entao rode antes, uma unica vez:
1. **2 - Admin > Login > Login admin**
2. **2 - Admin > Termos > Publicar Termos de Uso (todos)**
3. Se quiser testar os outros documentos, publique tambem a Politica de Privacidade e o Consentimento Especifico. Depois de publicados, o cadastro passa a exigir `privacyPolicyVersion` e `specificConsentVersion` quando se aplicarem ao perfil.

Depois siga a ordem das pastas:
1. **1 - Publica**: registrar paciente, cuidador e medico; confirmar e-mail; logins. No login do medico, rode o passo 1, copie o codigo recebido por e-mail para `twoFactorCode` e rode o passo 2.
2. **2 - Admin > CRM**: conferir o CRM do medico. Sem isso o medico nao pode convidar pacientes nem editar dados clinicos.
3. **6 - Vinculos**: convide o paciente, aceite e crie o vinculo com o medico (o medico e o paciente precisam aceitar).
4. **4 - Paciente**, **5 - Medico** e **3 - Autenticado**.

O cadastro so funciona uma vez; para repetir, troque os e-mails no ambiente e os CPFs no corpo.

## Segundo fator por aplicativo autenticador
A chave `MFA_ENCRYPTION_KEY` precisa estar no `.env` (Base64 de 32 bytes, por exemplo `openssl rand -base64 32`). Em **3 - Autenticado > Segundo fator (MFA)**: configure (gera o segredo e o link `otpauth://`), cadastre no Microsoft Authenticator, informe o codigo em `mfaCode` e confirme. O mesmo codigo nao vale duas vezes; para desativar, espere o proximo codigo.

## Relatório de desempenho nos jogos

Em **5 - Medico (paciente vinculado) > Relatório de desempenho nos jogos**, use o token do médico com CRM conferido e o ID do paciente com vínculo ativo. A consulta retorna os totais por jogo e o tempo médio de resposta em milissegundos. Para um jogo sem partidas concluídas, os totais são zero e a média é `null`.

## Termos por tipo e publico
Em **2 - Admin > Termos**, `type` pode ser `TERMS_OF_USE`, `PRIVACY_POLICY` ou `SPECIFIC_CONSENT`, e `audience` pode ser `ALL` ou `SPECIFIC` (com a lista `roles`). Para testar a "nova versao": publique de novo, faca o login do paciente (`termsAccepted` vira `false`) e use **3 - Autenticado > Aceitar termos vigentes**.
