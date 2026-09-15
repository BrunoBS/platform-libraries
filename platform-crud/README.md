# platform-crud

Infraestrutura CRUD genérica, tipada e sem conhecimento de domínio.

## Objetivo

Padronizar o fluxo repetitivo de CRUD sem impor `name`, `active`, `accountId`, lifecycle, autorização, auditoria ou soft delete.

O microserviço continua responsável por entidade, constraints/migrations, autorização, regras de negócio, restore, integrações e semântica de erros.

## Contrato mínimo

`BaseCrudDTO<ID>` exige somente `ID id()`.

A lib não recria nem normaliza DTOs.

### Contrato de ID

```text
CREATE  -> id deve ser null
FIND    -> id deve ser informado
UPDATE  -> id deve ser informado
DELETE  -> id deve ser informado
```

O `BaseCrudValidator` aplica esse contrato automaticamente.

Chaves padrão:

```text
validation.id.required
validation.id.must-be-absent
```

O consumidor deve disponibilizar essas mensagens no seu i18n ou sobrescrever `idRequiredMessageKey()` / `idMustBeAbsentMessageKey()`.

## Componentes

### Repository

`BaseCrudRepository<E, ID>` estende `JpaRepository`. Consultas específicas permanecem no repository concreto.

### Mapper

`BaseCrudMapper<E, D>` define:

```text
toEntity
toDTO
updateEntity
```

O mapper transfere dados. Evite repository/service dentro dele.

### Validator

`BaseCrudValidator<D>` concentra validação de operação, atributos e integridade.

Fluxos:

```text
CREATE: required -> id absent -> attributes -> create integrity -> additional create
UPDATE: required -> id required -> attributes -> update integrity -> additional update
FIND:   required -> id required -> find validation
DELETE: required -> id required -> delete validation
```

Validação Java melhora a resposta ao usuário, mas não substitui constraint física do banco para concorrência.

## BaseCrudService

### Create

```text
validateForCreate
-> mapper.toEntity
-> beforeCreate
-> repository.save
-> afterCreate
-> mapper.toDTO
```

### Update

```text
validateForUpdate
-> getEntity
-> mapper.updateEntity
-> beforeUpdate
-> repository.save
-> afterUpdate
-> mapper.toDTO
```

`beforeUpdate` recebe a entidade já atualizada pelo mapper. Regras que precisam comparar com o estado persistido anterior devem ficar no validator.

### Delete

```text
validateForDelete
-> getEntity
-> beforeDelete
-> deleteEntity
-> afterDelete
```

O delete padrão é físico. Soft delete deve sobrescrever `deleteEntity`.

## Hooks

```text
beforeCreate / afterCreate
beforeUpdate / afterUpdate
beforeDelete / afterDelete
deleteEntity
getEntity
notFoundException
```

`create`, `update` e `delete` são transacionais. Os hooks `after*` executam após save/delete, mas ainda dentro da mesma transação; não significam AFTER_COMMIT.

Para ações realmente pós-commit, use evento transacional/outbox.

## BaseListCrudService

Adiciona listagem e filtros simples com `Map<String, String>`.

É indicado para coleções pequenas/controladas. Para alto volume, use paginação específica no consumer; não transforme a base em uma abstração universal.

## Controller

A `platform-crud` não fornece controller HTTP genérico.

O controller consumidor define path variables, autorização, status HTTP e composição de contexto. Helpers como `withId` ou `withContext` são conveniências locais do DTO e não fazem parte de `BaseCrudDTO`.

## O que não pertence à base

```text
restore
soft delete obrigatório
lifecycle
accountId/environmentId/applicationId
autorização
auditoria
Kafka/eventos
timestamps
UUID
```

## Princípio

A lib abstrai comportamento repetitivo real sem esconder o domínio. Se um consumer precisa lutar contra a base, a regra provavelmente não pertence à base.
