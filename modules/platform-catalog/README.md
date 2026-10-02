# platform-catalog

Infraestrutura reutilizável para catálogos persistidos e administráveis pelos microserviços da plataforma.

## Identidade padrão

Todos os catálogos usam `code` como identidade semântica e chave primária:

```text
code        VARCHAR(50) PK
label       VARCHAR
description TEXT
sortOrder   INTEGER
active      BOOLEAN
settings    TEXT/JSON
```

Não existe `Long id` nem `name` no contrato base.

O `code` é obrigatório, imutável depois da criação, possui no máximo 50 caracteres e deve obedecer:

```text
^[A-Z][A-Z0-9_]{0,49}$
```

Ele também é a chave usada em URLs e relacionamentos de banco.

## Tipos de catálogo

### Included Catalog

`IncludedCatalogService` (o modelo Included) é usado quando novos códigos podem ser incluídos em runtime e o banco é a fonte de verdade.

### Enum Catalog

`EnumCatalogService` é usado quando os códigos permitidos precisam existir em um enum que implementa `CatalogEnum`.

A persistência é igual nos dois modelos; muda somente quem governa quais `code` podem existir.

## API principal

```text
CatalogDTO
IncludedCatalogService
EnumCatalogService
CatalogController
```

`CatalogDTO` expõe:

```text
String code
String label
String description
Integer sortOrder
JsonNode settings
```

## Contrato público Golden

O consumidor deve compor um catálogo usando apenas os contratos necessários ao seu modelo:

```text
CatalogEntity
CatalogRepository
CatalogDTO
CatalogController
EnumCatalogService | IncludedCatalogService
CatalogEnum        (somente Enum Catalog)
AbstractCatalogCode (quando o domínio precisar de um VO de referência)
CatalogSettingsValidator (quando houver settings validados)
```

`CatalogMapper`, `AbstractCatalogService`, `AbstractCatalogValidator` e
`EnumCatalogValidator` sustentam a implementação da library. Não são pontos
de extensão do microserviço consumidor.

A regra Golden é preferir composição pelos contratos acima e não criar camadas
intermediárias no microserviço para substituir comportamento já fornecido pela
library.

## API HTTP padrão

```text
GET    /api/v1/<catalog>
GET    /api/v1/<catalog>/{code}
POST   /api/v1/<catalog>
PUT    /api/v1/<catalog>/{code}
DELETE /api/v1/<catalog>/{code}
POST   /api/v1/<catalog>/{code}/restore
```

## Comportamento padrão

- CRUD por `code`;
- soft delete por `active=false`;
- restore com revalidação;
- `sortOrder` automático quando não informado;
- validações acumuladas usam diretamente o `ValidationResult` compartilhado da `platform-messaging`;
- filtros padrão por `active` e `code`;
- ordenação por `sortOrder` e `code`;
- validação do formato e duplicidade do `code`;
- no `EnumCatalogService`, validação do `code` contra o enum;
- no `IncludedCatalogService`, criação de novos códigos em runtime;
- validação opcional de `settings` por `CatalogSettingsValidator`.

## Relacionamentos

Entidades de negócio devem usar o código como FK física:

```text
workspaces.workspace_type_code
        ↓
type_workspaces.code
```

## Contratos de código

O `code` persistido e administrável por `CatalogEntity` segue o formato uppercase/underscore documentado acima. `AbstractCatalogCode` é um Value Object semântico reutilizável e deliberadamente aceita também códigos dinâmicos como `workspace-service`; os dois contratos têm finalidades diferentes e não devem ser confundidos.

## Extension points

`AbstractCatalogService` é `sealed`. Os únicos pontos de extensão direta são:

```text
EnumCatalogService
IncludedCatalogService
```

## Ownership

```text
platform-catalog = COMO um catálogo persistido e administrável funciona
microserviço     = QUAL catálogo existe e suas regras de domínio
```

Veja também `docs/USAGE.md`.