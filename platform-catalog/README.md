# platform-catalog

Infraestrutura reutilizável para catálogos persistidos e administráveis pelos microserviços da plataforma.

## Princípio

A biblioteca define **como** um catálogo gerenciado funciona; cada microserviço continua dono de **quais** catálogos pertencem ao seu domínio.

A infraestrutura comum de ciclo de vida CRUD é fornecida pela `platform-crud`. Essa é a única dependência interna intencional da `platform-catalog`.

A `platform-catalog` não depende de `platform-messaging`, `platform-authorization` ou `platform-logging`. Ela possui sua própria semântica de erro e permite que o consumidor adapte essa semântica para HTTP, i18n, `ProblemDetail` ou qualquer outro padrão da aplicação.

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

### MANAGED_CONSTRAINED

Catálogo persistido e administrável, mas com conjunto de nomes permitido controlado em código.

- mantém CRUD, `active`, `restore`, `sortOrder` e `settings`;
- usa a mesma infraestrutura de `MANAGED`;
- adiciona `CatalogEnum` e `EnumCatalogValidator` para restringir os nomes aceitos.

## Contratos da base

Os catálogos persistidos da plataforma usam `Long` como identificador técnico. A API pública da biblioteca segue esse contrato de forma explícita em DTO, repository, validator, service e controller.

A base também **não impõe unicidade global de `name` no mapeamento JPA**. A constraint física pertence ao serviço consumidor e deve refletir a identidade real do catálogo, por exemplo:

```text
name
account_id + name
application_id + name
scope + name
```

`BaseCatalogValidator` fornece unicidade simples por nome como política padrão, mas o hook `validateUniqueness` pode ser sobrescrito para catálogos com escopo.

## Estrutura fornecida

```text
model/
  BaseCatalogEntity
  CatalogEnum

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
  EnumCatalogValidator

exception/
  CatalogException
  CatalogNotFoundException
  CatalogRestoreException
  CatalogValidationException

service/
  BaseCatalogService

web/
  BaseCatalogController
```

Internamente, DTO, repository, mapper, validator e service reutilizam `platform-crud`. O controller de catálogo permanece especializado porque seu contrato HTTP inclui listagem com filtros, criação em lote e `restore`, comportamentos que não pertencem ao CRUD genérico.

## Semântica de erros

A lib fornece exceptions próprias de catálogo como defaults, sem conhecer o mecanismo de apresentação do consumidor.

`BaseCatalogService` mantém pontos de extensão para que o serviço consumidor substitua a semântica quando necessário:

```text
notFoundException(id)
restoreException(id)
```

Na validação, `BaseValidator` usa o modelo neutro `CrudValidationResult` da `platform-crud` e converte o resultado para `CatalogValidationException`. O validator concreto pode sobrescrever `validationException(...)` se quiser outro contrato.

Assim, por exemplo, a `account-api` pode traduzir essas exceptions para o padrão de erro de `platform-messaging` sem criar dependência entre as duas bibliotecas.

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
- política padrão de unicidade por nome, extensível por hook;
- filtros adicionais extensíveis pelo serviço concreto;
- CRUD REST reutilizável.

## Extensibilidade

`BaseCatalogValidator` não conhece `enum`, `FeatureType`, `scope` ou qualquer catálogo concreto. Validações específicas devem ser adicionadas pelos hooks disponíveis no validator concreto.

Quando um catálogo precisar restringir seus nomes a valores conhecidos em código, o serviço pode estender `EnumCatalogValidator`. Assim, o vínculo com `enum` fica isolado na especialização e não contamina o modelo `MANAGED`.

Quando a unicidade depender de um escopo, o validator concreto sobrescreve `validateUniqueness` e o serviço define a constraint equivalente em sua migration.

Filtros particulares podem ser implementados sobrescrevendo `matchesAdditionalFilters` em `BaseCatalogService`.

A autorização permanece responsabilidade do controller concreto; a biblioteca não impõe `OWNER` nem qualquer política de acesso.

## Guia de implementação

O passo a passo completo para integrar a lib em um microserviço está em:

```text
docs/USAGE.md
```

## Ownership

A biblioteca fornece comportamento e infraestrutura. Ela não deve concentrar os valores de negócio dos microserviços.

```text
platform-crud
      ↓ infraestrutura CRUD
platform-catalog
      ↓ especialização de catálogo
account-api  -> seus próprios catálogos
event-api    -> seus próprios catálogos
route-api    -> seus próprios catálogos
```

## Regra de dependência

```text
platform-catalog → platform-crud
```

Nenhuma outra capability da plataforma é necessária para usar catálogo.
