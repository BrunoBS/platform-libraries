# Foundation Architecture

## Estado atual

A Foundation está consolidada em `BrunoBS/platform-libraries`.

Namespace Maven e Java vigente:

```text
br.com.portalmanager.platform
```

Versão dos artifacts próprios no baseline atual:

```text
1.0.0
```

## Estrutura

```text
platform-libraries/
├── platform-parent/
├── platform-dependencies/
├── platform-libraries-bom/
└── modules/
    ├── platform-starter/
    ├── platform-observability/
    ├── platform-messaging/
    ├── platform-schema-validation/
    ├── platform-authorization/
    ├── platform-audit/
    ├── platform-catalog/
    ├── platform-tagging/
    └── platform-testing-core/
```

O root POM é somente reactor/aggregator.

## Eixos Maven

### platform-parent

Governança de build:

- Java 25;
- Maven mínimo;
- Compiler;
- Surefire/Failsafe;
- JaCoCo;
- Enforcer;
- dependency convergence;
- plugins comuns.

Importa somente `platform-dependencies`.

### platform-dependencies

BOM tecnológico externo.

Baseline atual:

- Spring Boot 4.1.1;
- Testcontainers gerenciado pelo Spring Boot, efetivamente 2.0.5;
- overrides externos explícitos somente quando necessários.

Não gerencia capabilities próprias.

### platform-libraries-bom

Gerencia a release train compatível das capabilities:

- platform-starter;
- platform-observability;
- platform-messaging;
- platform-schema-validation;
- platform-authorization;
- platform-audit;
- platform-catalog;
- platform-tagging;
- platform-testing-core.

## Starter

O starter agrega somente:

```text
platform-observability
platform-messaging
platform-authorization
```

Schema validation, audit, catalog e tagging são capabilities explícitas e não são introduzidas transitivamente pelo starter.

Ao declarar `platform-schema-validation`, o consumidor deve possuir `JdbcTemplate` para a fonte JDBC default ou fornecer um `ResourceSchemaRepository` customizado. A ausência de ambas as fontes é erro de configuração e impede o startup.

`platform-testing-core` é exclusivo de testes e não faz parte do runtime starter.

## Dependências internas

```text
starter -> observability
starter -> messaging
starter -> authorization

schema-validation -> messaging

authorization -> messaging

audit -> authorization
audit -> messaging

catalog -> messaging
```

`platform-tagging` é independente das demais capabilities de runtime.

## Observability

`platform-observability` é a fronteira de observabilidade.

No baseline atual implementa somente logging estruturado:

```text
br.com.portalmanager.platform.observability.logging
```

Prefixo de configuração:

```text
platform.observability.logging
```

Metrics e tracing não são adicionados sem caso real aprovado.

## Testing

`platform-testing-core` fornece infraestrutura reutilizável para:

- unit tests;
- Spring integration tests;
- MySQL/Kafka Testcontainers;
- WireMock authorization;
- RestAssured;
- database cleanup/scripts;
- fixtures;
- performance de testes;
- architecture guard opt-in.

Infraestrutura pesada permanece opt-in.

## Guardrails

- `platform-crud` permanece removido;
- `platform-catalog` continua independente de CRUD genérico;
- nenhum alias para `br.com.portalmanager.core`;
- nenhum alias para `platform-logging` ou `platform-test-support`;
- domínio de aplicação continua fora do namespace `platform`.

## Distribuição

Registry oficial:

```text
https://maven.pkg.github.com/brunobs/platform-libraries
```

Fluxo:

```text
Foundation clean verify
→ publish
→ consumer resolve remoto
→ consumer clean verify
```

A migração definida na ADR-004 só é fechada após publicação e revalidação do `account-service`.
