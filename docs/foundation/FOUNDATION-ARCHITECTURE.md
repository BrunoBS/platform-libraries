# Foundation Architecture

## Estado da refatoração de consolidação

A Foundation está sendo consolidada em um único repositório oficial: `BrunoBS/platform-libraries`.

A arquitetura alvo e já implementada na branch `refactor/consolidate-foundation-repositories` é:

```text
platform-libraries/
├── pom.xml                    reactor/aggregator
├── platform-parent/           governança de build
├── platform-dependencies/     BOM tecnológico externo
├── platform-libraries-bom/    BOM das capabilities
├── platform-starter/
├── platform-logging/
├── platform-messaging/
├── platform-authorization/
├── platform-audit/
├── platform-catalog/
├── platform-tagging/
└── platform-test-support/
```

O root POM é somente aggregator/reactor. Não concentra regras de build.

## Responsabilidades

### platform-parent

Responsável por Java 25, Maven mínimo, compiler, Surefire, Failsafe, JaCoCo, Enforcer, dependency convergence e plugins comuns.

Importa apenas `platform-dependencies`.

Não contém versões de capabilities.

### platform-dependencies

Representa exclusivamente o baseline tecnológico externo, incluindo Spring Boot 4.1.1, Testcontainers e ferramentas comuns.

Não gerencia artifacts da própria Golden Platform.

### platform-libraries-bom

Representa o baseline das capabilities e gerencia:

- platform-starter;
- platform-logging;
- platform-messaging;
- platform-authorization;
- platform-audit;
- platform-catalog;
- platform-tagging;
- platform-test-support.

## Capabilities

O starter obrigatório continua agregando somente logging, messaging e authorization.

Audit, catalog e tagging permanecem explícitos. Test-support permanece capability de testes.

`platform-crud` continua removido e não participa do reactor.

## Dependências internas relevantes

```text
starter -> logging
starter -> messaging
starter -> authorization
authorization -> messaging
audit -> authorization
audit -> messaging
catalog -> messaging
```

Nenhum comportamento funcional aprovado das capabilities foi alterado nesta consolidação.

## Distribuição alvo

Após o checkpoint desta refatoração, o único registry oficial deverá ser:

`https://maven.pkg.github.com/brunobs/platform-libraries`

O fluxo alvo é:

```text
platform-libraries
  -> clean verify
  -> deploy
GitHub Packages / platform-libraries
  -> account-service
  -> clean verify
```

O registry de `platform-build` não fará parte do fluxo oficial.

## Estado atual

A estrutura alvo está implementada e foi validada no GitHub Actions Verify #60, run `35537837948`, com `BUILD SUCCESS`.

As oito capabilities `1.1.0` foram publicadas no registry de `platform-libraries` no run `35538290455`.

O cutover remoto dos artifacts estruturais permanece pendente: um probe Maven standalone, fora do reactor e com repositório local isolado, falhou no run `35538856486` ao resolver `platform-dependencies:1.0.3` pelo registry novo. O `account-service` também não resolve `platform-parent:1.1.0` exclusivamente desse registry.

Até esse ponto ser resolvido, a arquitetura alvo está implementada no código, mas a topologia de distribuição não pode ser declarada concluída.
