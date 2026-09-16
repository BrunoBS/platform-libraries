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
Mensagens padrão da biblioteca são resolvidas localmente via Spring `MessageSource`.

Fallback de locale: `en-US -> en -> pt-BR`.

A biblioteca não possui migrations e não inclui driver MySQL; usa o DataSource/JdbcTemplate do consumidor.

Java 21.

# ✉️ Módulo Platform Messaging (`platform-messaging`)

O **`platform-messaging`** é o módulo fundacional de governança de mensagens, tratamento de exceções e internacionalização das APIs da plataforma.

## 🚀 Como ativar

```xml
<dependency>
    <groupId>com.empresa.platform</groupId>
    <artifactId>platform-messaging</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 🛠️ Configuração

```yaml
platform:
  messaging:
    enabled: true
    default-locale: "pt-BR"
```

## 🌍 Resolução de mensagens

A resolução segue duas etapas. Primeiro são procurados overrides externos em todos os candidatos de locale; somente depois são consultados os providers locais:

```text
Redis
  ↓ miss
Banco / VIEW
  ↓ miss em todos os locales
Spring MessageSource / ApiMessageProvider
```

Assim, uma mensagem cadastrada no banco sempre prevalece sobre a definição padrão empacotada na biblioteca, inclusive quando o override existe apenas em um locale de fallback.

### Mensagens padrão

As mensagens padrão do módulo ficam em:

```text
src/main/resources/messages/platform-messages.properties
src/main/resources/messages/platform-messages_pt_BR.properties
src/main/resources/messages/platform-messages_en.properties
```

Cada entrada usa o contrato:

```text
messageKey=code|httpStatus|message|solution
```

Exemplo:

```properties
validation.id.required=VALIDATION-0002|400|O identificador é obrigatório.|Informe um identificador válido.
```

O `PlatformDefaultMessageProvider` resolve a propriedade pelo `MessageSource`, converte a definição para `ApiMessage` e a devolve ao resolver.

O caractere `|` é reservado como delimitador da definição e não deve ser usado nos campos.

### Redis

O Redis continua sendo utilizado apenas como cache de mensagens originadas do banco:

```text
platform:message:{messageKey}:{locale}
```

Mensagens oriundas dos arquivos `.properties` não são armazenadas no Redis porque já estão disponíveis localmente na aplicação.

## Providers especializados

Módulos como autorização e catálogo podem continuar expondo seus próprios `ApiMessageProvider`. O `PlatformDefaultMessageProvider` representa apenas o catálogo padrão do `platform-messaging`.
