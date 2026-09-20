# ADR-002 — Adotar namespace PortalManager na Foundation

## Status

Accepted.

## Contexto

A Foundation consolidada ainda utilizava o namespace provisório `com.empresa.platform` tanto nas coordenadas Maven quanto nos packages Java.

Antes do checkpoint `FOUNDATION-GOLDEN-V1`, foi decidido substituir esse namespace genérico pela identidade definitiva do produto, evitando publicar a Foundation oficial com coordenadas provisórias e evitando uma migração incompatível depois que consumidores reais estiverem estabilizados.

## Decisão

O namespace oficial da Foundation passa a ser:

```text
br.com.portalmanager.core
```

A decisão se aplica a dois contratos diferentes:

1. coordenadas Maven próprias da Foundation, via `groupId`;
2. package root Java e paths correspondentes em `src/main/java` e `src/test/java`.

Exemplos:

```text
com.empresa.platform:platform-parent
→ br.com.portalmanager.core:platform-parent

com.empresa.platform:platform-catalog
→ br.com.portalmanager.core:platform-catalog

com.empresa.platform.catalog
→ br.com.portalmanager.core.catalog
```

Os `artifactId` permanecem inalterados:

```text
platform-parent
platform-dependencies
platform-libraries-bom
platform-starter
platform-logging
platform-messaging
platform-authorization
platform-audit
platform-catalog
platform-tagging
platform-test-support
```

## Versionamento

O baseline oficial continua em `1.0.0`.

A publicação anterior em `com.empresa.platform:*` ocorreu antes do checkpoint e é considerada experimental/obsoleta. Como o novo `groupId` cria coordenadas Maven distintas, o baseline definitivo pode permanecer `1.0.0` sem sobrescrever os artifacts anteriores.

A release train continua controlada por:

```text
.mvn/maven.config
-Drevision=1.0.0
```

`platform-parent` e `platform-dependencies` continuam versionados de forma independente, também em `1.0.0` neste baseline inicial.

## Migração de código

Todos os paths rastreados contendo:

```text
com/empresa/platform
```

são movidos para:

```text
br/com/portalmanager/core
```

Todas as declarações de package, imports, referências de classes em resources, exemplos e coordenadas Maven atuais são atualizadas para o novo namespace.

Não será criada camada de compatibilidade ou alias para `com.empresa.platform`, pois o checkpoint oficial ainda não foi fechado e nenhum consumidor deve estabilizar o namespace provisório.

## Consequências

- a Foundation passa a possuir identidade Maven e Java definitiva;
- consumidores deverão usar `br.com.portalmanager.core` em parent, BOM, dependencies e imports;
- artifacts publicados anteriormente em `com.empresa.platform` não pertencem ao baseline oficial;
- após a publicação das novas coordenadas, o `account-service` deve provar consumo remoto exclusivamente do namespace novo.

## Validação obrigatória

Antes de mergear esta migração:

1. não pode existir path rastreado contendo `com/empresa/platform`;
2. não pode existir referência textual ativa a `com.empresa.platform` fora de documentação histórica explícita;
3. `mvn clean verify` deve concluir com sucesso em Java 25;
4. os POMs achatados da release train devem conter coordenadas concretas;
5. após o merge, o baseline `br.com.portalmanager.core:*:1.0.0` deve ser publicado remotamente e consumido por um serviço real antes do checkpoint.
