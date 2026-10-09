# platform-catalog

Infraestrutura reutilizável para catálogos persistidos e administráveis pelos microserviços da plataforma.

## Dependência arquitetural: Schema Validation

`platform-catalog` depende oficialmente de `platform-schema-validation`. Essa é uma decisão do Golden Platform Foundation, não um detalhe opcional escondido pela library.

Quando um catálogo possui `settings` governado por contrato, a validação padrão é JSON Schema via `SchemaValidator`, com `resourceType = CATALOG`. O consumidor informa o código do recurso de schema; o Catalog executa a validação no fluxo padrão.

A infraestrutura de Schema Validation precisa ter uma fonte de schemas disponível. O Golden Default usa a view `vw_platform_resource_schemas`, com as colunas `resource_type`, `resource_code`, `schema_version` e `definition`. O consumidor pode:

- disponibilizar a view default;
- alterar `platform.schema-validation.view-name`;
- fornecer uma implementação de `ResourceSchemaRepository`.

Na ausência de uma fonte válida, a aplicação falha no startup com diagnóstico `PLT-SCHEMA-005/007`. Cache local/Redis e demais detalhes de resolução pertencem ao `platform-schema-validation`; o Catalog não replica essas configurações.

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
AbstractCatalogFacade
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
AbstractCatalogFacade
EnumCatalogService | IncludedCatalogService
CatalogEnum         (somente Enum Catalog)
AbstractCatalogCode (quando o domínio precisar de um VO de referência)
SchemaValidator     (contrato padrão para settings governado por JSON Schema)
CatalogSettingsValidator (somente para regra adicional/não-schema)
```

`CatalogMapper`, `AbstractCatalogService`, `AbstractCatalogValidator` e
`EnumCatalogValidator` sustentam a implementação da library. Não são pontos
de extensão do microserviço consumidor.

A regra Golden é preferir composição pelos contratos acima e não criar camadas
intermediárias no microserviço para substituir comportamento já fornecido pela
library.

## Autorização e Facade

O fluxo obrigatório da API HTTP é `CatalogController → AbstractCatalogFacade → AbstractCatalogService`.
O controller recebe uma Facade concreta gerenciada pelo Spring e não deve injetar nem chamar
diretamente serviços de catálogo ou use cases.

Cada catálogo consumidor estende `AbstractCatalogFacade<E>`, injeta seu serviço
no construtor e registra a Facade como bean Spring. Todos os métodos públicos da
Facade abstrata exigem `@AuthorizationRequired(level = OWNER, action = ...)`.
O consumidor pode sobrescrever um método para personalizar a regra, desde que
declare novamente `@AuthorizationRequired` com `level` e `action` explícitos.
Métodos sobrescritos sem a anotação não são permitidos pelo contrato.

O Aspect Spring AOP depende de chamadas externas pelo proxy Spring. Não faça
self-invocation de métodos protegidos para tentar obter uma segunda autorização.
`super.findAll(...)` dentro de uma sobrescrita executa a lógica delegada,
mas não representa uma nova autorização; a autorização ocorre na entrada do método.

Os headers esperados são `Authorization`, `correlation-id`,
`workspace-identifier`, `environment-identifier` e
`application-identifier`. A ausência de token Bearer ou correlation-id
é rejeitada pelo Aspect antes de executar o serviço. A migração de consumidores
é intencionalmente incompatível: o construtor de `CatalogController` recebe
`AbstractCatalogFacade` em vez de `AbstractCatalogService`.

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
- settings governado por contrato usa `platform-schema-validation`;
- `CatalogSettingsValidator` fica reservado a regras adicionais ou não baseadas em JSON Schema.

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
platform-catalog             = COMO um catálogo persistido e administrável funciona
platform-schema-validation   = COMO o contrato JSON Schema é resolvido e validado
microserviço                 = QUAL catálogo existe, qual schema usa e suas regras de domínio
```

Veja também `docs/USAGE.md`.
