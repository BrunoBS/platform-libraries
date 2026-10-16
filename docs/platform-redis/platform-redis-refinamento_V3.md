# 16 — Platform Redis — Refinamento técnico V3

**Status:** aprovado para preparação do desenvolvimento (não implementado).
**Destino de implementação:** `BrunoBS/platform-libraries/modules/platform-redis`.
**Baseline:** Java 25, Spring Boot 4, Maven, Jackson 3.
**Objetivo:** biblioteca Redis genérica, plug and play, independente de Workspace, Configuration, Feature Toggle e platform-message-queue.

## 1. Princípios obrigatórios

- Uma dependência Maven e configuração mínima de conexão; todas as APIs passivas disponíveis por autoconfiguração.
- Inicialização sob demanda: somente listeners Pub/Sub ou Streams declarados iniciam consumidores ativos.
- Código legível por humanos; Clean Code, SOLID, composição, poucas interfaces coesas, nenhuma abstração especulativa.
- Não expor RedisTemplate/driver nos contratos simples.
- `RedisStore.set()` não publica eventos. `RedisPublisher.publish()` não grava dados.
- Redis Pub/Sub é transitório e sem ACK; Streams é persistente conforme retenção e suporta consumer groups/ACK. Não prometer exactly-once.
- Não criar dependência direta com Message Queue; reutilizar apenas conceitos de ergonomia. Reutilizar `platform-messaging` para o padrão existente de mensagens/exceções.

## 2. Organização obrigatória

```text
modules/platform-redis/
├── pom.xml
├── README.md
└── src/main/
    ├── java/br/com/portalmanager/platform/library/redis/
    │   ├── annotation/
    │   ├── configuration/
    │   ├── core/
    │   ├── operations/
    │   ├── pubsub/
    │   ├── stream/
    │   ├── serialization/
    │   └── internal/
    └── resources/
        ├── META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
        └── META-INF/platform-messages/
```

Pacotes são diretrizes de responsabilidade, não autorização para classes vazias ou hierarquias desnecessárias. A Onda 0 deve conferir o package base e o padrão real das demais libs antes de gerar código.

## 3. Contrato simples

```java
public interface RedisStore {
    <T> void set(String key, T value);
    <T> void set(String key, T value, Duration ttl);
    <T> Optional<T> get(String key, Class<T> type);
    <T> Optional<T> get(String key, TypeReference<T> type);
    boolean exists(String key);
    boolean delete(String key);
    Optional<Duration> ttl(String key);
    boolean expire(String key, Duration ttl);
    boolean persist(String key);
    <T> boolean setIfAbsent(String key, T value);
    <T> boolean setIfAbsent(String key, T value, Duration ttl);
    <T> RedisWrite<T> write(String key, T value);
}
public interface RedisWrite<T> {
    RedisWrite<T> ttl(Duration ttl);
    RedisWrite<T> ifAbsent();
    boolean execute();
}
```

- `RedisWrite` é builder por operação, de uso único; `execute()` retorna false somente se a condição NX não foi satisfeita; sucesso retorna true. Execução repetida gera erro padronizado.
- `AsyncRedisStore` espelha as operações diretas com `CompletionStage<Void>`, `CompletionStage<Boolean>` ou `CompletionStage<Optional<T>>`. Exigir I/O assíncrono real, não `supplyAsync` sobre chamada bloqueante.
- Ausência de chave resulta em `Optional.empty()`; falha de Redis, desserialização ou tipo errado gera erro padronizado, nunca empty.
- `null` não é aceito em escrita; para remoção usar `delete`. Sem TTL significa persistente; TTL zero/negativo é inválido. `set` sem TTL remove eventual expiração anterior. `setIfAbsent` é atômico.
- `ttl()` empty representa chave ausente **ou** persistente; documentar ambiguidade, ou criar consulta detalhada somente se justificada.

## 4. Contratos avançados

`RedisOperations` expõe interfaces segregadas: `hashes()`, `sets()`, `lists()`, `sortedSets()`, `atomic()`.

Matriz mínima de operações:
- Hash: put, get, getAll, remove, exists.
- Set: add, remove, contains, members, count.
- List: pushLeft, pushRight, popLeft, popRight, range, length, trim.
- Sorted Set: add, remove, score, incrementScore, range, rank, count.
- Atomic: increment, decrement, setIfAbsent, compareAndSet.

O Worker deve produzir **antes de implementar** uma matriz de assinaturas Java/retornos/comandos Redis na Onda 0. Evitar leituras ilimitadas e operações O(N) sem limites. `compareAndSet` não pode ser implementado como GET+SET separado; exigir operação atômica.

## 5. Pub/Sub e listeners

```java
redisPublisher.publish("configuration.changed", event);

@Component
@RedisSubscribe(channels = {"configuration.changed"})
class ConfigurationListener implements RedisMessageListener<ConfigurationEvent> {
    @Override public void onMessage(ConfigurationEvent event) { /* consumidor */ }
}
```

`RedisPublisher.publish(channel,payload)` retorna `long` (assinantes reportados pelo Redis; não é confirmação de processamento). `RedisMessageListener<T>.onMessage(T)` retorna void. A biblioteca descobre N listeners, canais e padrões, resolve tipo genérico, desserializa, inscreve e encerra automaticamente. Padrões de canais usam PSUBSCRIBE. Erros de handler são observáveis, sem retry garantido no Pub/Sub. Múltiplas instâncias inscritas recebem cópias; consumidores desconectados perdem mensagens.

Handlers executam fora do loop de I/O, com executor limitado, política explícita de saturação, reconexão com backoff/jitter e shutdown gracioso. Configuração declarativa alternativa é permitida, sem duplicar mecanismo interno.

## 6. Streams e notificações

`RedisStreams` expõe append, read, acknowledge e consumo por grupo; `@RedisStreamListener` habilita o consumidor. Especificar stream/group/consumer, concorrência, batch, bloqueio, início do grupo (0/$), recuperação de pending e retenção. ACK apenas após processamento bem-sucedido; falha deixa pending para recuperação. Sem DLQ ou exactly-once implícitos.

Keyspace Notifications são opcionais, requerem configuração do servidor e não devem ser habilitadas automaticamente em produção. Não confundir notificações nativas de chave com eventos de negócio explícitos.

## 7. Serialização e segurança

- Jackson 3 conforme versão real do parent; suporte `Class<T>` e `TypeReference<T>` da dependência efetiva.
- API simples armazena JSON UTF-8 em Redis String; operações avançadas usam tipos Redis nativos. Erro de tipo não converte silenciosamente.
- Não habilitar default typing/polimorfismo inseguro, nem aceitar nomes de classe arbitrários do payload.
- Namespace configurável, com modo explícito de compartilhamento entre microsserviços; evitar colisões e não aplicar prefixo silencioso em canais.
- TLS, credenciais e timeouts configuráveis; não logar segredo, payload ou valores sensíveis.
- Limites configuráveis para tamanho de chave/payload, batch, retornos, concorrência e filas; sem `KEYS`, `FLUSHDB` ou `FLUSHALL` na API pública.

## 8. Exceções e mensagens

Inspecionar os contratos reais do `platform-messaging` e **usar suas exceções/factories e bundles PT-BR/EN**. Não expor IllegalArgumentException, IllegalStateException, RuntimeException genérica ou hierarquia Redis própria como contrato. Mapear validação, indisponibilidade, timeout, autenticação, tipo incompatível, serialização, configuração e falha de listener para códigos/mensagens da plataforma. Preservar causa técnica sanitizada. Condição NX não satisfeita é resultado false, não exceção.

## 9. Spring Boot AutoConfiguration

Registrar APIs passivas com beans condicionais, sem sobrescrever customizações. Reutilizar infraestrutura de conexão. Nenhum subscriber ou polling Streams ativo se não houver listener declarado. Não exigir configuração de recurso avançado para simples leitura/escrita. Não adicionar automaticamente ao `platform-starter` sem decisão específica.

## 10. Testes e aceite

Testes unitários e integração real com Redis via Testcontainers, aproveitando `platform-testing` sem dependências circulares. Cobrir:
1. Bootstrap com dependência e propriedades, sem beans manuais.
2. DTO, lista, mapa, tipos parametrizados, ausência, null, JSON inválido e incompatibilidade de tipo.
3. TTL, overwrite, NX concorrente, builder fluido e reexecução.
4. Async I/O não bloqueante e falhas propagadas.
5. Hash/Set/List/Sorted Set/Atomic e limites de leitura.
6. Pub/Sub: N listeners, N canais, múltiplas instâncias, desconexão, reconexão, handler lento/falho e shutdown.
7. Streams: grupos, ACK, pending, claim/recovery, retenção, falha e reprocessamento.
8. Keyspace Notifications ligadas/desligadas.
9. TLS/autenticação, timeouts, mensagens PT-BR/EN e ausência de dados sensíveis em logs.
10. Maven reactor, BOM, documentação de pacote, README, CI green.

## 11. Plano de execução por ondas

- **Onda 0:** inspecionar parent/BOM, Jackson, messaging, exceptions, observability, testing e auto-configuração; apresentar matriz de assinaturas e incompatibilidades, sem inventar contratos.
- **Onda 1:** Maven, auto-configuração, Store, AsyncStore, serialização, API fluida e testes.
- **Onda 2:** operações avançadas, atomicidade e testes concorrentes.
- **Onda 3:** Pub/Sub, listeners, lifecycle, reconexão e testes.
- **Onda 4:** Streams, Keyspace Notifications opcionais e recuperação.
- **Onda 5:** segurança, performance, limpeza de dependências, README, CI e relatório.

Não alterar domínios consumidores. Não publicar pacote nem fazer merge automaticamente. Não criar classes sem responsabilidade comprovada. Cada onda deve compilar e passar testes. Reportar riscos e divergências antes de prosseguir.

**Documento:** V3 — fechamento dos contratos para preparação de implementação.
