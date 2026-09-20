# ADR-001 — Consolidate Foundation Repositories

## Status

Accepted for implementation.

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
platform-*                 capabilities independentes
```

O root POM será apenas aggregator e ponto de publicação do reactor. Não concentrará regras de build.

`platform-parent` conterá Java 25, Maven mínimo, plugins, testes, Enforcer, convergence e importará apenas o baseline tecnológico via `platform-dependencies`.

`platform-parent` não gerenciará nenhuma versão de capability.

`platform-dependencies` continuará contendo somente dependências externas.

`platform-libraries-bom` gerenciará:

- platform-starter;
- platform-logging;
- platform-messaging;
- platform-authorization;
- platform-audit;
- platform-catalog;
- platform-tagging;
- platform-test-support.

## Versioning strategy for this migration

Os eixos são independentes.

- `platform-dependencies:1.0.3`: nova publicação/topologia do mesmo baseline tecnológico, sem alteração de Spring Boot.
- `platform-parent:1.1.0`: mudança de contrato do parent ao remover dependency management das capabilities e migrar o contexto oficial.
- `platform-libraries-bom:1.1.0` e capabilities `1.1.0`: release train coerente da Foundation consolidada.
- root aggregator `platform-libraries:1.1.0`: versão operacional do reactor; não é um eixo de consumo.

A coincidência entre `platform-parent:1.1.0` e release train `1.1.0` não implica versionamento acoplado.

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
- release train das capabilities ainda exige atualização coordenada dos módulos e do BOM;
- o reactor passa a depender da resolução correta de parent/BOM siblings, que deve ser coberta pelo CI.

## Rollback

Enquanto o checkpoint novo não for declarado, o fluxo publicado anterior permanece disponível nos packages de `platform-build` e `platform-libraries`. Um rollback pode restaurar o consumo das versões 1.0.x sem apagar repositórios ou packages.
