# platform-audit

Biblioteca que cria eventos de auditoria nos serviços produtores e os publica na `platform-message-queue`. O `audit-api` consome o destino lógico `audit-events` e persiste os eventos.

## Fluxo de publicação

```text
operação de negócio
    ↓
@Auditable
    ↓
AuditEvent (eventId estável)
    ↓
platform-audit
    ↓
destino lógico audit-events
    ↓
AWS SQS FIFO ou Azure Service Bus Sessions
    ↓
audit-api consome e persiste
```

A chamada ao publisher da fila é síncrona e retorna depois que o provider confirma o envio (ou propaga a falha). O processamento pelo `audit-api` é desacoplado. A biblioteca não chama o endpoint HTTP de ingestão da Audit API nem mantém uma fila local Redis; a durabilidade após a confirmação é responsabilidade do broker.

Não há atomicidade entre a transação de negócio e a publicação no broker. Se a publicação falhar, a falha é propagada para que o evento não seja descartado silenciosamente. Garantia transacional exigiria um Transactional Outbox no serviço produtor e permanece fora deste módulo.

## Idempotência e ordenação

Cada ocorrência recebe um `eventId` UUID imutável na origem. O mesmo valor é preservado no payload e usado como `deduplicationId` para AWS SQS FIFO. Azure Service Bus não recebe esse campo, pois sua deduplicação tem configuração própria. Em ambos os providers, a entrega é *at least once*; o `audit-api` deve impor unicidade persistente por `eventId` e tratar redeliveries como já processadas.

A publicação calcula um `orderingKey` estável a partir de `resourceType` e `resourceIdentifier`. O SHA-256 limita a chave a 64 caracteres e evita expor o identificador do recurso como metadado do broker. A ordem é por recurso e não global.

O destino `audit-events` deve ser configurado como ordenado. No AWS, a fila e sua DLQ precisam ser FIFO. No Azure, a fila precisa ter sessões habilitadas. Retentativa, redelivery e DLQ são gerenciados pelo broker e pela `platform-message-queue`.

## Dependências

```xml
<dependency>
    <groupId>br.com.portalmanager.platform.library</groupId>
    <artifactId>platform-audit</artifactId>
</dependency>
<dependency>
    <groupId>br.com.portalmanager.platform.library</groupId>
    <artifactId>platform-message-queue</artifactId>
</dependency>
```

## Configuração AWS

```yaml
platform:
  audit:
    enabled: true
    service-name: account
    destination: audit-events
  message-queue:
    provider: AWS
    aws:
      region: sa-east-1
    destinations:
      audit-events:
        queue: audit-events.fifo
        ordered: true
        publisher:
          enabled: true
        consumer:
          enabled: false
```

A infraestrutura provisiona a fila FIFO e sua DLQ com redrive policy. A aplicação precisa de permissão mínima para enviar mensagens e resolver a URL da fila.

## Configuração Azure

```yaml
platform:
  audit:
    enabled: true
    service-name: account
    destination: audit-events
  message-queue:
    provider: AZURE
    azure:
      namespace: my-namespace.servicebus.windows.net
    destinations:
      audit-events:
        queue: audit-events
        ordered: true
        publisher:
          enabled: true
        consumer:
          enabled: false
```

A infraestrutura provisiona a entidade Service Bus com sessões habilitadas. A identidade da aplicação precisa da permissão de envio.

## Uso e payload

O corpo completo da requisição ou da resposta nunca é copiado automaticamente para o evento. O payload inicia vazio e recebe somente os campos declarados explicitamente na anotação:

```java
@Auditable(
    resource = "account",
    action = "UPDATE",
    resourceId = @AuditField(source = AuditFieldSource.PATH, field = "accountId"),
    payload = {
        @AuditField(source = AuditFieldSource.RESPONSE, field = "name"),
        @AuditField(source = AuditFieldSource.BODY, field = "status")
    }
)
```

Escolha apenas campos necessários para comprovar a ação. `AuditFieldSource` permite selecionar um campo de PATH, BODY, RESPONSE ou HEADER. Para reduzir exposição de dados, não inclua tokens, credenciais ou dados pessoais desnecessários.

Um identificador do recurso é obrigatório. Se não puder ser resolvido, o evento não será publicado. Com `fail-on-error=true` (padrão), a operação falha explicitamente; com `false`, o evento é descartado e a ocorrência é registrada em log de erro.

## Consumo pelo audit-api

O consumidor deve registrar um listener para o destino `audit-events` e processar `MessageQueueMessage<AuditEventRequest>`. O handler confirma a mensagem ao retornar normalmente; portanto, deve confirmar a persistência idempotente antes do retorno. Falhas devem ser propagadas para permitir redelivery e encaminhamento à DLQ após o limite configurado no broker.

## Contexto do evento

`platform-audit` depende de `platform-authorization`. O `AuditAuthorizationContextResolver` deriva do `UserContext`:

- `userName` → actor;
- `accountId` → accountId;
- `applicationId` → applicationId;
- `environmentId` → environmentId;
- `traceId` → correlationId.

A ausência do contexto autorizado interrompe a operação por padrão. Se `fail-on-error=false`, o evento é ignorado e a ocorrência é registrada em log de erro.

## Propriedades

| Propriedade | Default | Descrição |
| --- | --- | --- |
| `platform.audit.enabled` | `true` | Habilita auditoria |
| `platform.audit.service-name` | `unknown` | Identifica o serviço produtor no evento |
| `platform.audit.destination` | `audit-events` | Destino lógico configurado em `platform.message-queue.destinations` |
| `platform.audit.fail-on-error` | `true` | Propaga falhas ao resolver o contexto ou identificador do recurso; falhas de publicação na fila sempre são propagadas |

O destino precisa existir, estar habilitado para publicação e estar marcado com `ordered: true`; a aplicação falha no startup se esses requisitos não forem atendidos.

## Limites desta etapa

- O contrato do consumidor e do broker é *at least once*; `audit-api` precisa deduplicar pelo `eventId`.
- Não há outbox transacional entre banco de negócio e broker.
- Ainda é necessário validar redelivery, DLQ, permissões, conectividade e tempos de processamento em homologação com AWS e Azure reais.
- A `platform-message-queue` não foi submetida a teste de carga para um throughput específico.
