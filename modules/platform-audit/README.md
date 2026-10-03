# platform-audit

Biblioteca de integração transparente entre microserviços e a Audit API da plataforma.

## Contrato Golden de entrega

O modo padrão é **assíncrono at-least-once com persistência durável antes da entrega HTTP**.

```text
operação de negócio
    ↓
@Auditable
    ↓
AuditEvent
    ↓
AuditEventQueue (Redis por padrão)
    ↓
operação pode concluir
    ↓
recovery/publisher
    ↓
Audit API
    ↓
sucesso → remove do store
```

A indisponibilidade da Audit API não derruba a operação de negócio. O evento já está persistido e permanece pendente para reenvio.

A gravação no fila de auditoria faz parte da aceitação do evento. Se essa gravação falhar, a falha é propagada: a biblioteca não confirma silenciosamente um evento que não conseguiu reter.

Este contrato não promete atomicidade entre a transação de negócio e a auditoria. Garantia transacional entre banco de negócio e evento exigiria um padrão como Transactional Outbox e não faz parte desta versão Golden.

Reenvios podem acontecer. Cada ocorrência auditável recebe um `eventId` UUID imutável na origem, antes de entrar na fila. Redis, recovery e publicação HTTP preservam exatamente o mesmo identificador em todas as tentativas.

A library garante a identidade estável do evento. A Audit API deve tratar `eventId` como chave de idempotência e manter unicidade persistente: a primeira entrega grava o evento e entregas repetidas do mesmo `eventId` devem ser consideradas já processadas, sem criar uma segunda auditoria. O `eventId` não é derivado do payload; duas ocorrências legítimas com conteúdo igual recebem identificadores diferentes.

## Dependência

```xml
<dependency>
    <groupId>br.com.portalmanager.platform.library</groupId>
    <artifactId>platform-audit</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Configuração padrão

O fila de auditoria Redis é habilitado por padrão no modo assíncrono.

```yaml
platform:
  audit:
    enabled: true
    service-url: http://audit-api
    service-name: account
    publish-path: /api/v1/events
    fail-on-error: false
    http:
      connect-timeout: 5s
      read-timeout: 5s
    queue:
      enabled: true
      key-prefix: platform:audit:pending:
      recovery-interval: 1s
      batch-size: 50
      lock:
        key-prefix: platform:audit:recovery:lock:
        ttl: 2m
```

O nome `queue` é mantido nesta etapa por compatibilidade da configuração existente, mas seu papel no modo assíncrono é de **fila de auditoria**, não de queue posterior à chamada HTTP.

O consumidor deve disponibilizar Redis no classpath e configurar `StringRedisTemplate`, ou fornecer uma implementação própria de `AuditEventQueue`.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

Se `fail-on-error=false` e nenhum fila de auditoria estiver disponível, a aplicação falha no startup com `PLT-AUD-002`.

## Modo estrito

Quando `fail-on-error=true`, a publicação é síncrona e a chamada à Audit API participa do fluxo da requisição.

```yaml
platform:
  audit:
    fail-on-error: true
    queue:
      enabled: false
```

Nesse modo, uma falha HTTP é propagada e o fila de auditoria não é obrigatório.

## Recovery

O Redis mantém uma lista por serviço:

```text
platform:audit:pending:<service-name>
```

O recovery usa lock distribuído Redis e processa até `batch-size` eventos por ciclo. Um evento só é removido depois de a Audit API confirmar a publicação.

O fluxo atual é FIFO. Retry, poison event/DLQ, renovação do lock e idempotência serão endurecidos nos próximos itens da auditoria; não são considerados resolvidos por esta mudança.

## Contexto

`platform-audit` depende de `platform-authorization`. O `AuditAuthorizationContextResolver` deriva do `UserContext`:

- `userName` → actor;
- `accountId` → accountId;
- `applicationId` → applicationId;
- `environmentId` → environmentId;
- `traceId` → correlationId.

## Propriedades principais

| Propriedade | Default | Descrição |
| --- | --- | --- |
| `platform.audit.enabled` | `true` | Habilita auditoria |
| `platform.audit.service-url` | — | URL da Audit API |
| `platform.audit.service-name` | `unknown` | Serviço consumidor |
| `platform.audit.publish-path` | `/api/v1/events` | Endpoint de publicação |
| `platform.audit.fail-on-error` | `false` | Usa modo síncrono estrito quando `true` |
| `platform.audit.http.connect-timeout` | `5s` | Timeout de conexão |
| `platform.audit.http.read-timeout` | `5s` | Timeout de leitura |
| `platform.audit.queue.enabled` | `true` | Habilita fila de auditoria/recovery padrão |
| `platform.audit.queue.key-prefix` | `platform:audit:pending:` | Prefixo da fila Redis |
| `platform.audit.queue.recovery-interval` | `1s` | Intervalo de recovery |
| `platform.audit.queue.batch-size` | `50` | Máximo por ciclo |
| `platform.audit.queue.lock.ttl` | `2m` | TTL do lock distribuído |

## Limites ainda abertos

Esta etapa fecha apenas a garantia de entrega do item 1 da auditoria.

Ainda precisam ser tratados explicitamente:

1. poison event, retry e DLQ;
2. expiração/renovação do lock;
3. política de payload;
4. falhas silenciosas de resolução do Aspect;
5. validações adicionais de configuração.

