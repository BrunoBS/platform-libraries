# Platform Libraries — módulos da Foundation

## Estrutura consolidada

```text
platform-parent
platform-dependencies
platform-libraries-bom

platform-starter
platform-observability
platform-messaging
platform-authorization
platform-audit
platform-catalog
platform-tagging
platform-testing-core
```

Namespace:

```text
br.com.portalmanager.platform
```

Release atual:

```text
1.0.0
```

## Módulos estruturais

### platform-parent

Governança Maven e gates de build.

### platform-dependencies

Baseline tecnológico externo.

### platform-libraries-bom

Versões das capabilities da plataforma.

## Capabilities

### platform-starter

Agrega somente:

- platform-observability;
- platform-messaging;
- platform-authorization.

### platform-observability

Capability de observabilidade. No baseline atual implementa logging estruturado sob:

```text
br.com.portalmanager.platform.observability.logging
```

Não implementa métricas/tracing ainda.

### platform-messaging

Mensagens, i18n e tratamento padronizado de exceções. JDBC/Redis permanecem integrações opcionais.

### platform-authorization

Autorização e contexto do usuário. Depende de messaging.

### platform-audit

Publicação explícita de auditoria. Depende de authorization e messaging. Não entra pelo starter.

### platform-catalog

Catálogos persistidos/gerenciados. Independente de CRUD genérico. Não entra pelo starter.

### platform-tagging

Tagging persistido, owner isolation e reconciliação de origem. Não entra pelo starter.

### platform-testing-core

Capability de testing reutilizável:

- `@PlatformUnitTest`;
- `@PlatformIntegrationTest`;
- `@PlatformArchitectureTest`;
- MySQL/Kafka Testcontainers;
- WireMock authorization;
- RestAssured;
- database scripts/cleanup;
- fixtures/builders/factories/scenarios;
- métricas de performance de testes.

Dependências pesadas permanecem opcionais e só entram quando o consumidor declara a infraestrutura correspondente.

## Remoções e renames

```text
platform-crud         → removido
platform-logging      → platform-observability
platform-test-support → platform-testing-core
```

Os nomes antigos não são aliases e não fazem parte do baseline vigente.

## Validação

O reactor completo deve permanecer verde em Java 25 e o consumidor deve validar resolução remota antes do próximo checkpoint funcional.
