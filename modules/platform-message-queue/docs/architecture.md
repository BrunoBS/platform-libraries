# Platform Message Queue: arquitetura interna

## Para que serve este guia

Este guia acompanha a implementação de `platform-message-queue`. Ele começa pela inicialização e configuração, depois segue os fluxos de publicação, consumo e dead-letter. A ideia é explicar por que cada parte existe e como as chamadas atravessam a biblioteca, para que alguém novo no módulo consiga navegar e mantê-lo.

O [guia da Golden Platform](https://github.com/BrunoBS/golden-platform/blob/main/docs/libraries/modules/platform-message-queue.md) explica como um serviço consumidor configura e usa a biblioteca. Este documento complementa esse guia com a arquitetura interna.

## Visão da arquitetura

A biblioteca expõe uma API independente de broker. O serviço escolhe um destino lógico e publica um objeto ou declara um listener. A configuração seleciona AWS SQS ou Azure Service Bus; o adapter converte a API comum para a SDK do provider.

```mermaid
flowchart TD
    Boot["Spring Boot"] --> Imports["AutoConfiguration.imports"]
    Imports --> Common["MessageQueueAutoConfiguration"]
    Imports --> Providers["Configuração AWS ou Azure"]
    Common --> Shared["Propriedades, registry e publisher"]
    Providers --> Adapter["Transport e consumer do provider"]
    Shared --> Adapter
    Adapter --> Broker["SQS ou Service Bus"]
```

A API se divide por finalidade: `api.annotation` declara listeners e metadados do payload; `api.contract` contém interfaces e tipos neutros; `api.configuration` define as propriedades e o seletor de provider. Configurações Spring ficam fora de `api`, enquanto detalhes de SDK e processamento ficam em `provider` e `internal`.

## Organização dos pacotes

| Pacote | O que contém | Avaliação |
|---|---|---|
| `api.annotation` | `@MessageQueueListener`, `@MessageQueueDeadLetterListener` e `@QueueMessage`. | Adequado: são declarações que o serviço consumidor usa no código. |
| `api.configuration` | `MessageQueueProperties` e `MessageQueueProvider`. | Adequado: propriedades e enum fazem parte do contrato de configuração público que o Spring faz bind. |
| `api.contract` | `MessageQueuePublisher`, envelopes/opções, dead-letter, contexto de entrega e catálogos de integração. | Adequado: agrupa tipos públicos neutros em relação ao broker. Os catálogos são interfaces de leitura para integrações da plataforma; o contexto de entrega permite consultar dados confiáveis durante a execução do listener. |
| `configuration` | Auto configuração comum do Spring Boot. | Adequado: cria beans compartilhados, sem depender da SDK de um provider. |
| `provider.aws` e `provider.azure` | Auto configuração, clients, transportes, consumers e descoberta de capacidades de cada broker. | Adequado: concentra diferenças de protocolo e SDK sem expô-las ao serviço consumidor. |
| `internal` | Resolução de destinos, envelope, serialização, registry de listeners/capacidades, MDC e transporte interno. | Adequado: são mecanismos de implementação e não pontos de extensão para aplicações. |

A separação existente já dá um caminho de leitura claro. Não abri subpacotes adicionais dentro de `api.contract`: são poucos tipos e ficam juntos por compor a superfície neutra da biblioteca. Dividi-los em pastas de request/response/model aqui criaria mais navegação sem uma diferença de ciclo de vida ou responsabilidade que justificasse a divisão.

Um ponto para acompanhar é o tamanho da API pública: os catálogos e o contexto de entrega atendem integrações da plataforma além do uso básico de publicação e listeners. Mantê-los em `api.contract` deixa explícito que são contratos públicos; qualquer mudança futura deve considerar compatibilidade com essas integrações.

## Dependências do módulo

| Dependência no POM | Uso |
|---|---|
| `spring-boot-autoconfigure` | Auto configurações, condições e registro de beans. |
| `spring-boot-starter-json` | JSON da mensagem; o serializer recebe o `ObjectMapper` do contexto. |
| `slf4j-api` | Logs do polling, processamento e falhas de transporte. |
| `platform-messaging-core` | Exceções e definições padronizadas de erro e validação. |
| AWS SDK `sqs` | Publicação, polling, exclusão, atributos e consulta de filas SQS. |
| `azure-messaging-servicebus` | Envio, recebimento, sessões, conclusão/abandono e administração do Service Bus. |
| `azure-identity` | Credencial Azure padrão via `DefaultAzureCredentialBuilder`. |
| `spring-boot-configuration-processor` | Opcional; metadados das propriedades para IDEs. |
| `spring-boot-test`, `platform-testing-core`, fixtures AWS e Azure | Suporte de testes, somente no escopo de teste. |

As duas SDKs estão no mesmo artefato porque o módulo oferece uma única API para os dois brokers. Os adapters ficam separados onde as semânticas realmente divergem, como confirmação de mensagem, sessões e opções de ordenação.

## Propriedades de configuração

Todas as propriedades usam o prefixo `platform.message-queue`. `MessageQueueProperties` rejeita campos desconhecidos e valida a configuração na inicialização. Assim, erro de digitação em nome de propriedade não passa despercebido.

Exemplo AWS:

```yaml
platform:
  message-queue:
    provider: AWS
    shutdown-timeout: 30s
    poll-failure-backoff: 1s
    aws:
      region: us-east-1
      defaults:
        wait-time: 20s
        concurrency: 2
    destinations:
      orders:
        queue: golden-orders
        publisher:
          enabled: true
        consumer:
          enabled: true
          concurrency: 4
        aws:
          dead-letter-queue: golden-orders-dlq
          visibility-timeout: 90s
```

Exemplo Azure:

```yaml
platform:
  message-queue:
    provider: AZURE
    shutdown-timeout: 30s
    poll-failure-backoff: 1s
    azure:
      namespace: golden.servicebus.windows.net
      max-auto-lock-renewal-duration: 5m
      defaults:
        wait-time: 20s
        concurrency: 2
    destinations:
      orders:
        queue: golden-orders
        publisher:
          enabled: true
        consumer:
          enabled: true
          concurrency: 4
```

| Propriedade | Obrigatória / padrão | AWS | Azure | Finalidade |
|---|---|---|---|---|
| `provider` | Obrigatória: `AWS` ou `AZURE`. | Ativa SQS. | Ativa Service Bus. | Seleciona a auto configuração e o adapter. |
| `shutdown-timeout` | Opcional; padrão `30s`. | Usada. | Usada. | Tempo máximo de encerramento gracioso dos workers antes de cancelar e fechar recursos. |
| `poll-failure-backoff` | Opcional; padrão `1s`. | Usada após falhas de polling. | Usada após falhas de polling ou de aquisição de sessão. | Evita um loop apertado de tentativas quando o broker falha. |
| `aws.region` | Obrigatória com AWS. | Usada para construir `SqsClient`. | Não usada. | Define a região AWS. A cadeia padrão de credenciais da SDK resolve as credenciais. |
| `azure.namespace` | Obrigatória com Azure. | Não usada. | Usada para construir os clients de Service Bus. | Identifica o namespace Azure; a credencial vem de `DefaultAzureCredential`. |
| `azure.max-auto-lock-renewal-duration` | Opcional; padrão `5m`; máximo validado de 12 horas. | Não usada. | Usada nos receivers. | Limita por quanto tempo o client renova automaticamente o lock enquanto o listener processa a mensagem. |
| `aws.defaults` / `azure.defaults` | Opcionais. | Defaults de `wait-time`, `concurrency` e `visibility-timeout`. | Defaults de `wait-time` e `concurrency`. | Fornecem valores comuns para destinos sem configuração própria. O bloco do provider inativo não é usado. |
| `destinations` | Obrigatória; precisa conter pelo menos um destino. | Usada. | Usada. | Mapeia um nome lógico usado pela aplicação para o nome físico da fila. |
| `destinations.<nome>.queue` | Obrigatória para cada destino. | Nome da fila SQS. | Nome da fila Service Bus. | Identificador físico usado pelo broker. |
| `destinations.<nome>.publisher.enabled` | Opcional; padrão `true`. | Usada. | Usada. | Permite bloquear publicação para um destino sem remover seu mapeamento. |
| `destinations.<nome>.consumer.enabled` | Opcional; padrão `true`. | Usada. | Usada. | Liga ou desliga os workers registrados para aquele destino. |
| `destinations.<nome>.consumer.wait-time` | Opcional. | Long polling SQS; de 0 a 20 segundos. | Espera de recebimento; inteiro de segundos maior que zero. | Ajusta a espera do receive para aquele destino. Se omitido, o provider usa seu default efetivo de 20 segundos. |
| `destinations.<nome>.consumer.concurrency` | Opcional; se omitido, efetivo `1`. | Usada. | Usada. | Quantidade de workers de consumo para o destino. Deve ser positiva quando definida. |
| `destinations.<nome>.aws.wait-time` e `concurrency` | Opcionais; somente AWS. | Sobrescrevem as opções equivalentes em `consumer`. | Não usadas. | Ajustes do destino específicos de SQS. |
| `destinations.<nome>.aws.visibility-timeout` | Opcional; de 0 a 12 horas. | Usada se configurada; sem valor, o request não sobrescreve a configuração da fila. | Não usada. | Define por quanto tempo a mensagem fica invisível enquanto está sendo processada. |
| `destinations.<nome>.aws.dead-letter-queue` | Opcional; somente AWS. | Fila SQS física explícita para o listener de dead-letter. | Não usada. | Se omitida, a biblioteca deriva o nome adicionando `-dlq` ou `-dlq.fifo` ao nome da origem. |
| `destinations.<nome>.azure.wait-time` e `concurrency` | Opcionais; somente Azure. | Não usadas. | Sobrescrevem as opções equivalentes em `consumer`. | Ajustes do destino específicos do Service Bus. |

A precedência de consumo é: ajuste do provider no destino, opção comum do destino e default do provider. Se concorrência não estiver definida em nenhum nível, o consumer usa um worker. Para o tempo de espera, os adapters usam 20 segundos quando não recebem valor.

Azure não usa `aws.*`; AWS não usa `azure.*`. Os blocos existem juntos no objeto de propriedades para permitir um prefixo de configuração comum, mas somente o provider selecionado lê e valida o seu bloco. `aws.defaults.visibility-timeout` é o default AWS adicional; não há equivalente de configuração global para o lock Azure, que é controlado por `azure.max-auto-lock-renewal-duration`.

## Inicialização no Spring

1. `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` registra a configuração comum, a AWS e a Azure.
2. `MessageQueueAutoConfiguration` habilita `MessageQueueProperties` e cria o relógio UTC, serializer, resolução de destino, envelope, registry de listeners e registry de capacidades. O publisher padrão só é criado se a aplicação não fornecer outro.
3. `MessageQueueProperties.afterPropertiesSet()` valida provider, destinos, tempos e o bloco do provider escolhido.
4. `@ConditionalOnProperty` ativa somente a configuração SQS ou Service Bus. `@ConditionalOnMissingBean` permite substituir client, transport ou publisher por beans fornecidos pela aplicação.
5. `MessageQueueListenerRegistry`, um `BeanPostProcessor`, encontra métodos anotados nos beans Spring e valida assinatura, tipo concreto do payload e destino.
6. Os consumers implementam `SmartLifecycle`; ao iniciar a aplicação, criam workers para os listeners registrados e habilitados. No encerramento, aguardam até `shutdown-timeout`, depois cancelam workers e fecham receivers.

| Anotação / mecanismo | Papel neste módulo |
|---|---|
| `@AutoConfiguration` | Marca as classes que o Spring Boot importa pelo arquivo de registro. |
| `@EnableConfigurationProperties` | Registra `MessageQueueProperties` para binding e validação. |
| `@ConfigurationProperties` | Faz o binding de `platform.message-queue`; `ignoreUnknownFields=false` rejeita chaves estranhas. |
| `@ConditionalOnProperty` | Escolhe AWS ou Azure conforme `provider`. |
| `@ConditionalOnMissingBean` | Mantém defaults substituíveis por integrações customizadas. |
| `@Bean` | Registra recursos e componentes no contexto; os clients/transports com lifecycle fecham no encerramento. |
| `BeanPostProcessor` | Descobre listeners nas classes gerenciadas pelo Spring. |
| `SmartLifecycle` | Inicia e encerra o polling no ciclo de vida da aplicação. |

## Publicação de mensagens

O ponto de entrada é `MessageQueuePublisher`. A aplicação passa o nome lógico do destino e o payload. O publisher verifica payload e configuração, resolve fila e opções, constrói o envelope neutro e serializa JSON. O transport do provider faz o envio final.

```mermaid
sequenceDiagram
    participant App as Serviço
    participant Pub as DefaultMessageQueuePublisher
    participant Shared as Resolver, envelope e serializer
    participant Broker as Transport e broker
    App->>Pub: publish(destino, payload, opções)
    Pub->>Shared: resolver destino e criar envelope
    Shared-->>Pub: mensagem JSON
    Pub->>Broker: enviar para fila física
    Broker-->>App: confirmação ou erro padronizado
```

`MessageEnvelopeFactory` gera `messageId`, timestamp UTC via `Clock`, tipo e versão, correlation ID, headers e payload. `@QueueMessage` permite declarar `type` e `version`; sem anotação, o tipo é o nome lógico do destino e a versão é `1`. O mesmo resolver é usado ao receber, para validar o contrato quando o payload tem a anotação.

`DestinationResolver` traduz o nome lógico em fila física e combina valores globais e específicos do destino. Também verifica se a publicação está habilitada. `MessageQueueSerializer` usa o `ObjectMapper` do Spring e converte falhas em erros padronizados do `platform-messaging-core`.

### Ordenação e deduplicação

O publisher aceita `orderingKey` e `deduplicationId` em `MessageQueuePublishOptions`. O registry consulta as capacidades reais da fila e guarda o resultado em cache:

- Em SQS, a ordenação é habilitada para filas FIFO. A chave vai para `MessageGroupId`; deduplicação usa `MessageDeduplicationId` quando necessário, ou a configuração de content-based deduplication da fila.
- Em Service Bus, a ordenação usa sessões; `orderingKey` é mapeada para `SessionId`. `deduplicationId` é específico de SQS FIFO e é rejeitado no Azure.
- Se a fila não for ordenada, não se pode enviar uma chave de ordenação. Os adapters retornam erro de validação em opções incompatíveis.

`QueueCapabilitiesRegistry` mantém a descoberta comum à biblioteca; `AwsQueueCapabilitiesResolver` consulta atributos SQS e `AzureQueueCapabilitiesResolver` consulta a administração do Service Bus. Manter a consulta dentro do adapter impede que nomes de atributos ou APIs de administração dos providers vazem para o contrato.

## Consumo de mensagens

A aplicação anota um método com `@MessageQueueListener("orders")` e recebe `MessageQueueMessage<Pedido>`. Durante a criação dos beans, `MessageQueueListenerRegistry` guarda o método, o bean e o tipo concreto do payload. Uma única assinatura inválida ou listener duplicado para o mesmo destino faz a configuração falhar no startup, antes de o serviço começar a consumir.

```mermaid
sequenceDiagram
    participant Broker as SQS ou Service Bus
    participant Consumer as Consumer do provider
    participant Shared as Serializer e listener registry
    participant App as Método anotado
    Broker->>Consumer: mensagem disponível
    Consumer->>Shared: desserializar e recuperar listener
    Shared->>App: invocar MessageQueueMessage<T>
    App-->>Consumer: sucesso ou exceção
    Consumer->>Broker: excluir/concluir ou manter para redelivery
```

No SQS, cada worker faz long polling de uma mensagem por chamada. Se o listener termina sem erro, o consumer exclui a mensagem. Se desserialização ou listener falha, não exclui; o SQS pode entregá-la de novo depois que a visibilidade expirar. O visibility timeout específico, se configurado, é enviado no receive.

No Service Bus, o receiver é criado com auto-complete desabilitado. Depois do sucesso o consumer chama `complete`; em falha chama `abandon`, para que o broker possa redeliver. Filas com sessões são consumidas com receiver de sessão, preservando a ordenação por session ID. O lock é renovado segundo `azure.max-auto-lock-renewal-duration`.

Antes de chamar o listener, ambos os adapters abrem `MessageQueueDeliveryContext` com provider, entry point, destino, identificadores do transporte e da mensagem, correlation ID e timestamp. `MessageQueueMdcContext` instala `messageId` e `correlationId` no MDC durante a chamada e restaura o contexto anterior ao sair. Assim os logs do serviço recebem o contexto sem deixar dados de uma mensagem contaminarem o worker seguinte.

## Dead-letter

`@MessageQueueDeadLetterListener("orders")` registra um handler para mensagens que já estão na dead-letter queue/subqueue. O payload é `DeadLetterMessage<T>`, com a mensagem neutra e metadados disponibilizados pelo broker.

Em SQS, a configuração `aws.dead-letter-queue` define a fila física. Se não informada, o nome é derivado da fila de origem; um listener de dead-letter exige que a referência exista. Depois de processado com sucesso, o consumer exclui a cópia da DLQ. Se o handler falha, a mensagem continua na fila.

No Service Bus, o consumer lê a dead-letter subqueue da fila e preserva razão, descrição, contagem de entregas e metadados retornados pela SDK quando disponíveis. Após sucesso conclui a mensagem na subqueue; na falha, abandona para redelivery. Campos que o provider não oferece ficam nulos, conforme o contrato de `DeadLetterMessage`.

## Classes nos fluxos

Em vez de decorar uma lista de nomes, siga o caminho da chamada:

- **Na configuração:** `MessageQueueProperties` representa o que o consumidor declarou; `MessageQueueAutoConfiguration` monta os componentes comuns; as auto configurações AWS/Azure montam as dependências e adapters do provider escolhido.
- **Para escolher a fila:** `DestinationResolver` aplica os valores da configuração e produz `ResolvedDestination`, um tipo interno já pronto para os adapters.
- **Para publicar:** `DefaultMessageQueuePublisher` coordena validação, envelope, serialização e envio. `MessageEnvelopeFactory`, `MessageMetadataResolver` e `MessageQueueSerializer` cuidam de partes pequenas e reutilizáveis desse fluxo.
- **Para consumir:** `MessageQueueListenerRegistry` descobre e valida handlers; `SqsMessageQueueConsumer` e `AzureServiceBusMessageQueueConsumer` controlam workers e acknowledge/redelivery com as regras de cada broker.
- **Para capacidades do broker:** `QueueCapabilitiesRegistry` guarda o resultado e delega a consulta a `AwsQueueCapabilitiesResolver` ou `AzureQueueCapabilitiesResolver`.
- **Para erro e observabilidade:** `MessageQueueMessageKeys` fornece as chaves de erro, `MessageQueueTechnicalErrors` cria a definição técnica de configuração, e `MessageQueueMdcContext` isola o MDC por entrega.

## Revisão de simplicidade

### Organização mantida

Os pacotes atuais já separam superfície pública, boot do Spring, comportamento compartilhado e SDKs concretas. Não recomendo mover classes agora: isso não corrigiria uma responsabilidade cruzada observada e faria a navegação depender de mais pastas.

Os dois consumers e transports são necessários porque SQS e Service Bus diferem no ack, DLQ, visibilidade/lock e ordenação. A lógica comum fica em envelope, serializer, resolução e contratos, sem uma abstração que esconda as operações importantes do broker.

### Acompanhar

- A API pública inclui os catálogos de destino/listeners e `MessageQueueDeliveryContext`, além do publisher e dos tipos de mensagem. Isso é útil para integrações da plataforma, mas cada tipo público passa a ser um compromisso de compatibilidade; confirme sua necessidade quando revisar consumidores do módulo.
- `MessageQueueProperties` concentra propriedades comuns e específicas dos dois providers em uma árvore de configuração. Essa forma dá um único prefixo consistente; se novos providers forem adicionados, reavaliar o tamanho e a validação central dessa classe.
- As opções `orderingKey` e `deduplicationId` são uma API neutra com suporte desigual entre providers. O comportamento hoje está explícito e validado; documentar essa diferença é mais simples que fingir equivalência entre brokers.

