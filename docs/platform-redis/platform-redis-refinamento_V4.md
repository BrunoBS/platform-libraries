# 16 — Platform Redis — Refinamento técnico V4

**Status:** refinamento evolutivo; integração Spring Cache planejada após conclusão das ondas V3.
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

**Documento:** V4 — preserva integralmente os requisitos da V3 e adiciona o refinamento da integração Spring Cache.


---

## 12. Evolução V4 — Integração opcional com Spring Cache

### 12.1 Objetivo e limites

Oferecer cache declarativo de métodos via anotações oficiais Spring Cache, sem reinventar interceptação AOP nem introduzir `@PlatformCacheable` ou anotações proprietárias. A integração será opcional, sem alterar os contratos de `RedisStore`, `RedisPublisher` e `RedisStreams`, nem adicionar dependência de domínio. **Não faz parte das ondas V3 em andamento**: implementar como **Onda 6**, após validação das ondas anteriores.

### 12.2 Experiência do consumidor

Com a dependência da biblioteca e suporte de cache habilitado:

```yaml
platform:
  redis:
    cache:
      enabled: true
      default-ttl: 30m
```

O consumidor utiliza a anotação Spring, sem cadastrar previamente a região:

```java
@Cacheable(cacheNames = "messages", key = "#messageKey")
public MessageOutput findMessage(String messageKey) {
    return repository.findMessage(messageKey);
}
```

O `CacheManager` resolve dinamicamente `messages`, aplica TTL padrão de 30 minutos e armazena somente quando houver cache miss. A região é lógica, não um recurso que precise ser criado antecipadamente no Redis. Outra anotação com `cacheNames = "accounts"` também funciona automaticamente.

Sobrescrita opcional por região:

```yaml
platform:
  redis:
    cache:
      enabled: true
      default-ttl: 30m
      regions:
        messages:
          ttl: 1h
        accounts:
          ttl: 15m
```

`messages` expira em 1 hora; `accounts` em 15 minutos; quaisquer outras regiões em 30 minutos. **O TTL é definido por região, não por anotação individual.** O nome de região vem de `cacheNames`; a anotação não cria uma configuração YAML.

### 12.3 Contrato Spring e autoconfiguração

- Utilizar `org.springframework.cache.CacheManager` e preferencialmente `RedisCacheManager` do Spring Data Redis, aproveitando a infraestrutura de conexão existente.
- Ativar o suporte às anotações `@Cacheable`, `@CachePut`, `@CacheEvict` e `@Caching` **somente** quando `platform.redis.cache.enabled=true`. Garantir que a configuração de cache Spring esteja efetivamente ativa (equivalente a `@EnableCaching`) sem exigir configuração manual do consumidor.
- Registrar beans de forma condicional e respeitar `CacheManager` fornecido pela aplicação. Se já existir um `CacheManager`, não substituí-lo silenciosamente: documentar precedência, conflito e a possibilidade de selecionar um gerenciador pelo atributo `cacheManager` da anotação.
- Permitir criação dinâmica de regiões desconhecidas; aplicar o TTL default. Para regiões declaradas, aplicar overrides. Usar namespace/prefixo de chave previsível e isolado por serviço/ambiente.
- Configuração desabilitada não ativa interceptação de cache, não exige infraestrutura extra e não altera a API RedisStore.
- Não implementar um segundo motor de cache; delegar ao Spring a interceptação e ao Spring Data Redis o armazenamento.
- Garantir compatibilidade de serialização com Java 25/Spring Boot 4/Jackson 3 real do BOM. Não habilitar desserialização polimórfica insegura ou gravar classes arbitrárias em payload. Validar a capacidade de reconstruir o tipo de retorno sem depender de metadados de classe não confiáveis.
- Não cachear `null` por padrão. Não cachear exceções. TTL deve ser estritamente positivo; valor inválido falha com mensagem padronizada durante configuração.

### 12.4 Semântica de consulta e atualização

```java
@Cacheable(cacheNames = "accounts", key = "#identifier")
public AccountOutput findByIdentifier(UUID identifier) {
    return repository.findByIdentifier(identifier);
}

@CacheEvict(cacheNames = "accounts", key = "#identifier")
public void update(UUID identifier, AccountRequest request) {
    repository.update(identifier, request);
}
```

- `@Cacheable`: hit não executa método; miss executa e armazena retorno.
- `@CachePut`: executa método e grava retorno.
- `@CacheEvict`: invalida entrada. Em métodos transacionais, alinhar invalidação ao commit bem-sucedido para evitar cache stale; estudar `TransactionAwareCacheManagerProxy`/configuração transacional compatível e documentar limitações.
- Chamadas internas ao próprio objeto (`this.metodo()`) não passam pelo proxy Spring; documentar isso.
- Chaves de cache devem incluir todos os fatores que afetam o resultado: tenant/workspace, locale, parâmetros, filtros e, quando necessário, identidade/permissões. Não reutilizar resultados filtrados por autorização entre usuários.
- `findAll` com paginação ou autorização não deve ser cacheado indiscriminadamente. A biblioteca não adivinha dependências de autorização.
- Cache entre instâncias usa Redis compartilhado; `@CacheEvict` remove a entrada Redis. Não presumir cache local adicional nem invalidar outros caches por Pub/Sub automaticamente.
- Falha do Redis: política explícita e configurável de `fail-fast` versus `fail-open` para cache de leitura; nunca ocultar falhas de banco nem transformar erros de escrita em sucesso silencioso. Padrão conservador documentado após inspeção da política da plataforma.
- Evitar cache stampede quando múltiplas requisições simultâneas causarem miss; documentar limites de `sync=true` conforme suporte real do gerenciador.

### 12.5 Estrutura de pacotes

Adicionar responsabilidade `cache/` sob o pacote base de `platform-redis`, somente com classes necessárias para propriedades, autoconfiguração e políticas de serialização. Não replicar `CacheManager`, `Cache` ou anotações nativas. A Onda 6 valida os nomes concretos e mantém o padrão do módulo.

### 12.6 Critérios de aceite da Onda 6

1. Com `cache.enabled=false`, nenhum suporte de cache é ativado pela biblioteca.
2. Com `cache.enabled=true`, `@Cacheable("messages")` funciona sem declarar `regions.messages`.
3. Região não declarada utiliza `default-ttl`; região declarada usa override.
4. Duas regiões não colidem; isolamento por namespace/ambiente validado.
5. Hit, miss, `@CachePut`, `@CacheEvict`, `@Caching`, expiração e não cachear null são testados com Redis Testcontainers.
6. Cache de método transacional não mantém resultado inválido após rollback.
7. Tipos DTO/coleções e serialização segura são testados; falhas têm mensagens padronizadas via `platform-messaging`.
8. Cenários multiusuário/multitenant e locale são testados para prevenir vazamento.
9. Auto-configuração não sobrescreve `CacheManager` customizado; comportamento em conflito é documentado.
10. README inclui exemplo mínimo e override por região; builds e pipelines verdes.

### 12.7 Relação com Platform Messaging

O `platform-messaging` já documenta cache Redis opcional para mensagens vindas da VIEW, com fallback local e TTL. **A V4 não determina migração automática do Messaging para Spring Cache.** Avaliar separadamente eventual reutilização da infraestrutura `platform-redis`, sem alterar sua semântica de fallback, seus códigos de erro ou suas dependências de forma circular.

### 12.8 Ordem de implementação

**Não interromper o Worker em execução nas Ondas 0–5 da V3.** Após concluir, validar, documentar e estabilizar essas ondas, encaminhar a V4 para implementar exclusivamente a Onda 6, em branch e escopo acordados. Nenhum merge, publicação de artefatos ou mudança nos serviços consumidores sem autorização.


---

## 13. Evolução V4 — Platform Testing Redis (padrão existente verificado)

### 13.1 Evidências da implementação existente

A estrutura da branch `main` de `BrunoBS/platform-libraries/modules/platform-testing` foi inspecionada antes deste refinamento:

- `platform-testing/pom.xml` é um agregador Maven (`packaging=pom`), sem código Java, e lista explicitamente os submódulos.
- `platform-testing/README.md` exige artefatos consumíveis independentes, usados em `test`, com `platform-testing-core` compartilhado.
- `platform-testing-database` usa `@WithMySql`, `MySqlTestConfiguration`, `MySqlTestContextCustomizerFactory`, `MySqlContainerImages` e registro `META-INF/spring.factories`.
- `platform-testing-kafka` usa `@WithKafka`, `KafkaTestConfiguration`, `KafkaTestContextCustomizerFactory`, `KafkaContainerImages` e registro `META-INF/spring.factories`.
- O padrão atual valida imagens Docker versionadas via `PinnedDockerImage` do Core e traduz configuração inválida com `PlatformConfigurationException` de `platform-messaging`.
- O POM de `platform-testing-database` depende de `platform-testing-core`, `spring-boot-testcontainers`, Testcontainers específico, `platform-messaging` e mantém dependências de teste próprias com escopo `test`.

**Decisão:** criar `modules/platform-testing/platform-testing-redis` como JAR separado, adicionar o módulo ao agregador e incluir o artefato no BOM/publicação, seguindo rigorosamente os padrões reais de package, registro Spring e dependências mínimas. Não adicionar Redis Testcontainers diretamente ao `platform-testing-core`.

### 13.2 Contrato proposto para o consumidor

```java
@SpringBootTest
@WithRedis
class RedisStoreIntegrationTest {
    @Autowired RedisStore redisStore;

    @Test
    void shouldStoreAndRetrieve() {
        redisStore.set("test:key", "hello");
        assertEquals("hello", redisStore.get("test:key", String.class).orElseThrow());
    }
}
```

- Nome **`@WithRedis`** em vez de `@PlatformRedisTest`, para corresponder a `@WithMySql` e `@WithKafka`.
- A anotação importa `RedisTestConfiguration`, com `image()` configurável e valor padrão versionado em `RedisContainerImages`.
- `RedisTestContextCustomizerFactory` descobre a anotação, valida imagem versionada usando `PinnedDockerImage` e registra a configuração antes do refresh do contexto.
- Utilizar `GenericContainer` com porta 6379 e Testcontainers, salvo se o BOM disponibilizar integração oficial mais adequada. A conexão Spring deve receber host/porta dinâmicos antes da criação dos beans; verificar propriedades exatas do Spring Boot 4 e propriedades específicas de `platform-redis`.
- Garantir isolamento de dados entre testes (namespace exclusivo e/ou limpeza limitada ao namespace de teste), sem `FLUSHALL` indiscriminado e sem impacto em outros contextos.
- Não iniciar container quando a anotação não estiver presente. Reuso de container/contexto apenas quando seguro e compatível com os módulos existentes.
- Reutilizar mensagens e exceções padronizadas de `platform-messaging`, incluindo validação da imagem, falha de inicialização e configuração inválida.
- Manter `platform-testing-redis` independente de `platform-testing-database`, Kafka, HTTP, AWS e Azure; não introduzir dependência circular com `platform-redis`.

### 13.3 Estrutura orientativa (ajustar após inspeção de código)

```text
modules/platform-testing/
├── pom.xml                         # adicionar <module>platform-testing-redis</module>
└── platform-testing-redis/
    ├── pom.xml
    └── src/main/
        ├── java/br/com/portalmanager/platform/library/testing/redis/
        │   ├── annotation/WithRedis.java
        │   ├── RedisContainerImages.java
        │   ├── RedisTestConfiguration.java
        │   ├── RedisTestContextCustomizerFactory.java
        │   └── RedisTestingTechnicalErrors.java
        └── resources/META-INF/spring.factories
```

Classes auxiliares somente se necessárias e alinhadas ao padrão efetivo do repositório. Atualizar README do agregador, guias de uso, mapa de pacotes, BOM e pipelines pertinentes.

### 13.4 Cobertura de testes

Usar `@WithRedis` para testar `RedisStore`, `AsyncRedisStore`, TTL, NX, concorrência, operações atômicas, Pub/Sub, reconexão, Streams, serialização e, na Onda 6, `@Cacheable`, `@CachePut`, `@CacheEvict`, regiões dinâmicas e TTL padrão/específico. Incluir testes de bootstrap, imagem configurável, isolamento, indisponibilidade e ausência de dependências de produção indevidas.

### 13.5 Ordem de execução

A infraestrutura `platform-testing-redis` é **pré-requisito recomendado para a Onda 6**, e também pode ser aproveitada para testes das ondas V3 já concluídas. Não interromper o Worker nem reabrir ondas em execução sem autorização. Antes de implementar, comparar com o código vigente na branch de trabalho, pois a inspeção acima se refere à `main`.
