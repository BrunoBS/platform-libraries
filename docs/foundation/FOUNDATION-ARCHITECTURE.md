# Foundation Architecture

## Estado atual

A Foundation está consolidada no repositório oficial `BrunoBS/platform-libraries`.

A topologia física em `main` é:

```text
platform-libraries/
├── pom.xml                    reactor/aggregator
├── platform-parent/           governança de build
├── platform-dependencies/     BOM tecnológico externo
├── platform-libraries-bom/    BOM das capabilities
└── modules/
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

O namespace Maven e Java oficial é:

```text
br.com.portalmanager.core
```

Os `artifactId` `platform-*` permanecem estáveis.

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

As capabilities seguem inicialmente uma release train coerente controlada por `.mvn/maven.config`.

## Capabilities

O starter obrigatório agrega somente logging, messaging e authorization.

Audit, catalog e tagging permanecem explícitos. Test-support permanece capability de testes.

`platform-crud` continua removido e não participa do reactor.

`platform-catalog` permanece independente de CRUD genérico.

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

## Baseline preparado

O baseline atual preparado para publicação é:

```text
br.com.portalmanager.core:platform-dependencies:1.0.0
br.com.portalmanager.core:platform-parent:1.0.0
br.com.portalmanager.core:platform-libraries-bom:1.0.0
br.com.portalmanager.core:platform-*:1.0.0
```

A igualdade das versões neste baseline inicial não altera a independência conceitual dos eixos de build, tecnologia e capabilities.

## Distribuição oficial

O único registry oficial alvo é:

`https://maven.pkg.github.com/brunobs/platform-libraries`

O fluxo de saída da Foundation é:

```text
platform-libraries
  -> clean verify
  -> deploy
GitHub Packages / platform-libraries
  -> resolve remoto
consumer
  -> clean verify
```

O registry de `platform-build` não faz parte do fluxo oficial alvo.

## Evidência atual

O `main` foi validado no Verify #85, run `35544899150`, após:

- consolidação de `platform-parent` e `platform-dependencies`;
- criação do `platform-libraries-bom`;
- remoção do gerenciamento de capabilities do parent;
- migração Maven e Java para `br.com.portalmanager.core`;
- limpeza dos POMs gerados pelo Flatten Plugin;
- gates de CI contra regressão de namespace e higiene do repositório.

O reactor concluiu com `BUILD SUCCESS`.

A publicação anterior do baseline `1.0.0` no run `35542606756` usou o namespace provisório `com.empresa.platform`. Ela permanece apenas como evidência operacional histórica do fluxo de deploy e não representa o baseline oficial final.

## Condição para fechamento

A arquitetura local está consolidada. O fechamento do checkpoint ainda depende de:

1. publicar `br.com.portalmanager.core:*:1.0.0` no registry de `platform-libraries`;
2. validar resolução remota dessas coordenadas por um consumidor;
3. executar `mvn clean verify` verde no consumidor sem depender de `platform-build` ou `mvn install` local;
4. registrar as evidências finais em `FOUNDATION-PUBLISHING.md` e `FOUNDATION-CHECKPOINT.md`.

Até essas provas remotas, o checkpoint de consolidação permanece aberto.
