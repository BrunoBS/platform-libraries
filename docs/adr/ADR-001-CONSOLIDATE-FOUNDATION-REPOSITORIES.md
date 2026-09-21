# ADR-001 — Consolidate Foundation Repositories

## Status

Accepted. A decisão posterior de namespace é registrada na ADR-002.

## Context

O primeiro consumidor real da Foundation mostrou que o split entre `platform-build` e `platform-libraries` produz dois problemas: o `platform-parent` conhece versões de capabilities e consumidores precisam consultar dois package repositories.

## Problem

O parent de build deve versionar governança de build, não releases funcionais das libraries.

A Foundation também precisa ter um único contexto oficial de publicação sem recriar `platform-crud`, sem criar outro repositório e sem esconder responsabilidades no aggregator.

## Alternatives considered

### Manter dois repositórios e criar apenas um libraries BOM

Rejeitada. Retiraria versões das capabilities do parent, mas manteria dois contextos oficiais de publicação e o `platform-build` continuaria necessário.

### Consolidar os repositórios e manter versions das capabilities no parent

Rejeitada. Resolveria a topologia física, mas preservaria o acoplamento de versionamento que motivou a mudança.

### Consolidar e separar parent / technology BOM / libraries BOM

Escolhida.

## Decision

O repositório oficial será `BrunoBS/platform-libraries` com:

```text
pom.xml                    reactor/aggregator
platform-parent            governança de build
platform-dependencies      BOM tecnológico externo
platform-libraries-bom     BOM das capabilities
modules/platform-*         capabilities independentes
```

O root POM será apenas aggregator e ponto de publicação do reactor. Não concentrará regras de build. As capabilities ficam agrupadas fisicamente sob `modules/`. A consolidação originalmente preservou coordenadas; a alteração posterior do `groupId` e do namespace Java é decidida separadamente pela ADR-002. Os `artifactId` permanecem inalterados.

`platform-parent` conterá Java 25, Maven mínimo, plugins, testes, Enforcer, convergence e importará apenas o baseline tecnológico via `platform-dependencies`.

`platform-parent` não gerenciará nenhuma versão de capability.

`platform-dependencies` continuará contendo somente dependências externas.

`platform-libraries-bom` gerenciará:

- platform-starter;
- platform-observability;
- platform-messaging;
- platform-authorization;
- platform-audit;
- platform-catalog;
- platform-tagging;
- platform-testing.

## Versioning strategy for this migration

Os eixos são independentes.

- `platform-dependencies:1.0.0`: baseline inicial da topologia consolidada.
- `platform-parent:1.0.0`: baseline inicial do parent de build na topologia consolidada.
- `platform-libraries-bom:1.0.0` e capabilities `1.0.0`: release train inicial coerente da Foundation consolidada.
- root aggregator `platform-libraries:1.0.0`: versão operacional do reactor; não é um eixo de consumo.

A coincidência entre `platform-parent:1.0.0` e release train `1.0.0` não implica versionamento acoplado.

### Single source of truth da release train

A versão da release train de capabilities passa a ter uma única fonte em `.mvn/maven.config`:

```text
-Drevision=1.0.0
```

O root reactor, `platform-libraries-bom` e as oito capabilities usam `${revision}` como versão do próprio artifact. Dependências internas entre capabilities usam `${project.version}`, preservando o release train sem repetir números de versão nos POMs.

Como o baseline Maven permanece 3.9.9, os artifacts que usam CI-friendly versions são publicados com `flatten-maven-plugin` em modo `resolveCiFriendliesOnly`, garantindo POM consumidor com versão resolvida.

`platform-parent` e `platform-dependencies` permanecem fora dessa `revision` e mantêm versionamento independente. O parent apenas gerencia a configuração do Flatten Plugin; capabilities o ativam explicitamente, evitando impor esse comportamento aos serviços consumidores.

## Publishing

Todos os artefatos serão publicados em:

`https://maven.pkg.github.com/brunobs/platform-libraries`.

Após o checkpoint nenhum fluxo oficial deverá consultar `maven.pkg.github.com/brunobs/platform-build`.

## Consumers

Um consumidor deve combinar explicitamente:

1. `platform-parent` como parent;
2. `platform-libraries-bom` em dependency management;
3. capabilities sem versão.

## platform-build

O repositório antigo não será apagado nem arquivado durante a implementação. Após build, publicação e consumo remoto validados, será documentado como legado/obsoleto e recomendado para arquivamento manual.

## Positive consequences

- capability releases deixam de forçar versões do parent;
- um único registry oficial;
- responsabilidades Maven explícitas;
- consumidores escolhem build baseline e capability baseline separadamente.

## Trade-offs

- mais um artefato BOM explícito no POM consumidor;
- a release train continua sendo coordenada, mas a versão é alterada em um único ponto via `.mvn/maven.config`;
- o reactor passa a depender da resolução correta de parent/BOM siblings, que deve ser coberta pelo CI.

## Rollback

Enquanto o checkpoint novo não for declarado, o fluxo publicado anterior permanece disponível nos packages de `platform-build` e `platform-libraries`. Um rollback pode restaurar o consumo das versões 1.0.x sem apagar repositórios ou packages.


## Implementation finding — GitHub Packages Maven cutover

A estrutura decidida por este ADR compila e testa no mesmo reactor. O Verify #60, run `35537837948`, concluiu com sucesso.

Durante a publicação foi encontrado um limite operacional que não altera a separação arquitetural decidida acima:

- deploy de `platform-dependencies:1.0.3` para o repository de `platform-libraries` retorna HTTP 422;
- um probe Maven standalone no run `35538856486` confirma que esse artifact não é resolvido remotamente pelo novo endpoint;
- `account-service` também não resolve `platform-parent:1.1.0` exclusivamente pelo novo endpoint.

As capabilities `1.1.0` foram publicadas no novo registry, mas não formam ainda um baseline consumível sem o parent estrutural.

O registry Maven do GitHub usa escopo por repository. A migração dos artifacts estruturais existentes exige uma decisão operacional explícita; ela não será inferida silenciosamente.

### Alternatives pending decision

1. migrar destrutivamente os packages estruturais existentes para o contexto de `platform-libraries`, preservando as coordenadas;
2. adotar novas coordenadas Maven para os artifacts estruturais e migrar consumidores;
3. manter os artifacts estruturais no registry de `platform-build`, o que preserva compatibilidade mas não atende ao objetivo deste ADR de eliminar o repository antigo do fluxo oficial.

Nenhuma dessas alternativas é aprovada automaticamente por este ADR. Até a decisão, `platform-build` não deve ser arquivado.
