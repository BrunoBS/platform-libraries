# platform-audit

Biblioteca de integração transparente entre microserviços e a Audit API da plataforma.

## Objetivo

O módulo abstrai a publicação de eventos de auditoria. O serviço consumidor declara
o evento com `@Auditable`; a biblioteca resolve contexto, identificadores e payload
e envia o evento para a Audit API.

A biblioteca não possui entidade JPA, repository ou tabela própria.

## Fluxo principal

```text
Microserviço
    ↓
@Auditable
    ↓
AuditAspect
    ↓
AuditPublisher
    ↓
RestAuditPublisher
    ↓
Audit API
```

## Dependência

```xml
<dependency>
    <groupId>br.com.portalmanager.platform</groupId>
    <artifactId>platform-audit</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Configuração básica — sem Redis

O fallback Redis é opcional e vem desabilitado por padrão.

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
    fallback:
      enabled: false
```

Nesse modo:

```text
evento
  ↓
Audit API
  ├─ sucesso → fim
  └─ falha   → erro é logado
```

Com `fail-on-error=false`, a publicação é assíncrona e uma falha da Audit API
não derruba a operação principal.

Com `fail-on-error=true`, a publicação é síncrona e a falha é propagada.
Nesse modo estrito, o fallback Redis não é utilizado.

## Configuração com fallback Redis

Quando o consumidor quiser retenção temporária dos eventos que falharam no HTTP,
deve habilitar explicitamente o fallback:

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

    fallback:
      enabled: true
      key-prefix: platform:audit:pending:
      recovery-interval: 5m
      batch-size: 50
      lock:
        key-prefix: platform:audit:recovery:lock:
        ttl: 2m
```

Como o suporte Redis é opcional na biblioteca, o serviço consumidor também deve
ter Redis disponível no classpath, por exemplo:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

E configurar o Redis normalmente pelo Spring Boot:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

Se `platform.audit.fallback.enabled=true` e não existir um
`AuditFallbackStore` válido / Redis configurado, a aplicação falha no startup.
A biblioteca não sobe silenciosamente sem a resiliência solicitada.

## Como o fallback funciona

A implementação padrão usa Redis convencional com uma lista por serviço.

Exemplo de chave:

```text
platform:audit:pending:account
```

Quando a Audit API falha:

```text
evento
  ↓
Audit API
  ↓ falha
Redis List
```

O evento completo é serializado em JSON e colocado no final da lista.

Não existe TTL automático nessa fila. O evento permanece pendente até ser
reenviado com sucesso ou removido administrativamente.

## Recovery

Quando o fallback está habilitado, o módulo cria um scheduler dedicado.

Antes de drenar a fila, cada instância tenta adquirir um lock distribuído no Redis:

```text
platform:audit:recovery:lock:<service-name>
```

O lock usa `SET NX` com TTL. Apenas a instância que adquiriu o lock executa
o recovery. A liberação usa comparação do token do proprietário + `DEL` em
script Redis atômico, evitando que uma instância remova o lock de outra.

Por padrão:

```text
a cada 5 minutos
    ↓
tenta adquirir lock
    ├─ não conseguiu → encerra o ciclo
    └─ conseguiu
         ↓
       existem eventos pendentes?
         ├─ não → libera lock e encerra
         └─ sim
              ↓
            tenta o primeiro
              ├─ falhou → encerra e libera lock
              └─ sucesso
                   ↓
                 remove do Redis
                   ↓
                 continua até batch-size
```

O primeiro evento funciona como teste real de disponibilidade da Audit API.
Se ele ainda falhar, o worker não continua disparando o restante do lote.

Exemplo:

```yaml
platform:
  audit:
    fallback:
      enabled: true
      recovery-interval: 5m
      batch-size: 20
      lock:
        ttl: 2m
```

Nesse caso, cada ciclo recupera no máximo 20 eventos.

## Beans condicionais

Com fallback desabilitado:

```text
AuditPublisher
RestAuditPublisher
AuditAuthorizationContextResolver
AuditAspect
platformAuditTaskExecutor
```

Nenhum bean Redis de fallback do `platform-audit` é criado.

Com fallback habilitado:

```text
AuditFallbackStore
RedisAuditFallbackStore
AuditRecoveryLock
RedisAuditRecoveryLock
AuditRecoveryService
platformAuditRecoveryTaskScheduler
```

também são criados.

## Uso

```java
@Auditable(
    resource = "account",
    action = "UPDATE",
    resourceId = @AuditField(
        source = AuditFieldSource.PATH,
        field = "accountId"
    )
)
public ResponseEntity<AccountResponse> update(String accountId, ...) {
    ...
}
```

## Contexto de autorização

`platform-audit` depende diretamente de `platform-authorization`.

Toda publicação auditável usa o `UserContext` já validado pela autorização.
O `AuditAuthorizationContextResolver` transforma o `UserSession` em contexto
de auditoria usando:

- `userName` → actor;
- `accountId` → accountId;
- `applicationId` → applicationId;
- `environmentId` → environmentId;
- `traceId` → correlationId.

A biblioteca não relê esses dados dos headers para montar o contexto do evento.

Se não existir `UserContext`:
- com `fail-on-error=false`, o evento é descartado e o erro é registrado;
- com `fail-on-error=true`, a ausência de contexto é propagada como erro.

## Timeouts HTTP

A chamada para a Audit API possui limites explícitos:

```yaml
platform:
  audit:
    http:
      connect-timeout: 5s
      read-timeout: 5s
```

Os dois valores são máximos. Se conexão ou resposta ocorrerem antes, o fluxo
continua imediatamente. O consumidor pode sobrescrever ambos.

## Propriedades

| Propriedade | Default | Descrição |
| --- | --- | --- |
| `platform.audit.enabled` | `true` | Habilita o módulo |
| `platform.audit.service-url` | — | URL da Audit API |
| `platform.audit.service-name` | `unknown` | Nome lógico do serviço consumidor |
| `platform.audit.publish-path` | `/api/v1/events` | Endpoint de publicação |
| `platform.audit.fail-on-error` | `false` | Propaga falha da auditoria quando habilitado |
| `platform.audit.http.connect-timeout` | `5s` | Tempo máximo para estabelecer conexão HTTP |
| `platform.audit.http.read-timeout` | `5s` | Tempo máximo para aguardar leitura da resposta |
| `platform.audit.core-pool-size` | `2` | Threads mínimas da publicação assíncrona |
| `platform.audit.max-pool-size` | `4` | Threads máximas da publicação assíncrona |
| `platform.audit.queue-capacity` | `500` | Capacidade da fila assíncrona local |
| `platform.audit.fallback.enabled` | `false` | Habilita fallback Redis |
| `platform.audit.fallback.key-prefix` | `platform:audit:pending:` | Prefixo da lista no Redis |
| `platform.audit.fallback.recovery-interval` | `5m` | Intervalo entre ciclos de recovery |
| `platform.audit.fallback.batch-size` | `50` | Máximo recuperado por ciclo |
| `platform.audit.fallback.lock.key-prefix` | `platform:audit:recovery:lock:` | Prefixo do lock distribuído |
| `platform.audit.fallback.lock.ttl` | `2m` | TTL do lock de recovery |

## Limites atuais

Esta primeira versão não implementa hash/eventId determinístico nem deduplicação.
A idempotência será definida junto com o contrato definitivo da Audit API.

Em ambientes com múltiplas instâncias do mesmo serviço, a Audit API deverá ser
preparada para receber reenvios/duplicidades quando a estratégia de idempotência
for introduzida.

Se a Audit API e o Redis estiverem indisponíveis simultaneamente, a biblioteca
não consegue garantir retenção do evento.
