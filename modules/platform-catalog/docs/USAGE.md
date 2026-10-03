# Como usar o `platform-catalog`

A identidade padrão de qualquer catálogo é um `code` semântico persistido como chave primária.

## 1. Contrato persistido

```text
code        VARCHAR(50) PRIMARY KEY
label
description
sortOrder
active
settings
```

Não existe `Long id` nem `name` no contrato base.

O código deve obedecer:

```text
^[A-Z][A-Z0-9_]{0,49}$
```

Exemplos: `JAVA`, `MANAGER`, `WORKSPACE_REGISTRATION`, `OPEN_API`.

O `code` é imutável depois da criação.

## 2. Included ou Enum

Use `IncludedCatalogService` para o modelo Included, quando novos códigos podem ser incluídos em runtime.

Use `EnumCatalogService` quando o código da aplicação define os valores permitidos. Nesse caso, o `code` precisa existir no enum que implementa `CatalogEnum`.

A estrutura persistida é igual nos dois modelos.

## 3. Enum Catalog

```java
@Entity
@Table(name = "type_languages")
public class LanguageType extends CatalogEntity {
}
```

```java
public enum LanguageTypeEnum implements CatalogEnum<LanguageTypeEnum> {
    JAVA,
    DOTNET,
    GO,
    PYTHON
}
```

```java
public interface LanguageTypeRepository
        extends CatalogRepository<LanguageType> {
}
```

```java
@Service
public class LanguageTypeService
        extends EnumCatalogService<LanguageType, LanguageTypeEnum> {

    public LanguageTypeService(
            LanguageTypeRepository repository,
            ObjectMapper objectMapper) {
        super(repository, objectMapper, LanguageType.class, LanguageTypeEnum.class);
    }
}
```

## 4. Included Catalog

O Included Catalog usa `IncludedCatalogService` e não possui enum. Novos `code` podem ser criados via API.

### Settings validados por JSON Schema

`platform-catalog` depende oficialmente de `platform-schema-validation`. Para `settings` governado por contrato, JSON Schema é o mecanismo Golden e o consumidor não precisa criar uma ponte própria de validação. Injete o `SchemaValidator` e informe somente o código do recurso de schema:

```java
@Service
public class LanguageTypeService
        extends EnumCatalogService<LanguageType, LanguageTypeEnum> {

    public LanguageTypeService(
            LanguageTypeRepository repository,
            ObjectMapper objectMapper,
            SchemaValidator schemaValidator) {
        super(
                repository,
                objectMapper,
                LanguageType.class,
                LanguageTypeEnum.class,
                "language-type",
                schemaValidator
        );
    }
}
```

O `platform-catalog` define internamente `resourceType = CATALOG`, valida
`CatalogDTO.settings` por `platform-schema-validation` e converte erros de
payload para campos `settings.*`. Erros técnicos de resolução/compilação do
schema não são convertidos em erros funcionais.

`platform-schema-validation` precisa de uma fonte de schemas no startup. O Golden Default consulta `vw_platform_resource_schemas` (`resource_type`, `resource_code`, `schema_version`, `definition`). Para customizar, altere `platform.schema-validation.view-name` ou forneça `ResourceSchemaRepository`. Se nenhuma fonte estiver disponível, o startup falha de forma descritiva com `PLT-SCHEMA-005/007`.

Configurações de cache local/Redis continuam pertencendo exclusivamente ao `platform-schema-validation`; o Catalog não duplica essas properties.

Use `CatalogSettingsValidator` diretamente apenas quando existir uma regra adicional ou uma regra de settings que não seja JSON Schema.

## 5. DTO padrão

`CatalogDTO` possui:

```text
String code
String label
String description
Integer sortOrder
JsonNode settings
```

Exemplo:

```json
{
  "code": "JAVA",
  "label": "Java",
  "description": "Aplicação Java",
  "sortOrder": 1,
  "settings": {}
}
```

## 6. Endpoints

```text
GET    /api/v1/<catalog>
GET    /api/v1/<catalog>/{code}
POST   /api/v1/<catalog>
PUT    /api/v1/<catalog>/{code}
DELETE /api/v1/<catalog>/{code}
POST   /api/v1/<catalog>/{code}/restore
```

## 7. Regras

- `code`, `label` e `description` são obrigatórios;
- `code` é chave primária;
- `code` aceita somente `A-Z`, `0-9` e `_`, inicia por letra e tem no máximo 50 caracteres;
- `code` não é alterado em updates;
- `EnumCatalogService` valida o código contra o enum;
- `IncludedCatalogService` permite novos códigos em runtime;
- `active=false` representa soft delete;
- restore revalida o registro;
- `sortOrder` é calculado quando ausente ou menor que 1;
- filtros padrão são `active` e `code`.

## 8. Relacionamentos

Uma entidade de negócio deve referenciar a entidade de catálogo usando o `code` como FK física:

```text
application.language_type_code
        ↓
type_languages.code
```

## 9. Value Objects de código

`AbstractCatalogCode` é o contrato compartilhado para referências semânticas a códigos de catálogo dentro dos domínios consumidores. Ele não representa a regra de criação do `CatalogEntity`: VOs dinâmicos podem aceitar valores como `workspace-service`, enquanto o `code` administrável do catálogo mantém o formato uppercase/underscore desta documentação.

## 10. Extension points

Os únicos pontos oficiais de extensão direta de service são:

```text
EnumCatalogService
IncludedCatalogService
```

`AbstractCatalogService` permanece `sealed`.

## 11. Checklist

Para qualquer catálogo:

1. migration com `code VARCHAR(50) PRIMARY KEY`;
2. entity;
3. repository;
4. service `EnumCatalogService` ou `IncludedCatalogService`;
5. controller;
6. autorização;
7. testes.

No Enum Catalog, adicione também o enum implementando `CatalogEnum`.

## 12. Contrato público Golden

No microserviço consumidor, use diretamente os contratos fornecidos pela library:

```text
CatalogEntity
CatalogRepository
CatalogDTO
CatalogController
EnumCatalogService | IncludedCatalogService
CatalogEnum        (somente Enum Catalog)
AbstractCatalogCode (quando necessário como VO)
SchemaValidator     (settings governado por JSON Schema)
CatalogSettingsValidator (regra adicional/não-schema)
```

Não estenda nem replique `CatalogMapper`, `AbstractCatalogService`,
`AbstractCatalogValidator` ou `EnumCatalogValidator`. Essas classes fazem
parte da implementação interna do fluxo padrão.

O microserviço é responsável por declarar qual catálogo existe, seu enum quando
aplicável, schema de settings, autorização e regras de domínio. A library é
responsável pelo comportamento comum de persistência, CRUD, lifecycle,
ordenação e validação estrutural do catálogo.

