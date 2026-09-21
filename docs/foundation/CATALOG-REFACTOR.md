# F3 — Catalog Refactor

## Objetivo

Desacoplar `platform-catalog` de `platform-crud` sem transformar o catálogo em um CRUD genérico renomeado e preservando o comportamento específico de catálogos gerenciados.

## Resultado

A dependência Maven `platform-catalog -> platform-crud` foi removida.

As abstrações anteriormente herdadas do CRUD foram substituídas por conceitos próprios do catálogo:

- `BaseCatalogDTO` declara diretamente o contrato do DTO de catálogo.
- `BaseCatalogMapper` mantém somente o mapeamento necessário ao catálogo.
- `BaseCatalogRepository` passa a estender diretamente `JpaRepository<E, Long>` e `JpaSpecificationExecutor<E>`.
- `BaseCatalogValidator` contém o ciclo de validação necessário aos catálogos.
- `CatalogValidationResult` representa o resultado de validação específico do catálogo e utiliza `ValidationDetail` de messaging.
- `BaseCatalogService` contém diretamente o ciclo de vida específico de catálogo, sem herdar `BaseListCrudService`.
- `DynamicCatalogService`, `EnumCatalogService`, `BaseRelatedCatalogValidator`, `EnumCatalogValidator` e `CatalogSettingsValidator` usam a validação própria do catálogo.
- `CatalogValidationException` não utiliza mais tipos de validação do CRUD.

Foram preservados comportamentos próprios de catálogo: active/inactive, soft delete, restore, ordenação por `sortOrder`, busca por nome e nomes, filtros de catálogo, unicidade, imutabilidade de nome por padrão e extensões de validação/lifecycle necessárias a catálogos gerenciados.

## Varredura de dependência

As buscas de código no escopo `platform-catalog` retornaram zero ocorrências para:

- `platform-crud`
- `BaseCrud`
- `CrudValidation`
- `CrudControllerSupport`
- `br.com.portalmanager.platform.crud`

A busca remota do GitHub indicou `incomplete_results=true`; por isso ela é registrada como evidência auxiliar, não como única prova. A prova funcional principal é a remoção da dependência Maven seguida da compilação e testes bem-sucedidos do módulo e do reactor.

## Validação

Commit validado: `e6daf7480e7b134e25779c8e8d29c43bd6065c57`.

GitHub Actions Verify #22, run `35528800480`:

`mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify`

Resultado:

- Maven Enforcer: sucesso.
- Dependency Convergence: sucesso.
- `platform-catalog`: compilação e empacotamento com sucesso.
- `platform-catalog`: 17 testes, 0 falhas, 0 erros.
- reactor completo: `BUILD SUCCESS`.

## Conclusão da F3

O `platform-catalog` está desacoplado do `platform-crud` no POM e no código compilado. O comportamento mantido pertence ao domínio técnico de catálogos gerenciados e não depende mais das abstrações genéricas de CRUD.

Com este checkpoint, `platform-crud` pode entrar na F4 para busca global, classificação das referências remanescentes e remoção física.
