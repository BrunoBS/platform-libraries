# Como usar o `platform-catalog`

Este guia apresenta o caminho simples para novos catálogos e o caminho extensível para catálogos com regras próprias.

## 1. Classifique o catálogo

Use um destes modelos arquiteturais:

- `STATIC`: apenas enum, sem tabela e sem CRUD;
- `MANAGED`: persistido, banco como fonte de verdade;
- `MANAGED_CONSTRAINED`: persistido, mas `name` limitado por `CatalogEnum`.

No código, use nomes mais diretos:

```text
MANAGED             -> DynamicCatalogService
MANAGED_CONSTRAINED -> EnumCatalogService
```

## 2. Dependência

```xml
<dependency>
    <groupId>com.empresa.platform</groupId>
    <artifactId>platform-catalog</artifactId>
</dependency>
```

A versão deve vir do dependency management da plataforma.

---

# Caminho simples

Use quando o catálogo possui apenas:

```text
id
name
label
description
sortOrder
active
settings
```

Nesse caso você **não precisa criar DTO, mapper ou validator próprios**.

## 3. Catálogo controlado por enum em cinco classes

### Entidade

```java
@Entity
@Table(
    name = "type_languages",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_type_languages_name",
        columnNames = "name"
    )
)
public class LanguageType extends BaseCatalogEntity {
}
```

### Enum

```java
public enum LanguageTypeEnum implements CatalogEnum<LanguageTypeEnum> {
    JAVA,
    DOTNET,
    GO,
    PYTHON
}
```

### Repository

```java
public interface LanguageTypeRepository
        extends BaseCatalogRepository<LanguageType> {
}
```

### Service

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

### Controller

```java
@RestController
@RequestMapping("/api/v1/language-type")
public class LanguageTypeController
        extends CatalogController<LanguageType> {

    public LanguageTypeController(LanguageTypeService service) {
        super(service);
    }
}
```

O controller expõe:

```text
GET    /api/v1/language-type
GET    /api/v1/language-type/{id}
POST   /api/v1/language-type
PUT    /api/v1/language-type/{id}
DELETE /api/v1/language-type/{id}
POST   /api/v1/language-type/{id}/restore
```

## 4. Catálogo dinâmico em quatro classes

Quando os nomes podem ser criados em runtime, não crie enum:

```java
@Service
public class SegmentTypeService
        extends DynamicCatalogService<SegmentType> {

    public SegmentTypeService(
            SegmentTypeRepository repository,
            ObjectMapper objectMapper) {
        super(repository, objectMapper, SegmentType.class);
    }
}
```

Estrutura:

```text
SegmentType.java
SegmentTypeRepository.java
SegmentTypeService.java
SegmentTypeController.java
```

## 5. DTO padrão

O caminho simples usa `CatalogDTO`:

```text
Long id
String name
String label
String description
Integer sortOrder
JsonNode settings
```

## 6. Validação de `settings`

Se houver uma regra simples para `settings`, passe-a no construtor do service:

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
            (dto, result) -> schemaValidator.validateJson(
                DEFAULT_JSON_SCHEMA,
                dto.settings(),
                "settings",
                result
            )
        );
    }
}
```

A interface usada nesse callback é `CatalogSettingsValidator`.

## 7. Regras padrão

- `name`, `label` e `description` são obrigatórios;
- `description` segue os limites comuns da lib;
- `name` deve ser único por padrão;
- no `EnumCatalogService`, `name` também precisa existir no enum;
- `name` é imutável depois da criação;
- `active=false` representa soft delete;
- restore revalida o registro antes de ativá-lo;
- `sortOrder` é calculado quando ausente ou inválido;
- `active=true` é aplicado apenas na criação;
- filtros padrão são executados no banco.

---

# Caminho extensível

Use quando houver:

```text
campos adicionais
DTO específico
relacionamento com outro catálogo
unicidade composta
filtros específicos
mapper específico
validações complexas
semântica de service própria
```

As abstrações são:

```text
BaseCatalogDTO
BaseCatalogMapper
BaseCatalogValidator
EnumCatalogValidator
BaseRelatedCatalogValidator
BaseCatalogService
BaseCatalogController
```

## 8. Catálogo relacionado

Para identidade como `(scope, name)`, use `BaseRelatedCatalogValidator`.

Ele padroniza:

```text
relação obrigatória
        ↓
registro relacionado existe e está ativo
        ↓
unicidade (relação + name)
```

## 9. Filtros específicos

`BaseCatalogService` usa `Specification`.

Sobrescreva:

```java
@Override
protected Specification<MyType> additionalSpecification(
        Map<String, String> filters) {
    ...
}
```

## 10. DTO próprio

Crie um DTO próprio somente quando houver campos além do contrato padrão.

## 11. Como escolher rapidamente

```text
Precisa tabela?
  não -> STATIC
  sim -> os nomes podem nascer em runtime?
           sim -> DynamicCatalogService
           não -> EnumCatalogService

Tem campos/regras extras?
  não -> CatalogDTO + CatalogController
  sim -> classes Base* do caminho extensível
```

## 12. Checklist

### Dinâmico

1. migration;
2. entity;
3. repository;
4. service com `DynamicCatalogService`;
5. controller com `CatalogController`;
6. autorização;
7. testes.

### Controlado por enum

1. migration;
2. entity;
3. enum;
4. repository;
5. service com `EnumCatalogService`;
6. controller com `CatalogController`;
7. autorização;
8. testes.

### Avançado

Comece pelo caminho simples e use as classes `Base*` apenas quando surgir uma regra que realmente exija customização.
