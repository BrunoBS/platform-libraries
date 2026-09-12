# Platform Test Support

Biblioteca compartilhada para testes de integração dos microsserviços da plataforma.

## Uso

```xml
<dependency>
    <groupId>com.empresa.platform</groupId>
    <artifactId>platform-test-support</artifactId>
    <version>${platform-libraries.version}</version>
    <scope>test</scope>
</dependency>
```

```java
@PlatformIntegrationTest
@WithMySql
@WithKafka
class AccountControllerIT {
}
```

`@WithMySql` limpa as tabelas antes de cada teste por padrão. O comportamento pode ser alterado com `cleanup = CleanupMode.AFTER_EACH` ou `CleanupMode.NONE`.

O módulo contém somente infraestrutura genérica. Clients, builders, factories e scenarios específicos de cada domínio permanecem no microsserviço.
