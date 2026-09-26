# Platform Testing

O `platform-testing` é a biblioteca compartilhada para padronização dos testes unitários e de integração dos microsserviços da plataforma.

Se você está utilizando a biblioteca pela primeira vez, comece pelo [Comece aqui — Testes com platform-testing](COMECE_AQUI_TESTES.md).

Para referência completa de recursos e configurações, consulte o [Guia completo de uso](GUIA_DE_USO.md).

O módulo centraliza a inicialização do Spring Boot, Testcontainers, MySQL, Kafka, RestAssured, limpeza do banco e abstrações reutilizáveis para clients, responses, builders e factories de teste.

## Objetivo

Permitir que cada teste declare apenas a infraestrutura necessária:

```java
@PlatformIntegrationTest
@WithMySql
@WithKafka
class AccountControllerIT {
}
```

As classes específicas do domínio, como `AccountClient`, `AccountBuilder`, `AccountFactory` e `CatalogScenario`, continuam no microsserviço consumidor.

## Pré-requisitos

- Java 25;
- Spring Boot 4.1.1 no baseline atual;
- Docker em execução para testes que utilizam Testcontainers;
- acesso ao repositório Maven em que as bibliotecas da plataforma são publicadas.

## 1. Adicionar a dependência

Adicione o módulo no `pom.xml` do microsserviço com escopo `test`:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform</groupId>
    <artifactId>platform-testing</artifactId>
    <version>${platform-libraries.version}</version>
    <scope>test</scope>
</dependency>
```

Exemplo da propriedade de versão:

```xml
<properties>
    <platform-libraries.version>1.0.0</platform-libraries.version>
</properties>
```

O escopo `test` impede que a biblioteca e suas dependências sejam incluídas no artefato de produção do microsserviço.

## 2. Criar o profile de teste

Crie o arquivo:

```text
src/test/resources/application-test.yml
```

Configuração mínima sugerida:

```yaml
spring:
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: create-drop
    show-sql: false
```

Não é necessário informar URL, usuário ou senha do MySQL quando o teste utiliza `@WithMySql`. A conexão é registrada automaticamente a partir do container.

## 3. Criar um teste de integração

```java
package com.empresa.account.integration;

import br.com.portalmanager.platform.library.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.annotation.WithMySql;
import org.junit.jupiter.api.Test;

@PlatformIntegrationTest
@WithMySql
class AccountControllerIT {

    @Autowired
    private AccountClient accountClient;

    @Autowired
    private AccountFactory accountFactory;

    @Test
    void deveCriarConta() {
        accountClient
                .create(accountFactory.valid())
                .expectCreated()
                .expectNotNull("id");
    }
}
```

`@PlatformIntegrationTest` oferece:

- contexto completo do Spring Boot;
- servidor HTTP em porta aleatória;
- profile `test` ativo;
- fábrica de requests configurada com a porta HTTP do contexto;
- novo correlation ID para cada teste;
- limpeza do contexto do teste ao final da execução.

## Testes unitários

Use `@PlatformUnitTest` para testar services, validators, mappers e outras classes isoladas, sem inicializar o Spring:

```java
@PlatformUnitTest
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductService productService;

    @Test
    void deveBuscarProduto() {
        when(repository.findById(1L)).thenReturn(Optional.of(product));

        Product result = productService.findById(1L);

        assertThat(result).isEqualTo(product);
        verify(repository).findById(1L);
    }
}
```

A anotação configura o `MockitoExtension` e limpa o `TestContext` e o MDC antes e depois de cada teste. Ela não carrega Spring, banco, containers, WireMock ou servidor HTTP.

### Relógio determinístico

```java
Clock clock = TestClock.fixed("2026-09-12T12:00:00Z");
```

Também é possível informar o fuso:

```java
Clock clock = TestClock.fixed(
        "2026-09-12T12:00:00Z",
        ZoneId.of("America/Sao_Paulo")
);
```

### UUID determinístico

```java
UUID productId = TestIds.uuid("product-1");
```

A mesma seed sempre produz o mesmo UUID, facilitando fixtures e assertions previsíveis.

## MySQL

### Comportamento padrão

```java
@PlatformIntegrationTest
@WithMySql
class AccountRepositoryIT {
}
```

`@WithMySql`:

1. inicializa um container MySQL 8;
2. registra o datasource no contexto Spring;
3. limpa as tabelas antes de cada teste;
4. preserva `flyway_schema_history` por padrão;
5. encerra o container junto com o contexto Spring.

### Modos de limpeza

O comportamento padrão é:

```java
@WithMySql(cleanup = CleanupMode.BEFORE_EACH)
```

Para limpar depois de cada teste:

```java
@WithMySql(cleanup = CleanupMode.AFTER_EACH)
```

Para desativar a limpeza automática:

```java
@WithMySql(cleanup = CleanupMode.NONE)
```

O modo `NONE` é útil quando o teste controla manualmente o estado do banco.

### Preservar tabelas adicionais

```java
@WithMySql(
        excludeTables = {
                "flyway_schema_history",
                "account_types",
                "environment_types"
        }
)
```

As tabelas informadas em `excludeTables` não são truncadas durante a limpeza.

> Testes que compartilham o mesmo banco não devem ser executados em paralelo quando utilizam limpeza automática.

### Scripts opcionais e views

Use `@WithDatabaseScripts` quando o teste precisar criar e remover views, procedures, triggers ou preparar dados por SQL. A anotação utiliza o `DataSource` disponível no contexto Spring e não é acoplada ao MySQL:

```java
@PlatformIntegrationTest
@WithMySql
@WithDatabaseScripts(
        setup = "classpath:sql/views/create-catalog-views.sql",
        cleanup = "classpath:sql/views/drop-catalog-views.sql"
)
class CatalogRepositoryIT {
}
```

Os arquivos pertencem ao microsserviço consumidor:

```text
src/test/resources/sql/views/create-catalog-views.sql
src/test/resources/sql/views/drop-catalog-views.sql
```

Exemplo de criação:

```sql
CREATE OR REPLACE VIEW vw_active_catalogs AS
SELECT * FROM catalogs WHERE active = true;
```

Exemplo de limpeza idempotente:

```sql
DROP VIEW IF EXISTS vw_active_catalogs;
```

Por padrão, o `setup` é executado antes da classe e o `cleanup` depois da classe. Para executar os mesmos scripts em cada cenário da classe:

```java
@WithDatabaseScripts(
        setup = "classpath:sql/scenarios/create-data.sql",
        cleanup = "classpath:sql/scenarios/remove-data.sql",
        setupPhase = DatabaseSetupPhase.BEFORE_EACH,
        cleanupPhase = DatabaseCleanupPhase.AFTER_EACH
)
```

Quando a execução ocorre por método, a ordem é: limpeza genérica do MySQL, `setup`, teste, `cleanup` e, quando configurada, limpeza genérica posterior. O `DatabaseCleaner` continua truncando somente tabelas; a remoção de views e outros objetos é responsabilidade do script de `cleanup`.

Também é possível declarar um script diretamente no método. Nesse caso, o setup e o cleanup sempre envolvem somente aquele teste:

```java
@Test
@WithDatabaseScripts(
        setup = "classpath:sql/scenarios/create-pending-account.sql",
        cleanup = "classpath:sql/scenarios/delete-pending-account.sql"
)
void deveProcessarContaPendente() {
}
```

A anotação é repetível e pode combinar scripts estruturais e scripts de cenário. Um caminho inexistente ou sem permissão de leitura interrompe imediatamente o teste com uma mensagem contendo o recurso inválido.

Os scripts são opcionais. A biblioteca não tenta gerar automaticamente o SQL inverso, e interrompe o teste no primeiro erro por padrão. `continueOnError = true` deve ser usado somente quando o cenário aceitar falhas parciais.

## Kafka

Adicione `@WithKafka` somente nos testes que necessitam do broker:

```java
@PlatformIntegrationTest
@WithKafka
class AccountEventPublisherIT {

    @Test
    void devePublicarEvento() {
        // execução e validação do cenário
    }
}
```

`@WithKafka` inicializa o container e registra automaticamente o endereço do broker no contexto Spring.

### MySQL e Kafka no mesmo teste

```java
@PlatformIntegrationTest
@WithMySql
@WithKafka
class AccountFlowIT {
}
```

Os containers são independentes. O teste utiliza somente as anotações correspondentes à infraestrutura necessária.

## Mock de autorização

Use `@WithMockAuthorization` nos testes que consomem a `platform-authorization`:

```java
@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class AccountControllerIT {

    @Autowired
    private AuthorizationMock authorizationMock;
}
```

A anotação inicia um WireMock em porta aleatória, configura automaticamente `platform.authorization.service-url` e registra uma autorização permitida antes de cada teste.

### Comportamento padrão da classe

```java
@WithMockAuthorization
```

Equivale a:

```java
@WithMockAuthorization(
        defaultResult = AuthorizationMockResult.ALLOWED
)
```

Também é possível definir outro comportamento inicial:

```java
@WithMockAuthorization(
        defaultResult = AuthorizationMockResult.DENIED
)
```

Valores disponíveis:

- `ALLOWED`;
- `DENIED`;
- `FORBIDDEN`;
- `INTERNAL_ERROR`.

### Alterar o comportamento dentro do teste

```java
authorizationMock.allow();
authorizationMock.deny();
authorizationMock.forbidden();
authorizationMock.internalError();
authorizationMock.expiredSession();
```

Também é possível verificar se o microsserviço chamou o autorizador com o contrato esperado:

```java
authorizationMock.verifyCalled();
authorizationMock.verifyCalled(1);
authorizationMock.verifyNotCalled();
authorizationMock.verifyCalledWithAccount("account-123");
authorizationMock.verifyCalledWithEnvironment("DEV");
authorizationMock.verifyCalledWithApplication("application-456");
authorizationMock.verifyCalledWithPolicy("ADMIN");
```

### Customizar a sessão autorizada

```java
authorizationMock.allow(session -> session
        .userName("bruno.barbosa")
        .email("bruno@empresa.com")
        .accountId("account-123")
        .applicationId("application-456")
        .environmentId("environment-789")
        .groups("PM5_OWNER", "ADMIN")
        .addAuthorizerGroup(
                "GRP_APP_DEV_ADMIN",
                "ADMIN",
                "DEV",
                "APP"
        ));
```

O `AuthorizationSessionBuilder` produz diretamente o `UserSession` da `platform-authorization`, mantendo o mock alinhado ao contrato utilizado em produção.

Não é necessário herdar nem implementar um helper de autorização. O endpoint, os códigos de resposta, a serialização e a configuração do WireMock são fornecidos integralmente pela biblioteca; o microsserviço informa somente os dados variáveis da sessão.

### Sessão padrão do microsserviço

Quando vários testes utilizam a mesma sessão, o microsserviço pode declarar um customizador funcional:

```java
@TestConfiguration(proxyBeanMethods = false)
class AccountAuthorizationTestConfiguration {

    @Bean
    AuthorizationSessionCustomizer accountTestSession() {
        return session -> session
                .userName("integration-account")
                .accountId("account-123")
                .applicationId("account-api")
                .environmentId("DEV")
                .groups("PM5_OWNER");
    }
}
```

Importe essa configuração no teste que utilizar o autorizador:

```java
@PlatformIntegrationTest
@WithMockAuthorization
@Import(AccountAuthorizationTestConfiguration.class)
class AccountControllerIT {
}
```

Todo teste com `@WithMockAuthorization` utilizará esse customizador para o resultado `ALLOWED`. É possível declarar mais de um customizador; o Spring aplica todos na ordem configurada. Um teste ainda pode substituir o comportamento do cenário chamando `authorizationMock.allow(session -> ...)`.

O recurso é opcional: sem `@WithMockAuthorization`, nenhum WireMock, bean ou propriedade de autorização é criado. A dependência da `platform-authorization` também é marcada como opcional no `platform-testing`; o microsserviço só precisa dela quando efetivamente utiliza o autorizador.

### Resposta totalmente customizada

```java
authorizationMock.custom(
        429,
        "{\"message\":\"Limite excedido\"}"
);
```

## Clients de teste

O módulo fornece `PlatformRequestSpecificationFactory`. A fábrica resolve a porta do servidor para cada request e não altera o estado global do RestAssured, permitindo execução paralela de contextos HTTP diferentes.

Por padrão, a especificação contém somente JSON e `X-Correlation-Id`:

```java
RequestSpecification request = requests.create();
```

Os headers de autorização são adicionados explicitamente:

```java
RequestSpecification request = requests.createAuthorized(
        AuthorizationRequestData.builder()
                .token("token-valido")
                .accountId("account-123")
                .environment("DEV")
                .applicationId("account-api")
                .build()
);
```

O client do domínio pode usar composição:

```java
@Component
public final class AccountClient {

    private static final String BASE_PATH = "/api/v1/accounts";
    private final PlatformRequestSpecificationFactory requests;

    public AccountClient(PlatformRequestSpecificationFactory requests) {
        this.requests = requests;
    }

    public AccountResponse create(AccountRequest request) {
        return new AccountResponse(
                RestAssured.given()
                        .spec(requests.createAuthorized())
                        .body(request)
                        .when()
                        .post(BASE_PATH)
                        .then()
        );
    }
}
```

`BaseClient` permanece disponível como conveniência, mas exige a fábrica no construtor; herança não é obrigatória. Customizações comuns podem ser declaradas como beans de `PlatformRequestSpecificationCustomizer`.

## Responses fluentes

Crie uma response específica estendendo `BaseResponse`:

```java
public final class AccountResponse extends BaseResponse<AccountResponse> {

    public AccountResponse(ValidatableResponse response) {
        super(response);
    }

    public AccountDTO extractBody() {
        return extract(AccountDTO.class);
    }
}
```

Isso permite escrever validações fluentes:

```java
accountClient
        .create(accountFactory.valid())
        .expectCreated()
        .expectNotNull("id")
        .expect("name", "Conta de teste");
```

Validações disponíveis:

- `expectOk()`;
- `expectCreated()`;
- `expectNoContent()`;
- `expectBadRequest()`;
- `expectUnauthorized()`;
- `expectForbidden()`;
- `expectNotFound()`;
- `expectConflict()`;
- `expect2xx()`;
- `expect4xx()`;
- `expect5xx()`;
- `expect(path, value)`;
- `expectNotNull(path)`;
- `expectContains(path, value)`;
- `expectSize(path, size)`.

Para a ação principal do teste, prefira validar o status HTTP exato. As validações por família, como `expect2xx()`, são mais apropriadas para preparação de cenários.

## Builders

A biblioteca não define campos de negócio. O microsserviço cria seu builder estendendo `AbstractTestDataBuilder` e declara somente os atributos do próprio domínio:

```java
public final class AccountBuilder
        extends AbstractTestDataBuilder<AccountRequest, AccountBuilder> {

    private String name = "Conta de teste";
    private String description = "Conta válida para teste de integração";
    private boolean active = true;

    public static AccountBuilder builder() {
        return new AccountBuilder();
    }

    public AccountBuilder withName(String name) {
        this.name = name;
        return self();
    }

    public AccountBuilder withDescription(String description) {
        this.description = description;
        return self();
    }

    public AccountBuilder inactive() {
        this.active = false;
        return self();
    }

    @Override
    public AccountRequest build() {
        return new AccountRequest(name, description, active);
    }
}
```

Também está disponível o contrato mínimo `TestDataBuilder<T>` quando o projeto preferir implementar uma interface em vez de herdar a classe abstrata.

## Factories

O contrato principal é `TestDataFactory<T>` e pode ser implementado sem herança:

```java
public final class AccountFactory implements TestDataFactory<AccountRequest> {

    @Override
    public AccountRequest valid() {
        return AccountBuilder.builder().build();
    }
}
```

`AbstractTestDataFactory` permanece como conveniência e também implementa esse contrato. Estados inválidos ou especiais pertencem ao microsserviço:

```java
public final class AccountFactory
        extends AbstractTestDataFactory<AccountRequest, AccountBuilder> {

    @Override
    protected AccountBuilder builder() {
        return AccountBuilder.builder();
    }

    public AccountRequest withoutName() {
        return builder()
                .withName(null)
                .build();
    }

    public AccountRequest inactive() {
        return builder()
                .inactive()
                .build();
    }
}
```

A implementação herdada de `valid()` executa `builder().build()`. Nenhuma regra como `withoutName`, `withoutDescription` ou campos de catálogo é imposta pela biblioteca.

## Scenarios

Um cenário concreto pode implementar `TestScenario<R>`, em que `R` representa o contexto preparado:

```java
public record AccountScenarioResult(
        Long accountTypeId,
        Long environmentId
) {
}
```

```java
public final class AccountScenario
        implements TestScenario<AccountScenarioResult> {

    @Override
    public AccountScenarioResult setup() {
        Long accountTypeId = createAccountType();
        Long environmentId = createEnvironment();
        return new AccountScenarioResult(accountTypeId, environmentId);
    }
}
```

Uso no teste:

```java
AccountScenarioResult scenario = new AccountScenario().setup();
```

## Organização recomendada no microsserviço

```text
src/test/java/com/empresa/account
├── client
│   ├── AccountClient.java
│   └── response
│       └── AccountResponse.java
├── builder
│   └── AccountBuilder.java
├── factory
│   └── AccountFactory.java
├── scenario
│   └── AccountScenario.java
└── integration
    └── AccountControllerIT.java
```

## Responsabilidades

| Responsabilidade | Local |
|---|---|
| Configuração dos testes unitários | `platform-testing` |
| Inicialização do Spring Boot | `platform-testing` |
| MySQL e Kafka Testcontainers | `platform-testing` |
| Mock do serviço de autorização | `platform-testing` |
| Limpeza genérica do banco | `platform-testing` |
| Fábrica de requests RestAssured | `platform-testing` |
| Validações HTTP genéricas | `platform-testing` |
| Clients de endpoints específicos | Microsserviço |
| Builders e factories do domínio | Microsserviço |
| Scenarios de negócio | Microsserviço |
| Casos de teste | Microsserviço |

## Execução

Para executar os testes unitários:

```bash
mvn test
```

Para executar todo o ciclo de verificação, incluindo testes `*IT` quando o Maven Failsafe estiver configurado:

```bash
mvn verify
```

## Exemplo completo

```java
@PlatformIntegrationTest
@WithMySql(
        cleanup = CleanupMode.BEFORE_EACH,
        excludeTables = "flyway_schema_history"
)
@WithKafka
class AccountControllerIT {

    @Autowired
    private AccountClient accountClient;

    @Autowired
    private AccountFactory accountFactory;

    @Test
    void deveCriarContaValida() {
        CatalogScenario.builder()
                .withAccountTypes()
                .withOnboardingPhases()
                .setup();

        accountClient
                .create(accountFactory.valid())
                .expectCreated()
                .expectNotNull("id");
    }
}
```


## Performance da suíte

O `platform-testing` instrumenta testes com `@PlatformIntegrationTest` e registra métricas com o prefixo `[TEST-PERF]`.

Ao final de cada classe são registrados o tempo total, a quantidade de testes e os cinco cenários mais lentos. Testes individuais acima do limite configurado geram `WARN`.

O limite padrão é 2 segundos e pode ser alterado na execução:

```bash
mvn verify -Dplatform.testing.performance.slow-test-ms=3000
```

O `DatabaseCleaner` também mede o tempo de limpeza em nível `DEBUG`. A descoberta de tabelas no `INFORMATION_SCHEMA` é cacheada por URL JDBC e catálogo durante a JVM de testes. A limpeza continua ocorrendo em cada cenário conforme o `CleanupMode`; apenas a descoberta repetitiva da estrutura é eliminada.

Quando `@WithDatabaseScripts` executa scripts SQL, o cache de metadados do banco é invalidado automaticamente. O consumidor não precisa conhecer nem gerenciar esse cache no fluxo normal.

A recomendação continua sendo manter os testes HTTP isolados e evitar execução paralela quando diferentes testes compartilham o mesmo banco com limpeza automática.


## Validação arquitetural genérica

A validação é opt-in. O serviço consumidor ativa a regra uma única vez:

```java
@PlatformArchitectureTest(basePackages = "com.minhaempresa.meuservico")
class ArchitectureTest {

    @Test
    void customizedPlatformBehaviorMustHaveTests() {
    }
}
```

Por padrão, a regra observa classes-base dos pacotes `br.com.portalmanager.platform`. Qualquer classe concreta da aplicação que sobrescreva comportamento declarado por uma classe ou interface da plataforma precisa ter cobertura específica.

A cobertura pode ser reconhecida por convenção:

```text
MinhaClasseTest
MinhaClasseIntegrationTest
MinhaClasseIT
```

ou explicitamente quando um único teste cobre várias classes:

```java
@CoversClasses({
    MeuService.class,
    MeuValidator.class
})
class MeuFluxoIntegrationTest {
}
```

O mecanismo é genérico e não conhece catálogo, CRUD, autorização, messaging ou qualquer domínio específico. Ele apenas detecta sobrescritas de comportamento herdado das bibliotecas observadas.

Também é possível customizar os pacotes-base observados:

```java
@PlatformArchitectureTest(
    basePackages = "com.minhaempresa.meuservico",
    observedBasePackages = {
        "br.com.portalmanager.platform",
        "com.minhaempresa.framework"
    }
)
class ArchitectureTest {
}
```

Quando uma classe customiza comportamento e não possui cobertura reconhecida, o build falha mostrando a classe, o método sobrescrito e a classe/interface onde o comportamento foi originalmente declarado.

A regra não é habilitada automaticamente pelo `platform-testing`; cada serviço decide explicitamente se quer adotá-la.
