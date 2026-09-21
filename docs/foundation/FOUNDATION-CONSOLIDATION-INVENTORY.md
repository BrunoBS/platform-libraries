# Foundation Consolidation Inventory

## Objetivo

Inventário anterior à consolidação de `platform-build` dentro de `platform-libraries`.

Referência de partida de `platform-libraries`: `refactor/golden-foundation`, commit `db95a108d68aeddd4f6bddf3391a4986ac37064c`.

## Estado encontrado — platform-build

O repositório oficial anterior contém:

- root reactor `platform-build`;
- `platform-parent`;
- `platform-dependencies`;
- workflow `build.yml`;
- workflow manual `publish.yml`.

O `platform-parent:1.0.2` governa Java 25, Maven >= 3.9.9, Compiler, Surefire, Failsafe, JaCoCo, Maven Enforcer e dependency convergence.

Também importa `platform-dependencies:1.0.2`.

Problema confirmado: o parent ainda contém propriedades e dependency management das versões de todas as capabilities `1.0.1`. Portanto uma mudança de release train das libraries força nova versão do artefato de build.

A publicação aponta para `https://maven.pkg.github.com/brunobs/platform-build`.

## Estado encontrado — platform-libraries

O reactor estabilizado contém somente:

- platform-audit;
- platform-messaging;
- platform-authorization;
- platform-observability;
- platform-starter;
- platform-testing;
- platform-catalog;
- platform-tagging.

`platform-crud` não existe na branch estabilizada.

O root atual herda remotamente `platform-parent:1.0.2` e publica no package repository de `platform-libraries`.

As capabilities usam `platform-libraries:1.0.1` como parent intermediário e possuem release train `1.0.1`. Dependências internas ainda registram `1.0.1` explicitamente.

Os settings de verify/publicação ainda precisam consultar o registry de `platform-build` para resolver o parent.

## Estado encontrado — account-service

O consumidor atual:

- usa `platform-parent:1.0.2`;
- declara `platform-starter` e `platform-testing` sem versão;
- não possui BOM de capabilities próprio;
- depende do dependency management de capabilities existente no parent;
- configura os registries de `platform-build` e `platform-libraries`.

Isso comprova o acoplamento que motivou a refatoração.

## Risco de reactor

Ao mover `platform-parent` e `platform-dependencies` para o mesmo reactor:

1. as capabilities podem resolver o parent pelo caminho local `../platform-parent/pom.xml`;
2. `platform-parent` precisa importar `platform-dependencies` como BOM sibling;
3. essa resolução não será presumida como válida: o primeiro gate de implementação será um checkout limpo executando `mvn clean verify` no CI.

O aggregator não receberá regras de build para contornar eventual problema. Se o reactor não resolver o BOM sibling corretamente, o problema será tratado explicitamente sem recriar o acoplamento anterior.

## Estado de publicação

Antes desta migração existem dois contextos oficiais:

```text
platform-build -> GitHub Packages / platform-build
platform-libraries -> GitHub Packages / platform-libraries
```

Estado alvo:

```text
platform-libraries -> GitHub Packages / platform-libraries
```

O repositório `platform-build` permanecerá existente e não arquivado até a conclusão do checkpoint.
