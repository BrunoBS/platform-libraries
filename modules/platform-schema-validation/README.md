# platform-schema-validation

Biblioteca Spring Boot para validação estrutural de payloads HTTP por JSON Schema publicado.

O padrão segue o mesmo desenho do `platform-messaging`:

```text
Controller
  ↓ @ValidateResourceSchema
RequestBodyAdvice
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

Ela não conhece as tabelas internas do serviço owner do Schema.

## Uso

```java
@PostMapping
@ValidateResourceSchema(type = "APPLICATION", code = "application")
public ResponseEntity<?> create(@RequestBody CreateApplicationRequest request) {
    ...
}
```

A validação ocorre no JSON bruto, antes da desserialização do request. Isso preserva a diferença entre campo ausente e campo explicitamente enviado como `null`.

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
