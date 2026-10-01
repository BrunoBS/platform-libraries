# 🛡️ Módulo Platform Authorization (`platform-authorization`)

O **`platform-authorization`** é o módulo corporativo de governança de segurança, controle de acesso e gerenciamento de contexto de usuário.

## Integração com `platform-messaging`

`platform-authorization` depende diretamente de `platform-messaging`.

A decisão é intencional: autorização, messaging/i18n e logging compõem o baseline obrigatório dos microsserviços da plataforma, e os erros de autorização devem entrar automaticamente no tratamento corporativo de mensagens.

A hierarquia continua específica de autorização:

```text
AuthorizationException
├── UnauthorizedAccessException
└── ForbiddenAccessException
```

`AuthorizationException` estende `ApiException` do `platform-messaging`. Com isso, o `ApiExceptionHandler` já consegue:

- resolver a chave de mensagem;
- considerar o locale/`Accept-Language`;
- aplicar parâmetros;
- obter o HTTP status configurado no catálogo de mensagens;
- devolver o response de erro padronizado.

O consumidor não precisa criar `try/catch` ou `@RestControllerAdvice` específico para os erros emitidos pelo módulo.

```text
requisição
   ↓
platform-authorization
   ↓
UnauthorizedAccessException / ForbiddenAccessException
   ↓
platform-messaging
   ↓
i18n + status HTTP + response padronizado
```

---

## 🚀 Como usar no microsserviço

Nos serviços que utilizam o `platform-service-parent`, o módulo faz parte do baseline e não precisa ser declarado individualmente no `pom.xml`.

O baseline inclui:

```text
platform-observability
platform-messaging
platform-authorization
```

Para uso isolado fora desse parent, a dependência pode ser declarada diretamente:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform</groupId>
    <artifactId>platform-authorization</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

`platform-messaging` será trazido transitivamente pelo módulo.

---

## 🛠️ Configuração (`application.yml`)

```yaml
platform:
  authorization:
    mode: REAL
    service-url: "https://empresa.com"
```

- `mode=REAL`: usa o fluxo real e encaminha a decisão de autorização para a Authorization API.
- `mode=MOCK`: usa exclusivamente a sessão local simulada e não chama a Authorization API.
- Quando `mode` não é informado, o padrão seguro é `REAL`.

---

## 💎 Funcionalidades Core

### 1. Autorização declarativa de endpoint

`@AuthorizationRequired` decide se o usuário pode executar o endpoint conforme o nível de autorização exigido pela operação.

```java
@GetMapping("/faturamento")
@AuthorizationRequired(level = AuthorizationLevel.RESTRICTED)
public ResponseEntity<Dados> buscarDados() { ... }
```

### 2. Visibilidade de recursos retornados

`@ResourceVisibility` não autoriza a execução do endpoint. O objetivo é limitar os recursos retornados de acordo com os grupos autorizadores presentes na sessão do usuário.

A separação de responsabilidades é intencional:

```text
@AuthorizationRequired
→ o usuário pode executar este endpoint?

@ResourceVisibility
→ quais recursos retornados por este endpoint o usuário pode enxergar?
```

A annotation deve ser aplicada em métodos cujo retorno contenha objetos que implementem `AuthorizableResource`.

Para coleções, os itens cujo `authorizerGroup` não pertence ao usuário são removidos da resposta:

```java
@ResourceVisibility
public List<AccountDTO> findAll() {
    return repository.findAll();
}
```

Exemplo de comportamento:

```text
Conta A → usuário possui o authorizerGroup → retorna
Conta B → usuário não possui o authorizerGroup → filtrada
Conta C → usuário possui o authorizerGroup → retorna
```

Para um recurso único, se o usuário não possuir o `authorizerGroup`, o recurso não é retornado e a operação resulta em `ForbiddenAccessException`:

```java
@ResourceVisibility
public AccountDTO findById(Long id) {
    return repository.findById(id);
}
```

Usuários OWNER ignoram o filtro de visibilidade. Objetos que não implementam `AuthorizableResource` não são filtrados pela annotation.

`@ResourceVisibility` não deve ser usada como substituta de `@AuthorizationRequired` e não deve ser interpretada como autorização de escrita. Regras de permissão para criar, alterar, excluir ou restaurar recursos continuam sendo definidas pelo nível exigido no endpoint e pelas regras específicas do domínio.

### 3. `UserContext`

Após a autorização, a sessão é disponibilizada no `UserContext` e limpa ao final da requisição.

### 4. Modo local

Com `platform.authorization.mode=MOCK`, o módulo permite desenvolvimento local sem depender do serviço central de autorização.

Sem configuração adicional, o contexto continua usando o guest padrão:

```text
userName = guest
groups = [GUEST]
authorizerGroups = [GUEST]
```

Para simular cenários reais de autorização e visibilidade, a sessão local pode ser configurada:

```yaml
platform:
  authorization:
    mode: MOCK
    mock:
      user-name: local-user
      email: local-user@empresa.com
      account-id: account-local
      application-id: application-local
      environment-id: DEV
      trace-id: trace-local
      groups:
        - USER
        - TESTER
      authorizer-groups:
        - full-group: GRP_WORKSPACE_DEV_TEAM_A
          profile: DEV
          environment: DEV
          authorizer: TEAM_A
        - full-group: GRP_WORKSPACE_DEV_TEAM_B
          profile: DEV
          environment: DEV
          authorizer: TEAM_B
```

`groups` representa os grupos gerais da sessão. Quando `authorizer-groups` não é informado, grupos no padrão `^PM5-(?:(ENG|NEG)-)?(DEV|TST|ADM)_(.+)# 🛡️ Módulo Platform Authorization (`platform-authorization`)

O **`platform-authorization`** é o módulo corporativo de governança de segurança, controle de acesso e gerenciamento de contexto de usuário.

## Integração com `platform-messaging`

`platform-authorization` depende diretamente de `platform-messaging`.

A decisão é intencional: autorização, messaging/i18n e logging compõem o baseline obrigatório dos microsserviços da plataforma, e os erros de autorização devem entrar automaticamente no tratamento corporativo de mensagens.

A hierarquia continua específica de autorização:

```text
AuthorizationException
├── UnauthorizedAccessException
└── ForbiddenAccessException
```

`AuthorizationException` estende `ApiException` do `platform-messaging`. Com isso, o `ApiExceptionHandler` já consegue:

- resolver a chave de mensagem;
- considerar o locale/`Accept-Language`;
- aplicar parâmetros;
- obter o HTTP status configurado no catálogo de mensagens;
- devolver o response de erro padronizado.

O consumidor não precisa criar `try/catch` ou `@RestControllerAdvice` específico para os erros emitidos pelo módulo.

```text
requisição
   ↓
platform-authorization
   ↓
UnauthorizedAccessException / ForbiddenAccessException
   ↓
platform-messaging
   ↓
i18n + status HTTP + response padronizado
```

---

## 🚀 Como usar no microsserviço

Nos serviços que utilizam o `platform-service-parent`, o módulo faz parte do baseline e não precisa ser declarado individualmente no `pom.xml`.

O baseline inclui:

```text
platform-observability
platform-messaging
platform-authorization
```

Para uso isolado fora desse parent, a dependência pode ser declarada diretamente:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform</groupId>
    <artifactId>platform-authorization</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

`platform-messaging` será trazido transitivamente pelo módulo.

---

## 🛠️ Configuração (`application.yml`)

```yaml
platform:
  authorization:
    mode: REAL
    service-url: "https://empresa.com"
```

- `mode=REAL`: usa o fluxo real e encaminha a decisão de autorização para a Authorization API.
- `mode=MOCK`: usa exclusivamente a sessão local simulada e não chama a Authorization API.
- Quando `mode` não é informado, o padrão seguro é `REAL`.

---

## 💎 Funcionalidades Core

### 1. Autorização declarativa de endpoint

`@AuthorizationRequired` decide se o usuário pode executar o endpoint conforme o nível de autorização exigido pela operação.

```java
@GetMapping("/faturamento")
@AuthorizationRequired(level = AuthorizationLevel.RESTRICTED)
public ResponseEntity<Dados> buscarDados() { ... }
```

### 2. Visibilidade de recursos retornados

`@ResourceVisibility` não autoriza a execução do endpoint. O objetivo é limitar os recursos retornados de acordo com os grupos autorizadores presentes na sessão do usuário.

A separação de responsabilidades é intencional:

```text
@AuthorizationRequired
→ o usuário pode executar este endpoint?

@ResourceVisibility
→ quais recursos retornados por este endpoint o usuário pode enxergar?
```

A annotation deve ser aplicada em métodos cujo retorno contenha objetos que implementem `AuthorizableResource`.

Para coleções, os itens cujo `authorizerGroup` não pertence ao usuário são removidos da resposta:

```java
@ResourceVisibility
public List<AccountDTO> findAll() {
    return repository.findAll();
}
```

Exemplo de comportamento:

```text
Conta A → usuário possui o authorizerGroup → retorna
Conta B → usuário não possui o authorizerGroup → filtrada
Conta C → usuário possui o authorizerGroup → retorna
```

Para um recurso único, se o usuário não possuir o `authorizerGroup`, o recurso não é retornado e a operação resulta em `ForbiddenAccessException`:

```java
@ResourceVisibility
public AccountDTO findById(Long id) {
    return repository.findById(id);
}
```

Usuários OWNER ignoram o filtro de visibilidade. Objetos que não implementam `AuthorizableResource` não são filtrados pela annotation.

`@ResourceVisibility` não deve ser usada como substituta de `@AuthorizationRequired` e não deve ser interpretada como autorização de escrita. Regras de permissão para criar, alterar, excluir ou restaurar recursos continuam sendo definidas pelo nível exigido no endpoint e pelas regras específicas do domínio.

### 3. `UserContext`

Após a autorização, a sessão é disponibilizada no `UserContext` e limpa ao final da requisição.

### 4. Modo local

Com `platform.authorization.mode=MOCK`, o módulo permite desenvolvimento local sem depender do serviço central de autorização.

Sem configuração adicional, o contexto continua usando o guest padrão:

```text
userName = guest
groups = [GUEST]
authorizerGroups = [GUEST]
```

Para simular cenários reais de autorização e visibilidade, a sessão local pode ser configurada:

```yaml
platform:
  authorization:
    mode: MOCK
    mock:
      user-name: local-user
      email: local-user@empresa.com
      account-id: account-local
      application-id: application-local
      environment-id: DEV
      trace-id: trace-local
      groups:
        - USER
        - TESTER
      authorizer-groups:
        - full-group: GRP_WORKSPACE_DEV_TEAM_A
          profile: DEV
          environment: DEV
          authorizer: TEAM_A
        - full-group: GRP_WORKSPACE_DEV_TEAM_B
          profile: DEV
          environment: DEV
          authorizer: TEAM_B
```

 são convertidos automaticamente em `ParsedGroup` para `hasAuthorizer(...)` e `@ResourceVisibility`. Por exemplo, `PM5-ENG-DEV_WSE` gera `profile=ENG`, `environment=DEV` e `authorizer=WSE`. `authorizer-groups` continua disponível como override explícito.

As propriedades de mock são consideradas somente em `mode=MOCK`. Em `mode=REAL`, a sessão vem exclusivamente da Authorization API.

A library não decide a semântica dos níveis de autorização, incluindo `OPEN`. Ela encaminha token, contexto da requisição e `AuthorizationLevel` para a Authorization API, que valida o token e toma a decisão de autorização.

### 5. Cliente resiliente

A comunicação com o serviço de autorização utiliza `RestClient` e política de retry/backoff.

---

## 📋 Comportamento

| `mode` | Interceptor | `UserContext` | Chamada externa |
| :---: | :---: | :--- | :---: |
| `REAL` | `AuthorizationInterceptor` | sessão real | Sim |
| `MOCK` | interceptor local | sessão guest/local | Não |
