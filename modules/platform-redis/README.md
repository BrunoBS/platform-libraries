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
ou implantação Redis. Host, porta e credenciais são configurados pelas propriedades
de conexão do Spring Boot em `spring.data.redis.*`; `platform.redis.*` contém os limites
e opções próprios do módulo.

## Desenvolvimento

Os testes de namespace verificam o prefixo padrão e a substituição por namespace
explícito; os testes de integração precisam de Docker por meio de
`platform-testing-redis`.
