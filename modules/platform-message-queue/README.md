# Platform Message Queue

Módulo Spring Boot para publicar e consumir mensagens usando AWS SQS ou Azure Service Bus. O provider é escolhido pela aplicação; os nomes lógicos de destino usados no código são mapeados para filas físicas pela configuração.

## Dependência

Inclua o módulo no serviço consumidor. Se o projeto importa o BOM das libraries da plataforma, a versão é gerenciada pelo BOM:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform.library</groupId>
    <artifactId>platform-message-queue</artifactId>
</dependency>
```

## Configuração comum

Toda aplicação deve definir um provider e pelo menos um destino. O nome sob `destinations` é lógico e é o valor usado por `publish` e pelas annotations de listener. `queue` é o nome físico provisionado no broker.

Cada destino permite habilitar/desabilitar publicação e consumo. Concorrência e tempo de espera podem ser definidos como defaults do provider, por destino, ou como override específico do provider. O override específico do provider tem precedência sobre a configuração comum do destino, que tem precedência sobre o default do provider. O visibility timeout pode ser definido na configuração AWS (`aws.defaults.visibility-timeout` ou `destinations.<destino>.aws.visibility-timeout`); se omitido, o consumidor respeita o valor configurado na própria fila SQS. A renovação automática de visibility timeout no SQS não é feita pela biblioteca, então o valor da fila ou o override deve cobrir o tempo máximo do handler. No Azure, a duração inicial do lock é configurada na fila do Service Bus. O receiver renova o lock automaticamente até `azure.max-auto-lock-renewal-duration`, cujo padrão é cinco minutos; configure esse limite conforme o tempo máximo esperado do handler.

## Escopo desta versão

- AWS SQS Standard e FIFO, e Azure Service Bus sem sessões ou com sessões habilitadas.
- A biblioteca detecta automaticamente se a fila é SQS FIFO ou se a entidade Azure exige sessões. Destinos com ordenação exigem `orderingKey` em cada publicação; a chave é mapeada para `MessageGroupId` no SQS e `SessionId` no Azure. Não é necessário declarar `ordered` na configuração.
- `deduplicationId` permanece específico do SQS FIFO; o Azure tem configuração e semântica próprias de detecção de duplicatas.
- A biblioteca não oferece ordenação global de mensagens.

## Exemplo: AWS SQS

```yaml
platform:
  message-queue:
    provider: AWS
    aws:
      region: sa-east-1
      defaults:
        concurrency: 2
        wait-time: 20s
        visibility-timeout: 60s
    destinations:
      orders:
        queue: orders
        consumer:
          enabled: true
        publisher:
          enabled: true
        aws:
          # Opcional. O padrão é "<fila física>-dlq".
          dead-letter-queue: orders-dlq
```

A aplicação usa as credenciais AWS do SDK associadas à identidade da workload. Em ECS, use a **task role** da aplicação (não a task execution role); no EKS, use a role vinculada à identidade do pod. O módulo não cria filas nem altera políticas de redrive.

A role precisa destas permissões, conforme as filas e operações habilitadas:

| Recurso/uso | Ações IAM necessárias |
| --- | --- |
| Descoberta de capacidades, sempre, em cada fila principal configurada | `sqs:GetQueueUrl`, `sqs:GetQueueAttributes` |
| Publicação em uma fila principal | `sqs:SendMessage` |
| Consumo de uma fila principal | `sqs:ReceiveMessage`, `sqs:DeleteMessage` |
| Listener de dead-letter | `sqs:GetQueueUrl`, `sqs:ReceiveMessage`, `sqs:DeleteMessage` na DLQ |

A descoberta ocorre no startup para todos os destinos configurados, inclusive quando um destino é somente produtor ou está desabilitado para consumo. `GetQueueUrl` também é necessário para a DLQ quando há listener de dead-letter. Não é necessário conceder `sqs:CreateQueue`: o provisionamento fica com a infraestrutura.

Policy IAM de exemplo para uma aplicação que publica e consome nas filas principais e consome suas DLQs. Troque os ARNs pelos recursos reais. Mantenha a descoberta para todas as filas principais; retire permissões de publicação/consumo não usadas e o statement de DLQ se não houver listener:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DiscoverMainQueues",
      "Effect": "Allow",
      "Action": ["sqs:GetQueueUrl", "sqs:GetQueueAttributes"],
      "Resource": [
        "arn:aws:sqs:<region>:<account-id>:<main-queue-1>",
        "arn:aws:sqs:<region>:<account-id>:<main-queue-2>"
      ]
    },
    {
      "Sid": "PublishAndConsumeMainQueues",
      "Effect": "Allow",
      "Action": ["sqs:SendMessage", "sqs:ReceiveMessage", "sqs:DeleteMessage"],
      "Resource": [
        "arn:aws:sqs:<region>:<account-id>:<main-queue-1>",
        "arn:aws:sqs:<region>:<account-id>:<main-queue-2>"
      ]
    },
    {
      "Sid": "ResolveAndConsumeDeadLetterQueues",
      "Effect": "Allow",
      "Action": ["sqs:GetQueueUrl", "sqs:ReceiveMessage", "sqs:DeleteMessage"],
      "Resource": [
        "arn:aws:sqs:<region>:<account-id>:<main-queue-1-dlq>",
        "arn:aws:sqs:<region>:<account-id>:<main-queue-2-dlq>"
      ]
    }
  ]
}
```


### Fila principal e DLQ na AWS

Crie a fila principal e a DLQ na infraestrutura do serviço consumidor. Configure a redrive policy da fila principal apontando para a DLQ e defina o `maxReceiveCount` conforme a política de retry do serviço. O módulo da aplicação não cria nem altera recursos AWS.

Por padrão, o listener de dead-letter procura a fila física `<fila principal>-dlq`. Use `destinations.<destino>.aws.dead-letter-queue` para apontar para outro nome. Para fila FIFO, a fila principal e sua DLQ devem terminar em `.fifo`; o nome padrão da DLQ é derivado como `<nome>-dlq.fifo`. A política de redrive continua sendo responsabilidade da infraestrutura.

### Fila FIFO na AWS

A fila FIFO é identificada automaticamente pelos atributos do SQS. Use o sufixo exigido pelo serviço e não declare uma flag de ordenação:

```yaml
platform:
  message-queue:
    provider: AWS
    aws:
      region: sa-east-1
    destinations:
      ordered-orders:
        queue: orders.fifo
```

Na publicação em uma fila FIFO, informe uma chave de ordenação para agrupar mensagens. Se a fila não usa deduplicação baseada no conteúdo, informe também um `deduplicationId` estável para a operação lógica. Sem esse recurso, cada envelope recebe um `messageId` e timestamp novos, então o conteúdo do envelope não identifica republicações equivalentes:

```java
publisher.publish(
        "ordered-orders",
        event,
        new MessageQueuePublishOptions(correlationId, Map.of(), orderId, eventId));
```

A ordem é garantida pelo SQS apenas dentro de cada chave; chaves diferentes podem ser processadas em paralelo. `orderingKey` tem no máximo 128 caracteres para manter o mesmo contrato entre provedores. A infraestrutura da aplicação deve criar a fila FIFO e a DLQ FIFO correspondentes, com redrive configurado.

## Exemplo: Azure Service Bus

```yaml
platform:
  message-queue:
    provider: AZURE
    azure:
      namespace: my-namespace.servicebus.windows.net
      defaults:
        concurrency: 2
        wait-time: 20s
    destinations:
      orders:
        queue: orders
        consumer:
          enabled: true
        publisher:
          enabled: true
```

A aplicação usa `DefaultAzureCredential`. Atribua a role integrada **Azure Service Bus Data Owner** diretamente à identidade da workload (managed identity ou service principal) no escopo do namespace, em **Access control (IAM)**. Não é necessário criar um grupo nem atribuir as roles **Data Sender** e **Data Receiver** separadamente: Data Owner cobre a consulta de propriedades da fila feita no startup, além de publicação e consumo. Como Data Owner concede acesso amplo às entidades do namespace, limite a atribuição à identidade da aplicação. Não confunda essa role de dados do Service Bus com as roles gerais `Owner` ou `Contributor` da assinatura.

O módulo consulta as propriedades de todas as filas configuradas ao iniciar para detectar automaticamente se exigem sessões. Por isso, mesmo uma aplicação que só publica precisa da role de descoberta. No Azure, dead-letter é a subfila nativa; a mesma role permite consumir a DLQ quando um listener estiver configurado. Configure `MaxDeliveryCount` na entidade conforme a política operacional. O módulo não provisiona nem configura filas.

A fila Azure com sessões habilitadas é detectada automaticamente. Não declare uma flag de ordenação:

```yaml
platform:
  message-queue:
    provider: AZURE
    azure:
      namespace: my-namespace.servicebus.windows.net
    destinations:
      ordered-orders:
        queue: orders
```

Publique com o mesmo contrato de opções usado pela AWS. A `orderingKey` será enviada como `SessionId`. Cada worker recebe uma sessão por vez e processa sequencialmente as mensagens daquela sessão; quando não encontra novas mensagens, libera a sessão para que outro worker ou outra instância possa adquiri-la. A infraestrutura precisa criar a entidade com sessões habilitadas antes do envio e do consumo.

## Publicação e consumo

Anote o tipo de payload com `@QueueMessage` para definir metadados estáveis no envelope. Sem a annotation, o tipo usa o nome do destino e a versão `1`. O consumidor recebe `messageType` e `messageVersion`, mas a lib não valida automaticamente a compatibilidade entre versões; o serviço deve decidir como tratar versões que não conhece.

```java
@QueueMessage(type = "order.created", version = "1")
public record OrderCreated(String orderId) {}
```

Publique pelo destino lógico. A chamada é síncrona e retorna depois que o SDK conclui o envio ao broker (ou lança uma exceção); o processamento pelo consumidor ocorre de forma desacoplada:

```java
publisher.publish("orders", new OrderCreated("123"));
```

Para propagar correlação e headers:

```java
publisher.publish("orders", event, correlationId, Map.of("tenant", tenantId));
```

Um listener recebe o envelope completo. Ele deve ser um método público, retornar `void` e declarar exatamente um parâmetro `MessageQueueMessage<T>` com payload de tipo concreto:

```java
@MessageQueueListener("orders")
public void onOrder(MessageQueueMessage<OrderCreated> message) {
    orderService.process(message.payload());
}
```

O processamento é confirmado somente quando o listener retorna normalmente. Se lançar uma exceção em runtime, a mensagem não é confirmada e o broker pode entregá-la novamente. Isso implica entrega *at least once*: handlers precisam ser idempotentes, pois duplicatas também podem ocorrer se o processamento concluir e a confirmação falhar. Configure a visibilidade no SQS para cobrir o tempo máximo do handler. No Azure, o SDK renova automaticamente o lock até `azure.max-auto-lock-renewal-duration` (padrão de cinco minutos); a renovação ainda pode falhar se a conexão ou o lock se perderem.

## Consumir mensagens da dead-letter

Registre, opcionalmente, um listener para o mesmo destino lógico:

```java
@MessageQueueDeadLetterListener("orders")
public void onDeadLetter(DeadLetterMessage<OrderCreated> deadLetter) {
    deadLetterService.record(
            deadLetter.message().messageId(),
            deadLetter.reason(),
            deadLetter.deliveryCount());
}
```

Na AWS, esse listener consome a fila DLQ física indicada pela configuração (ou `<fila>-dlq`). No Azure, consome a subfila dead-letter nativa. A mensagem é confirmada/removida da DLQ somente quando o handler retorna normalmente. Se falhar, ela permanece para nova tentativa. Os campos `reason`, `description` e `deadLetteredAt` podem ser nulos. `deliveryCount` é informado pelo broker e tem semântica específica de cada provider; não o trate como um contador normalizado das falhas na fila original. Defina se o handler vai registrar, corrigir e republicar, ou encaminhar para tratamento manual; não descarte mensagens sem uma decisão explícita.

## Observabilidade

Quando a aplicação disponibiliza um `MeterRegistry`, a lib registra os counters:

- `platform.message.queue.publish`: publicação com resultado;
- `platform.message.queue.consume`: processamento com destino, provider, tipo de fila e resultado;
- `platform.message.queue.poll.failure`: falha técnica ao consultar o broker;
- `platform.message.queue.ack.failure`: falha ao confirmar/remover uma mensagem processada.

Os logs de falha incluem destino e, quando o envelope já foi lido, `messageId` e `correlationId`. A aplicação deve exportar as métricas e configurar alertas conforme seus SLOs.

## Testes

Use `platform-testing` no escopo de testes. O suporte de emuladores e fixtures pertence ao ambiente de teste; não é dependência de runtime da aplicação.

Para SQS, a fixture pode criar uma fila principal e sua DLQ no LocalStack, com redrive policy:

```java
@WithAwsLocalStack(sqs = @AwsSqs(queues = {
    @AwsSqs.Queue(name = "orders", deadLetterEnabled = true, maxReceiveCount = 3)
}))
class OrderQueueTest {
}
```

O nome padrão da DLQ no mock é `orders-dlq`; é possível informar `deadLetterQueue = "custom-dlq"`. Para Azure Service Bus, configure o limite de entregas do emulador junto da fila: `@WithAzureEmulator(serviceBus = @AzureServiceBus(queues = @AzureServiceBus.Queue(name = "orders", maxDeliveryCount = 3)))`; a DLQ é a subfila nativa. Para testar uma fila Azure ordenada, configure a fixture com `sessionsEnabled = true`; em produção, a biblioteca detecta a capacidade diretamente no broker. Filas AWS cujo nome termina em `.fifo` são provisionadas como FIFO pelo mock; a DLQ padrão correspondente também termina em `.fifo`. A fixture não habilita deduplicação baseada no conteúdo, para que os testes usem sempre IDs explícitos.

## Checklist antes de produção

- Provisionar filas, DLQ/redrive (AWS) ou `MaxDeliveryCount` (Azure) na infraestrutura do serviço consumidor.
- Conceder somente as permissões necessárias à identidade da aplicação.
- Validar conectividade, publicação, consumo, confirmação e falha/redelivery em homologação usando os recursos reais do provider.
- Confirmar concorrência, tempos de espera/visibilidade e política de retentativa com o tempo máximo de processamento do handler.
- Definir alertas e procedimento para mensagens acumuladas ou na DLQ; testar o fluxo de reprocessamento.
- Garantir idempotência dos handlers e propagar `correlationId` nos logs do serviço.
