# Comece aqui — Testes com platform-testing

Este guia é para quem nunca utilizou o `platform-testing` e precisa criar testes de integração em um microsserviço da plataforma.

## 1. Modelo mental

Pense no teste usando estas peças:

```text
Builder → Factory → Scenario → Client → IntegrationTest
```

Nem toda feature precisa de todas.

| Peça | Responsabilidade |
|---|---|
| Builder | Montar um DTO/Request válido e permitir variar campos |
| Factory | Representar cenários semânticos, como `withoutName()` |
| Scenario | Preparar dependências antes da ação principal |
| Client | Encapsular HTTP/RestAssured |
| Response | Encapsular assertions e extração da resposta |
| IntegrationTest | Descrever o comportamento esperado da feature |

A biblioteca cuida da infraestrutura; o microsserviço cuida do domínio.

## 2. Classe base

Para API com MySQL e autorização:

```java
@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
public abstract class BaseIntegrationTest {

    @Autowired
    protected AuthorizationMock authMock;
}
```

Adicione apenas o que precisar:

- `@WithMySql`: banco real via Testcontainers;
- `@WithKafka`: Kafka real;
- `@WithMockAuthorization`: autorizador simulado;
- `@WithDatabaseScripts`: views, procedures ou SQL de preparação.

## 3. Infraestrutura opcional

Além de MySQL, Kafka e autorização simulada, o módulo oferece fixtures para AWS LocalStack e emuladores Azure. Cada fixture cloud exige dependências específicas no microsserviço; consulte [AWS LocalStack e Azure Emulator](GUIA_DE_USO.md#17-aws-localstack-e-azure-emulator) antes de usar essas anotações.

## 4. Organização recomendada

```text
src/test/java/com/empresa/product
├── client
│   ├── ProductClient.java
│   └── response/ProductResponse.java
├── common
│   ├── builder/ProductBuilder.java
│   ├── factory/ProductFactory.java
│   └── scenario/CatalogScenario.java
└── web/product/ProductIntegrationTest.java
```

## 5. Builder

O Builder representa a estrutura do objeto.

Regra principal:

> `builder().build()` deve gerar um objeto válido por padrão.

```java
public final class ProductBuilder
        implements TestDataBuilder<ProductDTO> {

    private String name = "Produto de teste";
    private String description = "Produto válido para integração";

    public static ProductBuilder builder() {
        return new ProductBuilder();
    }

    public ProductBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ProductBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public ProductDTO build() {
        return new ProductDTO(null, name, description);
    }
}
```

Use Builder quando pensar:

```text
"quero um objeto válido, mas alterando estes campos"
```

### Atenção com collections

Se existir `addX()`, a collection precisa ser mutável.

Evite:

```java
private Set<ApproverDTO> approvers = Set.of(defaultApprover);
```

Prefira:

```java
private Set<ApproverDTO> approvers =
        new LinkedHashSet<>(Set.of(defaultApprover));
```

## 6. Factory

A Factory representa cenários conhecidos.

```java
public final class ProductFactory
        extends AbstractTestDataFactory<ProductDTO, ProductBuilder> {

    @Override
    protected ProductBuilder builder() {
        return ProductBuilder.builder();
    }

    public ProductDTO withoutName() {
        return create(builder -> builder.withName(null));
    }

    public ProductDTO duplicatedName(String name) {
        return create(builder -> builder.withName(name));
    }
}
```

Regra:

```text
Builder → construção estrutural
Factory → cenário semântico
```

Não transforme a Factory em um segundo Builder.

## 7. Client

O Client esconde os detalhes do HTTP:

```java
@Component
public final class ProductClient extends BaseClient {

    private static final String BASE_PATH = "/api/v1/products";

    public ProductClient(
            PlatformRequestSpecificationFactory requests
    ) {
        super(requests);
    }

    public ProductResponse create(ProductDTO request) {
        return response(
                given()
                        .spec(authorizedRequest())
                        .body(json(request))
                        .when()
                        .post(BASE_PATH)
                        .then(),
                ProductResponse::new
        );
    }

    public ProductResponse findById(Long id) {
        return response(
                given()
                        .spec(authorizedRequest())
                        .when()
                        .get(BASE_PATH + "/{id}", id)
                        .then(),
                ProductResponse::new
        );
    }
}
```

O teste passa a falar na linguagem do domínio:

```java
ProductDTO created = productClient
        .create(productFactory.valid())
        .expectCreated()
        .extractBody();
```

### Atenção com query params opcionais

Não envie `null` automaticamente.

Evite:

```java
.queryParam("typeName", typeName)
```

Prefira:

```java
if (typeName != null) {
    request.queryParam("typeName", typeName);
}
```

Um `null` enviado como `?typeName=` pode mudar a semântica da consulta.

## 8. Scenario

Scenario prepara pré-condições.

Exemplo: antes de criar Product precisam existir Category e ProductType.

```java
@BeforeEach
void prepareDependencies() {
    catalogs().setup();
}
```

Regra:

> Scenario prepara o mundo, mas não deve esconder a ação principal do teste.

Se você está testando criação de Product, a criação deve continuar visível:

```java
productClient.create(...);
```

## 9. Primeiro teste

```java
@DisplayName("Integração - Product")
class ProductIntegrationTest
        extends BaseIntegrationTest {

    private final ProductFactory productFactory =
            new ProductFactory();

    @Autowired
    private ProductClient productClient;

    @BeforeEach
    void prepareDependencies() {
        authMock.allow(session ->
                session.groups("PM5_OWNER"));

        catalogs().setup();
    }

    @Test
    @DisplayName("SMOKE - deve criar e buscar produto")
    void deveCriarEBuscarProduto() {

        ProductDTO created = productClient
                .create(productFactory.valid())
                .expectCreated()
                .extractBody();

        ProductDTO found = productClient
                .findById(created.id())
                .expectOk()
                .extractBody();

        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.name()).isEqualTo(created.name());
    }
}
```

## 10. Ordem recomendada para criar uma suíte

Quando uma feature ainda não possui testes:

```text
1. Crie o Builder válido
2. Crie a Factory
3. Crie Client e Response
4. Identifique dependências
5. Crie Scenario somente se houver preparação repetitiva
6. Implemente happy path
7. Implemente validações
8. Implemente filtros/queries
9. Implemente update
10. Implemente delete/restore/lifecycle
11. Implemente autorização
12. Revise edge cases
13. Rode a classe isolada
14. Rode mvn verify
```

## 11. Checklist para CRUD

### CREATE

- [ ] objeto válido;
- [ ] campos internos gerados automaticamente;
- [ ] collections persistidas;
- [ ] regras automáticas do domínio;
- [ ] duplicidade;
- [ ] obrigatórios;
- [ ] limites mínimos e máximos.

### READ / QUERY

- [ ] find by id;
- [ ] not found;
- [ ] ativos;
- [ ] inativos, se houver soft delete;
- [ ] filtros;
- [ ] filtros vazios;
- [ ] case-insensitive, quando aplicável;
- [ ] summary/projection.

### UPDATE

- [ ] campos simples;
- [ ] relacionamentos;
- [ ] collections;
- [ ] campos derivados;
- [ ] manter o próprio valor único;
- [ ] conflito com outro registro;
- [ ] id inexistente.

### DELETE / RESTORE

- [ ] delete;
- [ ] id inexistente;
- [ ] restore;
- [ ] restore com alteração;
- [ ] restore com conflito.

### AUTH

- [ ] permitido;
- [ ] sem autorização;
- [ ] grupo insuficiente;
- [ ] grupo correto;
- [ ] OWNER, se houver bypass;
- [ ] update/delete com nível correto.

## 12. Limites de validação

Não teste somente `null`.

Para uma regra `name = 3..100`:

```text
2   → inválido
3   → válido
100 → válido
101 → inválido
```

Isso protege as bordas reais da regra.

## 13. Categorias de testes

Use `@DisplayName` como documentação executável:

```text
SMOKE
CREATE
QUERY
UPDATE
DELETE
RESTORE
VALIDATION
AUTH
ONBOARDING
```

Exemplo:

```java
@DisplayName("UPDATE - deve substituir approvers")
```

O relatório Maven passa a mostrar claramente o que a suíte protege.

## 14. Teste invariantes do domínio

Se o backend gera algo automaticamente, crie teste para isso.

Exemplo:

```java
@Test
@DisplayName("CREATE - deve gerar tags default")
void deveGerarTagsDefault() {
    ProductDTO created = productClient
            .create(ProductBuilder.builder()
                    .withTags(Set.of())
                    .build())
            .expectCreated()
            .extractBody();

    assertThat(created.tags())
            .contains("tag-gerada-pelo-sistema");
}
```

Não busque apenas cobertura de linhas. Proteja regras importantes.

## 15. Autorização

Permitido:

```java
authMock.allow(session -> session
        .groups("USER")
        .addAuthorizerGroup(
                "GRP_PRODUCT_DEV_ADMIN",
                "ADMIN",
                "DEV",
                "PRODUCT"
        ));
```

Sem permissão suficiente:

```java
authMock.allow(session -> session.groups("USER"));

productClient.update(id, request)
        .expectForbidden();
```

OWNER:

```java
authMock.allow(session ->
        session.groups("PM5_OWNER"));
```

## 16. O que não deve ir para platform-testing

A lib fornece infraestrutura.

Não coloque nela regras específicas como:

```text
AccountType.ADMIN
nome de endpoint de Account
regras de tags de Account
onboarding de uma feature
payload de domínio
grupos específicos de um serviço
```

Isso pertence ao microsserviço.

## 17. Evite abstração excessiva

Bom:

```java
productClient
        .create(productFactory.valid())
        .expectCreated()
        .extractBody();
```

Evite esconder tudo em DSLs como:

```java
productScenario()
        .givenValidProduct()
        .whenCreated()
        .thenPersisted()
        .thenAuthorized();
```

O teste precisa continuar fácil de depurar.

## 18. Como saber se a suíte está boa

Pergunte:

> se eu quebrar uma regra importante do domínio, algum teste falha?

Exemplos:

- permitir nome duplicado;
- remover tags automáticas;
- perder approvers durante update;
- ignorar autorização;
- tratar filtro vazio como valor real.

Se essas mudanças quebram testes, a suíte está protegendo comportamento, não apenas linhas.

## 19. Resumo rápido

| Pergunta | Use |
|---|---|
| Como construir o DTO? | Builder |
| Qual massa representa um cenário? | Factory |
| Como chamar a API? | Client |
| Como validar/extrair resposta? | Response |
| O que precisa existir antes? | Scenario |
| Onde fica a regra testada? | IntegrationTest |
| Quem sobe banco/Kafka/auth? | platform-testing |

## 20. Projeto de referência

O `account-api` é o projeto piloto de referência.

A suíte de Account demonstra:

- CRUD;
- filtros;
- soft delete;
- restore;
- validações;
- collections;
- tags automáticas;
- onboarding;
- autorização;
- OWNER bypass;
- edge cases.

Para recursos avançados da biblioteca, consulte também:

- `README.md`;
- `GUIA_DE_USO.md`.
