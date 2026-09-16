# platform-audit

Biblioteca de integração transparente entre microserviços e a Audit API da plataforma.

## Objetivo

O módulo abstrai a publicação de eventos de auditoria. O serviço consumidor declara
o evento com `@Auditable`; a biblioteca resolve contexto, identificadores e payload
e envia o evento para a Audit API.

A biblioteca não possui entidade JPA, repository ou tabela própria.

## Fluxo

```text
Microserviço
    ↓
@Auditable
    ↓
AuditAspect
    ↓
AuditPublisher
    ↓
AuditEventClient
    ↓
Audit API
```

## Configuração

```yaml
platform:
  audit:
    enabled: true
    service-url: http://audit-api
    service-name: account
    publish-path: /api/v1/events
    fail-on-error: false
```

Por padrão, a publicação é assíncrona e uma falha da Audit API não derruba a
operação principal. Quando `fail-on-error=true`, a chamada é síncrona e a falha
é propagada.

## Uso

```java
@Auditable(
    resource = "account",
    action = "UPDATE",
    resourceId = @AuditField(
        source = AuditFieldSource.PATH,
        field = "accountId"
    )
)
public ResponseEntity<AccountResponse> update(String accountId, ...) {
    ...
}
```

## Contexto

O `AuditContextProvider` é substituível. A implementação padrão lê:

- `X-Account-Id`
- `X-Application-Id`
- `X-Environment`
- `X-Correlation-Id`
- `HttpServletRequest.getUserPrincipal()`

Aplicações que usam outro mecanismo de contexto podem registrar seu próprio
`AuditContextProvider`.
