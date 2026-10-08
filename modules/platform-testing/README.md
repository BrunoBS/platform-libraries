# Platform Testing

Os módulos de teste são organizados por contexto para que cada serviço traga apenas as fixtures que usa. `platform-testing` é somente um agregador Maven: os artefatos consumíveis são os sete JARs abaixo. Todos são usados com escopo `test`.

| Artefato | Responsabilidade | Dependências próprias |
|---|---|---|
| `platform-testing-core` | Arquitetura, ciclo de vida, `TestContext`, builders, factories, cenários, relógio e IDs | ArchUnit, JUnit, AssertJ, Spring Test, Mockito Extension e SLF4J |
| `platform-testing-http` | `@PlatformIntegrationTest`, clients e requests RestAssured | Spring Boot Web, RestAssured e Jackson |
| `platform-testing-authorization` | `@WithMockAuthorization` e mock do serviço de autorização | `platform-authorization` e WireMock |
| `platform-testing-kafka` | `@WithKafka`, container e provisionamento de tópicos | Spring Kafka, Testcontainers Kafka e `platform-messaging` |
| `platform-testing-database` | `@WithMySql`, scripts, limpeza e ciclo de vida do banco | JDBC, driver MySQL, Testcontainers MySQL e `platform-messaging` |
| `platform-testing-cloud-aws` | LocalStack, SQS, S3 e notificações S3→SQS | Testcontainers LocalStack, AWS SDK SQS/S3 e `platform-messaging` |
| `platform-testing-cloud-azure` | Azurite, Service Bus e evento Blob→Service Bus | Testcontainers Azure/SQL Server, Azure SDKs e `platform-messaging` |

## Dependências Maven

Importe o BOM da plataforma no `dependencyManagement` do serviço e declare somente os contextos usados. Por exemplo, para HTTP, MySQL e autorização simulada:

```xml
<dependencies>
    <dependency>
        <groupId>br.com.portalmanager.platform.library</groupId>
        <artifactId>platform-testing-http</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>br.com.portalmanager.platform.library</groupId>
        <artifactId>platform-testing-database</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>br.com.portalmanager.platform.library</groupId>
        <artifactId>platform-testing-authorization</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

`platform-testing-authorization` já depende de `platform-testing-core` e `platform-authorization`. Os módulos HTTP, Kafka, database, AWS e Azure também dependem do core, sem dependência entre si. O módulo Authorization permanece isolado e não é dependência do Core; assim não há ciclo entre `platform-authorization` e fixtures de teste.

## Escopos e imagens

As dependências de cada integração são transitivas a partir do artefato correspondente. Datafaker não é uma API do Platform Testing; serviços que o usam devem declará-lo diretamente. JUnit, AssertJ, Mockito e Spring Test para as suites vêm de `platform-testing-core`. Cada módulo declara apenas as dependências específicas de teste que seus próprios testes usam; elas não são exportadas para os consumidores. As imagens padrão ficam junto do módulo que as utiliza; a validação comum de referências versionadas fica no Core.

`platform-testing-database` usa MySQL via Testcontainers e não inclui H2. H2 não faz parte de nenhum artefato `platform-testing`; se um serviço precisar de H2 em testes, deve declará-lo diretamente no próprio projeto de testes.

## Mapa do código

Cada artefato mantém seus pacotes sob `br.com.portalmanager.platform.library.testing`, por contexto. O agregador não contém classes Java nem dependências de runtime. A árvore e os detalhes das APIs estão em [Responsabilidades por pacote e classe](docs/PACKAGES_AND_CLASSES.md); exemplos completos estão no [Guia de uso](docs/GUIA_DE_USO.md).
