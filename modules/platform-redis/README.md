# platform-redis

Biblioteca Redis genérica para Java 25 / Spring Boot 4.1 / Jackson 3. Servidor Redis 6.2+; integração validada com Redis 7 fixado por digest. Independente de Workspace, Configuration, Feature Toggle e Message Queue. Contratos, tipos nativos e decisões de implementação: [matriz da Onda 0](../../docs/platform-redis/ONDA-0.md). [Refinamento V3](../../docs/platform-redis/platform-redis-refinamento_V3.md).

## Começar

Com o BOM da plataforma, inclua somente esta capability (não está no platform-starter):

```xml
<dependency>
    <groupId>br.com.portalmanager.platform.library</groupId>
    <artifactId>platform-redis</artifactId>
</dependency>
```

```yaml
spring:
  application:
    name: orders-service
  data:
    redis:
      host: localhost
      port: 6379
      connect-timeout: 2s
      timeout: 2s
```

Injete RedisStore, AsyncRedisStore, RedisOperations, RedisPublisher ou RedisStreams. A autoconfiguração registra APIs passivas sem iniciar consumidores. Conexões/StringRedisTemplate/ReactiveStringRedisTemplate do Boot são reutilizadas; APIs customizadas são preservadas por ConditionalOnMissingBean. Nenhuma configuração de Streams/PubSub é exigida para Store. `platform.redis.enabled=false` desabilita o módulo.

O driver padrão é Lettuce, com infraestrutura síncrona e reativa compartilhada. Uma conexão customizada deve oferecer ambos os contratos do Spring Data Redis para disponibilizar AsyncRedisStore com I/O real. O módulo não cria uma segunda conexão independente com credenciais diferentes.

## JSON, leitura, TTL e API fluida

```java
store.set("order.42", order);
Optional<Order> order = store.get("order.42", Order.class);
Optional<List<Order>> orders = store.get("orders", new TypeReference<List<Order>>() {});
boolean created = store.write("lock.42", token)
        .ttl(Duration.ofSeconds(30))
        .ifAbsent()
        .execute();
asyncStore.get("order.42", Order.class).thenAccept(this::process);
```

TypeReference é `tools.jackson.core.type.TypeReference` (Jackson 3). Valores são JSON UTF-8 em Redis String. DTO/lista/mapa/genéricos suportados. Tipagem polimórfica global está desabilitada e coerções escalares são recusadas; nomes de classe recebidos no JSON não resolvem classes arbitrárias. Personalizações/módulos do ObjectMapper da aplicação são mantidos numa cópia segura, sem alterar o mapper original.

Chave ausente retorna Optional.empty. JSON inválido, JSON null externo, erro de tipo e falha de infraestrutura geram exceções padronizadas; nunca são convertidos em ausência. Java null/JsonNode null não são aceitos em escrita. Use delete para remover.

set sem TTL é persistente e remove expiração anterior; TTL deve ser pelo menos 1 ms. setIfAbsent é SET NX atômico. expire/persist/delete retornam boolean do Redis. ttl empty significa **chave ausente ou persistente**; não distingue os dois casos. O builder representa uma única operação: execute repetido (mesmo após falha), ttl ou ifAbsent após executar geram redis.builder.used. false em execute significa exclusivamente condição NX não satisfeita.

AsyncStore espelha operações diretas; usa I/O reativo Lettuce e CompletionStage, sem supplyAsync nem encapsular I/O bloqueante. Serialização ocorre na thread chamadora; erros de validação/serialização/Redis completam o stage excepcionalmente. Cancelamento do future cancela a assinatura reativa, sem promessa de desfazer comando já enviado.

## Lotes de chaves independentes — V5

`RedisStore.setAll` usa um único **MSET**; `getAll` usa um único **MGET**. São chaves Redis String independentes, diferentes de `operations.hash().getAll`, que lê campos de um único Hash.

```java
Map<String, String> values = Map.of("setting:a", "true", "setting:b", "100", "setting:c", "enabled");
store.setAll(values);
Map<String, String> found = store.getAll(values.keySet(), String.class);
Map<String, String> partial = store.getAll(List.of("setting:a", "missing", "setting:c"), String.class);

record Setting(boolean enabled, int threshold) {}
store.setAll(Map.of("setting:one", new Setting(true, 10), "setting:two", new Setting(false, 20)));
Map<String, Setting> settings = store.getAll(List.of("setting:one", "setting:two"), Setting.class);

Map<String, List<String>> groups = Map.of("roles:a", List.of("ADMIN", "READER"), "roles:b", List.of("READER"));
store.setAll(groups);
Map<String, List<String>> roles = store.getAll(groups.keySet(), new TypeReference<List<String>>() {});
asyncStore.setAll(groups).thenCompose(ignored -> asyncStore.getAll(groups.keySet(), new TypeReference<List<String>>() {}));
```

- `TypeReference` é o mesmo Jackson 3 das operações individuais. Não há serializador separado nem tipagem polimórfica por entrada. Cada valor é codificado independentemente; a leitura exige que todos os valores encontrados sejam compatíveis com o tipo solicitado. Recomenda-se um tipo homogêneo por lote.
- MSET sobrescreve todas as entradas atomicamente **no lote enviado**, remove TTL anterior e não atribui TTL. O módulo não divide o lote automaticamente e não executa SET/GET em loop.
- MGET omite respostas nil, preserva o alinhamento por índice e retorna chaves lógicas na ordem da primeira ocorrência solicitada, em um mapa não modificável. Duplicatas são deduplicadas antes do comando. Conforme a semântica nativa de MGET, uma chave de tipo Redis diferente de String também produz nil; JSON String inválido, null externo ou incompatível com o tipo solicitado é erro, nunca ausência.
- Mapas/coleções vazios são no-op sem conexão/comando. Argumento/tipo/chave/valor nulo, chave vazia ou inválida geram `redis.validation`. Um tipo nulo é inválido mesmo numa coleção vazia. Async completa o stage excepcionalmente, sem bloquear em I/O.
- `platform.redis.max-batch-size` limita escrita; `max-read-size` limita chaves distintas lidas; `max-key-bytes` inclui namespace e `max-payload-bytes` vale por valor. Toda a escrita é validada/serializada antes do MSET. Leituras MGET validam o tamanho de cada payload recebido no codec, sem scripts adicionais. Erros de conexão, serialização e limites seguem as exceções existentes.
- Em Redis Cluster com a factory Lettuce, todas as chaves físicas precisam compartilhar o hash slot. Use nomes como `{scope}:a` e `{scope}:b`; prefixos são aplicados antes do cálculo. Lotes entre slots são recusados com `redis.validation` antes do comando, impedindo scatter/gather implícito do driver e perda de atomicidade. Factories customizadas sem metadados de topologia Lettuce adotam a regra conservadora de mesmo slot, inclusive quando delegam a standalone; use hash tags comuns. A validação de slots é coberta por testes; a integração com Redis real usa standalone.

O tamanho de lote do serviço consumidor é uma política externa à lib. Se o consumidor escolher 500 (configurável e positivo), 1.200 itens serão três chamadas de 500, 500 e 200. Não existe atomicidade entre essas chamadas nem com o banco; um erro posterior não desfaz lotes anteriores. A lib permanece genérica e não conhece publicação de configurações ou seus schedulers.

Relatório de implementação e decisões: [V5](../../docs/platform-redis/ONDA-7-RELATORIO-V5.md).

## Namespace e limites

ISOLATED é o padrão: chave física `<spring.application.name>:<key>`; com namespace explícito, `<namespace>.<spring.application.name>:<key>`. SHARED requer namespace explícito e gera `<namespace>:<key>` entre serviços. Compartilhar é decisão explícita:

```yaml
platform:
  redis:
    namespace-mode: SHARED
    namespace: common-data
```

Namespace usa letras/números/ponto/traço/underscore. A validação de tamanho inclui o prefixo físico. Chaves/canais/campos não podem ser vazios, conter controles ou exceder max-key-bytes. **Canais e padrões não recebem prefixo**, em nenhum modo.

| Propriedade platform.redis | Default | Finalidade |
| --- | --- | --- |
| max-key-bytes | 256 | bytes UTF-8 de chave física/canal/campo/nome |
| max-payload-bytes | 1048576 | JSON por valor/evento, incluindo valores externos lidos |
| max-batch-size | 1000 | batch de comandos/ACK/inscrições |
| max-read-size | 1000 | máximo de itens/janela por leitura |
| listener-threads | 4 | workers de handlers Pub/Sub |
| listener-queue-capacity | 256 | fila limitada Pub/Sub |
| max-stream-workers | 16 | teto total de concorrência declarada Streams |
| stream-max-length | 10000 | retenção máxima/default de Streams |
| shutdown-timeout | 5s | prazo por executor para encerramento gracioso |
| reconnect-min / reconnect-max | 100ms / 5s | backoff exponencial com jitter |
| keyspace-enabled | false | permite inscrição nas notificações nativas |

Limites devem ser positivos. Tetos de configuração: chave 65536 bytes, payload 64 MiB, batch/retorno/fila 100000 e workers/threads 256. Não são limites de vazão nem garantia de memória total: dimensione payload × batch × concorrência para a aplicação. GET usa Lua para recusar String externa acima do limite antes de transmiti-la ao cliente; outros valores são verificados pelo codec. Não há API pública KEYS, FLUSHDB ou FLUSHALL.

## Estruturas nativas e operações atômicas

```java
operations.hashes().put("order.42", "owner", owner);
Map<String, Owner> owners = operations.hashes().getAll("owners", Owner.class, 100);
operations.sets().add("tags", "priority");
List<Order> page = operations.lists().range("queue", 0, 49, Order.class);
operations.sortedSets().add("ranking", "customer.42", 12.5);
long next = operations.atomic().increment("sequence", 1);
boolean changed = operations.atomic().compareAndSet("status", "DRAFT", "PUBLISHED");
```

Hash/Set/List/Sorted Set são tipos Redis nativos com valores/membros JSON; não convertem tipos Redis incompatíveis. Counters usam números Redis. getAll/members exigem limite e recusam cardinalidade maior em Lua atômico (sem truncamento silencioso). range/trim de listas e range de sorted sets exigem índices não negativos e janela limitada; `end=-1` não é permitido. Sorted Set mantém ordem crescente de score/rank. Deltas de contador são positivos; scores/deltas de sorted set devem ser finitos.

CAS usa Lua, nunca GET/SET separados. Compara representação JSON exata (incluindo ordem dos campos); expected=null exige ausência. replacement não pode ser null. Sucesso preserva TTL existente; criação é persistente. Redis Cluster: CAS e operações limitadas de estruturas usam uma única chave. As operações guardadas descritas abaixo usam duas chaves, que devem pertencer ao mesmo hash slot; não há coordenação entre slots.

## Comparação atômica e varredura de chaves

Os contratos são técnicos e reutilizáveis; não conhecem entidades, eventos ou regras de serviços consumidores:

```java
boolean written = operations.atomic().setIfValue("{scope}:guard", token, "{scope}:item", dto);
boolean removed = operations.atomic().deleteIfValue("{scope}:guard", token, "{scope}:item");
boolean released = operations.atomic().compareAndDelete("{scope}:guard", token);
operations.keys().scan("item.", 100, this::processKey);
```

setIfValue compara a representação JSON do guard e grava o valor de destino persistentemente numa única operação Redis; sucesso remove TTL anterior do destino. deleteIfValue compara e remove atomicamente; true significa guard correspondente, inclusive se o destino já estiver ausente. Guard ausente, expirado ou diferente retorna false sem modificar o destino. Guard e destino devem ser distintos. compareAndDelete remove a própria chave somente quando seu JSON corresponde ao esperado. Expected e valor não podem ser null. Estes contratos não adquirem nem renovam locks e não inferem regras de domínio.

As operações com guard e destino usam Lua de duas chaves. Em Redis Cluster, ambas devem estar no mesmo hash slot: use hash tags comuns, como `{scope}`, nos nomes lógicos. Sem essa condição, Redis rejeita o comando; não há fallback GET/SET ou perda de atomicidade.

keys().scan recebe prefixo literal (não glob), count positivo limitado por max-read-size e callback com chaves lógicas sem o prefixo físico da biblioteca. Prefixo vazio percorre somente o namespace atual; caracteres glob são escapados. O cursor é encerrado pela lib e não materializa a lista inteira. count é uma dica de batch do Redis, não um teto de trabalho total. SCAN pode retornar duplicados e não é um snapshot consistente sob alterações concorrentes: callbacks devem tolerar repetição. Exceções do callback são propagadas; falhas de transporte usam os erros padronizados. Não há API KEYS ou FLUSH.

## Pub/Sub

```java
publisher.publish("order.changed", event);

@Component
@RedisSubscribe(channels = {"order.changed", "order.cancelled"}, patterns = {"shipment.*"})
class OrderListener implements RedisMessageListener<OrderEvent> {
    @Override public void onMessage(OrderEvent event) { /* processar */ }
}
```

Publish retorna o long reportado pelo Redis, não confirmação do handler. Store.set não publica; Publisher.publish não grava valores. N beans, canais e padrões são suportados, com tipo genérico resolvido, inclusive herança parametrizada. Classes listener devem declarar tipos resolvíveis; proxies Spring são suportados pelo tipo alvo.

Somente listeners anotados iniciam consumidores. Handlers rodam fora do I/O, em executor/fila limitados. Saturação **descarta a tarefa excedente**, registra redis.saturation e incrementa contador; não bloqueia I/O. Pub/Sub é transitório, sem ACK/replay/retry garantido. Instâncias inscritas recebem cópias; mensagens durante desconexão são perdidas. Reconexão é automática com backoff/jitter. Shutdown remove inscrições e aguarda handlers pelo prazo configurado, depois interrompe tarefas restantes.

## Streams

```java
String id = streams.append("orders.events", event); // MAXLEN exato e limitado
streams.createGroup("orders.events", "billing", GroupStart.BEGINNING);
List<RedisStreamRecord<OrderEvent>> records = streams.readGroup(
        "orders.events", "billing", "worker-1", 20, Duration.ofMillis(250), OrderEvent.class);
streams.acknowledge("orders.events", "billing", records.stream().map(RedisStreamRecord::id).toList());
```

read usa ID exclusivo no formato `0-0`; Duration.ZERO faz leitura não bloqueante. Bloqueio máximo de leitura explícita é 5s e deve ser inferior ao timeout da conexão. createGroup é idempotente, usa MKSTREAM e início 0/$ (BEGINNING/LATEST). recover percorre uma janela limitada com XPENDING sem IDLE, filtra idle localmente e reclama com XCLAIM, que revalida minIdle atomicamente. Não usa XPENDING IDLE sem delimitar trabalho. O overload com afterId retorna RedisStreamRecovery(nextId, records); use nextId para continuar, mesmo se records estiver vazio. nextId=0-0 reinicia a varredura. A conveniência sem cursor examina apenas a primeira janela; listeners avançam seu cursor automaticamente. Nenhum GET+SET simula atomicidade. Consulte a [revisão de performance](../../docs/platform-redis/REVISAO-PERFORMANCE.md).

```java
@Component
@RedisStreamListener(stream = "orders.events", group = "billing", concurrency = 2,
        batchSize = 20, blockMillis = 250, minIdleMillis = 60000,
        recoveryIntervalMillis = 10000, start = GroupStart.BEGINNING, maxLength = 10000)
class BillingListener implements RedisStreamMessageListener<OrderEvent> {
    @Override public void onMessage(OrderEvent event) { /* lançar exceção em falha */ }
}
```

Consumidor físico inclui token por instância/worker, mesmo com consumer explícito, para evitar colisão. Concorrência total limitada. O polling dedicado só existe com listeners anotados; não há fila ilimitada. maxLength=0 na anotação herda o default; retenção é aplicada em append e nas reconciliações de pending do listener. workers fazem backoff/jitter em falha técnica.

ACK somente depois do handler retornar normalmente. Falha de handler/JSON permanece pending e não bloqueia outros registros do batch. Pending é recuperado periodicamente, inclusive de consumidores desconectados. Escolha minIdle maior que o tempo normal de processamento para evitar claim de trabalho ainda em andamento. Se o processo parar depois do handler e antes do ACK, haverá reprocessamento: torne o consumidor idempotente. Não há DLQ, exactly-once, retries limitados ou transação distribuída implícitos. Retenção pode remover payload ainda pending. Um registro permanentemente inválido requer intervenção da aplicação; counters/error handler permitem detectar esse caso.

O transporte dos listeners permanece funcional mesmo quando uma aplicação substitui o bean passivo RedisStreams. Não há dependência com Message Queue.

## Keyspace Notifications

```java
@Component
@RedisKeyspaceSubscribe(patterns = {"__keyevent@0__:expired"})
class ExpirationListener implements RedisKeyspaceListener {
    @Override public void onMessage(RedisKeyspaceEvent event) { /* canal/body nativos */ }
}
```

Exige platform.redis.keyspace-enabled=true e configuração explícita do servidor (por exemplo notify-keyspace-events=Ex). A biblioteca nunca executa CONFIG SET para habilitar notificações em produção. São eventos nativos de chave, não payloads JSON nem eventos de negócio; compartilham os limites/lifecycle/observabilidade de Pub/Sub.

## Segurança, erros e observabilidade

TLS, usuário/ACL, senha, Sentinel/Cluster e timeouts seguem propriedades oficiais spring.data.redis. Para TLS validado, configure spring.data.redis.ssl.enabled=true e um SSL bundle de confiança; o módulo não desativa verificação de certificados/hostname. Há teste real TLS com autenticação e rejeição de senha inválida. Arquivos tls em src/test/resources são credenciais públicas **exclusivamente de teste**.

Exceções são ValidationException/ApiException/PlatformConfigurationException de platform-messaging; não há hierarquia Redis própria. Códigos PLT-REDIS-001..013 e bundles redis_pt_BR/redis_en cobrem validação, indisponibilidade, timeout, autenticação, tipo, serialização, builder, configuração, listener, limites e saturação. Causa técnica é sanitizada (classe/categoria, sem mensagem/cause original, valores, credenciais ou payload). Mapear timeout não garante que o comando não executou; NX/CAS não resolvem resultado indeterminado de rede automaticamente.

RedisListenerErrorHandler pode ser substituído. Default registra apenas transport e messageKey. RedisListenerMetrics oferece contadores monotônicos de processados/falhas/rejeitados, sem labels sensíveis. Integradores podem exportá-los para seu sistema de métricas. Nenhuma chave/canal/payload é registrada pela biblioteca; não habilite logs TRACE de drivers sobre dados sensíveis.

## Verificar

```sh
mvn --batch-mode --no-transfer-progress -pl modules/platform-redis -am verify
mvn --batch-mode --no-transfer-progress clean verify
python3 scripts/verify-library-namespace.py
```

Integrações usam Testcontainers Redis real fixado por digest e platform-testing-redis/@WithRedis e platform-testing-core/PinnedDockerImage; não substituem Redis por mocks para testar seus comandos. Unitários cobrem codec/segurança, I/O reativo e arquitetura. Testes incluem TLS/ACL, pausa/timeout real, corte/reconexão de Pub/Sub, Streams/pending/recovery, concorrência e bootstrap somente por dependência. Não executar deploy nesta tarefa. Evidências por onda em docs/platform-redis.


## Spring Cache opcional — V4 / Onda 6

A capability de cache só é ativada com `platform.redis.cache.enabled=true`. Não é necessário declarar `@EnableCaching`, `CacheManager` ou infraestrutura Redis manualmente. Um CacheManager ou CachingConfigurer da aplicação tem precedência. Com vários managers, defina @Primary ou selecione cacheManager/cacheResolver na anotação; resolução implícita ambígua falha com erro de configuração. Usa interceptação oficial Spring e RedisCacheManager/RedisCacheWriter nativos; não há outro motor de cache ou dependência de serviços consumidores.

```yaml
platform:
  redis:
    cache:
      enabled: true
      default-ttl: 30m
      environment: production
      failure-policy: FAIL_FAST
      lock-ttl: 5s
      regions:
        messages:
          ttl: 1h
        accounts:
          ttl: 15m
```

Regiões não declaradas são criadas dinamicamente com TTL default. Todos os TTLs devem ser positivos e de pelo menos 1 ms. Sem environment explícito, utiliza profiles ativos ordenados ou `default`. Prefixos de dados e locks incluem namespace, serviço, ambiente e região, inclusive no modo SHARED. Limpeza usa SCAN limitado por batch; não há FLUSH global. Exemplo de chave ISOLATED: `team.orders:cache:orders:production:messages::42`; em SHARED: `common:cache:orders:production:messages::42`.

```java
@Cacheable(cacheNames = "messages", key = "#tenant + ':' + #locale + ':' + #id")
public Message find(String tenant, String locale, String id) { /* repositório */ }

@CachePut(cacheNames = "messages", key = "#tenant + ':' + #locale + ':' + #id")
public Message update(String tenant, String locale, String id) { /* atualizar */ }

@CacheEvict(cacheNames = "messages", key = "#tenant + ':' + #locale + ':' + #id")
public void delete(String tenant, String locale, String id) { /* remover */ }
```

`@Caching`, `allEntries` e seleção explícita de manager/resolver seguem Spring. A aplicação define chaves e inclui tenant, locale e identidade quando alteram a resposta. Não armazenar indiscriminadamente `findAll`, resultados de autorização ou respostas dependentes de usuário em uma chave global. Chamadas dentro do próprio bean não passam pelo proxy: self-invocation não ativa cache.

JSON não contém nomes de classe nem habilita default typing. Tipos são resolvidos das assinaturas dos métodos anotados: DTO, arrays, List, Map e Optional com genéricos concretos. Uma região deve ter um único tipo de valor compatível; retorno Object, void, tipos não resolvíveis, Future/CompletionStage e Publisher são recusados. Conflitos falham com PLT-REDIS-012: separe regiões de tipos diferentes. Acesso programático com serialização exige uma região registrada por método tipado. JSON externo inválido nunca vira cache miss silencioso. Valores null, Optional.empty e exceções do método não são armazenados, inclusive com sync=true. Um @CachePut que retorna null preserva uma entrada anterior; use @CacheEvict para removê-la.

Manager transactionAware adia put, evict e clear normais até o commit de transação Spring; rollback preserva o estado anterior. A autoconfiguração de cache é ordenada após a infraestrutura transacional do Boot, mantendo a transação ao redor do interceptor de cache na configuração padrão, inclusive para rollbackOnly sem exceção. Se a aplicação customizar a ordem dos advisors, deve manter essa relação; o decorator só observa transações ativas no momento da operação. `sync=true` em transação ativa é recusado com PLT-REDIS-013 porque o loader nativo faz escrita imediata. Operações nativas putIfAbsent/evictIfPresent e `beforeInvocation=true` são imediatas. Uma falha Redis no callback após commit pode ser propagada quando o banco já foi confirmado: não há transação distribuída nem rollback retroativo do banco.

FAIL_FAST é o padrão. FAIL_OPEN_READ permite fallback e população best effort apenas em @Cacheable para indisponibilidade, timeout e autenticação Redis. Falhas de JSON/contrato continuam explícitas. @CachePut/@CacheEvict e erros do repositório nunca são ocultados. Selecionar `cacheManager="platformRedisCacheManager"` explicitamente utiliza a visão estrita; para a política de leitura, use o resolver padrão ou `cacheResolver="platformRedisCacheResolver"`. Configuração/resolver customizado assume a própria política.

`sync=true` usa locking nativo Redis com lease lock-ttl, isolado por serviço/ambiente/região. O lock é por região, podendo serializar chaves distintas; dimensione seu TTL acima do tempo esperado do loader. Expiração do lease pode permitir trabalho duplicado. Não há exactly-once, renovação de lock, cache local ou invalidação por Pub/Sub implícitos. Escritas do writer são imediatas para observar erros no thread chamador; apenas as operações transactionAware aguardam commit.

A fixture de integração é documentada em [platform-testing-redis](../platform-testing/platform-testing-redis/README.md). Decisões e evidências: [plano da Onda 6](../../docs/platform-redis/ONDA-6-PLANO.md) e [relatório V4](../../docs/platform-redis/ONDA-6-RELATORIO-V4.md).
