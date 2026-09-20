# Platform Libraries Modules

## F2 — Starter mínimo

Status: concluída.

O baseline obrigatório do `platform-starter` permanece:

- `platform-logging`
- `platform-messaging`
- `platform-authorization`

`platform-catalog`, `platform-audit` e `platform-tagging` permanecem capacidades opcionais e não são dependências do starter.

### Authorization

Foi preservado o comportamento atual de Authorization. No consumidor mínimo de validação, `platform.authorization.enabled=false` é configurado explicitamente. Esta configuração não representa configuração de capacidade opcional; Authorization faz parte do baseline obrigatório.

### Messaging sem DataSource

O consumidor mínimo revelou que `platform-messaging` propagava `spring-boot-starter-jdbc`, fazendo o Spring Boot tentar configurar um DataSource mesmo quando o consumidor não utiliza persistência de mensagens.

A correção preserva o comportamento existente:

- sem `JdbcTemplate`: `NoOpApiMessageRepository`;
- com `JdbcTemplate`: `JdbcApiMessageRepository`.

Para isso:

1. `spring-boot-starter-jdbc` passou a ser dependência opcional do `platform-messaging`;
2. a configuração JDBC foi isolada em `PlatformMessagingJdbcAutoConfiguration`, condicionada à presença de `JdbcTemplate`;
3. `PlatformMessagingAutoConfiguration` permanece carregável sem JDBC e fornece o fallback NoOp;
4. `spring-tx` passou a ser dependência explícita porque `ApiExceptionHandler` usa diretamente exceções de Spring DAO.

### Validação

A validação foi executada no GitHub Actions com Java 25 pelo workflow Verify, usando `mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify`.

Run #12 (`35527805316`) concluiu com `BUILD SUCCESS`.

Evidências relevantes:

- `PlatformMessagingAutoConfigurationTest`: 3 testes, 0 falhas, 0 erros;
- `PlatformStarterMinimalConsumerTest`: 1 teste, 0 falhas, 0 erros;
- suíte completa do reactor: sucesso.

O consumidor mínimo comprova startup sem DataSource/JDBC, usando o caminho `NoOpApiMessageRepository`.

## Próxima fase

Com F2 concluída e registrada, a execução segue para F3 — desacoplamento de `platform-catalog` de `platform-crud`.
