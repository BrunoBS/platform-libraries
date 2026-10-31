# platform-message-queue

Módulo Maven de Platform Libraries. Contratos, configuração e exemplos estão no [guia oficial](https://github.com/BrunoBS/golden-platform/blob/main/docs/libraries/modules/platform-message-queue.md).

- [POM e dependências](pom.xml)
- [Desenvolvimento e build do reactor](../../README.md)

Execute os comandos de build na raiz do repositório para incluir os módulos dependentes. Fixtures de integração exigem Docker; agregadores POM não são dependências JAR.

## Evidência de entrega SQS

O consumidor SQS oficial expõe `SqsMessageQueueConsumer.currentDelivery()` como leitura opcional do contexto privado da execução atual. Somente o consumidor abre/fecha esse escopo; construir um Delivery não o ativa. O snapshot preserva listener/destino/queue URL observados, IDs do transporte/envelope, correlação e timestamp do envelope. Não representa o timestamp original da ocorrência Audit.

O contexto é removido em finally, não é herdado por threads e não é propagado para tarefas assíncronas. O MDC é limitado a identificadores seguros, com restauração do contexto anterior. Headers do payload não podem substituir o destino observado. A autorização, quando integrada, continua na capability platform-authorization; esta biblioteca não implementa autorização de domínio.

ACK permanece exclusivamente no consumidor, após retorno normal do handler; exceção impede delete e falha de delete permite repetição. IAM, redrive/DLQ e idempotência transacional continuam exigindo configuração/implementação próprias de seus responsáveis, sem provisioning implícito.

Para payloads anotados com `@QueueMessage`, o serializer de entrada verifica tipo, versão e payload presente; incompatibilidade segue a falha normal de desserialização, sem confirmação. Payloads existentes sem a anotação mantêm o contrato anterior. Não foi introduzido um envelope alternativo ou decoder implícito de notificações S3 nativas.
