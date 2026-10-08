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
    datasource:
      enabled: false
      view-name: "vw_api_message"
    cache:
      enabled: false
      ttl: 1h
```


Todos esses valores já possuem defaults seguros em `PlatformMessagingProperties`; a aplicação consumidora só precisa declarar o que deseja sobrescrever. JDBC e Redis permanecem opt-in (`enabled: false`). A biblioteca não publica um `application.yml` próprio, evitando inserir propriedades no `Environment` da aplicação consumidora.

## Validação de unicidade no startup

Ao criar o `PlatformDefaultMessageProvider`, todos os bundles em
`META-INF/platform-messages/*.properties` são carregados e indexados.
Bundles inválidos, chaves duplicadas ou definições malformadas falham no startup. Durante a resolução em runtime, falhas transitórias da fonte continuam seguindo a cadeia de fallback.

A combinação abaixo deve ser única:

```text
locale + messageKey
```

Se dois módulos publicarem a mesma chave para o mesmo locale, a aplicação falha
durante a inicialização com uma mensagem indicando a chave, o locale e os dois
bundles conflitantes.

Exemplo inválido:

```text
catalog_pt_BR.properties -> not.found
catalog_pt_BR.properties -> not.found
```

A mesma chave em idiomas diferentes é permitida:

```text
audit_pt_BR.properties -> audit.event.not.found
audit_en.properties    -> audit.event.not.found
```

Por convenção, cada módulo deve usar seu próprio namespace, por exemplo
`audit.*`, `routing.*`, `authorization.*` e `catalog.*`.


## Namespace automático por bundle

O nome do bundle define o namespace global das mensagens.

Formato obrigatório:

```text
<service>_<locale>.properties
```

Exemplo:

```text
catalog_pt_BR.properties
```

Conteúdo do arquivo usa apenas a chave local:

```properties
not.found=CAT-404-001|404|Catálogo não encontrado.|Verifique o identificador informado.
name.required=CAT-400-003|400|Nome obrigatório.|Informe o nome do catálogo.
```

Durante o startup, o provider transforma automaticamente:

```text
catalog + not.found      -> catalog.not.found
catalog + name.required  -> catalog.name.required
```

A chave completa é a identidade usada no Java, no banco e no Redis:

```text
Java:   catalog.not.found
Banco:  catalog.not.found
Redis:  platform:message:catalog.not.found:pt-BR
Bundle: not.found
```

Uma chave já prefixada dentro do bundle, por exemplo
`catalog.not.found` dentro de `catalog_pt_BR.properties`, é rejeitada no startup.

Dois serviços podem ter a mesma chave local sem colisão:

```text
audit_pt_BR.properties -> event.not.found -> audit.event.not.found
catalog_pt_BR.properties -> event.not.found -> catalog.event.not.found
```

A colisão ocorre apenas quando a mesma chave global é publicada duas vezes
para o mesmo locale, por exemplo dois bundles `catalog_pt_BR.properties`
contendo `not.found`.


## Serviços sem DataSource

O módulo também funciona em serviços que não possuem banco de dados.

Quando existe um `JdbcTemplate` no contexto, o módulo cria
`JdbcApiMessageRepository` e mantém o fluxo:

```text
Redis -> Banco / VIEW -> Bundle
```

Quando não existe `JdbcTemplate`, o módulo cria automaticamente
`NoOpApiMessageRepository`. Nesse modo, o repositório sempre retorna vazio e
a resolução segue para os bundles locais:

```text
NoOp repository -> PlatformDefaultMessageProvider -> Bundle
```

Não é necessário configurar DataSource apenas para utilizar mensagens locais.


## Resiliência das fontes

A resolução de mensagens é fail-safe. Falhas de infraestrutura em cache, datasource/view ou bundle são registradas e não interrompem a cadeia de fallback:

```text
Redis -> Banco / VIEW -> Bundle -> Platform Default
```

Um miss é fluxo normal e não é tratado como falha. Quando nenhuma fonte conhece a chave, a library devolve uma mensagem técnica interna e imutável com HTTP 500, sem depender de I/O ou configuração.

Quando o datasource está habilitado, a library executa no startup uma consulta sem linhas (`WHERE 1 = 0`) com as colunas do contrato para diagnosticar existência/acesso da view. Falhas são registradas, mas não impedem o startup; em runtime a resolução continua pelos fallbacks disponíveis.

Falhas repetidas durante a resolução são limitadas por fonte/tipo de erro em uma janela curta para evitar tempestade de logs.

## Validation response details

The response envelope uses the code, message, solution, and HTTP status resolved for the exception's message key. Each validation detail also includes the resolved message `code`, `message`, and `solution` alongside its `field`. This keeps field-level message codes visible when multiple fields fail validation; the envelope's HTTP status remains the single HTTP status for the response.
