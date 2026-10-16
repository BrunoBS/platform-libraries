# Platform Testing Redis

Fixture de Redis real via Testcontainers, com conexão dinâmica e isolamento por namespace. Compõe `platform-testing-core`, `platform-messaging`, Spring Data Redis e suporte de testes do Spring Boot; não depende de `platform-redis` nem das fixtures Database, Kafka, HTTP ou cloud.

## Dependência Maven

Importe `platform-libraries-bom` no `dependencyManagement` e declare a fixture em `test`:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform.library</groupId>
    <artifactId>platform-testing-redis</artifactId>
    <scope>test</scope>
</dependency>
```

A aplicação fornece seu starter Redis do Boot ou a capability `platform-redis`. Para testar HTTP, declare também `platform-testing-http`; Redis sem HTTP usa somente `@SpringBootTest` com a fixture.

## Fixture

```java
import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.application.name=product-api")
@WithRedis
class ProductRedisIT {

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private Environment environment;

    @Test
    void deveGravarEBuscarValor() {
        String key = environment.getRequiredProperty("platform.redis.namespace")
                + ".product-api:product:42";

        redis.opsForValue().set(key, "Notebook");

        assertThat(redis.opsForValue().get(key)).isEqualTo("Notebook");
    }
}
```

`@WithRedis` importa `RedisTestConfiguration`, registra `RedisTestExtension` e é descoberta pelo `RedisTestContextCustomizerFactory` via `spring.factories`. A conexão recebe host, porta mapeada e URL antes da criação dos beans. A service connection oficial do Boot aponta para o container mesmo quando existem propriedades de Cluster/Sentinel na aplicação. Não é necessário `@DynamicPropertySource` nem endpoint fixo. Sem anotação, a fixture não inicia container.

Com `platform-redis` na aplicação, injete `RedisStore` normalmente: `store.set("product:42", "Notebook")` e `store.get("product:42", String.class)` já aplicam o namespace. A fixture não ativa Spring Cache; para essa capability, configure `platform.redis.cache.enabled=true` explicitamente.

## Imagem

O default está em `RedisContainerImages.REDIS`, fixado por digest:

```text
redis@sha256:8b81dd37ff027bec4e516d41acfbe9fe2460070dc6d4a4570a2ac5b9d59df065
```

Override por tag explícita ou digest:

```java
@WithRedis(image = "redis:7.4.8-alpine")
```

`PinnedDockerImage` do Core valida a referência e rejeita tags flutuantes, como `latest`.

## Namespace, limpeza e ciclo de vida

Cada contexto de classe recebe um namespace exclusivo `test-...`, modo ISOLATED e seu container. O contexto pode ser reutilizado dentro da classe; o container é encerrado quando ele fecha.

Por padrão, antes de cada método, a extensão procura um `StringRedisTemplate` resolvido sem ambiguidade e remove somente chaves com prefixo `<namespace>.<spring.application.name>:` usando SCAN/DEL em batches. O serviço deve ter nome com letras, números, ponto, traço ou underscore. O exemplo compõe esse prefixo porque usa o template diretamente. Chaves fora do namespace permanecem. Não há FLUSHDB/FLUSHALL.

Para manter estado deliberadamente entre métodos:

```java
@WithRedis(cleanupBeforeEach = false)
```

Sem template resolvido sem ambiguidade, não há limpeza automática; testes apenas reativos ou com conexões próprias devem limpar seus dados entre métodos. Não sobrescreva o namespace/namespace-mode definidos pela fixture. Métodos que compartilham contexto e limpeza devem executar sequencialmente.

## Erros

| Código | Situação |
|---|---|
| `PLT-TST-REDIS-001` | Configuração ou imagem inválida |
| `PLT-TST-REDIS-002` | Falha ao iniciar o container |
| `PLT-TST-REDIS-003` | Falha na limpeza do namespace |

Falhas usam `PlatformConfigurationException` de Messaging e causas técnicas sanitizadas. Docker é necessário. Cenários específicos de TLS/autenticação com containers customizados continuam sob responsabilidade do teste.

## Executar

Na raiz do repositório:

```sh
mvn --batch-mode --no-transfer-progress -pl modules/platform-testing/platform-testing-redis -am verify
```

## Documentação relacionada

- [Seleção de artefatos](../README.md)
- [Guia completo — Redis](../docs/GUIA_DE_USO.md#16-redis)
- [Arquitetura](../docs/ARCHITECTURE.md)
- [Pacotes e classes](../docs/PACKAGES_AND_CLASSES.md#brcomportalmanagerplatformlibrarytestingredis)
