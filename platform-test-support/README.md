# Platform Test Support

O `platform-test-support` é a biblioteca compartilhada para padronização dos testes de integração dos microsserviços da plataforma.

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

- Java 21 ou superior;
- Spring Boot compatível com a versão utilizada pelo `platform-libraries`;
- Docker em execução para testes que utilizam Testcontainers;
- acesso ao repositório Maven em que as bibliotecas da plataforma são publicadas.

## 1. Adicionar a dependência

Adicione o módulo no `pom.xml` do microsserviço com escopo `test`:

```xml
<dependency>
    <groupId>com.empresa.platform</groupId>
    <artifactId>platform-test-support</artifactId>
    <version>${platform-libraries.version}</version>
    <scope>test</scope>
</dependency>
```

Exemplo da propriedade de versão:

```xml
<properties>
    <platform-libraries.version>1.0.0-SNAPSHOT</platform-libraries.version>
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

import com.empresa.platform.testing.annotation.PlatformIntegrationTest;
import com.empresa.platform.testing.annotation.WithMySql;
import org.junit.jupiter.api.Test;

@PlatformIntegrationTest
@WithMySql
class AccountControllerIT {

    @Test
    void deveCriarConta() {
        AccountClient.client()
                .create(AccountFactory.valid())
                .expectCreated()
                .expectNotNull("id");
    }
}
```

`@PlatformIntegrationTest` oferece:

- contexto completo do Spring Boot;
- servidor HTTP em porta aleatória;
- profile `test` ativo;
- porta do RestAssured configurada automaticamente;
- novo correlation ID para cada teste;
- limpeza do contexto do teste ao final da execução.

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

### Resposta totalmente customizada

```java
authorizationMock.custom(
        429,
        "{\"message\":\"Limite excedido\"}"
);
```

## Clients de teste

O módulo fornece `BaseClient` para centralizar a configuração comum do RestAssured:

```java
public final class AccountClient extends BaseClient {

    private static final String BASE_PATH = "/api/v1/accounts";

    private AccountClient() {
        super();
    }

    public static AccountClient client() {
        return new AccountClient();
    }

    public AccountResponse create(AccountRequest request) {
        return new AccountResponse(
                given()
                        .spec(spec)
                        .body(request)
                        .when()
                        .post(BASE_PATH)
                        .then()
        );
    }
}
```

O client específico do domínio permanece no microsserviço. A biblioteca fornece somente a configuração HTTP compartilhada.

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
AccountClient.client()
        .create(AccountFactory.valid())
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

`AbstractTestDataFactory` conhece apenas o contrato do builder. Estados inválidos ou especiais pertencem ao microsserviço:

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
| Inicialização do Spring Boot | `platform-test-support` |
| MySQL e Kafka Testcontainers | `platform-test-support` |
| Mock do serviço de autorização | `platform-test-support` |
| Limpeza genérica do banco | `platform-test-support` |
| Configuração do RestAssured | `platform-test-support` |
| Validações HTTP genéricas | `platform-test-support` |
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

    @Test
    void deveCriarContaValida() {
        CatalogScenario.builder()
                .withAccountTypes()
                .withOnboardingPhases()
                .setup();

        AccountClient.client()
                .create(AccountFactory.valid())
                .expectCreated()
                .expectNotNull("id");
    }
}
```
