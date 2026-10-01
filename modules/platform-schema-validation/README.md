# platform-schema-validation

Biblioteca Spring Boot para validação estrutural de inputs de use case por JSON Schema publicado.

A validação pertence à fronteira da aplicação, e não ao protocolo de entrada:

```text
REST ─────┐
MCP ──────┤
BFF ──────┤
Consumer ─┤
          ↓
Use Case
  ↓ @ValidateResourceSchema
@SchemaPayload Input
  ↓
ResourceSchemaValidationAspect
  ↓
ResourceSchemaValidator
  ↓
ResourceSchemaResolver
  ↓
Repository
  ↓
VIEW configurada no banco do consumidor
```

A biblioteca conhece apenas o contrato da VIEW:

```text
resource_type
resource_code
schema_version
definition
```

Ela não conhece controllers, protocolos de entrada ou as tabelas internas do serviço owner do Schema.

## Uso

Marque o método de use case e exatamente um parâmetro que representa o payload estrutural:

```java
@ValidateResourceSchema(type = "APPLICATION", code = "application")
public ApplicationOutput create(
        String workspaceIdentifier,
        @SchemaPayload CreateApplicationInput input
) {
    ...
}
```

Antes da execução do método, o aspect serializa o input para `JsonNode` e o valida contra o schema resolvido. O método só é executado quando a estrutura é válida.

Um método com `@ValidateResourceSchema` deve possuir exatamente um parâmetro `@SchemaPayload`. Ausência ou duplicidade é tratada como erro de configuração.

## Fronteiras

- Entrypoints (REST, MCP, BFF etc.) adaptam protocolo e aplicam autenticação/autorização.
- `@ValidateResourceSchema` garante a estrutura do input na fronteira do use case.
- Validators do serviço permanecem responsáveis pelas regras de negócio.
- Domain não depende de JSON Schema, AOP ou desta biblioteca.

## Resolução

```text
(resourceType, resourceCode)
        ↓ miss
(resourceType, DEFAULT)
        ↓ miss
erro de configuração
```

## Configuração

```yaml
platform:
  schema-validation:
    enabled: true
    fallback-code: DEFAULT
    datasource:
      enabled: true
      view-name: vw_platform_resource_schemas
```

Quando não existe `JdbcTemplate` ou o datasource está desabilitado, a biblioteca usa `NoOpResourceSchemaRepository`.

## Contrato da VIEW

O serviço owner do Schema deve publicar uma VIEW com as colunas:

```text
resource_type
resource_code
schema_version
definition
```

A biblioteca consulta essa VIEW diretamente, assim como `platform-messaging` consulta sua view de mensagens.


### Defaults Golden

O módulo possui uma única raiz pública de configuração: `PlatformSchemaValidationProperties`.

- `platform.schema-validation.enabled`: `true`
- `platform.schema-validation.fallback-code`: `DEFAULT`
- `platform.schema-validation.datasource.enabled`: `false`
- `platform.schema-validation.datasource.view-name`: `vw_platform_resource_schemas`

Valores textuais em branco usam o default do módulo. As auto-configurations apenas consomem essa raiz; componentes de negócio não consultam `Environment` ou propriedades diretamente.


## Contrato AOP

O aspect mantém uma responsabilidade pequena: localizar o parâmetro `@SchemaPayload`, convertê-lo para `JsonNode`, delegar a validação e somente então prosseguir com o use case.

A posição do `@SchemaPayload` é resolvida uma vez por método e mantida em cache. Assim, a reflexão usada para validar o contrato da annotation não é repetida em cada chamada. O contrato continua exigindo exatamente um `@SchemaPayload` por método anotado.

A biblioteca não move regras de negócio para o aspect e não exige annotations de protocolo nos controllers.


## Runtime e resolução de schema

A resolução continua consultando a fonte configurada em cada validação para descobrir a versão publicada atual. Ausência de registro permite o fallback `(type, DEFAULT)`; falhas de infraestrutura do datasource não são convertidas em ausência e são propagadas.

Após a resolução, a definição publicada é parseada e compilada para a validação corrente. O módulo não mantém cache de resolução, versão ou JSON Schema compilado.

Essa decisão mantém a VIEW como fonte de verdade imediata e evita TTL, invalidação e retenção de versões históricas em memória. O cache existente no módulo é restrito ao metadado estático de reflection do contrato AOP (método → posição do parâmetro `@SchemaPayload`). Cache de schema poderá ser introduzido futuramente apenas se medições demonstrarem necessidade.
