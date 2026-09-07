# platform-messaging

Biblioteca Spring Boot para centralizar mensagens e erros de APIs.

Uso:
```java
throw new ApiException("user.not.found");
```

Com parâmetros:
```java
throw new ApiException("user.not.found", Map.of("userId", userId));
```

A aplicação usa `messageKey`; o código técnico (`ERR-0001`) fica no catálogo.

A biblioteca lê somente uma VIEW existente no banco do consumidor. Contrato:
`code`, `message_key`, `locale`, `message`, `solution`, `http_status`.

Redis é opcional e somente cache. Sem Redis, consulta a VIEW diretamente.

Fallback de locale: `en-US -> en -> pt-BR`.

A biblioteca não possui migrations e não inclui driver MySQL; usa o DataSource/JdbcTemplate do consumidor.

Java 21.
