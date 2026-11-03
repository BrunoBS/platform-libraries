# platform-redis

Módulo Maven de Platform Libraries. Contratos, configuração e exemplos estão no
[guia oficial](https://github.com/BrunoBS/golden-platform/blob/docs/golden-platform-foundation/docs/libraries/modules/platform-redis.md).

- [POM e dependências](pom.xml)
- [Desenvolvimento e build do reactor](../../README.md)

## Namespace das chaves

O namespace padrão é o valor de `spring.application.name`. Portanto, uma chave lógica
`customer:42` será armazenada como `workspace-service:customer:42` quando o nome da
aplicação for `workspace-service`.

Para usar outro namespace em uma ou mais operações, crie uma visão leve com
`withNamespace`. O namespace explícito substitui o nome da aplicação; não cria outro
bean e não empilha prefixos:

```java
import java.time.Duration;

store.set("account-service.customer-limit", 42);
store.withNamespace("dynamic-parameter").set("account-service.customer-limit", 42);
store.withNamespace("dynamic-parameter").get("account-service.customer-limit", Integer.class);
store.withNamespace("dynamic-parameter").set(
        "account-service.customer-limit", 42, Duration.ofMinutes(5));
```

Sem `withNamespace`, as operações usam `spring.application.name`. O namespace
`dynamic-parameter` é uma escolha do consumidor; o `platform-redis` não reserva nem
interpreta nomes de namespace. A visão também pode ser usada para leitura, exclusão,
TTL, operações em lote e `scan`.

O ambiente não entra no nome da chave. Cada ambiente deve usar sua própria instância
ou implantação Redis. Toda a conexão é configurada pelo próprio módulo em
`platform.redis.connection.*`; o serviço consumidor não precisa configurar propriedades
de conexão do Spring Data Redis.

```yaml
platform:
  redis:
    connection:
      topology: STANDALONE # STANDALONE, SENTINEL ou CLUSTER
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      database: 0
      username: ${REDIS_USERNAME:}
      password: ${REDIS_PASSWORD:}
      command-timeout: 2s
      connect-timeout: 2s
      ssl:
        enabled: false # true para habilitar TLS
        # bundle: redis-server-trust # bundle TLS opcional da aplicação
    limits:
      max-key-bytes: 256
      max-payload-bytes: 1048576
      max-batch-size: 1000
      max-read-size: 1000
    listener:
      threads: 16
      queue-capacity: 256
      reconnect-min: 100ms
      reconnect-max: 5s
      keyspace-enabled: false
    lifecycle:
      shutdown-timeout: 5s
    streams:
      max-workers: 16
      max-length: 10000
```

`connection.topology` seleciona a conexão simples ou a configuração `cluster.nodes`
ou `sentinel.master`/`sentinel.nodes`. Para TLS com certificado confiável pelo bundle
da aplicação, informe `connection.ssl.bundle`. As demais propriedades ficam agrupadas
por responsabilidade: limites de operações, listeners, ciclo de vida e streams.
As propriedades de cache são um contexto próprio em `platform.redis.cache.*` (`enabled`,
`default-ttl`, `regions`, `failure-policy` e `lock-ttl`).

Exemplo de Cluster:

```yaml
platform:
  redis:
    connection:
      topology: CLUSTER
      cluster:
        nodes:
          - redis-1.internal:6379
          - redis-2.internal:6379
```

Exemplo de Sentinel:

```yaml
platform:
  redis:
    connection:
      topology: SENTINEL
      sentinel:
        master: primary
        nodes:
          - sentinel-1.internal:26379
          - sentinel-2.internal:26379
        database: 0
        username: ${REDIS_SENTINEL_USERNAME:}
        password: ${REDIS_SENTINEL_PASSWORD:}
```

## Desenvolvimento

Os testes de namespace verificam o prefixo padrão e a substituição por namespace
explícito; os testes de integração precisam de Docker por meio de
`platform-testing-redis`.
