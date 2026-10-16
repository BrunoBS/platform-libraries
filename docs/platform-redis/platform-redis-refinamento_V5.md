# 16 — Platform Redis — Refinamento V5: setAll e getAll

**Status:** proposta para implementação pelo Worker.
**Repositório de implementação:** `BrunoBS/platform-libraries`, módulo `platform-redis`.
**Referência:** `platform-redis-refinamento_V4.md`. Preservar contratos e padrões existentes; conferir branch/código antes de alterar.

## Objetivo
Adicionar operações em lote para **chaves Redis independentes**, sem confundir com `HashOperations.getAll` (campos de uma única chave). Usar `MSET` e `MGET`, mantendo `RedisStore` como fachada pública e reutilizando serializers, tratamento de erros e autoconfiguração atuais.

## Contratos propostos
Adaptar à assinatura real do projeto na Onda 0:

```java
public interface RedisStore {
    <T> void setAll(Map<String, T> entries);
    <T> Map<String, T> getAll(Collection<String> keys, Class<T> type);
    <T> Map<String, T> getAll(Collection<String> keys, TypeReference<T> type);
}
```

**Nota de compatibilidade:** o `get` V4 retorna `Optional<T>`; `getAll` retorna apenas entradas encontradas. A implementação deve manter a correspondência entre chave e valor mesmo quando `MGET` devolve `null` para chaves ausentes. Caso os serializers não permitam misturar tipos num mesmo `Map`, documentar a restrição e usar tipos homogêneos por lote.

## Semântica
- `setAll`: `MSET` atômico dentro de um lote; sobrescreve valores e **remove TTL anterior** das chaves sobrescritas. Não atribui TTL.
- `getAll`: `MGET`, uma leitura de múltiplas chaves; omitir chaves inexistentes do mapa retornado. Não confundir ausência com erro de desserialização.
- Mapas/coleções vazios: retorno imediato, sem comando Redis. Argumento nulo, chave nula/vazia, valor nulo ou tipo nulo: exceção de validação no padrão da biblioteca.
- Duplicatas na coleção de chaves de leitura: deduplicar preservando a primeira ocorrência; resultado tem uma entrada por chave.
- `MGET` retorna valores na ordem das chaves solicitadas; reconstruir resultado por índice, **não** filtrar nulos antes do alinhamento.
- Erros de conexão, serialização e tipos incompatíveis seguem exceções existentes, sem retornar mapa vazio silenciosamente.
- Não implementar `setAll` com `for` + `SET`, nem `getAll` com `for` + `GET`.
- Em Redis Cluster, `MSET`/`MGET` com chaves de slots diferentes podem falhar com CROSSSLOT. Verificar suporte de deployment; não prometer operação única entre slots. Se necessário, definir estratégia explícita de hash tags por escopo ou tratamento de limitação, sem alterar silenciosamente a semântica atômica.

## Exemplos para documentação e testes

### Escrita e leitura de três chaves independentes
```java
Map<String, String> values = Map.of(
    "account:123:config:a", "true",
    "account:123:config:b", "100",
    "account:123:config:c", "enabled"
);
redisStore.setAll(values);

Map<String, String> found = redisStore.getAll(values.keySet(), String.class);
assertThat(found).containsAllEntriesOf(values);
assertThat(redisStore.get("account:123:config:a", String.class))
    .contains("true");
```

### Leitura parcial: chave ausente
```java
Map<String, String> found = redisStore.getAll(
    List.of("config:exists", "config:missing", "config:another"),
    String.class
);
// Se exists e another existirem, found terá somente essas duas entradas.
// A associação não pode deslocar valores ao ignorar o null do MGET.
```

### Sobrescrita e TTL
```java
redisStore.set("config:a", "old", Duration.ofMinutes(5));
redisStore.setAll(Map.of("config:a", "new", "config:b", "other"));
// config:a passa a conter "new" e deixa de expirar.
// Confirmar ausência de expiração via comando TTL no teste de integração.
```

### Tipos de objetos serializados
```java
record FeatureConfig(boolean enabled, int threshold) {}
redisStore.setAll(Map.of(
    "config:feature-a", new FeatureConfig(true, 10),
    "config:feature-b", new FeatureConfig(false, 20)
));
Map<String, FeatureConfig> result = redisStore.getAll(
    List.of("config:feature-a", "config:feature-b"), FeatureConfig.class
);
```

### Tipos parametrizados
```java
Map<String, List<String>> values = Map.of(
    "config:roles-a", List.of("ADMIN", "READER"),
    "config:roles-b", List.of("READER")
);
redisStore.setAll(values);
Map<String, List<String>> result = redisStore.getAll(
    values.keySet(), new TypeReference<List<String>>() {}
);
```
Conferir import real de `TypeReference` conforme Jackson 3 e assinatura atual.

### Mapas e coleções vazios
```java
redisStore.setAll(Map.of()); // no-op
assertThat(redisStore.getAll(List.of(), String.class)).isEmpty();
```

### Entradas inválidas
```java
// Validar: setAll(null), setAll(Map com chave "" ou valor null),
// getAll(null, String.class), getAll(List.of(""), String.class),
// getAll(List.of("a"), null).
// Usar exceções padronizadas do módulo, não NullPointerException acidental.
```

### Integração no Workspace Service: lotes de 500
**Esta política pertence ao serviço consumidor, não ao RedisStore.** Valor padrão `500`, configurável e positivo. Exemplo conceitual:

```java
int batchSize = 500; // configuração do Workspace Service
List<Map.Entry<String, String>> entries =
    new ArrayList<>(configurations.entrySet());

for (int start = 0; start < entries.size(); start += batchSize) {
    Map<String, String> batch = new LinkedHashMap<>();
    for (var entry : entries.subList(start, Math.min(start + batchSize, entries.size()))) {
        batch.put(entry.getKey(), entry.getValue());
    }
    redisStore.setAll(batch);
}
```

Para 1.200 itens: lotes de 500, 500 e 200. Aplicar o mesmo particionamento para `getAll` quando necessário. Cada `MSET` é atômico **somente no seu lote**; falha posterior não desfaz lotes anteriores. Reconciliar divergências pelo scheduler da feature Configuration. Não presumir atomicidade entre banco e Redis.

## Onda 0 — Inspeção obrigatória
1. Localizar `RedisStore`, `DefaultRedisStore`, serializers, `RedisOperations`, `AsyncRedisStore` e testes Redis reais.
2. Conferir se existe suporte prévio a batch e se a arquitetura exige espelhar métodos no `AsyncRedisStore`. Caso espelhe, usar I/O assíncrono real, conforme V4.
3. Verificar Spring Data Redis, Jackson 3, política de prefixos, tipos de exceção e Redis Cluster.
4. Confirmar a API final antes de alterar código; não criar `strings()` só para esses dois métodos.

## Implementação e testes
- Implementar via `opsForValue().multiSet(...)` e `multiGet(...)` ou equivalente da infraestrutura real.
- Preservar serialização idêntica a `set`/`get`.
- Testcontainers Redis seguindo o padrão `platform-testing` já existente; não introduzir infraestrutura paralela.
- Testar 1, 3, 500 e 501 chaves, valores longos, sobrescrita, TTL removido, ausência parcial, ordem desalinhada, tipos parametrizados, entradas inválidas e falhas de conexão.
- Se Redis Cluster for suportado, adicionar teste ou restrição documentada para CROSSSLOT.
- Atualizar README com exemplos de `setAll`, `getAll` e explicação Hash versus chaves independentes.
- Não modificar comportamento de `set`, `get`, `delete`, Hash, Pub/Sub ou Streams.

## Critérios de aceite
- Escrita com `MSET`, leitura com `MGET`, sem N chamadas sequenciais.
- Retorno corretamente associado às chaves e ausências omitidas.
- TTL e serialização testados.
- APIs anteriores compatíveis; documentação e testes de integração aprovados.
- Worker reporta decisões sobre assinatura, `AsyncRedisStore` e Redis Cluster.
