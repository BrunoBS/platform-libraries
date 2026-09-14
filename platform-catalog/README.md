# platform-catalog

Infraestrutura reutilizável para catálogos persistidos e administráveis pelos microserviços da plataforma.

## Objetivo

A biblioteca define **como** um catálogo funciona. Cada microserviço continua dono de **quais** catálogos pertencem ao seu domínio, das migrations, das regras específicas e da autorização.

A infraestrutura de CRUD é reutilizada de `platform-crud` e a semântica padrão de erro usa `platform-messaging`.

## Modelos arquiteturais

Os nomes abaixo servem para classificação e documentação. No código, a API usa nomes mais diretos.

### STATIC

Catálogo apenas em código, normalmente por `enum`.

- não possui tabela;
- não possui CRUD;
- não usa a infraestrutura persistida da lib.

### MANAGED

Catálogo persistido e administrável em runtime.

- banco é a fonte de verdade;
- novos valores podem ser criados sem recompilar;
- no código, use `DynamicCatalogService`.

### MANAGED_CONSTRAINED

Catálogo persistido cujo `name` é limitado por `CatalogEnum`.

- mantém CRUD, `active`, `restore`, `sortOrder` e `settings`;
- o banco continua persistindo os registros;
- o enum define os nomes permitidos;
- no código, use `EnumCatalogService`.

## Developer experience

### Caminho simples

Quando o catálogo só possui os campos padrão da lib, **não é necessário criar DTO, mapper ou validator próprios**.

A API principal para o desenvolvedor é:

```text
CatalogDTO
DynamicCatalogService
EnumCatalogService
CatalogController
```

Para um catálogo controlado por enum, o consumidor normalmente cria somente:

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
        extends EnumCatalogService<LanguageType, LanguageTypeEnum> {

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
        extends CatalogController<LanguageType> {

    public LanguageTypeController(LanguageTypeService service) {
        super(service);
    }
}
```

Para um catálogo totalmente dinâmico, use `DynamicCatalogService` e não crie enum.

### Caminho extensível

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

Exemplos atuais:

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
- unicidade simples por `name` como padrão;
- suporte a unicidade relacionada por `BaseRelatedCatalogValidator`;
- suporte opcional a validação de `settings` por `CatalogSettingsValidator`.

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

`CatalogDTO` expõe o contrato padrão de API, com `settings` como `JsonNode`.

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

No caminho simples, o service pode receber uma validação adicional sem exigir uma classe `Validator` dedicada:

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

## Regra de simplicidade

A API pública privilegia nomes que expliquem o comportamento:

```text
DynamicCatalogService  = nomes definidos em runtime/banco
EnumCatalogService     = nomes permitidos pelo enum
CatalogController      = controller para o contrato padrão
CatalogDTO             = DTO padrão
```

Os nomes `MANAGED` e `MANAGED_CONSTRAINED` permanecem apenas como classificação arquitetural.

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
