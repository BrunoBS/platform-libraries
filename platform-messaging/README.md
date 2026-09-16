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

A biblioteca lê uma VIEW existente no banco do consumidor. Contrato:
`code`, `message_key`, `locale`, `message`, `solution`, `http_status`.

Redis é opcional e funciona como cache apenas das mensagens encontradas no repositório/view.
Mensagens padrão são resolvidas localmente pelo `PlatformDefaultMessageProvider`.

Fallback de locale: `en-US -> en -> default-locale -> pt-BR`.

## Resolução

```text
Redis
  ↓ miss
Banco / VIEW
  ↓ miss em todos os locales
PlatformDefaultMessageProvider
  ↓
bundles dos módulos no classpath
```

Existe uma única implementação oficial de `ApiMessageProvider`: `PlatformDefaultMessageProvider`.

Cada módulo que precisa publicar mensagens padrão adiciona seus próprios bundles seguindo a convenção:

```text
src/main/resources/META-INF/platform-messages/<module>_pt_BR.properties
src/main/resources/META-INF/platform-messages/<module>_en.properties
```

Exemplos:

```text
platform-messaging    -> platform_pt_BR.properties
platform-authorization -> authorization_pt_BR.properties
platform-catalog       -> catalog_pt_BR.properties
```

O provider descobre automaticamente todos os arquivos compatíveis presentes no classpath. Portanto, o módulo de messaging não precisa conhecer os nomes dos módulos consumidores.

Cada entrada segue o contrato:

```text
messageKey=code|httpStatus|message|solution
```

Exemplo:

```properties
validation.id.required=VALIDATION-0002|400|O identificador é obrigatório.|Informe um identificador válido.
```

O caractere `|` é reservado como delimitador.

## Redis

O Redis armazena apenas mensagens originadas do banco:

```text
platform:message:{messageKey}:{locale}
```

Mensagens dos bundles não são armazenadas no Redis.

## Configuração

```yaml
platform:
  messaging:
    enabled: true
    default-locale: "pt-BR"
```


## Validação de unicidade no startup

Ao criar o `PlatformDefaultMessageProvider`, todos os bundles em
`META-INF/platform-messages/*.properties` são carregados e indexados.

A combinação abaixo deve ser única:

```text
locale + messageKey
```

Se dois módulos publicarem a mesma chave para o mesmo locale, a aplicação falha
durante a inicialização com uma mensagem indicando a chave, o locale e os dois
bundles conflitantes.

Exemplo inválido:

```text
audit_pt_BR.properties -> event.not.found
crud_pt_BR.properties  -> event.not.found
```

A mesma chave em idiomas diferentes é permitida:

```text
audit_pt_BR.properties -> audit.event.not.found
audit_en.properties    -> audit.event.not.found
```

Por convenção, cada módulo deve usar seu próprio namespace, por exemplo
`audit.*`, `crud.*`, `routing.*`, `authorization.*` e `catalog.*`.
