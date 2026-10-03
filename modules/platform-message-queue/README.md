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

Cada destino permite habilitar/desabilitar publicação e consumo. Concorrência, tempos de espera e visibility timeout podem ser definidos como defaults do provider, por destino, ou como override específico do provider. O override específico do provider tem precedência sobre a configuração comum do destino, que tem precedência sobre o default do provider.

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

A aplicação precisa de credenciais AWS fornecidas pelo mecanismo padrão do SDK (por exemplo, role da workload) e permissões para enviar/receber mensagens e consultar a URL das filas.

### Fila principal e DLQ na AWS

Crie a fila principal e a DLQ na infraestrutura do serviço consumidor. Configure a redrive policy da fila principal apontando para a DLQ e defina o `maxReceiveCount` conforme a política de retry do serviço. O módulo da aplicação não cria nem altera recursos AWS.

Por padrão, o listener de dead-letter procura a fila física `<fila principal>-dlq`. Use `destinations.<destino>.aws.dead-letter-queue` para apontar para outro nome. A política de redrive continua sendo responsabilidade da infraestrutura.

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

A aplicação usa `DefaultAzureCredential`; forneça uma identidade de workload e as permissões de Service Bus necessárias no namespace. No Azure, dead-letter é a subfila nativa da fila. Configure `MaxDeliveryCount` na entidade da fila conforme a política operacional. O módulo não provisiona nem configura a fila.

## Publicação e consumo

Anote o tipo de payload com `@QueueMessage` para definir metadados estáveis no envelope. Sem a annotation, o tipo usa o nome do destino e a versão `1`. O consumidor recebe `messageType` e `messageVersion`, mas a lib não valida automaticamente a compatibilidade entre versões; o serviço deve decidir como tratar versões que não conhece.

```java
@QueueMessage(type = "order.created", version = "1")
public record OrderCreated(String orderId) {}
```

Publique pelo destino lógico:

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

O processamento é confirmado somente quando o listener retorna normalmente. Se lançar uma exceção em runtime, a mensagem não é confirmada e o broker pode entregá-la novamente. Isso implica entrega *at least once*: handlers precisam ser idempotentes, pois duplicatas também podem ocorrer se o processamento concluir e a confirmação falhar.

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

O nome padrão da DLQ no mock é `orders-dlq`; é possível informar `deadLetterQueue = "custom-dlq"`. Para Azure Service Bus, configure o limite de entregas do emulador junto da fila: `@WithAzureEmulator(serviceBus = @AzureServiceBus(queues = @AzureServiceBus.Queue(name = "orders", maxDeliveryCount = 3)))`; a DLQ é a subfila nativa.

## Checklist antes de produção

- Provisionar filas, DLQ/redrive (AWS) ou `MaxDeliveryCount` (Azure) na infraestrutura do serviço consumidor.
- Conceder somente as permissões necessárias à identidade da aplicação.
- Validar conectividade, publicação, consumo, confirmação e falha/redelivery em homologação usando os recursos reais do provider.
- Confirmar concorrência, tempos de espera/visibilidade e política de retentativa com o tempo máximo de processamento do handler.
- Definir alertas e procedimento para mensagens acumuladas ou na DLQ; testar o fluxo de reprocessamento.
- Garantir idempotência dos handlers e propagar `correlationId` nos logs do serviço.
