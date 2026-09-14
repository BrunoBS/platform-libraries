# platform-catalog

Infraestrutura reutilizável para catálogos persistidos e administráveis pelos microserviços da plataforma.

## Objetivo

A biblioteca define **como** um catálogo gerenciado funciona. Cada microserviço continua dono de **quais** catálogos pertencem ao seu domínio, das migrations, das regras específicas e da autorização.

A infraestrutura de CRUD é reutilizada de `platform-crud` e a semântica padrão de erro usa `platform-messaging`.

## Modelos

### STATIC

Catálogo apenas em código, normalmente por `enum`.

- não possui tabela;
- não possui CRUD;
- não usa a infraestrutura persistida da lib.

### MANAGED

Catálogo persistido e administrável em runtime.

- banco é a fonte de verdade;
- novos valores podem ser criados sem recompilar;
- usa a infraestrutura base de catálogo.

### MANAGED_CONSTRAINED

Catálogo persistido, porém com `name` limitado por `CatalogEnum`.

- mantém CRUD, `active`, `restore`, `sortOrder` e `settings`;
- o banco continua persistindo os registros;
- o enum define os nomes permitidos.

## Developer experience

Existem dois caminhos de uso.

### Happy path — catálogo padrão

Quando o catálogo só possui os campos padrão da lib, **não é necessário criar DTO, mapper ou validator próprios**.

A lib fornece:

```text
DefaultCatalogDTO
DefaultCatalogMapper
DefaultCatalogValidator
DefaultEnumCatalogValidator
DefaultManagedCatalogService
DefaultConstrainedCatalogService
DefaultCatalogController
```

Para um `MANAGED_CONSTRAINED`, o consumidor normalmente cria somente:

```text
LanguageType.java
LanguageTypeEnum.java
LanguageTypeRepository.java
LanguageTypeService.java
LanguageTypeController.java
```

Exemplo:

```java
@Entity
@Table(name = "type_languages")
public class LanguageType extends BaseCatalogEntity {
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
        extends BaseCatalogRepository<LanguageType> {
}
```

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

Para `MANAGED`, troque `DefaultConstrainedCatalogService` por `DefaultManagedCatalogService` e remova o enum.

### Caminho extensível — catálogo com regras próprias

Quando o catálogo possui campos adicionais, relacionamento, unicidade composta, filtros específicos ou contrato próprio de DTO, use as abstrações completas:

```text
BaseCatalogDTO
BaseCatalogMapper
BaseCatalogValidator
EnumCatalogValidator
BaseRelatedCatalogValidator
BaseCatalogService
BaseCatalogController
```

Exemplos atuais de necessidade do caminho extensível:

```text
FeatureScopeType -> FeatureType
SchemaScopeType  -> SchemaType
```

## Comportamento padrão

A infraestrutura fornece:

- CRUD;
- listagem de ativos/inativos;
- busca por nome e id;
- criação em lote no controller;
- soft delete por `active=false`;
- restore com revalidação;
- `sortOrder` automático quando não informado;
- `name` imutável por padrão;
- filtro padrão por `active` e `name` no banco;
- filtros adicionais por `Specification`;
- unicidade simples por `name` como default;
- suporte a unicidade relacionada por `BaseRelatedCatalogValidator`;
- suporte opcional a validação de `settings` por callback.

## Campos padrão

`BaseCatalogEntity` fornece:

```text
id          Long
name        String
label       String
description String
sortOrder   Integer
active      boolean
settings    String
```

`DefaultCatalogDTO` expõe os mesmos dados de API, com `settings` como `JsonNode`.

## Identidade e unicidade

A lib não impõe `unique=true` em `name` no mapeamento base. A constraint física pertence ao microserviço e deve refletir a identidade real do catálogo:

```text
name
account_id + name
application_id + name
scope + name
```

A validação Java melhora a resposta ao consumidor, mas não substitui a constraint no banco.

## Relacionamentos

`BaseRelatedCatalogValidator` padroniza catálogos cuja identidade depende de outro catálogo:

```text
relação obrigatória
        ↓
registro relacionado existe e está ativo
        ↓
unicidade (relação + name)
```

## `settings`

No happy path, o service pode receber uma validação adicional sem exigir uma classe `Validator` dedicada:

```java
super(
    repository,
    objectMapper,
    LanguageType.class,
    LanguageTypeEnum.class,
    (dto, result) -> schemaValidator.validateJson(
        DEFAULT_SCHEMA,
        dto.settings(),
        "settings",
        result
    )
);
```

Se não houver regra adicional, use o construtor simples.

## Extensibilidade

O happy path é opcional. Assim que o catálogo precisar de comportamento específico, o consumidor pode voltar para as classes-base sem abandonar a infraestrutura.

A lib não deve conhecer regras como:

```text
AccountType
EnvironmentType
FeatureType
SchemaType
accountId
applicationId
políticas específicas de autorização
valores concretos dos enums dos serviços
```

## Ownership

```text
platform-catalog = COMO um catálogo administrável funciona
microserviço     = QUAL catálogo existe e suas regras de domínio
```

## Guia completo

Veja:

```text
docs/USAGE.md
```
