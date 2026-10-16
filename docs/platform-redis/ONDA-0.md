# Platform Redis V3 — Onda 0

Referência integral baixada de https://raw.githubusercontent.com/BrunoBS/guideline-java/main/docs/refinamento/16-platform-redis/platform-redis-refinamento_V3.md em 14/10/2026. Branch feature/platform-redis-v3 criada sobre main, preservando seu commit local. Workspace/Account Service pausado, sem mudança de código consumidor.

## Inspeção e decisões anteriores à implementação

Parent Java 25, Boot 4.1.1, Jackson 3.2.3 (tools.jackson.core.type.TypeReference), Spring Data Redis 4.1.1, Lettuce 7.5.2. Pacote base real br.com.portalmanager.platform.library.redis. Parent já configura Enforcer/convergence, Surefire, Failsafe e JaCoCo; BOM gerencia apenas capabilities próprias. Testes reutilizam platform-testing-core (PinnedDockerImage) e Testcontainers, sem dependência circular. Nenhum módulo Redis no starter.

Messaging disponibiliza ValidationException e ApiException com messageKey/cause e PlatformConfigurationException com PlatformErrorDefinition; usar essas classes diretamente, sem hierarquia Redis. Bundles redis_en.properties/redis_pt_BR.properties. Causas técnicas terão categoria/classe, sem mensagens/payload/credenciais originais. Observabilidade por SLF4J, códigos estáveis e contadores de falhas/saturação, sem registrar dados.

Autoconfiguração após DataRedisAutoConfiguration/DataRedisReactiveAutoConfiguration. Starter Redis reativo fornece ambas as APIs, usando a mesma infraestrutura Lettuce. AsyncStore usa ReactiveStringRedisTemplate e toFuture, nunca supplyAsync. Infraestrutura passiva não conecta por iniciativa do módulo. Só listeners declarados iniciam consumidores.

Namespace ISOLATED padrão deriva de spring.application.name; SHARED exige namespace explícito. Nome da aplicação já é requisito de messaging. Chaves são prefixadas; canais/padrões nunca são prefixados. Credenciais/TLS/timeouts pelas propriedades oficiais spring.data.redis. Keyspace opcional: apenas inscrição, jamais CONFIG SET.

## Matriz de assinaturas aprovada para implementação nesta tarefa

Tipos: T valor JSON; Class<T> para operações avançadas; RedisStore/AsyncStore suportam também TypeReference<T>. Durações positivas. Contagens/índices/retornos limitados por platform.redis; operações de retorno integral verificam cardinalidade atomicamente antes de ler. Nenhuma API KEYS/FLUSH.

| API / assinatura Java | Retorno | Comando |
| --- | --- | --- |
| Store.set(key,T[,Duration]) | void | SET [PX] |
| Store.get(key,Class/TypeReference) | Optional<T> | GET |
| exists/delete/expire/persist | boolean | EXISTS/DEL/PEXPIRE/PERSIST |
| ttl(key) | Optional<Duration> | PTTL; empty ausente ou persistente |
| setIfAbsent(key,T[,Duration]) | boolean | SET NX [PX] |
| write(key,T).ttl(Duration).ifAbsent().execute() | RedisWrite<T>/boolean | SET; builder single use |
| AsyncStore mesmas operações diretas | CompletionStage dos retornos acima | I/O reativo Lettuce |
| hashes.put(key,field,T) | void | HSET |
| hashes.get(key,field,Class<T>) | Optional<T> | HGET |
| hashes.getAll(key,Class<T>,int limit) | Map<String,T> | Lua HLEN + HGETALL, falha se excede limite |
| hashes.remove/exists(key,field) | boolean | HDEL/HEXISTS |
| sets.add/remove/contains(key,T) | boolean | SADD/SREM/SISMEMBER |
| sets.members(key,Class<T>,int limit) | Set<T> | Lua SCARD + SMEMBERS, falha se excede limite |
| sets.count(key) | long | SCARD |
| lists.pushLeft/pushRight(key,T) | long | LPUSH/RPUSH |
| lists.popLeft/popRight(key,Class<T>) | Optional<T> | LPOP/RPOP |
| lists.range(key,start,end,Class<T>) | List<T> | LRANGE; índices não negativos, janela limitada |
| lists.length(key) / trim(key,start,end) | long / void | LLEN/LTRIM |
| sortedSets.add(key,T,double score) | boolean | ZADD |
| sortedSets.remove(key,T) | boolean | ZREM |
| sortedSets.score(key,T) / incrementScore(key,T,double) | Optional<Double> / double | ZSCORE/ZINCRBY |
| sortedSets.range(key,start,end,Class<T>) | Set<T> ordenado | ZRANGE; janela limitada |
| sortedSets.rank(key,T) / count(key) | Optional<Long> / long | ZRANK/ZCARD |
| atomic.increment/decrement(key,long positiveDelta) | long | INCRBY/DECRBY |
| atomic.setIfAbsent(key,T) | boolean | SET NX |
| atomic.compareAndSet(key,expected,T replacement) | boolean | Lua GET/SET KEEPTTL; expected null exige ausência |
| publisher.publish(channel,T) | long | PUBLISH; assinantes, não confirmação |
| streams.append(stream,T[,long maxLength]) | String id | XADD MAXLEN (exato) |
| streams.read(stream,afterId,count,block,Class<T>) | List<RedisStreamRecord<T>> | XREAD COUNT [BLOCK] |
| streams.createGroup(stream,group,GroupStart) | void idempotente | XGROUP CREATE MKSTREAM, 0 ou $ |
| streams.readGroup(stream,group,consumer,count,block,Class<T>) | List<RedisStreamRecord<T>> | XREADGROUP > |
| streams.acknowledge(stream,group,List<String> ids) | long | XACK; batch limitado |
| streams.recover(stream,group,consumer,minIdle,count,Class<T>) | List<RedisStreamRecord<T>> | XPENDING IDLE limitado + XCLAIM |
| streams.trim(stream,maxLength) | long | XTRIM MAXLEN exato |

Listeners Pub/Sub: @RedisSubscribe(channels={},patterns={}) + RedisMessageListener<T>. Streams: @RedisStreamListener(stream,group,consumer,concurrency,batchSize,blockMillis,start,minIdleMillis,recoveryIntervalMillis,maxLength) + RedisStreamMessageListener<T>. Consumo de grupos explícito via API e automático via anotação. Streams têm workers limitados; ACK somente após handler concluir; falha permanece pending. Polling tem backoff/jitter, interrupção e shutdown com prazo. Pub/Sub executor limitado, política descartar tarefa excedente + registrar falha/counter (não bloquear loop I/O), reconexão backoff/jitter. Falhas observáveis via RedisListenerErrorHandler customizável e contadores.

## Riscos e interpretação do V3

V3 não fixa assinaturas avançadas nem parâmetros de namespace/limites: a matriz acima fecha esses contratos antes de código. ttl empty é deliberadamente ambíguo e documentado. JSON válido null externo é incompatível com Optional, portanto erro de serialização (escrita null proibida). Comparação CAS é por representação JSON, sem coerção; preserva TTL. Leituras de coleção completas falham se cardinalidade excede o limite, sem truncamento silencioso. Retenção Streams pode remover payload ainda pending; não se promete retenção infinita nem exactly-once. Sem modificação de módulos consumidores e sem dependência message-queue.

## Validação Onda 0

mvn --offline --batch-mode --no-transfer-progress -pl modules/platform-messaging,modules/platform-testing/platform-testing-core -am verify: BUILD SUCCESS, 3.801s, 14/10/2026 23:42:04. Log /private/tmp/platform-redis-wave0.log. Implementação somente após registrar esta matriz.

## Atualização da revisão final

O contrato de recuperação foi ampliado de forma compatível para incluir janela/cursor explícito e limitar trabalho por rodada. Ver [revisão de performance](REVISAO-PERFORMANCE.md), registrada antes da implementação do ajuste. A matriz original acima documenta as decisões iniciais da Onda 0.
