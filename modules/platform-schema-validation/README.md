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
