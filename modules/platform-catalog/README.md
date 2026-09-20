# platform-catalog

Infraestrutura reutilizável para catálogos persistidos e administráveis pelos microserviços da plataforma.

## Objetivo

A biblioteca define **como** um catálogo persistido funciona. Cada microserviço continua dono de **quais** catálogos pertencem ao seu domínio, das migrations, das regras específicas e da autorização.

A infraestrutura de catálogo é autocontida no `platform-catalog` e a semântica padrão de erro usa `platform-messaging`. O módulo não depende de uma abstração genérica de CRUD.

## Tipos de catálogo

Para quem usa a `platform-catalog`, existem somente dois tipos.

### Dynamic Catalog

Use `DynamicCatalogService` quando o banco é a fonte de verdade para os valores do catálogo.

- novos `name` podem ser criados em runtime;
- não exige enum;
- novos valores não exigem alteração de código ou deploy;
- mantém CRUD, `active`, `restore`, `sortOrder` e `settings`.

### Enum Catalog

Use `EnumCatalogService` quando os valores permitidos precisam ser conhecidos pelo código.

- o catálogo continua persistido e administrável;
- `name` precisa existir em um enum que implementa `CatalogEnum`;
- um novo `name` exige alteração do enum e deploy;
- `label`, `description`, `settings`, ordenação e ativação continuam administráveis no banco.

> Se algo existe somente como enum Java e não precisa de persistência ou administração, ele está fora do escopo desta biblioteca.

## Como escolher

```text
Os names podem ser criados em runtime?
│
├── SIM
│   └── DynamicCatalogService
│
└── NÃO, o código define os valores permitidos
    └── EnumCatalogService
```

Depois disso, faça uma segunda pergunta:

```text
O catálogo possui campos ou regras específicas?
│
├── NÃO
│   └── caminho simples
│
└── SIM
    └── caminho extensível com Base*
```

## Developer experience

### Caminho simples

Quando o catálogo só possui os campos padrão da lib, **não é necessário criar DTO, mapper ou validator próprios**.

A API principal é:

```text
CatalogDTO
DynamicCatalogService
EnumCatalogService
CatalogController
```

Para um Enum Catalog, o consumidor normalmente cria somente:

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

Para um Dynamic Catalog, use `DynamicCatalogService` e não crie enum.

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

## Integridade operacional

- Restore revalida o DTO antes de reativar o registro.
- JSON inválido persistido em `settings` é tratado como corrupção de dados e falha explicitamente; não é convertido silenciosamente para `{}`.
- `sortOrder` automático usa o maior valor atual quando o valor recebido é ausente ou menor que 1.
- Se o domínio exigir `sortOrder` estritamente único sob concorrência, deve fornecer constraint/locking próprio.
- Validação Java não substitui constraints físicas do banco.

## Regra de simplicidade

A API pública usa nomes que explicam diretamente o comportamento:

```text
DynamicCatalogService = valores definidos em runtime/banco
EnumCatalogService    = valores permitidos pelo enum
CatalogController     = controller para o contrato padrão
CatalogDTO            = DTO padrão
```

## Ownership

```text
platform-catalog = COMO um catálogo persistido e administrável funciona
microserviço     = QUAL catálogo existe e suas regras de domínio
```

## Guia completo

Veja:

```text
docs/USAGE.md
```
