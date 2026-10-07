# Guia de uso do platform-testing

Este guia mostra como adicionar o módulo aos testes de um microsserviço. Para exemplos mais extensos de builders, factories e CRUD, consulte [COMECE_AQUI_TESTES.md](../COMECE_AQUI_TESTES.md) e [GUIA_DE_USO.md](../GUIA_DE_USO.md). A arquitetura e as responsabilidades internas estão em [ARCHITECTURE.md](ARCHITECTURE.md).

## Pré-requisitos

- Java 25, conforme o baseline atual da plataforma.
- Spring Boot compatível com o BOM da plataforma.
- Docker em execução para testes que usam Testcontainers.
- O repositório Maven da plataforma configurado no projeto consumidor.

## Dependência Maven

Declare a biblioteca com escopo `test`. Quando o BOM estiver importado, não informe uma versão própria:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform.library</groupId>
    <artifactId>platform-testing</artifactId>
    <scope>test</scope>
</dependency>
```

Sem o BOM, declare a versão alinhada à release de `platform-libraries`.

Kafka, AWS, Azure e as dependências do container MySQL são fornecidos transitivamente. O consumidor declara apenas `platform-testing` com escopo `test`; os recursos continuam opt-in pelas anotações. O starter JDBC permanece opcional para não ativar a auto-configuração de `DataSource` em serviços sem banco. Serviços que já usam JPA normalmente já recebem JDBC pela dependência de persistência.

## Migração dos imports cloud

As anotações de serviço agora ficam no pacote da feature. Atualize os imports dos consumidores ao usar a nova versão:

| Anotação | Pacote anterior | Pacote atual |
|---|---|---|
| `AwsS3` | `testing.cloud.aws.annotation` | `testing.cloud.aws.s3.annotation` |
| `AwsSqs` | `testing.cloud.aws.annotation` | `testing.cloud.aws.sqs.annotation` |
| `AwsS3SqsNotification` | `testing.cloud.aws.annotation` | `testing.cloud.aws.s3notificationsqs.annotation` |
| `AzureBlobStorage` | `testing.cloud.azure.annotation` | `testing.cloud.azure.blob.annotation` |
| `AzureServiceBus` | `testing.cloud.azure.annotation` | `testing.cloud.azure.servicebus.annotation` |

As anotações agregadoras `WithAwsLocalStack` e `WithAzureEmulator` mantêm seus pacotes de provider.

## Teste de integração básico

`@PlatformIntegrationTest` inicializa Spring Boot com profile `test`; por padrão, usa servidor HTTP em porta aleatória:

```java
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import org.junit.jupiter.api.Test;

@PlatformIntegrationTest
class HealthControllerIT {

    @Test
    void deveCarregarContexto() {
    }
}
```

O padrão é `RANDOM_PORT`, indicado para testes que fazem chamadas HTTP. Para testes de integração de repositório ou mensageria que não precisam de servidor web, use `webEnvironment = SpringBootTest.WebEnvironment.NONE` e evite abrir uma porta HTTP:

```java
@PlatformIntegrationTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@WithMySql
class ProductRepositoryIT {
}
```

Importe `org.springframework.boot.test.context.SpringBootTest` para usar o enum. Configure valores específicos do profile em `src/test/resources/application-test.yml`.

## Testes unitários

Use `@PlatformUnitTest` em testes que não precisam inicializar Spring:

```java
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformUnitTest;
import org.junit.jupiter.api.Test;

@PlatformUnitTest
class ProductServiceTest {

    @Test
    void deveValidarRegraDeNegocio() {
    }
}
```

A anotação habilita Mockito e a extensão de isolamento do módulo. Mantenha o teste unitário focado na classe e nas dependências mockadas.

## MySQL

`@WithMySql` é opt-in e inicializa o MySQL pelo Testcontainers. A limpeza padrão ocorre antes de cada método e preserva `flyway_schema_history`:

```java
@PlatformIntegrationTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@WithMySql
class ProductRepositoryIT {
}
```

Para alterar o ciclo de limpeza ou preservar tabelas de catálogo:

```java
@WithMySql(
    cleanup = CleanupMode.BEFORE_EACH,
    excludeTables = {"flyway_schema_history", "product_types"}
)
```

Os modos disponíveis são `BEFORE_EACH`, `AFTER_EACH` e `NONE`.

### Scripts SQL

Use `@WithDatabaseScripts` para criar views ou preparar dados específicos:

```java
@PlatformIntegrationTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@WithMySql
@WithDatabaseScripts(
    setup = {"classpath:sql/views/create-product-views.sql"},
    cleanup = {"classpath:sql/views/drop-product-views.sql"}
)
class ProductViewRepositoryIT {
}
```

Por padrão, setup e cleanup são executados antes/depois da classe. Para scripts por método:

```java
@WithDatabaseScripts(
    setup = {"classpath:sql/scenarios/create-product.sql"},
    cleanup = {"classpath:sql/scenarios/delete-products.sql"},
    setupPhase = DatabaseSetupPhase.BEFORE_EACH,
    cleanupPhase = DatabaseCleanupPhase.AFTER_EACH
)
```

Os atributos `setup` e `cleanup` aceitam arrays; reúna numa mesma anotação os arquivos que compartilham configuração. Repita `@WithDatabaseScripts` apenas para grupos com fases ou `continueOnError` diferentes. Scripts de cleanup devem ser idempotentes. Um recurso inexistente ou ilegível interrompe o teste.

## Kafka

A fixture Kafka inicia um container quando `@WithKafka` é declarada:

```java
@PlatformIntegrationTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@WithKafka
class ProductEventsIT {
}
```

As dependências Kafka necessárias já chegam transitivamente por `platform-testing`; não é necessário declará-las no POM consumidor.

O Spring Boot recebe a conexão do container por meio de Service Connection. Tópicos, serializers, producers e consumers continuam sendo responsabilidade do serviço.

## Autorização simulada

Adicione `platform-authorization` ao escopo de teste se o serviço usa a library de autorização. Depois, combine `@WithMockAuthorization` com o teste:

```java
@PlatformIntegrationTest
@WithMockAuthorization
class ProductAuthorizationIT {

    @Autowired
    private AuthorizationMock authorizationMock;

    @Test
    void permiteAcessoAoGrupo() {
        authorizationMock.allow(session ->
                session.groups("PM5_OWNER"));

        // Executa a chamada HTTP do serviço e verifica a resposta.
        authorizationMock.verifyCalled();
    }
}
```

O mock também oferece `deny()`, `forbidden()`, `internalError()`, `expiredSession()`, `verifyNotCalled()` e verificações dos headers workspace, environment, application e policy. Os stubs são reiniciados entre os testes.

## Clients HTTP e respostas

Injete `PlatformRequestSpecificationFactory` no client do serviço. Ela cria uma especificação nova por request e acrescenta correlation ID:

```java
@Component
final class ProductClient {

    private final PlatformRequestSpecificationFactory requests;

    ProductClient(PlatformRequestSpecificationFactory requests) {
        this.requests = requests;
    }

    ProductResponse findById(String id) {
        ValidatableResponse response = given()
                .spec(requests.createAuthorized())
                .when()
                .get("/api/v1/products/{id}", id)
                .then();

        return new ProductResponse(response);
    }
}
```

Use `create()` para chamadas sem headers de autorização da plataforma. Use `createAuthorized(AuthorizationRequestData)` quando precisar informar token ou identificadores explicitamente. O token recebe o prefixo `Bearer ` se ainda não estiver presente.

`BaseClient` e `BaseResponse` são helpers opcionais: estenda-os se reduzirem repetição no serviço; não é necessário criar uma hierarquia de clients para usar a factory.

## AWS LocalStack

Declare as filas e os buckets em suas anotações próprias. Quando precisar publicar eventos de criação do S3 no SQS, declare a relação com a terceira anotação:

```java
@PlatformIntegrationTest
@WithAwsLocalStack(
    sqs = @AwsSqs(queues = {
        @AwsSqs.Queue(name = "orders"),
        @AwsSqs.Queue(name = "order-events")
    }),
    s3 = @AwsS3(buckets = "order-files"),
    s3SqsNotifications = @AwsS3SqsNotification(
        bucket = "order-files",
        queue = "order-events"
    )
)
class OrderCloudIT {
}
```

O vínculo é opt-in: sem `s3SqsNotifications`, o bucket não publica eventos. A fixture configura a permissão da fila para o bucket e registra eventos `s3:ObjectCreated:*` no SQS declarado. Notificações S3 não podem ter como destino direto uma fila FIFO.

Para validar o fluxo, envie um objeto usando o `S3Client`, receba a mensagem pelo `SqsClient` e use `Records[0].s3.bucket.name` e `Records[0].s3.object.key` para ler o objeto. O teste de integração interno `cloud.aws.s3notificationsqs.AwsS3CreatedEventIntegrationTest` cobre esse percurso com LocalStack.

As dependências do LocalStack e dos SDKs SQS/S3 chegam transitivamente por `platform-testing`; não é necessário declará-las no POM consumidor. A mensagem do LocalStack valida o comportamento integrado do emulador; confirme o formato de evento e as permissões também contra AWS real antes de assumir paridade de produção.

## Azure Emulator

A fixture habilita Service Bus e/ou Blob Storage:

```java
@PlatformIntegrationTest
@WithAzureEmulator(
    serviceBus = @AzureServiceBus(
        queues = @AzureServiceBus.Queue(name = "orders")
    ),
    blobStorage = @AzureBlobStorage(containers = "order-files")
)
class OrderAzureIT {
}
```

As dependências do emulador Azure, SQL Server, Service Bus e Blob Storage chegam transitivamente por `platform-testing`; não é necessário declará-las no POM consumidor.

Declare somente o serviço utilizado na anotação. A fixture cria os recursos declarados e disponibiliza os clients com endpoints do emulador.

Para a POC local de Blob para Service Bus, configure `blobCreatedQueue` com uma fila também declarada em `serviceBus`:

```java
@WithAzureEmulator(
    serviceBus = @AzureServiceBus(
        queues = @AzureServiceBus.Queue(name = "blob-events")
    ),
    blobStorage = @AzureBlobStorage(
        containers = "order-files",
        blobCreatedQueue = "blob-events"
    )
)
```

Uploads feitos pelo `BlobServiceClient` fornecido pela fixture publicam uma mensagem no formato Event Grid Schema após uma gravação bem-sucedida. O teste de integração compara a estrutura com o exemplo de BlobCreated documentado pela Microsoft em `src/test/resources/azure/blob-created-event-grid-schema.json`, confere as propriedades de entrega do Service Bus e lê o blob pela URL do evento.

Esse exemplo documenta o formato, mas não é uma captura da assinatura Azure usada em produção. A ponte continua sendo uma emulação de teste: ela não emula o Event Grid real, assinaturas, filtros ou políticas de entrega. Para confirmar paridade exata com produção, compare o teste com uma mensagem real capturada do Service Bus Azure.

## Validação arquitetural opt-in

Use `@PlatformArchitectureTest` uma vez em uma classe do serviço:

```java
@PlatformArchitectureTest(
    basePackages = "com.company.product",
    observedBasePackages = {"br.com.portalmanager.platform"}
)
class ArchitectureTest {
}
```

A regra exige que classes concretas que sobrescrevem comportamento herdado da plataforma tenham cobertura no mesmo pacote, por convenção (`MinhaClasseTest`, `MinhaClasseIntegrationTest` ou `MinhaClasseIT`) ou por `@CoversClasses`.

A biblioteca não ativa essa verificação automaticamente. O pacote observado padrão é `br.com.portalmanager.platform`; configure `basePackages` para os pacotes da aplicação que deseja inspecionar.

## Clock e identificadores determinísticos

`TestClock.fixed(...)` fornece um `Clock` fixo em UTC ou em um fuso informado. `TestIds.uuid(seed)` gera o mesmo UUID para a mesma seed:

```java
Clock clock = TestClock.fixed("2026-01-15T10:00:00Z");
UUID id = TestIds.uuid("product-test-1");
```

## Executar os testes

Execute a suíte do serviço:

```bash
mvn test
mvn verify
```

Execute uma classe específica:

```bash
mvn -Dtest=ProductServiceTest test
mvn -Dit.test=ProductRepositoryIT verify
```

Testes com Testcontainers precisam de Docker disponível. Use `mvn verify` para executar as fases de integração configuradas pelo projeto consumidor.
