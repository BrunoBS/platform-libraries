# platform-audit

Biblioteca para registrar alterações auditáveis de um serviço. O Use Case declara o fato com `@Auditable`; a library captura o estado do recurso e grava o evento na `AUDIT_OUTBOX` dentro da mesma transação da operação.

## Integração

Siga o [Guia de uso](docs/USAGE.md) para configurar o serviço, criar a migration, declarar a entidade e o repository da outbox e anotar os Use Cases.

O DDL MySQL de referência está em `src/main/resources/platform-audit/db/mysql/create-audit-outbox.sql`.

## Referência interna

A documentação [Arquitetura interna](docs/ARCHITECTURE.md) descreve os pacotes, as classes e o fluxo de execução para manutenção da library.
