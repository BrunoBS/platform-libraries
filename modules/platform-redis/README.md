# platform-redis

Módulo Maven de Platform Libraries. Contratos, configuração e exemplos estão no [guia oficial](https://github.com/BrunoBS/golden-platform/blob/docs/golden-platform-foundation/docs/libraries/modules/platform-redis.md).

- [POM e dependências](pom.xml)
- [Desenvolvimento e build do reactor](../../README.md)

Execute os comandos de build na raiz do repositório para incluir os módulos dependentes. Fixtures de integração exigem Docker; agregadores POM não são dependências JAR.

## Stores por ponto de injeção

`@RedisNamespace` seleciona um `RedisStore` síncrono sem alterar `platform.redis.*`:

```java
public RedisSessionRepository(
        @RedisNamespace("session") RedisStore store) {
    this.store = store;
}
```

Importe `br.com.portalmanager.platform.library.redis.annotation.RedisNamespace`.
O modo padrão da anotação é `ISOLATED`: `session.<spring.application.name>:<chave>`.
Para compartilhar uma publicação entre aplicações, use
`@RedisNamespace(value = "dynamic-parameter", mode = NamespaceMode.SHARED)`, importando
`PlatformRedisProperties.NamespaceMode`: `dynamic-parameter:<chave>`.
Não inclua o prefixo na chave lógica. Namespace não é autorização.

Construtores, parâmetros de métodos `@Bean` e campos `@Autowired` são suportados no
contexto Spring JVM. Sem anotação, o store padrão, qualifiers, `@Primary` e overrides
continuam com a resolução original. Cada par namespace/modo usa um store próprio
por contexto; todos reutilizam o template, conexão e codec existentes. Os limites
configurados continuam válidos. Redis desabilitado ou infraestrutura ausente não
produzem fallback para o store padrão. A anotação aceita somente `RedisStore`, não
`AsyncRedisStore`, operações, cache, pub/sub ou streams.

Encapsule acesso de negócio atrás de uma interface do consumidor, como
`SessionRepository`, implementada por `RedisSessionRepository`; o `SessionService`
depende dessa interface. Isso é recomendação para fronteiras de negócio, não uma
exigência de interface redundante para cada componente interno. O guia oficial
contém o exemplo completo e a estratégia de migração da configuração global.

A integração usa resolução dinâmica de dependências do Spring. Native Image/AOT
não foi validado e não é anunciado como suportado nesta entrega. Testes de namespace
estão em `RedisNamespaceTest` e `RedisNamespaceIT`, este com Redis real por
`platform-testing-redis`; a suíte existente continua cobrindo as demais operações.
