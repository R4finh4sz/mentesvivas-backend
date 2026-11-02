# MentesVivas Backend

## Requisitos

- JDK 21
- PostgreSQL

## Como iniciar

1. Crie um banco de dados no PostgreSQL, por exemplo `mentesvivas`.

2. Informe a conexão com o banco por variáveis de ambiente.

   PowerShell:

   ```
   $env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/mentesvivas"
   $env:SPRING_DATASOURCE_USERNAME="postgres"
   $env:SPRING_DATASOURCE_PASSWORD="sua_senha"
   ```

   Git Bash / Linux / macOS:

   ```
   export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/mentesvivas"
   export SPRING_DATASOURCE_USERNAME="postgres"
   export SPRING_DATASOURCE_PASSWORD="sua_senha"
   ```

3. Inicie a aplicação:

   ```
   ./mvnw spring-boot:run
   ```

   No Windows (cmd/PowerShell): `mvnw.cmd spring-boot:run`

A API sobe em `http://localhost:8080`.
