# platform-schema-validation

Biblioteca Spring Boot para validação estrutural de inputs de use case por JSON Schema publicado.

A validação pertence à fronteira da aplicação, e não ao protocolo de entrada:

```text
REST ─────┐
MCP ──────┤
BFF ──────┤
Consumer ─┤
          ↓
Use Case
  ↓ @ValidateResourceSchema
@SchemaPayload Input
  ↓
ResourceSchemaValidationAspect
  ↓
SchemaValidator
  ↓
ResourceSchemaValidator
  ↓
Caffeine (Schema compilado por type/code/version)
  ↓
ResourceSchemaResolver
  ↓
Redis opcional (ResourceSchema)
  ↓ miss/indisponível
Repository
  ↓
VIEW configurada no banco do consumidor
```

A biblioteca conhece apenas o contrato da VIEW:

```text
resource_type
resource_code
schema_version
definition
```

Ela não conhece controllers, protocolos de entrada ou as tabelas internas do serviço owner do Schema.

## Uso

Marque o método de use case e exatamente um parâmetro que representa o payload estrutural:

```java
@ValidateResourceSchema(type = "APPLICATION", code = "application")
public ApplicationOutput create(
        String workspaceIdentifier,
        @SchemaPayload CreateApplicationInput input
) {
    ...
}
```

Antes da execução do método, o aspect serializa o input para `JsonNode` e o valida contra o schema resolvido. O método só é executado quando a estrutura é válida.

Um método com `@ValidateResourceSchema` deve possuir exatamente um parâmetro `@SchemaPayload`. Ausência ou duplicidade é tratada como erro de configuração.

## Fronteiras

- Entrypoints (REST, MCP, BFF etc.) adaptam protocolo e aplicam autenticação/autorização.
- `@ValidateResourceSchema` garante a estrutura do input na fronteira do use case.
- Validators do serviço permanecem responsáveis pelas regras de negócio.
- Domain não depende de JSON Schema, AOP ou desta biblioteca.

## Resolução

```text
(resourceType, resourceCode)
        ↓ miss
(resourceType, DEFAULT)
        ↓ miss
erro de configuração
```

## Configuração

No caminho Golden, nenhuma configuração é obrigatória:

```yaml
platform:
  schema-validation:
    fallback-code: DEFAULT
    view-name: vw_platform_resource_schemas
    cache:
      redis:
        enabled: false
        ttl: 6h
      local:
        enabled: true
        ttl: 15m
        max-size: 500
```

Os dois valores acima já são defaults e só precisam ser declarados quando houver override.

A resolução da fonte segue o próprio container Spring:

```text
ResourceSchemaRepository customizado existe
        ↓
usa implementação do serviço

nenhum custom + JdbcTemplate disponível
        ↓
JdbcResourceSchemaRepository da library
        ↓
valida contrato da VIEW no startup
        ↓
view-name

nenhum ResourceSchemaRepository + nenhum JdbcTemplate
        ↓
PLT-SCHEMA-007
        ↓
startup failure
```

Não existe `enabled`, `datasource.enabled` ou property de `mode`. Adicionar o módulo declara a intenção de usar Schema Validation; por isso, a aplicação não inicia sem uma fonte. O caminho default exige `JdbcTemplate`; alternativamente, o serviço deve fornecer um `ResourceSchemaRepository` customizado. Se o serviço fornece um `ResourceSchemaRepository`, a implementação JDBC da library não é criada por causa de `@ConditionalOnMissingBean`. Assim o serviço pode resolver schemas por tabela própria, JPA, JDBC customizado ou outra fonte sem alterar o contrato da validação.

## Contrato da VIEW

O serviço owner do Schema deve publicar uma VIEW com as colunas:

```text
resource_type
resource_code
schema_version
definition
```

A biblioteca consulta essa VIEW diretamente, assim como `platform-messaging` consulta sua view de mensagens.

Quando o caminho JDBC default é utilizado, a library valida a fonte no startup com uma consulta estrutural que seleciona as quatro colunas contratuais usando `WHERE 1 = 0`. A consulta não exige schema publicado nem lê dados de negócio; ela comprova acesso à fonte, existência da VIEW e compatibilidade das colunas. Falha de conexão, permissão, VIEW inexistente ou contrato incompatível impede a inicialização. Implementações customizadas de `ResourceSchemaRepository` não recebem health check genérico da library.

O contrato exige **no máximo uma linha por `(resource_type, resource_code)`**. A VIEW deve expor somente a versão publicada corrente de cada recurso. A library não usa `LIMIT 1` para esconder duplicidade: zero linhas significa ausência e permite fallback; uma linha é o schema resolvido; mais de uma linha é violação do contrato da VIEW e gera `PlatformConfigurationException` (`PLT-SCHEMA-006`), preservando a exceção JDBC original como causa.

A VIEW MySQL não possui índices próprios. Performance e unicidade devem ser garantidas pelas tabelas base do serviço owner. Como referência, o modelo owner deve possuir índice/constraint para localizar a configuração por tipo/código e índice adequado para localizar a versão `PUBLISHED` corrente. Os nomes e DDL exatos pertencem ao serviço owner e não à library.

Quando o consumidor fornece um `ResourceSchemaRepository` customizado, ele deve preservar a mesma semântica: cardinalidade lógica 0..1 para cada `(resourceType, resourceCode)`; `Optional.empty()` exclusivamente para schema inexistente; mais de um schema é violação de contrato e deve lançar `PlatformConfigurationException`, nunca escolher um registro arbitrariamente. Falhas de infraestrutura/configuração também devem ser propagadas como erro de plataforma, não convertidas em ausência.


### Defaults Golden

O módulo possui uma única raiz pública de configuração: `PlatformSchemaValidationProperties`.

- `platform.schema-validation.fallback-code`: `DEFAULT`
- `platform.schema-validation.view-name`: `vw_platform_resource_schemas`
- `platform.schema-validation.cache.redis.enabled`: `false`
- `platform.schema-validation.cache.redis.ttl`: `6h`
- `platform.schema-validation.cache.local.enabled`: `true`
- `platform.schema-validation.cache.local.ttl`: `15m`
- `platform.schema-validation.cache.local.max-size`: `500`

Valores textuais em branco usam o default do módulo. TTLs e `max-size` devem ser maiores que zero. Componentes de negócio não consultam `Environment` ou propriedades diretamente.

## Contrato AOP

O aspect mantém uma responsabilidade pequena: localizar o parâmetro `@SchemaPayload`, convertê-lo para `JsonNode`, delegar a validação e somente então prosseguir com o use case.

A posição do `@SchemaPayload` é resolvida uma vez por método e mantida em cache. Assim, a reflexão usada para validar o contrato da annotation não é repetida em cada chamada. O contrato continua exigindo exatamente um `@SchemaPayload` por método anotado.

A biblioteca não move regras de negócio para o aspect e não exige annotations de protocolo nos controllers.


## Runtime e resolução de schema

A resolução consulta primeiro o cache Redis quando `cache.redis.enabled=true`. Um miss consulta o `ResourceSchemaRepository`/VIEW e armazena o schema resolvido no Redis pelo TTL configurado. Falha do Redis não interrompe a validação: é registrada com throttling de 10 minutos e o fluxo continua para a fonte. Falhas da VIEW continuam sendo erro de plataforma; não são convertidas em ausência.

O fallback continua sendo `(type, code) -> (type, DEFAULT)`. O resultado DEFAULT é armazenado somente sob sua própria chave real; ele não é gravado sob o código solicitado, evitando mascarar a publicação posterior de um schema específico durante o TTL.

Depois da resolução, o JSON Schema compilado é mantido em Caffeine, internamente, pela chave `resourceType + resourceCode + schemaVersion`. O cache local usa `expireAfterAccess`, TTL de 15 minutos e limite default de 500 entradas. Uma nova versão publicada gera uma nova chave e, portanto, não reutiliza a compilação da versão anterior.

Redis é opcional e depende da infraestrutura `StringRedisTemplate` da aplicação. Caffeine é detalhe de implementação: o consumidor não usa sua API nem precisa configurá-lo diretamente.


## Contrato de mensagens de validação

A implementação do engine JSON Schema é detalhe interno da library. Mensagens produzidas pelo NetworkNT não fazem parte do contrato público e não são propagadas diretamente ao consumidor.

```text
NetworkNT Error
        ↓
SchemaValidationErrorMapper
        ↓
schemavalidation.*
        ↓
ValidationResult
        ↓
platform-messaging
        ↓
bundle default / override central
```

Keywords são normalizadas de forma determinística para chaves do namespace `schemavalidation.*`, como `schemavalidation.required`, `schemavalidation.min-length` e `schemavalidation.additional-properties`. Se a chave normalizada não estiver cadastrada no `platform-messaging`, a resposta usa `schemavalidation.invalid` como fallback; o texto técnico do NetworkNT não é exposto.

Essas mensagens estruturais pertencem ao fluxo normal do `platform-messaging`: possuem definição default nos bundles `schemavalidation_*.properties` e podem ser sobrescritas pelo mecanismo central de mensagens sem alterar a library ou o serviço consumidor.

Erros técnicos `PLT-SCHEMA-001` a `PLT-SCHEMA-007` são diferentes: representam configuração ou integridade da plataforma, usam `PlatformErrorDefinition` e permanecem como contrato técnico estável, fora do mecanismo de override das mensagens estruturais.


### Extensibilidade de keywords

A chave de mensagem não depende de um `switch` fechado da library. A keyword retornada pelo engine é normalizada de forma determinística de camelCase para kebab-case:

```text
minLength             -> schemavalidation.min-length
additionalProperties  -> schemavalidation.additional-properties
futureKeyword         -> schemavalidation.future-keyword
```

As assertions conhecidas do Draft 2020-12 possuem defaults nos bundles da library. Uma keyword futura pode ser atendida pelo mecanismo central do `platform-messaging` usando a chave normalizada, sem exigir nova versão desta library. Keywords ausentes, vazias ou com formato inseguro usam `schemavalidation.invalid`.

O texto interno produzido pelo NetworkNT nunca faz parte do contrato público. A ativação de assertions é uma decisão separada do mapeamento de mensagens. No runtime Golden atual, assertions de `format` são habilitadas explicitamente; portanto, formatos declarados no schema, como `email`, participam da validação e podem reprovar o payload.


## Validação explícita

Além do uso automático por AOP, a library expõe `SchemaValidator` como contrato público para casos em que o tipo e o código do schema são definidos dinamicamente pelo próprio use case.

```java
private final SchemaValidator schemaValidator;

schemaValidator.validate(resourceType, resourceCode, payload);
```

O consumidor depende somente de `SchemaValidator`. `ResourceSchemaValidator`, NetworkNT, resolução e mapeamento de erros permanecem detalhes internos da implementação. O Aspect também consome o mesmo contrato, garantindo um único fluxo de validação para chamadas automáticas e explícitas.
