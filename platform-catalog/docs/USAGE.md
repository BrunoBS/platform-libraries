# Como usar o `platform-catalog`

Este guia apresenta o caminho simples para novos catálogos persistidos e o caminho extensível para catálogos com regras próprias.

## 1. Escolha o tipo de catálogo

A `platform-catalog` trabalha com dois tipos.

### Dynamic Catalog

Use `DynamicCatalogService` quando os valores podem ser criados em runtime e o banco é a fonte de verdade.

```text
Novo name sem alteração de código
Sem enum
Banco define os valores existentes
```

### Enum Catalog

Use `EnumCatalogService` quando o código precisa definir quais valores são permitidos.

```text
name precisa existir no enum
Novo name exige alteração do enum e deploy
Banco mantém os dados administrativos do catálogo
```

O enum deve implementar `CatalogEnum`.

Se um enum Java não precisa ser persistido ou administrado, ele não é um caso de uso da `platform-catalog`.

### Decisão rápida

```text
Os names podem ser criados em runtime?
│
├── SIM
│   └── DynamicCatalogService
│
└── NÃO, os valores são definidos pelo código
    └── EnumCatalogService
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

## 3. Enum Catalog em cinco classes

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

## 4. Dynamic Catalog em quatro classes

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

## 11. Como escolher o caminho de implementação

Depois de escolher entre Dynamic e Enum:

```text
O catálogo possui campos ou regras extras?
│
├── NÃO
│   └── CatalogDTO + CatalogController
│       + DynamicCatalogService ou EnumCatalogService
│
└── SIM
    └── use as classes Base* necessárias
```

## 12. Checklist

### Dynamic Catalog

1. migration;
2. entity;
3. repository;
4. service com `DynamicCatalogService`;
5. controller com `CatalogController`;
6. autorização;
7. testes.

### Enum Catalog

1. migration;
2. entity;
3. enum implementando `CatalogEnum`;
4. repository;
5. service com `EnumCatalogService`;
6. controller com `CatalogController`;
7. autorização;
8. testes.

### Catálogo avançado

Comece pelo caminho simples e use as classes `Base*` apenas quando surgir uma regra que realmente exija customização.
