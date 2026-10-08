# ADR-004 — Namespace da Platform e naming de capabilities

## Status

Accepted.

## Data

2026-09-21

## Supersedes

Esta ADR substitui a decisão de namespace da `ADR-002-PORTALMANAGER-NAMESPACE.md`.

## Contexto

O namespace `br.com.portalmanager.core` não comunica com precisão que os artifacts pertencem à Golden Platform e pode ser confundido com um domínio central de uma aplicação.

A revisão de naming também identificou dois módulos cujo nome público não representa mais adequadamente a responsabilidade:

- `platform-logging`: implementa hoje logging estruturado, mas é o ponto de entrada de observabilidade da plataforma;
- `platform-test-support`: tornou-se uma capability completa de testing, com annotations, infraestrutura de integração, Testcontainers, WireMock, RestAssured, fixtures e validação arquitetural.

## Decisão — namespace

O namespace oficial Maven e Java da Foundation passa a ser:

```text
br.com.portalmanager.platform
```

A mudança é aplicada em conjunto a:

- `groupId` Maven;
- package root Java;
- imports e resources;
- exemplos e documentação ativa.

Não haverá alias ou camada de compatibilidade com `br.com.portalmanager.core`.

## Decisão — observability

O artifact `platform-logging` passa a ser:

```text
platform-observability
```

O logging estruturado atual reside em:

```text
br.com.portalmanager.platform.observability.logging
```

As propriedades passam a usar:

```text
platform.observability.logging
```

O rename não cria métricas ou tracing. Essas capacidades só serão adicionadas quando houver caso real aprovado.

## Decisão — testing

O artifact `platform-test-support` passa a ser:

```text
platform-testing-core
```

Seu package público é:

```text
br.com.portalmanager.platform.testing
```

O nome representa a responsabilidade atual do módulo sem caracterizá-lo como framework próprio.

## Estrutura resultante

```text
platform-libraries/
├── platform-parent/
├── platform-dependencies/
├── platform-libraries-bom/
└── modules/
    ├── platform-starter/
    ├── platform-observability/
    ├── platform-messaging/
    ├── platform-authorization/
    ├── platform-audit/
    ├── platform-catalog/
    ├── platform-tagging/
    └── platform-testing-core/
```

## Versionamento

Por decisão explícita do projeto, todos os artifacts próprios permanecem em:

```text
1.0.0
```

O novo `groupId` cria coordenadas Maven distintas das coordenadas anteriores. Os dois módulos renomeados também possuem novos `artifactId`.

## Consequências

- consumidores devem migrar parent, BOM, dependencies e imports para `br.com.portalmanager.platform`;
- `platform-starter` agrega `platform-observability`;
- dependências de teste usam `platform-testing-core`;
- `platform.logging.*` deixa de ser contrato vigente; o prefixo passa a `platform.observability.logging.*`;
- a Golden Reference deve ser revalidada consumindo exclusivamente os artifacts remotos do novo namespace antes de iniciar G5.

## Validação obrigatória

1. nenhum path ativo em `br/com/portalmanager/core`;
2. nenhuma referência ativa em código/POM a `br.com.portalmanager.core`;
3. nenhum módulo ativo `platform-logging`;
4. nenhum módulo ativo `platform-test-support`;
5. reactor completo verde em Java 25;
6. publicação de `br.com.portalmanager.platform:*:1.0.0`;
7. `account-service` verde com Maven repository isolado consumindo o novo namespace.
