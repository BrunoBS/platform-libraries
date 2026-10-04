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

## Estrutura do código

- `annotation`: declara `@Auditable` e as origens de campo aceitas.
- `aspect`: intercepta a operação, trata o status de sucesso e delega a criação do evento.
- `field`: `AuditFieldResolver` projeta os campos configurados a partir dos argumentos e da resposta.
- `event`: `AuditEventFactory` monta, valida e limita o tamanho serializado do evento.
- `publisher`: publica o evento no destino lógico e define as opções de ordenação e deduplicação.
- `autoconfigure` e `config`: validam as propriedades e registram os beans do módulo.

O fluxo de leitura do código segue essa ordem: `AuditAspect → AuditEventFactory → AuditFieldResolver` e depois `AuditPublisher → MessageQueueAuditPublisher`.

Antes da primeira chamada ao broker, todos os eventos da invocação são construídos e validados; assim, erro de dados ou tamanho não publica apenas parte do lote. Se o broker falhar durante os envios sequenciais, ainda pode haver publicação parcial. A falha é propagada e garantia transacional exigiria um Transactional Outbox no serviço produtor, fora deste módulo.

## Idempotência e ordenação

Cada ocorrência recebe um `eventId` UUID imutável na origem. O mesmo valor é preservado no payload e usado como `deduplicationId` para AWS SQS FIFO. Azure Service Bus não recebe esse campo, pois sua deduplicação tem configuração própria. Em ambos os providers, a entrega é *at least once*; o `audit-api` deve impor unicidade persistente por `eventId` e tratar redeliveries como já processadas.

A publicação calcula um `orderingKey` estável a partir de `resourceType` e `resourceIdentifier`. O SHA-256 limita a chave a 64 caracteres e evita expor o identificador do recurso como metadado do broker. A ordem é por recurso e não global.

A biblioteca detecta as capacidades da fila provisionada: no AWS, `audit-events` precisa ser FIFO; no Azure, a entidade precisa exigir sessões. Não declare `ordered` na configuração. Retentativa, redelivery e DLQ são gerenciados pelo broker e pela `platform-message-queue`.

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
        publisher:
          enabled: true
        consumer:
          enabled: false
```

A infraestrutura provisiona a fila FIFO e sua DLQ com redrive policy. Para o publisher de auditoria, a role da workload precisa de `sqs:GetQueueUrl`, `sqs:GetQueueAttributes` e `sqs:SendMessage` na fila principal. Se o serviço também consumir filas ou tiver listener de dead-letter, conceda `sqs:ReceiveMessage` e `sqs:DeleteMessage` nas filas consumidas e também `sqs:GetQueueUrl` na DLQ. O README de `platform-message-queue` traz a policy completa para publisher, consumer e DLQ listener.

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
        publisher:
          enabled: true
        consumer:
          enabled: false
```

A infraestrutura provisiona a entidade Service Bus com sessões habilitadas. Atribua **Azure Service Bus Data Owner** diretamente à managed identity ou ao service principal da workload, no namespace; não é necessário criar um grupo. Essa role permite a consulta de propriedades no startup e já cobre envio e recebimento. O README de `platform-message-queue` descreve a role do módulo e o motivo pelo qual `Data Sender`/`Data Receiver` isoladas não bastam.

## Uso e payload

O corpo completo da requisição ou da resposta nunca é copiado automaticamente para o evento. O payload de negócio inicia vazio e recebe somente os campos declarados explicitamente na anotação:

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
public ResponseEntity<AccountResponse> update(
        @PathVariable("accountId") String accountId,
        @RequestBody UpdateAccountRequest request
) {
    // ...
}
```

Escolha apenas campos necessários para comprovar a ação. `AuditFieldSource` permite selecionar um campo de PATH, BODY, RESPONSE ou HEADER. Para PATH, use o nome declarado em `@PathVariable`; o nome do parâmetro Java também funciona quando o compilador preserva esses nomes. BODY busca apenas parâmetros marcados com `@RequestBody`. O campo `metadata` do evento é reservado para metadados adicionais e fica vazio nesta implementação. Campos com nomes que indicam credenciais ou segredos são bloqueados. Cabeçalhos são negados por padrão, exceto `correlation-id`; nomes adicionais precisam constar em `platform.audit.allowed-headers`. Mesmo com a allowlist, informe somente dados necessários para comprovar a ação.

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
| `platform.audit.service-name` | Obrigatório | Identifica o serviço produtor no evento |
| `platform.audit.destination` | `audit-events` | Destino lógico configurado em `platform.message-queue.destinations` |
| `platform.audit.fail-on-error` | `true` | Propaga falhas de resolução/validação; falhas de publicação na fila sempre são propagadas |
| `platform.audit.allowed-headers` | `[correlation-id]` | Allowlist explícita de cabeçalhos auditáveis |
| `platform.audit.max-event-size-bytes` | `65536` | Limite do evento JSON serializado |
| `platform.audit.max-events-per-invocation` | `100` | Limite de eventos antes de publicar qualquer item da coleção |

O nome do serviço deve ser informado. O destino precisa existir, estar habilitado para publicação e ter capacidade de ordenação no broker (fila FIFO no AWS ou sessões habilitadas no Azure); a aplicação falha no startup se esses requisitos não forem atendidos. O tamanho é medido sobre o JSON do evento antes do envelope do broker; mantenha o limite abaixo do máximo da plataforma escolhida. Se uma coleção exceder o limite de eventos, nenhuma publicação daquela chamada é iniciada.

## Limites desta etapa

- O contrato do consumidor e do broker é *at least once*; `audit-api` precisa deduplicar pelo `eventId`.
- Não há outbox transacional entre banco de negócio e broker.
- Ainda é necessário validar redelivery, DLQ, permissões, conectividade e tempos de processamento em homologação com AWS e Azure reais.
- A `platform-message-queue` não foi submetida a teste de carga para um throughput específico.
