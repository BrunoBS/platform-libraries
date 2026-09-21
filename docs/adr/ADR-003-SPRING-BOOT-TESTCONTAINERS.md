# ADR-003 — Spring Boot como fonte de verdade para Testcontainers

## Status

Accepted.

## Contexto

O `platform-dependencies:1.0.0` importava:

- `spring-boot-dependencies:4.1.1`;
- `testcontainers-bom:1.21.4`.

O Spring Boot 4.1.1 já gerencia a família Testcontainers em `2.0.5`. Manter um segundo BOM da mesma tecnologia criava duas fontes de verdade e permitia uma árvore híbrida, pois o Spring Boot podia gerenciar módulos transitivos de Testcontainers 2.x enquanto dependências opcionais do `platform-testing` ainda usavam coordenadas do Testcontainers 1.x.

## Decisão

Spring Boot é a fonte de verdade para Testcontainers enquanto a Foundation usar Spring Boot 4.1.1 como baseline tecnológico.

O `platform-dependencies`:

- não declara `testcontainers.version`;
- não importa `org.testcontainers:testcontainers-bom`.

O `platform-testing` usa as coordenadas de módulos compatíveis com Testcontainers 2.x:

```text
org.testcontainers:testcontainers-mysql
org.testcontainers:testcontainers-kafka
org.testcontainers:testcontainers-junit-jupiter
```

Essas dependências permanecem opcionais, preservando o contrato de infraestrutura de teste opt-in.

## Versionamento desta correção

A correção ocorre antes do fechamento de `FOUNDATION-GOLDEN-V1` e antes de uso produtivo.

Por decisão do projeto, as coordenadas próprias permanecem em `1.0.0`. A publicação anterior dessa versão deve ser removida do registry antes de uma nova publicação; não há sobrescrita in-place de release.

## Consequências

- uma única fonte de verdade para Testcontainers;
- alinhamento automático com o baseline Spring Boot;
- eliminação da combinação Testcontainers 1.x/2.x no mesmo grafo;
- nenhuma mudança de API Java da capability;
- nenhuma mudança no caráter opcional da infraestrutura de teste.

Se no futuro a Foundation precisar divergir deliberadamente da versão de Testcontainers gerenciada pelo Spring Boot, isso exigirá decisão explícita e teste de compatibilidade.
