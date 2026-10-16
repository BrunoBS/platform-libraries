# Platform Redis V4 — Onda 6 e Platform Testing Redis

Branch: `feature/platform-redis-v3`. Baseline V3 preservada; alterações locais disponíveis para revisão, sem commit, push, merge ou publicação. Documento V4 lido integralmente e preservado em `platform-redis-refinamento_V4.md`. Decisões anteriores à implementação em `ONDA-6-PLANO.md`.

## Funcionalidades

- Novo JAR independente `platform-testing-redis`, registrado no agregador/BOM e no inventário de publicação. `@WithRedis`, imagem versionada/pinned, GenericContainer, configuração dinâmica anterior às conexões Boot e @ServiceConnection nativo para ignorar Cluster/Sentinel herdados, lifecycle e namespace exclusivo por contexto de classe. Limpeza opcional antes de cada método por SCAN/DEL limitada ao namespace, sem FLUSH global. Fixture depende de Core/Messaging/Spring/Testcontainers, sem capability Redis nem banco, HTTP, Kafka ou cloud. Testes V3 reutilizam a fixture.
- Spring Cache opt-in por `platform.redis.cache.enabled=true`, sem necessidade de `@EnableCaching` manual. Anotações oficiais `@Cacheable`, `@CachePut`, `@CacheEvict`, `@Caching`; CacheManager da aplicação tem precedência. Não entra automaticamente no starter.
- Regiões dinâmicas, default TTL 30m e overrides por região. TTL positivo, namespace/serviço/ambiente/região isolados em dados e locks. Ambiente explícito ou profiles ativos/default.
- JSON Jackson 3 seguro com reconstrução de DTO, arrays e coleções/genéricos pela assinatura confiável do método. Sem nome de classe no payload/default typing. Uma região por tipo resolvido; conflitos falham na configuração. Null, Optional.empty e exceções não são armazenados.
- TransactionAware para put/evict/clear após commit, autoconfiguração após TransactionAutoConfiguration para observar rollbackOnly, preservação em rollback, política FAIL_FAST default e FAIL_OPEN_READ apenas para falhas técnicas de leitura/população de @Cacheable. Escritas explícitas e erros do método/repositório permanecem visíveis.
- `sync=true` com locking nativo Redis fora de transação, lease configurável e isolamento entre serviços/ambientes. Sem motor ou anotação de cache próprios. Mensagens adicionais PLT-REDIS-012/013 nos bundles PT-BR/EN e erros de fixture padronizados via Messaging.
- READMEs, guias, mapa de pacotes, arquitetura, BOM e inventário dos workflows atualizados. Workflow verify já executa o reactor inteiro, incluindo os novos módulos.

## Decisões concretas de implementação

Interceptação é do Spring; armazenamento, TTL, SCAN e locking são do Spring Data Redis. Extensões de RedisCacheManager/RedisCache limitam-se à serialização por região, null e política de erro. CacheReturnTypes registra tipos declarados, sem AOP próprio. ScopedRedisCacheWriter delega os comandos e isola o nome usado internamente pelos locks nativos, que de outra forma seria apenas o nome lógico da região.

O writer do Spring Data Redis 4.1 usa escritas assíncronas por default. Foi configurado `immediateWrites(true)` para CachePut não retornar antes da escrita e para falhas não desaparecerem em um future ignorado. O loader nativo de sync escreve o resultado imediatamente; um marcador privado aborta SET para null, preservando o desbloqueio nativo. Não há exceção Redis pública própria.

## Aceite e testes

| Critério V4 | Evidência |
| --- | --- |
| Cache desligado e manager customizado | RedisCacheIT: ausência de manager/interceptação quando desabilitado; precedência de ConcurrentMapCacheManager |
| Bootstrap plug and play | RedisCacheBootstrapIT: EnableAutoConfiguration + WithRedis + propriedade enabled, sem manager/resolver/EnableCaching manual |
| Regiões dinâmicas, TTL e overrides | RedisCacheIT: messages/unknown, PTTL real e expiração; configuração inválida falha |
| Hit/miss, Put/Evict/Caching/clear | RedisCacheIT com Redis real |
| Null/exceções e DTO/genéricos | List, Map<String,List<DTO>>, Optional, null, sync-null; não armazena falhas |
| Transações | TransactionTemplate com PlatformTransactionManager real do Spring: commit, rollback de put/evict e rejeição de sync em transação; bootstrap adicional com @Transactional e rollbackOnly |
| Isolamento e segurança | Namespace, serviço, ambiente/profiles, região, tenant/usuário/locale; JSON inválido e metadado @class externo sem resolução arbitrária |
| Indisponibilidade | Fail-fast e fail-open de leitura, falha de método preservada, Put/Evict continuam falhando |
| Stampede e locks | Concorrência sync: um loader; regiões homônimas de serviços distintos não compartilham lock |
| Fixture | Imagem configurável/validação, ausência da anotação, namespaces distintos, host/porta comparados ao container, propriedades Cluster/Sentinel conflitantes ignoradas, PING real e limpeza que preserva outro serviço |
| Regressão V3 | Store/Async/NX/CAS/estruturas, Pub/Sub/reconexão, Streams/ACK/pending/recovery, Keyspace, TLS/autenticação e mensagens PT-BR/EN |

Build completo: `mvn --offline --batch-mode --no-transfer-progress clean verify`, **23 módulos / 565 testes / zero falhas, erros, skips ou flakes**, 5min21, concluído em 15/10/2026 às 08:09:16. `platform-redis`: 51 testes (5 unitários/arquiteturais + 46 IT); fixture: 4 (2 unitários + 2 IT). Redis real via Testcontainers, imagem fixada por digest. O reactor também validou os demais módulos e suas integrações MySQL/Kafka/AWS/Azure. Em seguida, o ajuste final de ServiceConnection foi verificado novamente em todos os módulos afetados (Redis e fixture, com suas dependências): **153 testes, zero falhas/erros/skips**, 42.139s, concluído às 08:14:43.

Evidências locais:

- `/private/tmp/platform-redis-v4-reactor.log` e `/private/tmp/platform-redis-v4-reactor-results.json` — reactor completo.
- `/private/tmp/platform-redis-v4-verify.log` — verificação focada anterior completa de Redis + dependências.
- `/private/tmp/platform-redis-v4-testing.log` — fixture e dependências.
- `/private/tmp/platform-redis-v4-cache-focused.log` — cenários de cache durante implementação.
- `/private/tmp/platform-redis-v4-transaction-bootstrap.log` — reprodução transacional corrigida, antes do novo reactor completo.
- `/private/tmp/platform-redis-v4-final-module.log` — regressão posterior dos módulos afetados, incluindo @ServiceConnection e propriedades Cluster/Sentinel conflitantes.
- `python3 scripts/verify-library-namespace.py`, conferência adicional dos pacotes aninhados da fixture, `git diff --check` e metadata Flatten sem revision pendente aprovados.

## Problemas observados

O teste adicional de bootstrap reproduziu gravação após rollbackOnly sem exceção: o interceptor de cache estava fora da transação. Foi corrigido ordenando a autoconfiguração após TransactionAutoConfiguration, sem criar advisor próprio. O caso passou após a correção e foi ampliado para CachePut/CacheEvict com rollbackOnly; todos passaram no novo reactor completo. Evidência da reprodução: /private/tmp/platform-redis-v4-transaction-bootstrap-before-fix.log.

Foram corrigidos os efeitos do writer assíncrono padrão, escrita de null no loader sync, locks nativos sem namespace, URL vazia inválida no Boot e necessidade de disponibilizar propriedades de conexão antes da criação dos beans.

Execuções intermediárias tiveram timeout em operações e falha de conexão no início da limpeza da fixture. As execuções subsequentes passaram sem retry automático; foi acrescentada comparação explícita de host/porta com o container e PING, além da prova de limpeza isolada. Não há evidência suficiente para atribuir a causa dessas ocorrências ao código ou ao Docker. O reactor final passou integralmente. Docker Desktop também apresentou espera no credential helper antes de iniciar Ryuk; houve fallback normal, sem modificar credenciais.

## Limitações, divergências e pendências

- Não há requisito obrigatório da V4 deliberadamente omitido. Políticas não fixadas pelo refinamento foram decididas no plano. Um tipo por região evita metadados de classe não confiáveis; retorno Object/void/não resolvido, Future/CompletionStage e Publisher não são aceitos no cache declarativo gerenciado.
- sync=true em transação ativa é recusado explicitamente (PLT-REDIS-013). PutIfAbsent/evictIfPresent/beforeInvocation são imediatos, conforme Spring. Se a aplicação customizar a ordem dos advisors, deve manter a transação envolvendo o interceptor de cache: somente transações ativas são observáveis pelo decorator. Falha de Redis após commit não desfaz banco confirmado. Não há transação distribuída.
- Lock nativo é por região e expira; não há renovação ou exactly-once. Chaves diferentes podem ser serializadas; dimensionar lock-ttl acima do tempo do loader. Não há cache local nem invalidação Pub/Sub implícita.
- A aplicação define chaves contendo tenant, locale, identidade/permissões e filtros pertinentes; self-invocation segue a limitação do proxy Spring. Seleção explícita de manager usa a visão estrita; o resolver padrão aplica FAIL_OPEN_READ a Cacheable.
- CI remoto permanece pendente de envio da branch e execução do GitHub Actions; não foi disparado push, workflow de publicação/reset, merge ou deploy.
- Account Service/Configuration continuam pausados e preservados: SHA-256 de 784 arquivos sem alterações. Não houve migração do Messaging nem mudança em serviços consumidores.
- Artefatos alheios `.flattened-pom 2.xml` em platform-messaging e platform-catalog foram preservados e não integram a feature.

SHA-256 do refinamento V4: `703a85a8d0b38d1d0d188693e814e875c3f2a86b7c6d5055d1ff627f4f9ff77d`.
