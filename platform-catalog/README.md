# platform-catalog

Infraestrutura reutilizável para catálogos persistidos e administráveis pelos microserviços da plataforma.

## Princípio

A biblioteca define **como** um catálogo gerenciado funciona; cada microserviço continua dono de **quais** catálogos pertencem ao seu domínio.

A abstração base é totalmente independente de `enum`. O banco pode ser a fonte de verdade do catálogo. O suporte a `enum` existe apenas como especialização opcional para catálogos que precisam restringir os nomes permitidos.

Exemplos:

- `account-api` continua dono de `AccountType`, `EnvironmentType`, `LanguageType`, etc.;
- `event-api` pode ser dono de `OperationType`;
- nenhum catálogo de negócio deve ser centralizado nesta biblioteca.

## Modelos de catálogo

### STATIC

Catálogo fixo representado somente em código, normalmente por `enum`.

- não precisa tabela;
- não precisa CRUD;
- não usa a infraestrutura persistida desta lib.

### MANAGED

Catálogo persistido e administrável em runtime.

- banco é a fonte de verdade;
- novos valores podem ser criados sem recompilar o serviço;
- usa `BaseCatalogEntity`, `BaseCatalogDTO`, `BaseCatalogRepository`, `BaseCatalogValidator`, `BaseCatalogService` e `BaseCatalogController`.

Exemplo: `OperationType` administrável pelo `event-api`.

### MANAGED_CONSTRAINED

Catálogo persistido e administrável, mas com conjunto de nomes permitido controlado em código.

- mantém CRUD, `active`, `restore`, `sortOrder` e `settings`;
- usa a mesma infraestrutura de `MANAGED`;
- adiciona `CatalogEnum` e `EnumCatalogValidator` para restringir os nomes aceitos.

Esse modelo atende catálogos que precisam de representação forte no código sem tornar `enum` uma dependência da abstração genérica.

## Estrutura fornecida

```text
model/
  BaseCatalogEntity
  CatalogEnum                  # opcional

dto/
  BaseCatalogDTO

repository/
  BaseCatalogRepository

mapper/
  BaseMapper
  BaseCatalogMapper

validation/
  BaseValidator
  BaseCatalogValidator
  EnumCatalogValidator         # opcional

service/
  BaseCatalogService

web/
  BaseCatalogController
```

## Comportamento padrão de catálogos gerenciados

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

`BaseCatalogValidator` não conhece `enum`, `FeatureType`, `scope` ou qualquer catálogo concreto. Validações específicas devem ser adicionadas pelos hooks disponíveis no validator concreto.

Quando um catálogo precisar restringir seus nomes a valores conhecidos em código, o serviço pode estender `EnumCatalogValidator`. Assim, o vínculo com `enum` fica isolado na especialização e não contamina o modelo `MANAGED`.

Filtros particulares podem ser implementados sobrescrevendo `matchesAdditionalFilters` em `BaseCatalogService`.

A autorização permanece responsabilidade do controller concreto; a biblioteca não impõe `OWNER` nem qualquer política de acesso.

## Ownership

A biblioteca fornece comportamento e infraestrutura. Ela não deve concentrar os valores de negócio dos microserviços.

```text
platform-catalog
      ↓ infraestrutura
account-api  -> seus próprios catálogos
event-api    -> seus próprios catálogos
route-api    -> seus próprios catálogos
```

## Migração

Nesta etapa, a estrutura equivalente permanece dentro da `account-api`. A migração daquele serviço para `platform-catalog` será feita separadamente, após validação da abstração e dos contratos públicos da lib.
