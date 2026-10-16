# Platform Redis V5 — setAll e getAll

## Onda 0

Refinamento V5 lido integralmente: [referência local](platform-redis-refinamento_V5.md). Branch atual feature/platform-redis-v3, preservando todo o desenvolvimento V3/V4 e as alterações locais. Sem alterações no Workspace/Account Service, instalação ou publicação de artefatos, commit ou merge.

Contratos reais: RedisStore e AsyncRedisStore retornam Optional nas leituras individuais; TypeReference é tools.jackson.core.type.TypeReference. DefaultRedisStore usa StringRedisTemplate; DefaultAsyncRedisStore usa ReactiveStringRedisTemplate com toFuture e RedisErrors.translate. RedisOperations contém estruturas nativas, mas não havia batch de chaves independentes. Hash.getAll continua sendo leitura de campos de uma única chave.

RedisJsonCodec aplica o mapper Jackson 3 seguro e limites por valor; RedisAccess aplica namespace/validação/limites. Preservados. Spring Data Redis 4.1.1 e Lettuce existentes; nenhuma dependência nova. Async espelha as três assinaturas propostas com CompletionStage e I/O reativo real, sem supplyAsync/threads bloqueantes. APIs anteriores não foram alteradas.

## Implementação

- RedisStore: setAll(Map), getAll(Collection,Class), getAll(Collection,TypeReference).
- AsyncRedisStore: mesmos métodos via CompletionStage; validação/serialização antecipadas, falhas no stage.
- Default stores usam multiSet/multiGet: uma chamada MSET/MGET por lote, sem particionamento ou loops de SET/GET.
- RedisBatches é auxiliar interno de preparação/limites/alinhamento compartilhado, sem transporte e sem tipos de serviços consumidores.
- Escrita integralmente validada/serializada antes do comando, persistente e removendo TTL anterior.
- Deduplicação preserva primeira ocorrência, nil não desloca valores; resultado contém somente encontrados e não é modificável. JSON inválido/incompatível não é convertido em ausência.
- Mapas/coleções vazios não fazem I/O; tipo nulo é inválido mesmo em leitura vazia.
- Limites existentes aplicados; nenhum tamanho fixo 500 na lib. Esse particionamento pertence ao consumidor.
- Cluster Lettuce exige o mesmo hash slot e falha antes do envio para não depender do fan-out do driver. Hash tags de escopo são explícitas. Standalone aceita chaves em slots distintos. Factories customizadas sem metadados Lettuce também exigem mesmo slot por segurança. Restrição documentada; não foi criado cluster de Testcontainers.

## Validação

- Build inicial `mvn --offline --batch-mode --no-transfer-progress -pl modules/platform-redis -am verify`: aprovado, incluindo os 10 primeiros testes novos e a regressão existente. Log: /private/tmp/platform-redis-v5-verify.log.
- Build final `mvn --offline --batch-mode --no-transfer-progress -pl modules/platform-redis -am clean verify`: **167 testes aprovados** nos cinco projetos do reactor selecionado, zero falhas/erros/skips. Duração: 1min23s. Log: /private/tmp/platform-redis-v5-final.log.
- Módulo platform-redis: **65 testes** (9 unitários/arquiteturais e 56 integrações), incluindo **11 novos casos V5** (4 unitários e 7 integrações). Foram preservados os testes de Store individual, JSON/segurança, Hash e outras estruturas, Pub/Sub, Streams, keyspace e Spring Cache.
- Redis real via RedisIntegrationSupport/@WithRedis/platform-testing-redis e Testcontainers existentes. Sem infraestrutura paralela.
- Casos V5: 1/3/500/501 chaves; ausência antes/no meio/depois, todas ausentes, duplicatas e ordem; registros e TypeReference; valores longos; sobrescrita e TTL bruto -1; rejeição antes de escrever; limites de batch/leitura/payload; JSON inválido/null/tipo incompatível; namespace; falhas reais de conexão; no-op vazio; uma chamada multiSet/multiGet; async incompleto até a resposta reativa; recusa de slots diferentes e aceitação de hash tags comuns; factory customizada conservadora.
- O teste adicional de factory customizada detectou que uma substituição textual não havia aplicado a condição conservadora. A condição foi corrigida e o build limpo final passou integralmente.
- Links locais dos documentos modificados e git diff --check aprovados.

## Limitações e próximos passos

A integração real de Redis é standalone. Cluster tem restrição explícita de mesmo slot e testes da guarda, sem promessa de operação atômica entre slots e sem cluster provisionado para esta tarefa. Factories customizadas sem metadados Lettuce exigem o mesmo slot de forma conservadora.

MGET segue o protocolo Redis: nil também ocorre para chaves que não são String; elas são omitidas. Strings JSON inválidas/incompatíveis geram erro. Payloads MGET são limitados no codec após recebimento, sem introduzir Lua ou comandos de verificação por chave.

Workspace/Account Service foi preservado. A adoção de setAll/getAll e do particionamento configurável em 500 pertence a uma próxima alteração do consumidor. Não há mudanças de negócio na biblioteca, instalação Maven local, publicação de artefatos, commit, push ou merge nesta entrega.
