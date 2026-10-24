# platform-testing-authorization

`@WithMockAuthorization` imports the HTTP mock infrastructure from
`platform-testing-http`. It starts one dynamic-port WireMock server per Spring
context, configures the authorization service URL, and resets its authorization
stubs before each test. `AuthorizationMock` supplies allow, deny, forbidden,
resource-specific stubs and request verification. This simulates the provider;
it does not test the provider itself. A shared JUnit resource lock serializes
tests that modify the HTTP mock server.

Módulo Maven de Platform Libraries. Contratos, configuração e exemplos estão no [guia oficial](https://github.com/BrunoBS/golden-platform/blob/main/docs/libraries/modules/platform-testing-authorization.md).

- [POM e dependências](pom.xml)
- [Desenvolvimento e build do reactor](../../../README.md)

Execute os comandos de build na raiz do repositório para incluir os módulos dependentes. Fixtures de integração exigem Docker; agregadores POM não são dependências JAR.
