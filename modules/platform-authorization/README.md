# Platform Authorization (`platform-authorization`)

O `platform-authorization` padroniza a integração dos microsserviços com a Authorization API e mantém o contexto de autorização da requisição. A decisão de autorização pertence à Authorization API; a library atua como cliente/proxy e não consulta views nem reproduz a hierarquia de permissões.

## Modos

```yaml
platform:
  authorization:
    mode: REAL
    service-url: "https://authorization.internal"
    connect-timeout: 500ms
    read-timeout: 2s
    retry:
      max-retries: 2
      initial-delay: 200ms
      multiplier: 2
```

`REAL` é o padrão seguro e exige `service-url`. Nesse modo, token, contexto HTTP e `AuthorizationLevel` são encaminhados à Authorization API. `MOCK` cria uma sessão local para desenvolvimento e testes e não realiza chamada externa.

Os defaults de resiliência são: conexão 500 ms, resposta 2 s, até 2 retries, delay inicial 200 ms e multiplicador 2. Retry é aplicado somente a falhas de comunicação e 5xx. Respostas 401, 403 e demais 4xx não são retentadas.

## Autorização de endpoint

`@AuthorizationRequired` declara o nível exigido pelo endpoint. A library resolve a metadata, captura os identifiers presentes nas path variables e encaminha a requisição à Authorization API.

```java
@GetMapping("/{workspaceIdentifier}")
@AuthorizationRequired(level = AuthorizationLevel.DEV)
public WorkspaceResponse findById(@PathVariable String workspaceIdentifier) {
    // ...
}
```

A library não interpreta a semântica de `OPEN`, `DEV`, `TST`, `ADM` ou `OWNER`. Inclusive para `OPEN`, o token e o nível são enviados à Authorization API, que valida o token e toma a decisão.

## Contrato de falhas

```text
401 -> UnauthorizedAccessException
403 -> ForbiddenAccessException
outros 4xx -> erro de contrato HTTP
5xx / timeout / falha de conexão -> retry -> PLT-AUTH-002 (503)
```

Falhas técnicas permanecem fail closed: indisponibilidade da Authorization API nunca libera a requisição.

## UserContext

Após autorização bem-sucedida, a `UserSession` retornada pela Authorization API é colocada no `UserContext` e seus dados relevantes são propagados ao MDC. O contexto é limpo ao final da requisição.

## Visibilidade de recursos

`@ResourceVisibility` é independente de `@AuthorizationRequired`. Autorização responde se a operação pode ser executada; visibilidade limita quais registros podem ser consultados.

A entidade protegida declara exatamente um campo persistente `String` com `@AuthorizerGroup`:

```java
@Entity
public class Workspace {

    @AuthorizerGroup
    @Column(name = "authorizer_group")
    private String authorizerGroup;
}
```

O use case informa explicitamente a entidade cujo filtro deve ser ativado:

```java
@ResourceVisibility(Workspace.class)
public List<Workspace> findAll() {
    return repository.findAll();
}
```

A library registra um filtro Hibernate específico por entidade e injeta os grupos autorizadores da sessão. Usuários `OWNER` ignoram esse filtro. `@ResourceVisibility` não substitui autorização de escrita.

## Modo MOCK

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
      authorizer-groups:
        - full-group: PM5-ENG-DEV_WSE
          profile: ENG
          environment: DEV
          authorizer: WSE
```

Quando `authorizer-groups` não é configurado, grupos compatíveis com o padrão da plataforma são convertidos para `ParsedGroup`. As propriedades MOCK são ignoradas em `REAL`.

## Integração com platform-messaging

`UnauthorizedAccessException` e `ForbiddenAccessException` estendem o contrato de `ApiException` do `platform-messaging`, permitindo i18n e response padronizado sem `try/catch` específico no microsserviço. Falhas técnicas da Authorization API usam `PLT-AUTH-002` com status 503.
