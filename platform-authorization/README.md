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
platform-logging
platform-messaging
platform-authorization
```

Para uso isolado fora desse parent, a dependência pode ser declarada diretamente:

```xml
<dependency>
    <groupId>com.empresa.platform</groupId>
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
    enabled: true
    service-url: "https://empresa.com"
```

- `enabled=true`: usa o fluxo real de autorização.
- `enabled=false`: usa o modo local/mock disponibilizado pela autoconfiguração.

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

Com `platform.authorization.enabled=false`, o módulo permite desenvolvimento local sem depender do serviço central de autorização.

### 5. Cliente resiliente

A comunicação com o serviço de autorização utiliza `RestClient` e política de retry/backoff.

---

## 📋 Comportamento

| `enabled` | Interceptor | `UserContext` | Chamada externa |
| :---: | :---: | :--- | :---: |
| `true` | `AuthorizationInterceptor` | sessão real | Sim |
| `false` | interceptor local | sessão guest/local | Não |
