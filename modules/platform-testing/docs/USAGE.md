# Uso dos módulos de teste

Importe o BOM da plataforma no `dependencyManagement` e adicione cada artefato com escopo `test`. O agregador `platform-testing` organiza o build; não é publicado como dependência de aplicação.

| Necessidade | Artefato |
|---|---|
| Builders, lifecycle, contexto e testes arquiteturais | `platform-testing-core` |
| Anotação de integração HTTP, requests e clients RestAssured | `platform-testing-http` |
| Mock de chamadas ao Authorization | `platform-testing-authorization` |
| Kafka em Testcontainers | `platform-testing-kafka` |
| MySQL e scripts de banco | `platform-testing-database` |
| AWS LocalStack, SQS e S3 | `platform-testing-cloud-aws` |
| Emuladores Azure, Blob e Service Bus | `platform-testing-cloud-azure` |

Exemplo de seleção para uma API que usa HTTP, MySQL e mock de autorização:

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

Cada módulo funcional já traz o core. `platform-testing-authorization` também depende de `platform-authorization`; o core permanece independente dessa capability. Os guias detalhados mantêm exemplos de código, anotações e configurações:

- [Guia completo de uso](GUIA_DE_USO.md)
- [Comece aqui](COMECE_AQUI_TESTES.md)
- [Arquitetura e fronteiras dos módulos](ARCHITECTURE.md)
