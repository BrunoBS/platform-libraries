# Arquitetura do Platform Testing

`modules/platform-testing` é apenas o agregador Maven. Cada submódulo produz um JAR de teste independente, com dependências limitadas ao seu contexto. Não há dependência do core para autorização, banco, HTTP, mensageria ou nuvens.

## Artefatos e pacotes

| Artefato | Pacotes e funcionalidades |
|---|---|
| `platform-testing-core` | `architecture`, `context`, `fixture`, `lifecycle` e validação de imagem compartilhada |
| `platform-testing-http` | `http` e a anotação `lifecycle.annotation.PlatformIntegrationTest`, que compõe HTTP + Spring Boot |
| `platform-testing-authorization` | `authorization`, mock de autorização e `@WithMockAuthorization` |
| `platform-testing-kafka` | `kafka` e `kafka.annotation` |
| `platform-testing-database` | `database` e `database.annotation`, com MySQL/Testcontainers |
| `platform-testing-cloud-aws` | `cloud.aws`, LocalStack, SQS/S3 e notificações S3→SQS |
| `platform-testing-cloud-azure` | `cloud.azure`, Azurite, Service Bus e eventos Blob→Service Bus |

As APIs preservam o namespace Java `br.com.portalmanager.platform.library.testing`. Os pacotes são separados por contexto funcional e cada módulo declara diretamente as bibliotecas de que seus tipos públicos precisam.

## Dependências entre artefatos

```text
platform-testing-http ─────────┐
platform-testing-authorization ├──> platform-testing-core
platform-testing-kafka ────────┤
platform-testing-database ──────┤
platform-testing-cloud-aws ────┤
platform-testing-cloud-azure ──┘

platform-testing-kafka ─────────────> platform-messaging
platform-testing-database ───────────> platform-messaging
platform-testing-cloud-aws ──────────> platform-messaging
platform-testing-cloud-azure ────────> platform-messaging
platform-testing-authorization ──────> platform-authorization
```

O core fornece interfaces e utilitários comuns; não importa classes de feature. AWS e Azure não dependem uma da outra. `platform-authorization` não depende de nenhum artefato de teste, então a relação não forma ciclo. O agregador não entra no BOM e não é dependência do consumidor.

## Escopo das dependências

O core fornece JUnit, AssertJ, Spring Test, ArchUnit, SLF4J e Mockito Extension para APIs compartilhadas. Datafaker e `platform-messaging` não entram pelo core. Kafka, database, AWS e Azure declaram `platform-messaging` diretamente porque lançam erros de configuração específicos do contexto. RestAssured/Jackson ficam em HTTP; WireMock e a capability de autorização em authorization; Kafka/Testcontainers em kafka; JDBC, driver MySQL e Testcontainers MySQL em database; SDKs e containers de cada fornecedor no respectivo módulo cloud. As dependências de teste ficam somente nos módulos que usam suas APIs. As suites compartilham JUnit, AssertJ, Mockito e Spring Test através de `platform-testing-core`.

`platform-testing-database` fornece MySQL pelo Testcontainers e não declara H2. Os consumidores escolhem o artefato correspondente ao que usam em seus testes.

## Autoconfiguração de fixtures

Os `ContextCustomizerFactory` de Kafka, MySQL, AWS e Azure são registrados em `META-INF/spring.factories` dentro dos JARs que os implementam. Assim, declarar um artefato não inicia um container por si só: as anotações daquela feature ativam somente os recursos requeridos. Os factories cloud compartilham a implementação genérica de chave de cache do core, sem importar anotações dos fornecedores.

## Guias

- [Escolha de artefatos e exemplos](../README.md)
- [Guia rápido](COMECE_AQUI_TESTES.md)
- [Guia completo de uso das fixtures](GUIA_DE_USO.md)
- [Inventário de classes por pacote](PACKAGES_AND_CLASSES.md)
