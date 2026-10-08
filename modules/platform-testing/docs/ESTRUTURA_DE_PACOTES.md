# Estrutura de pacotes e módulos

O agregador `modules/platform-testing` contém sete artefatos, cada um com os pacotes Java do seu contexto. O namespace público continua sendo `br.com.portalmanager.platform.library.testing`.

| Artefato | Pacotes |
|---|---|
| `platform-testing-core` | `architecture`, `context`, `fixture`, `lifecycle`, `message`, `container` (validação genérica de imagem) |
| `platform-testing-http` | `http`, `http.response`, `lifecycle.annotation.PlatformIntegrationTest` |
| `platform-testing-authorization` | `authorization` e `authorization.annotation` |
| `platform-testing-kafka` | `kafka` e `kafka.annotation` |
| `platform-testing-database` | `database` e `database.annotation` |
| `platform-testing-cloud-aws` | `cloud.aws` e subpacotes `s3`, `sqs`, `s3notificationsqs` |
| `platform-testing-cloud-azure` | `cloud.azure` e subpacotes `blob`, `servicebus`, `blobservicebus` |

As anotações ficam em `annotation` dentro do contexto ao qual pertencem. Os pacotes mantêm suporte, configuração e anotações próximos do recurso ativado. HTTP mantém a anotação `PlatformIntegrationTest` no pacote `lifecycle.annotation` por compatibilidade com os imports públicos existentes.

## Dependências entre módulos

Cada módulo de funcionalidade depende de `platform-testing-core`. HTTP, Authorization, Kafka, Database, AWS e Azure não dependem uns dos outros. A exceção é `platform-testing-authorization`, que também compõe `platform-authorization`; o Core não importa Authorization.

`PlatformIntegrationTest` e os clients RestAssured ficam em HTTP, pois a anotação inicia o servidor aleatório e importa a configuração de request. O contexto de correlation ID permanece no Core, onde é compartilhado pelo ciclo de vida e pelas requests HTTP.

## Tipos reutilizáveis do core

| Uso no microsserviço | Tipo |
|---|---|
| Regra arquitetural para testes | `architecture.PlatformArchitectureExtension` e `architecture.annotation` |
| Contexto e correlation ID | `context.TestContext` |
| Builders e factories de teste | `fixture.builder` e `fixture.factory` |
| Preparação de pré-condições | `fixture.scenario.TestScenario` |
| Relógio e IDs determinísticos | `fixture.TestClock` e `fixture.TestIds` |
| Isolamento e métricas de teste | `lifecycle` e `lifecycle.annotation.PlatformUnitTest` |

Clients HTTP, fixtures de autorização, Kafka, banco e cloud são fornecidos somente pelo artefato correspondente. Consulte [o inventário de classes](PACKAGES_AND_CLASSES.md) e o [guia completo](GUIA_DE_USO.md) para exemplos.
