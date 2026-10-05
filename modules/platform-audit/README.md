# platform-audit

Biblioteca que captura o estado auditável no limite do Use Case e grava a intenção de auditoria em uma `AUDIT_OUTBOX` local. A gravação participa da mesma transação da alteração de negócio. Esta etapa não publica mensagens nem envia snapshots para Object Storage.

## Fluxo da Etapa 1

```text
Use Case @Transactional
    ↓
AuditAspect @Order(200)
    ↓
regra de negócio e persistência
    ↓
snapshot AFTER (ou BEFORE para PURGE)
    ↓
AUDIT_OUTBOX dentro da mesma transação
    ↓
COMMIT
```

Falha na operação ou na gravação da outbox reverte as duas alterações. A anotação fica no Service/Use Case, nunca no Controller.

## Contrato

```java
@Auditable(
    action = AuditAction.UPDATE,
    event = "UPDATED",
    resourceType = "KEY"
)
@Transactional
public KeyResponse execute(UpdateKeyInput input) {
    // ...
}
```

`AuditAction` é uma estratégia técnica controlada pela library e não vai para o metadata. `event` é o fato de negócio persistido como `eventType`. As ações CREATE, UPDATE, ACTIVATE, DEACTIVATE, RESTORE e DELETE capturam o retorno do Use Case. PURGE exige um `AuditBeforeSnapshotProvider` para obter o último estado antes da remoção física. CUSTOM falha até que sua estratégia seja definida.

O snapshot deve ser um objeto JSON com `identifier` ou `id`. `AuditMetadata` registra service, resourceType, eventType, resourceIdentifier, contexto da autorização/correlação e `occurredAt`. O estado e o metadata são salvos como JSON.

## Migração do schema

A migration MySQL de referência está em `src/main/resources/platform-audit/db/mysql/create-audit-outbox.sql`. Copie o DDL para a próxima migration versionada do serviço consumidor; a library não injeta uma migration global nem escolhe números de versão que possam colidir com o histórico de cada serviço.

O modelo contém `id`, `identifier`, `payload`, `metadata`, `status` e colunas técnicas de claim/retry/retention. A tabela não duplica propriedades funcionais do evento em colunas.

## Configuração

```yaml
platform:
  audit:
    enabled: true
    service-name: key-service
    max-event-size-bytes: 65536
    max-events-per-invocation: 100
```

`platform.audit.service-name` é obrigatório quando a capacidade está habilitada. Use um banco com suporte a JPA e JSON; a library registra a entidade da outbox automaticamente. O Use Case auditável precisa executar dentro de uma transação ativa. Sem transação, a library falha antes de executar a regra de negócio.

## Etapas seguintes

Claim concorrente, worker, retry, Object Storage versionado, publicação em fila, reader e idempotência ficam fora desta etapa. A outbox já contém os campos técnicos necessários para o processamento posterior.
