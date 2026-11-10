# platform-redis

Módulo Maven de Platform Libraries. Contratos, configuração e exemplos estão no
[guia oficial](https://github.com/BrunoBS/golden-platform/blob/main/docs/libraries/modules/platform-redis.md).

- [POM e dependências](pom.xml)
- [Desenvolvimento e build do reactor](../../README.md)

## Organização do módulo

Os tipos usados pelos serviços ficam em `api`; a configuração automática do Spring fica
em `configuration`; e detalhes de execução ficam em `internal`. O módulo não possui
`provider`, pois oferece uma única integração: Redis Standalone.

```text
redis/
├── api/
│   ├── configuration/  # propriedades públicas
│   ├── listener/       # tratamento de erros e métricas de listeners
│   ├── operations/     # operações Redis por tipo de dado
│   ├── pubsub/         # publicação, assinaturas e keyspace
│   ├── store/          # armazenamento JSON e páginas de scan
│   └── stream/         # consumo e recuperação de Streams
├── configuration/     # auto configurações Spring, incluindo cache
└── internal/          # adapters Redis, codecs e ciclo de vida
```

As propriedades de conexão e limites são `api.configuration.PlatformRedisProperties`;
as de cache são `api.configuration.PlatformRedisCacheProperties`. Os contratos mais
usados ficam em `api.store.RedisStore` e `api.operations.RedisOperations`.

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
de conexão do Spring Data Redis. O módulo suporta somente conexão Standalone por host e
porta; Redis Cluster e Sentinel não são suportados. Os consumidores de Streams e a recuperação de pendências requerem Redis 6.2 ou compatível. Essa é a versão mínima técnica; em produção, use uma versão ainda suportada pelo provedor.

A entrega dos Streams é pelo menos uma vez: handlers devem ser idempotentes. Falhas deixam registros pendentes para recuperação; a biblioteca não aplica limite automático de tentativas nem encaminha mensagens para uma DLQ. Dimensione a retenção e defina o tratamento operacional dessas mensagens no serviço.

```yaml
platform:
  redis:
    connection:
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

O endpoint configurado deve aceitar conexão Standalone. Para TLS com certificado
confiável pelo bundle da aplicação, informe `connection.ssl.bundle`. As demais
propriedades ficam agrupadas por responsabilidade: limites de operações, listeners,
ciclo de vida e streams.
As propriedades de cache são um contexto próprio em `platform.redis.cache.*` (`enabled`,
`default-ttl`, `regions`, `failure-policy` e `lock-ttl`).

## Desenvolvimento

Os testes de namespace verificam o prefixo padrão e a substituição por namespace
explícito; os testes de integração precisam de Docker por meio de
`platform-testing-redis`.
