# Guia completo de uso — Platform Test Support

Este guia mostra como adicionar o `platform-test-support` a um microsserviço e criar testes de integração completos usando o padrão da plataforma.

Os exemplos utilizam uma API fictícia de produtos, mas a mesma organização pode ser aplicada a contas, aplicações, ambientes, rotas, menus ou qualquer outra feature.

## 1. O que a biblioteca fornece

| Recurso | Responsabilidade |
|---|---|
| `@PlatformIntegrationTest` | Inicializar o Spring Boot com servidor HTTP em porta aleatória |
| `@WithMySql` | Inicializar o MySQL e limpar as tabelas entre os testes |
| `@WithDatabaseScripts` | Executar scripts opcionais de setup e cleanup |
| `@WithKafka` | Inicializar um Kafka para o teste |
| `@WithMockAuthorization` | Simular o autorizador comum da plataforma |
| `PlatformRequestSpecificationFactory` | Criar requests RestAssured sem estado global |
| `BaseClient` | Implementação opcional para clients baseados em herança |
| `BaseResponse` | Fornecer validações HTTP fluentes |
| `TestDataBuilder<T>` | Contrato para builders de massa de teste |
| `TestDataFactory<T>` | Contrato para factories de cenários válidos |
| `TestScenario<R>` | Contrato para preparação de cenários compostos |

Nenhum recurso de infraestrutura é ativado automaticamente. O teste seleciona apenas as anotações de que precisa.

## 2. Dependência Maven

Adicione a biblioteca no `pom.xml` do microsserviço:

```xml
<properties>
    <platform-libraries.version>1.0.0-SNAPSHOT</platform-libraries.version>
</properties>

<dependencies>
    <dependency>
        <groupId>com.empresa.platform</groupId>
        <artifactId>platform-test-support</artifactId>
        <version>${platform-libraries.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

O escopo deve ser `test`. Assim, Testcontainers, WireMock, RestAssured e as demais ferramentas de teste não são incluídas no artefato de produção.

Se o microsserviço utiliza o autorizador, ele também deve possuir a dependência normal da `platform-authorization`:

```xml
<dependency>
    <groupId>com.empresa.platform</groupId>
    <artifactId>platform-authorization</artifactId>
    <version>${platform-libraries.version}</version>
</dependency>
```

## 3. Profile de teste

Crie o arquivo:

```text
src/test/resources/application-test.yml
```

Exemplo:

```yaml
spring:
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: create-drop
    show-sql: false

platform:
  authorization:
    enabled: false
```

Quando `@WithMySql` for utilizado, não informe URL, usuário ou senha do datasource. A conexão será registrada automaticamente pelo Testcontainers.

Quando `@WithMockAuthorization` for utilizado, a própria anotação habilitará o autorizador e substituirá a URL pelo endereço do WireMock.

## 4. Organização recomendada

```text
src/test
├── java/com/empresa/product
│   ├── builder
│   │   └── ProductRequestBuilder.java
│   ├── client
│   │   ├── ProductClient.java
│   │   └── response
│   │       └── ProductResponse.java
│   ├── config
│   │   └── ProductAuthorizationTestConfiguration.java
│   ├── factory
│   │   └── ProductFactory.java
│   ├── scenario
│   │   ├── ProductScenario.java
│   │   └── ProductScenarioResult.java
│   └── integration
│       └── ProductControllerIT.java
└── resources
    ├── application-test.yml
    └── sql
        ├── views
        │   ├── create-product-views.sql
        │   └── drop-product-views.sql
        └── scenarios
            ├── create-product.sql
            └── delete-products.sql
```

As classes relacionadas ao domínio continuam no microsserviço. A biblioteca fornece apenas os contratos e a infraestrutura comum.

## 5. Primeiro teste de integração

O teste mínimo para uma API HTTP é:

```java
package com.empresa.product.integration;

import com.empresa.platform.testing.annotation.PlatformIntegrationTest;
import org.junit.jupiter.api.Test;

@PlatformIntegrationTest
class HealthControllerIT {

    @Test
    void deveCarregarContexto() {
    }
}
```

`@PlatformIntegrationTest` configura:

- `@SpringBootTest`;
- servidor HTTP em porta aleatória;
- profile `test`;
- fábrica de requests RestAssured;
- um novo correlation ID para cada teste.

## 6. MySQL

Adicione `@WithMySql` somente quando o teste necessitar do banco:

```java
@PlatformIntegrationTest
@WithMySql
class ProductRepositoryIT {
}
```

Por padrão, todas as tabelas são truncadas antes de cada teste, exceto `flyway_schema_history`:

```java
@WithMySql(
    cleanup = CleanupMode.BEFORE_EACH,
    excludeTables = "flyway_schema_history"
)
```

### Modos de limpeza

```java
@WithMySql(cleanup = CleanupMode.BEFORE_EACH)
```

Limpa antes de cada teste. É o comportamento recomendado.

```java
@WithMySql(cleanup = CleanupMode.AFTER_EACH)
```

Limpa depois de cada teste.

```java
@WithMySql(cleanup = CleanupMode.NONE)
```

Desativa a limpeza automática.

### Preservar tabelas de catálogo

```java
@WithMySql(
    excludeTables = {
        "flyway_schema_history",
        "product_types",
        "status_types"
    }
)
```

As views não são truncadas pelo `DatabaseCleaner`. Elas continuam existindo e passam a refletir o estado atualizado das tabelas.

## 7. Scripts SQL e views

Use `@WithDatabaseScripts` quando o teste precisar de views, procedures, triggers ou dados SQL específicos.

### Script para toda a classe

```java
@PlatformIntegrationTest
@WithMySql
@WithDatabaseScripts(
    setup = "classpath:sql/views/create-product-views.sql",
    cleanup = "classpath:sql/views/drop-product-views.sql"
)
class ProductViewRepositoryIT {
}
```

Criação da view:

```sql
CREATE OR REPLACE VIEW vw_active_products AS
SELECT id, name, price
FROM products
WHERE active = TRUE;
```

Limpeza:

```sql
DROP VIEW IF EXISTS vw_active_products;
```

Por padrão:

- `setup`: antes de todos os testes da classe;
- `cleanup`: depois de todos os testes da classe.

### Script antes e depois de cada teste

```java
@WithDatabaseScripts(
    setup = "classpath:sql/scenarios/create-product.sql",
    cleanup = "classpath:sql/scenarios/delete-products.sql",
    setupPhase = DatabaseSetupPhase.BEFORE_EACH,
    cleanupPhase = DatabaseCleanupPhase.AFTER_EACH
)
```

### Script específico de um método

```java
@Test
@WithDatabaseScripts(
    setup = "classpath:sql/scenarios/create-inactive-product.sql",
    cleanup = "classpath:sql/scenarios/delete-products.sql"
)
void deveBuscarProdutoInativo() {
}
```

Em métodos, setup e cleanup envolvem somente aquele teste.

### Mais de um conjunto de scripts

`@WithDatabaseScripts` é repetível:

```java
@WithDatabaseScripts(
    setup = "classpath:sql/views/create-product-views.sql",
    cleanup = "classpath:sql/views/drop-product-views.sql"
)
@WithDatabaseScripts(
    setup = "classpath:sql/scenarios/create-catalogs.sql",
    cleanup = "classpath:sql/scenarios/delete-catalogs.sql"
)
class ProductRepositoryIT {
}
```

O setup segue a ordem declarada. O cleanup é executado na ordem inversa.

Utilize caminhos com o prefixo `classpath:` e scripts de cleanup idempotentes, como `DROP VIEW IF EXISTS` e `DELETE` com condições seguras. Um arquivo inexistente ou ilegível interrompe o teste imediatamente.

## 8. Criação dos requests HTTP

Injete `PlatformRequestSpecificationFactory` no client do microsserviço:

```java
package com.empresa.product.client;

import com.empresa.platform.testing.client.PlatformRequestSpecificationFactory;
import com.empresa.product.client.response.ProductResponse;
import com.empresa.product.web.dto.CreateProductRequest;
import io.restassured.RestAssured;
import org.springframework.stereotype.Component;

@Component
public final class ProductClient {

    private static final String BASE_PATH = "/api/v1/products";

    private final PlatformRequestSpecificationFactory requests;

    public ProductClient(PlatformRequestSpecificationFactory requests) {
        this.requests = requests;
    }

    public ProductResponse create(CreateProductRequest body) {
        return new ProductResponse(
            RestAssured.given()
                .spec(requests.create())
                .body(body)
                .when()
                .post(BASE_PATH)
                .then()
        );
    }

    public ProductResponse findById(Long id) {
        return new ProductResponse(
            RestAssured.given()
                .spec(requests.create())
                .pathParam("id", id)
                .when()
                .get(BASE_PATH + "/{id}")
                .then()
        );
    }
}
```

Uma nova especificação deve ser criada em cada operação. Não armazene o `RequestSpecification` em um campo singleton, pois o correlation ID pertence ao teste atual.

O request padrão contém:

- porta aleatória do servidor;
- `Content-Type: application/json`;
- `X-Correlation-Id`.

Ele não contém autorização automaticamente.

## 9. Requests autorizados

Para enviar os headers da plataforma, use `createAuthorized`:

```java
import com.empresa.platform.testing.client.AuthorizationRequestData;

public ProductResponse create(CreateProductRequest body) {
    AuthorizationRequestData authorization = AuthorizationRequestData.builder()
        .token("token-product-test")
        .accountId("account-123")
        .environment("DEV")
        .applicationId("product-api")
        .build();

    return new ProductResponse(
        RestAssured.given()
            .spec(requests.createAuthorized(authorization))
            .body(body)
            .when()
            .post(BASE_PATH)
            .then()
    );
}
```

Isso adiciona:

```text
Authorization: Bearer token-product-test
X-Account-Id: account-123
X-Environment: DEV
X-Application-Id: product-api
```

Para utilizar os valores genéricos da biblioteca:

```java
requests.createAuthorized();
```

### Customização comum de requests

O microsserviço pode fornecer customizações por composição:

```java
@TestConfiguration(proxyBeanMethods = false)
class ProductHttpTestConfiguration {

    @Bean
    PlatformRequestSpecificationCustomizer productHeaders() {
        return builder -> builder.addHeader("X-Feature", "PRODUCT");
    }
}
```

Importe a configuração no teste:

```java
@Import(ProductHttpTestConfiguration.class)
```

## 10. Usando `BaseClient`

O uso de herança é opcional. Caso o projeto prefira um client base, utilize:

```java
@Component
public final class ProductClient extends BaseClient {

    private static final String BASE_PATH = "/api/v1/products";

    public ProductClient(PlatformRequestSpecificationFactory requests) {
        super(requests);
    }

    public ProductResponse create(CreateProductRequest body) {
        return new ProductResponse(
            RestAssured.given()
                .spec(authorizedRequest())
                .body(body)
                .when()
                .post(BASE_PATH)
                .then()
        );
    }
}
```

Métodos protegidos disponíveis:

```java
request();
authorizedRequest();
authorizedRequest(authorizationRequestData);
```

Para novos clients, a composição com `PlatformRequestSpecificationFactory` é a opção preferencial.

## 11. Responses fluentes

Crie uma response específica para o domínio:

```java
package com.empresa.product.client.response;

import com.empresa.platform.testing.client.response.BaseResponse;
import com.empresa.product.web.dto.ProductResponseDTO;
import io.restassured.response.ValidatableResponse;

public final class ProductResponse extends BaseResponse<ProductResponse> {

    public ProductResponse(ValidatableResponse response) {
        super(response);
    }

    public ProductResponseDTO extractBody() {
        return extract(ProductResponseDTO.class);
    }
}
```

Uso:

```java
productClient.create(request)
    .expectCreated()
    .expectNotNull("id")
    .expect("name", "Notebook")
    .expect("active", true);
```

Status disponíveis:

```java
expectOk();
expectCreated();
expectNoContent();
expectBadRequest();
expectUnauthorized();
expectForbidden();
expectNotFound();
expectConflict();
expect2xx();
expect4xx();
expect5xx();
```

Validações de body:

```java
expect("name", "Notebook");
expectNotNull("id");
expectContains("description", "Produto");
expectSize("items", 3);
```

## 12. Builder da massa de teste

### Implementação por interface

```java
package com.empresa.product.builder;

import com.empresa.platform.testing.builder.TestDataBuilder;
import com.empresa.product.web.dto.CreateProductRequest;

import java.math.BigDecimal;

public final class ProductRequestBuilder
        implements TestDataBuilder<CreateProductRequest> {

    private String name = "Produto de integração";
    private BigDecimal price = new BigDecimal("100.00");
    private boolean active = true;

    public static ProductRequestBuilder builder() {
        return new ProductRequestBuilder();
    }

    public ProductRequestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ProductRequestBuilder withPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public ProductRequestBuilder inactive() {
        this.active = false;
        return this;
    }

    @Override
    public CreateProductRequest build() {
        return new CreateProductRequest(name, price, active);
    }
}
```

### Implementação pela classe abstrata

Se o projeto quiser o método `self()`:

```java
public final class ProductRequestBuilder
        extends AbstractTestDataBuilder<CreateProductRequest, ProductRequestBuilder> {

    private String name = "Produto de integração";

    public ProductRequestBuilder withName(String name) {
        this.name = name;
        return self();
    }

    @Override
    public CreateProductRequest build() {
        return new CreateProductRequest(name);
    }
}
```

Os campos e regras do domínio devem permanecer no microsserviço.

## 13. Factory de dados

O contrato principal não exige herança:

```java
package com.empresa.product.factory;

import com.empresa.platform.testing.factory.TestDataFactory;
import com.empresa.product.builder.ProductRequestBuilder;
import com.empresa.product.web.dto.CreateProductRequest;
import org.springframework.stereotype.Component;

@Component
public final class ProductFactory
        implements TestDataFactory<CreateProductRequest> {

    @Override
    public CreateProductRequest valid() {
        return ProductRequestBuilder.builder().build();
    }

    public CreateProductRequest withoutName() {
        return ProductRequestBuilder.builder()
            .withName(null)
            .build();
    }

    public CreateProductRequest inactive() {
        return ProductRequestBuilder.builder()
            .inactive()
            .build();
    }
}
```

Também existe `AbstractTestDataFactory<T, B>` como implementação conveniente:

```java
public final class ProductFactory
        extends AbstractTestDataFactory<CreateProductRequest, ProductRequestBuilder> {

    @Override
    protected ProductRequestBuilder builder() {
        return ProductRequestBuilder.builder();
    }
}
```

## 14. Cenários compostos

Use `TestScenario<R>` quando um teste precisar criar vários recursos antes da ação principal.

Resultado do cenário:

```java
package com.empresa.product.scenario;

public record ProductScenarioResult(
    Long categoryId,
    Long productId
) {
}
```

Implementação:

```java
package com.empresa.product.scenario;

import com.empresa.platform.testing.scenario.TestScenario;
import com.empresa.product.client.ProductClient;
import com.empresa.product.factory.ProductFactory;

public final class ProductScenario
        implements TestScenario<ProductScenarioResult> {

    private final ProductClient productClient;
    private final ProductFactory productFactory;

    public ProductScenario(
            ProductClient productClient,
            ProductFactory productFactory
    ) {
        this.productClient = productClient;
        this.productFactory = productFactory;
    }

    @Override
    public ProductScenarioResult setup() {
        Long categoryId = createCategory();
        Long productId = productClient
            .create(productFactory.valid())
            .expectCreated()
            .extractBody()
            .id();

        return new ProductScenarioResult(categoryId, productId);
    }

    private Long createCategory() {
        // Preparação específica da API de produtos.
        return 1L;
    }
}
```

Uso:

```java
ProductScenarioResult scenario = productScenario.setup();

productClient.findById(scenario.productId())
    .expectOk();
```

O cenário deve preparar estado. As validações principais continuam no método de teste.

## 15. Mock de autorização

Adicione a anotação somente nos testes que precisam do autorizador:

```java
@PlatformIntegrationTest
@WithMockAuthorization
class ProductControllerIT {
}
```

A biblioteca fornece:

- WireMock em porta dinâmica;
- endpoint `POST /authorize`;
- configuração automática da URL;
- resposta no formato `UserSession`;
- reset antes de cada teste.

### Sessão permitida padrão

```java
authorizationMock.allow();
```

### Sessão permitida customizada

```java
authorizationMock.allow(session -> session
    .userName("product.integration")
    .email("product.integration@empresa.com")
    .accountId("account-123")
    .applicationId("product-api")
    .environmentId("DEV")
    .groups("PM5_OWNER", "PRODUCT_ADMIN")
    .addAuthorizerGroup(
        "GRP_PRODUCT_DEV_ADMIN",
        "ADMIN",
        "DEV",
        "PRODUCT"
    ));
```

### Outros resultados

```java
authorizationMock.deny();
authorizationMock.forbidden();
authorizationMock.internalError();
authorizationMock.expiredSession();
authorizationMock.custom(429, "{\"message\":\"Limite excedido\"}");
```

Também é possível definir o resultado inicial da classe:

```java
@WithMockAuthorization(defaultResult = AuthorizationMockResult.FORBIDDEN)
```

### Sessão padrão do microsserviço

Crie uma configuração de teste:

```java
package com.empresa.product.config;

import com.empresa.platform.testing.authorization.AuthorizationSessionCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class ProductAuthorizationTestConfiguration {

    @Bean
    AuthorizationSessionCustomizer productSession() {
        return session -> session
            .userName("product.integration")
            .accountId("account-123")
            .applicationId("product-api")
            .environmentId("DEV")
            .groups("PM5_OWNER");
    }
}
```

Importe no teste:

```java
@Import(ProductAuthorizationTestConfiguration.class)
```

Não é necessário criar ou herdar um helper de autorização. O microsserviço customiza somente os dados da sessão.

### Verificar a chamada ao autorizador

```java
authorizationMock.verifyCalled();
authorizationMock.verifyCalled(1);
authorizationMock.verifyNotCalled();
authorizationMock.verifyCalledWithAccount("account-123");
authorizationMock.verifyCalledWithEnvironment("DEV");
authorizationMock.verifyCalledWithApplication("product-api");
authorizationMock.verifyCalledWithPolicy("ADMIN");
```

## 16. Kafka

Adicione `@WithKafka` quando o teste precisar publicar ou consumir mensagens:

```java
@PlatformIntegrationTest
@WithKafka
class ProductEventPublisherIT {

    @Autowired
    private KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

    @Test
    void devePublicarEvento() {
        kafkaTemplate.send(
            "product-created",
            new ProductCreatedEvent(1L, "Notebook")
        );

        // Aguarde e valide a mensagem usando a infraestrutura do projeto.
    }
}
```

O endereço do broker é registrado automaticamente. As propriedades de serializers, consumers e nomes de tópicos continuam sob responsabilidade do microsserviço.

### MySQL, Kafka e autorização juntos

```java
@PlatformIntegrationTest
@WithMySql
@WithKafka
@WithMockAuthorization
class ProductCompleteFlowIT {
}
```

## 17. Exemplo completo

```java
package com.empresa.product.integration;

import com.empresa.platform.testing.annotation.PlatformIntegrationTest;
import com.empresa.platform.testing.annotation.WithDatabaseScripts;
import com.empresa.platform.testing.annotation.WithMockAuthorization;
import com.empresa.platform.testing.annotation.WithMySql;
import com.empresa.platform.testing.authorization.AuthorizationMock;
import com.empresa.product.client.ProductClient;
import com.empresa.product.config.ProductAuthorizationTestConfiguration;
import com.empresa.product.factory.ProductFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
@Import(ProductAuthorizationTestConfiguration.class)
@WithDatabaseScripts(
    setup = "classpath:sql/views/create-product-views.sql",
    cleanup = "classpath:sql/views/drop-product-views.sql"
)
class ProductControllerIT {

    @Autowired
    private ProductClient productClient;

    @Autowired
    private ProductFactory productFactory;

    @Autowired
    private AuthorizationMock authorizationMock;

    @Test
    void deveCriarProduto() {
        productClient.create(productFactory.valid())
            .expectCreated()
            .expectNotNull("id")
            .expect("active", true);

        authorizationMock.verifyCalled();
        authorizationMock.verifyCalledWithAccount("account-123");
        authorizationMock.verifyCalledWithApplication("product-api");
    }

    @Test
    void deveRejeitarProdutoSemNome() {
        productClient.create(productFactory.withoutName())
            .expectBadRequest();
    }

    @Test
    void deveNegarAcessoSemPermissao() {
        authorizationMock.forbidden();

        productClient.create(productFactory.valid())
            .expectForbidden();
    }
}
```

## 18. Qual recurso usar

| Necessidade | Recurso recomendado |
|---|---|
| Testar controller HTTP | `@PlatformIntegrationTest` |
| Testar com banco real | `@WithMySql` |
| Preservar tabela de catálogo | `excludeTables` |
| Criar uma view para a classe | `@WithDatabaseScripts` na classe |
| Preparar SQL para um único teste | `@WithDatabaseScripts` no método |
| Simular autorização | `@WithMockAuthorization` |
| Customizar a sessão | `AuthorizationSessionCustomizer` |
| Enviar request sem autenticação | `requests.create()` |
| Enviar request autenticado | `requests.createAuthorized(...)` |
| Criar massa válida | `TestDataFactory<T>` |
| Variar campos da massa | `TestDataBuilder<T>` |
| Preparar vários recursos | `TestScenario<R>` |
| Testar mensagens | `@WithKafka` |

## 19. Problemas comuns

### Docker não está disponível

Sintoma:

```text
Could not find a valid Docker environment
```

Solução: inicialize o Docker Desktop ou o runtime de containers utilizado pela equipe.

### Script não encontrado

Sintoma:

```text
Database script does not exist or is not readable
```

Verifique se o arquivo está em `src/test/resources` e utilize:

```text
classpath:sql/caminho/script.sql
```

### Não existe `DataSource`

Sintoma:

```text
@WithDatabaseScripts requires a DataSource in the Spring test context
```

Adicione `@WithMySql` ou configure outro `DataSource` no contexto de teste.

### Porta HTTP não encontrada

Sintoma:

```text
Required key 'local.server.port' not found
```

Utilize `@PlatformIntegrationTest` nos testes que criam requests pela `PlatformRequestSpecificationFactory`.

### Autorizador real está sendo chamado

Confirme que o teste possui:

```java
@WithMockAuthorization
```

Essa anotação substitui `platform.authorization.service-url` pela URL dinâmica do WireMock.

### Dados de outro teste permaneceram no banco

Confirme que `CleanupMode.NONE` não está ativo e que a tabela não foi adicionada acidentalmente a `excludeTables`.

### Execução paralela

Os requests HTTP não utilizam estado global e o correlation ID é isolado por thread. Porém, testes que compartilham o mesmo banco não devem executar em paralelo quando fazem limpeza das mesmas tabelas.

## 20. Checklist de adoção

- [ ] Adicionar `platform-test-support` com escopo `test`.
- [ ] Criar `application-test.yml`.
- [ ] Organizar `client`, `response`, `builder`, `factory`, `scenario` e `integration`.
- [ ] Adicionar `@PlatformIntegrationTest` aos testes HTTP.
- [ ] Adicionar `@WithMySql` somente onde houver banco.
- [ ] Manter scripts em `src/test/resources/sql`.
- [ ] Criar scripts de cleanup idempotentes.
- [ ] Adicionar `@WithMockAuthorization` somente quando necessário.
- [ ] Criar requests autorizados explicitamente.
- [ ] Criar um request novo em cada operação do client.
- [ ] Manter dados e regras específicas dentro do microsserviço.
- [ ] Executar `mvn test` com Java 21 e Docker disponíveis.

## 21. Comandos de execução

Executar os testes do módulo ou microsserviço:

```bash
mvn test
```

Executar o ciclo completo quando o Maven Failsafe estiver configurado:

```bash
mvn verify
```

Executar somente um teste:

```bash
mvn -Dtest=ProductControllerIT test
```
