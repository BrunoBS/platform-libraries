# Foundation Architecture — contrato vigente

> Revisado em 2026-10-20 contra `platform-libraries/pom.xml`, `platform-parent/pom.xml` e código das capabilities na branch `main`.

## Fonte oficial

A Foundation consolidada reside em `BrunoBS/platform-libraries`. O reactor agrega `platform-parent`, `platform-dependencies`, `platform-libraries-bom` e as capabilities sob `modules/`. Coordenadas Maven oficiais: `br.com.portalmanager.platform.library`; release train configurada por `revision` (atualmente `1.0.0`).

O repositório separado `platform-build` contém implementação histórica com `com.empresa.platform` e não define o parent atual.

## Três contratos Maven independentes

1. **`platform-parent`**: Java 25, Maven >= 3.9.9, Surefire, Failsafe, Compiler, Enforcer, JaCoCo e demais convenções. Importa `platform-dependencies`.
2. **`platform-dependencies`**: versões tecnológicas externas, incluindo Spring Boot 4.1.1 e suas dependências gerenciadas.
3. **`platform-libraries-bom`**: versões das capabilities reutilizáveis, importado explicitamente pelos consumidores.

O parent **não substitui** o BOM de capabilities. O POM raiz é reactor, não starter.

## Capabilities e relações

O `platform-starter` agrega `platform-observability`, `platform-messaging` e `platform-authorization`.

Capabilities adicionais declaradas conforme necessidade: `platform-audit`, `platform-catalog`, `platform-tagging`, `platform-schema-validation`, `platform-redis` e `platform-message-queue`. O catálogo completo e os submódulos de teste estão em [PLATFORM-LIBRARIES-MODULES.md](PLATFORM-LIBRARIES-MODULES.md).

O código de aplicação, inclusive regras de Workspace, não pertence à Foundation. Dependências entre camadas devem evitar ciclos e implementações paralelas das capacidades transversais.

## Segurança e escopo

`@AuthorizationRequired` declara autorização de operação; `@ResourceVisibility` aplica filtro Hibernate específico da entidade. O `ResourceVisibilityAspect` exige sessão, ignora filtro para OWNER, rejeita usuário sem grupos autorizadores e libera no `finally` somente o filtro que ativou. `ResourceVisibilityFilterManager` normaliza os grupos e limita a 500 entradas.

**Não presumir que filtro Hibernate proteja SQL nativo.** Consultas nativas e leituras entre workspaces/aplicações exigem escopo explícito e testes de acesso negativo.

## Persistência e schema

`platform-schema-validation` integra resolução/validação de schema; consumers com repositório JDBC default precisam de `JdbcTemplate` ou implementação customizada de `ResourceSchemaRepository`. Não confundir esta capability com o catálogo de versões de schema específico do Workspace Service.

A aplicação consumidora é responsável por suas migrations Flyway, integridade e transações de domínio.

## Testing

`modules/platform-testing` agrega módulos independentes: core, http, authorization, database, kafka, redis, cloud-aws e cloud-azure. Usar apenas os necessários. Não afirmar que `platform-testing-core` concentra toda a infraestrutura de MySQL, Kafka, HTTP e cloud.

## Guardrails e publicação

- `platform-crud` permanece removido.
- Não usar `platform-logging`, `platform-test-support` ou namespaces legados.
- Não introduzir métricas/tracing como funcionalidade implementada se só houver logging.
- Registry: `https://maven.pkg.github.com/brunobs/platform-libraries`.
- Prova completa: Foundation `clean verify`, publicação e consumer `clean verify` com resolução remota. Este documento não afirma que essas execuções ocorreram nesta revisão.
