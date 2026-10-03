# Platform Tagging

Tagging reutilizável com Spring Data JPA, mantendo uma tabela própria por recurso.

## Objetivo

O serviço consumidor define somente a entidade específica, a associação JPA com o owner/FK, a factory e um repository que estende `TagRepository`. A library centraliza normalização, reconciliação, consultas e persistência comum.

## Contratos

- `TagOwner`: owner com `id` interno e `identifier` público;
- `Tag`: `@MappedSuperclass` com `id`, `TagName` e `TagOriginType`;
- `TagRepository`: repository JPA genérico com as consultas comuns;
- `TagManager`: reconciliação e consultas;
- `TagFactory`: criação da entidade específica.

`TagName` normaliza trim, lowercase e espaços para hífen. `TagNameConverter` persiste o value object em coluna VARCHAR.

## Entidade do consumidor

A entidade específica estende `Tag<Workspace>`, mantém somente a associação `owner` anotada com `@ManyToOne`/`@JoinColumn`, chama `super(name, originType)` e implementa `getOwner()`. A propriedade da associação deve se chamar `owner`; a coluna continua específica, como `workspace_id` ou `application_id`.

## Repository do consumidor

O repository fica reduzido a `WorkspaceTagRepository extends TagRepository<WorkspaceTag, Workspace>`. Não é necessário repetir queries, `saveAll`, `deleteAll` ou adapters de persistência.

## Manager

A configuração cria `new TagManager<>(repository, WorkspaceTag::new)`.

## Regras

- cada recurso mantém sua própria tabela e FK;
- deve existir constraint única `(owner_id, name)`;
- `MANUAL` prevalece sobre `SYSTEM`;
- mudança de origem é salva explicitamente e não depende de dirty checking;
- buscas por tag são exatas depois da normalização;
- normalização pertence ao `platform-tagging`, não ao normalizer do domínio consumidor;
- command services consumidores chamam `reconcile` dentro da transação da operação de negócio.
