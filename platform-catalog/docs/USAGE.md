# Como usar o `platform-catalog`

Este guia apresenta o caminho simples para novos catálogos e o caminho extensível para catálogos com regras próprias.

## 1. Classifique o catálogo

Use um destes modelos:

- `STATIC`: apenas enum, sem tabela e sem CRUD;
- `MANAGED`: persistido, banco como fonte de verdade;
- `MANAGED_CONSTRAINED`: persistido, mas `name` limitado por `CatalogEnum`.

## 2. Dependência

```xml
<dependency>
    <groupId>com.empresa.platform</groupId>
    <artifactId>platform-catalog</artifactId>
</dependency>
```

A versão deve vir do dependency management da plataforma.

---

# Happy path

Use este caminho quando o catálogo possui apenas os campos padrão:

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

## 3. MANAGED_CONSTRAINED em cinco classes

### 3.1 Entidade

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

### 3.2 Enum

```java
public enum LanguageTypeEnum implements CatalogEnum<LanguageTypeEnum> {
    JAVA,
    DOTNET,
    GO,
    PYTHON
}
```

### 3.3 Repository

```java
public interface LanguageTypeRepository
        extends BaseCatalogRepository<LanguageType> {
}
```

### 3.4 Service

```java
@Service
public class LanguageTypeService
        extends DefaultConstrainedCatalogService<LanguageType, LanguageTypeEnum> {

    public LanguageTypeService(
            LanguageTypeRepository repository,
            ObjectMapper objectMapper) {
        super(repository, objectMapper, LanguageType.class, LanguageTypeEnum.class);
    }
}
```

O service recebe automaticamente:

```text
findAll
findById
findByName
findByNames
create
update
delete
restore
```

### 3.5 Controller

```java
@RestController
@RequestMapping("/api/v1/language-type")
public class LanguageTypeController
        extends DefaultCatalogController<LanguageType> {

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

A autorização continua no controller concreto.

## 4. MANAGED em quatro classes

Para um catálogo totalmente dinâmico, remova o enum e use `DefaultManagedCatalogService`:

```java
@Service
public class SegmentTypeService
        extends DefaultManagedCatalogService<SegmentType> {

    public SegmentTypeService(
            SegmentTypeRepository repository,
            ObjectMapper objectMapper) {
        super(repository, objectMapper, SegmentType.class);
    }
}
```

A estrutura fica:

```text
SegmentType.java
SegmentTypeRepository.java
SegmentTypeService.java
SegmentTypeController.java
```

## 5. DTO padrão

O happy path usa `DefaultCatalogDTO`:

```text
Long id
String name
String label
String description
Integer sortOrder
JsonNode settings
```

O JSON público continua simples e independente da entidade JPA.

## 6. Validação de `settings` sem criar Validator

Se o serviço possui uma validação padrão para `settings`, injete-a diretamente no construtor do service:

```java
@Service
public class LanguageTypeService
        extends DefaultConstrainedCatalogService<LanguageType, LanguageTypeEnum> {

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

Isso preserva o happy path sem obrigar a criação de uma classe `Validator` apenas para uma regra simples.

## 7. Regras padrão

No happy path:

- `name`, `label` e `description` são obrigatórios;
- `description` segue os limites comuns da lib;
- `name` deve ser único por padrão;
- em `MANAGED_CONSTRAINED`, `name` também precisa existir no enum;
- `name` é imutável depois da criação;
- `active=false` representa soft delete;
- restore revalida o registro antes de ativá-lo;
- `sortOrder` é calculado quando ausente ou inválido;
- `active=true` é aplicado apenas na criação e não é forçado durante update;
- filtros padrão são executados no banco.

## 8. Migration continua sendo do serviço

A lib não cria tabelas automaticamente.

```sql
CREATE TABLE type_languages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    label VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order INT NOT NULL,
    is_active BIT NOT NULL,
    settings TEXT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_type_languages_name UNIQUE (name)
);
```

A constraint física deve representar a identidade real do catálogo.

---

# Caminho extensível

Use as classes completas quando o catálogo possuir algum destes requisitos:

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

## 9. Estrutura completa

```text
MyType.java
MyTypeDTO.java
MyTypeRepository.java
MyTypeMapper.java
MyTypeValidator.java
MyTypeService.java
MyTypeController.java
```

As abstrações disponíveis são:

```text
BaseCatalogDTO
BaseCatalogMapper
BaseCatalogValidator
EnumCatalogValidator
BaseRelatedCatalogValidator
BaseCatalogService
BaseCatalogController
```

## 10. Catálogo relacionado

Para identidade como `(scope, name)`, use `BaseRelatedCatalogValidator`.

Ele padroniza:

```text
relação obrigatória
        ↓
registro relacionado existe e está ativo
        ↓
unicidade (relação + name)
```

Exemplos:

```text
FeatureScopeType -> FeatureType
SchemaScopeType  -> SchemaType
```

O repository concreto continua declarando a query específica de unicidade.

## 11. Filtros específicos

`BaseCatalogService` usa `Specification`.

Sobrescreva:

```java
@Override
protected Specification<MyType> additionalSpecification(
        Map<String, String> filters) {
    ...
}
```

Os filtros são aplicados no banco, não depois da materialização dos DTOs.

## 12. DTO próprio

Crie um DTO próprio somente quando houver campos além do contrato padrão.

```java
public record FeatureTypeDTO(
    Long id,
    String name,
    String label,
    String description,
    Integer sortOrder,
    Long featureScopeId,
    String featureScopeName,
    Boolean available,
    JsonNode settings
) implements BaseCatalogDTO<FeatureTypeDTO> {
    ...
}
```

Nesse caso use também mapper, validator e service concretos conforme necessário.

## 13. Regra de ownership

A lib conhece comportamento de catálogo, não catálogo de negócio.

```text
platform-catalog = COMO um catálogo funciona
microserviço     = QUAL catálogo existe
```

Não devem entrar na lib:

```text
AccountType
EnvironmentType
FeatureType
SchemaType
accountId
applicationId
regras específicas de autorização
valores concretos dos enums consumidores
```

## 14. Checklist rápido

### Catálogo padrão MANAGED

1. migration;
2. entity;
3. repository;
4. service com `DefaultManagedCatalogService`;
5. controller com `DefaultCatalogController`;
6. autorização;
7. testes.

### Catálogo padrão MANAGED_CONSTRAINED

1. migration;
2. entity;
3. enum;
4. repository;
5. service com `DefaultConstrainedCatalogService`;
6. controller com `DefaultCatalogController`;
7. autorização;
8. testes.

### Catálogo avançado

Comece pelo happy path e migre para as classes-base específicas apenas quando surgir uma regra que realmente exija isso.
