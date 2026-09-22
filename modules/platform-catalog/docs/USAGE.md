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

## 2. Dynamic ou Enum

Use `DynamicCatalogService` quando novos códigos podem ser criados em runtime.

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

## 4. Dynamic Catalog

O Dynamic Catalog usa `DynamicCatalogService` e não possui enum. Novos `code` podem ser criados via API.

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
- `DynamicCatalogService` permite novos códigos em runtime;
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

## 9. Extension points

Os únicos pontos oficiais de extensão direta de service são:

```text
EnumCatalogService
DynamicCatalogService
```

`AbstractCatalogService` permanece `sealed`.

## 10. Checklist

Para qualquer catálogo:

1. migration com `code VARCHAR(50) PRIMARY KEY`;
2. entity;
3. repository;
4. service `EnumCatalogService` ou `DynamicCatalogService`;
5. controller;
6. autorização;
7. testes.

No Enum Catalog, adicione também o enum implementando `CatalogEnum`.