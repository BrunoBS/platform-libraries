# Platform Testing Authorization

O `platform-testing-authorization` fornece fixtures para simular o serviço de autorização nos testes dos microsserviços. Ele compõe o `platform-testing-core` com as APIs do `platform-authorization`; quem declara esse módulo também recebe o suporte comum de testes.

## Dependência Maven

Declare o módulo com escopo `test` e versão gerenciada pelo `platform-libraries-bom`:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform.library</groupId>
    <artifactId>platform-testing-authorization</artifactId>
    <scope>test</scope>
</dependency>
```

## Fixture

Use `@WithMockAuthorization` para registrar o WireMock e configurar a resposta padrão. O módulo fornece `AuthorizationMock`, `AuthorizationSessionBuilder`, `AuthorizationSessionCustomizer`, `AuthorizationResourceMatcher` e a anotação `@WithMockAuthorization`. As regras e o cliente reais continuam em `platform-authorization`; este módulo contém apenas suporte de teste.
