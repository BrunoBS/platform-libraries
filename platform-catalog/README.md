# platform-catalog

Infraestrutura reutilizável para catálogos persistidos e administráveis pelos microserviços da plataforma.

## Princípio

A biblioteca define **como** um catálogo gerenciado funciona; cada microserviço continua dono de **quais** catálogos pertencem ao seu domínio.

Exemplos:

- `account-api` continua dono de `AccountType`, `EnvironmentType`, `LanguageType`, etc.;
- `event-api` pode ser dono de `OperationType`;
- nenhum catálogo de negócio deve ser centralizado nesta biblioteca.

## Estrutura fornecida

```text
model/
  BaseType
  BaseEnum

dto/
  BaseTypeDTO

repository/
  BaseRepository

mapper/
  BaseMapper
  BaseTypeMapper

validation/
  BaseValidator
  BaseTypeValidator

service/
  BaseService

web/
  BaseController
```

## Comportamento padrão

A abstração fornece:

- listagem por ativo/inativo;
- busca por nome e identificador;
- criação e atualização;
- soft delete;
- restore;
- ordenação (`sortOrder`);
- busca por múltiplos nomes (`findByNames`);
- validações comuns;
- filtros adicionais extensíveis pelo serviço concreto;
- CRUD REST reutilizável.

## Extensibilidade

A base não conhece `FeatureType`, `scope` nem qualquer outro catálogo específico. Filtros particulares devem ser implementados sobrescrevendo `matchesAdditionalFilters` no service concreto. Validações específicas de `settings` devem usar `validateSettings` no validator concreto.

A autorização também permanece responsabilidade do controller concreto, evitando impor `OWNER` a todos os consumidores da biblioteca.

## Migração

Nesta primeira etapa, a estrutura equivalente permanece dentro da `account-api`. A migração daquele serviço para `platform-catalog` será feita separadamente, após validação desta abstração.
