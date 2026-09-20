# Platform Libraries — módulos da Foundation

## Baseline obrigatório

O `platform-starter` agrega somente:

- `platform-logging`
- `platform-messaging`
- `platform-authorization`

`platform-catalog`, `platform-audit` e `platform-tagging` permanecem capacidades explícitas e opcionais.

## Revisão F5

### platform-logging

Responsabilidade transversal de logging estruturado. Não depende de outras libraries da plataforma. A integração web é opcional. Permanece no baseline obrigatório.

### platform-messaging

Responsabilidade transversal de mensagens, i18n e tratamento padronizado de exceções. JDBC e Redis são integrações opcionais. O caminho sem JDBC utiliza `NoOpApiMessageRepository`; a dependência `spring-tx` fornece apenas os tipos Spring DAO usados pelo tratamento de exceções. Permanece no baseline obrigatório.

### platform-authorization

Responsabilidade transversal de autorização e contexto do usuário. Depende de messaging para o contrato corporativo de erros. A correção de `RestClient.Builder` permanece via `spring-boot-starter-restclient`. O comportamento aprovado é preservado: autorização continua habilitada por padrão; consumidores mínimos de teste podem definir `platform.authorization.enabled=false`. Permanece no baseline obrigatório.

### platform-audit

Capacidade transversal explícita para publicação de auditoria. Depende de authorization e messaging. Redis é opcional e condicionado ao fallback solicitado. Não entra transitivamente pelo starter.

### platform-catalog

Capacidade específica para catálogos persistidos e gerenciados. Depende de messaging, JPA e web. Após F3 é autocontida e não depende de CRUD genérico. Mantém somente conceitos próprios de catálogo: lifecycle active/inactive, restore, ordenação, busca por nome(s), filtros, validação e suporte a catálogos dynamic/enum. Não entra transitivamente pelo starter.

### platform-tagging

Capacidade transversal explícita de tagging persistido, com isolamento de owner e reconciliação de origem. Depende de Spring/JPA, sem dependência de outras libraries da plataforma. Não entra transitivamente pelo starter.

### platform-test-support

Infraestrutura reutilizável de testes. Contém suporte unitário, integração Spring, Testcontainers, MySQL, Kafka, WireMock, RestAssured, builders/factories/scenarios genéricos e validação arquitetural opt-in. Authorization e web são opcionais quando aplicável. É biblioteca de teste e não faz parte do starter de runtime.

## Estrutura após F4

O reactor contém:

- `platform-audit`
- `platform-messaging`
- `platform-authorization`
- `platform-logging`
- `platform-starter`
- `platform-test-support`
- `platform-catalog`
- `platform-tagging`

`platform-crud` foi removido.

## Auto-configuração

Auto-configurações registradas explicitamente:

- audit: core + Redis opcional;
- authorization: autorização;
- messaging: JDBC opcional + core + Redis opcional;
- tagging: tagging.

Logging utiliza initializer próprio; catalog e test-support são consumidos explicitamente e não introduzem auto-configuração global de runtime.

## Resultado da revisão

Nenhum módulo sobrevivente exige remoção ou grande reescrita para o checkpoint atual. As dependências internas permanecem coerentes com as responsabilidades documentadas. A F5 não introduz novas capacidades nem altera contratos funcionais aprovados.

Durante a revisão foram corrigidas referências documentais residuais ao `platform-crud` nos READMEs de Catalog e Messaging.

A validação Maven completa deve permanecer verde após estas correções documentais antes do fechamento da F5.
