# platform-testing-http

Contratos e exemplos estão no [guia oficial](https://github.com/BrunoBS/golden-platform/blob/main/docs/libraries/modules/platform-testing-http.md).

`@PlatformIntegrationTest` starts the application on a random port and provides
`PlatformRequestSpecificationFactory` for requests to its controllers.

For outbound HTTP dependencies, use `@WithHttpMock` and inject
`HttpMockServer`. It starts one WireMock server on a dynamic port per Spring test
context, exposes `baseUrl()` and `port()`, accepts WireMock mappings through
`stubFor(...)`, resets before each test, and stops with the context. Its JUnit
resource lock serializes tests that manipulate the shared server.

```java
@WithHttpMock
class ExternalServiceIT {
    @Autowired HttpMockServer external;

    void configure() {
        external.stubFor(get(urlEqualTo("/status"))
                .willReturn(aResponse().withStatus(200)));
    }
}
```

`@WithMockAuthorization` composes this configuration and reuses its server.
Its allow and deny stubs still belong to `platform-testing-authorization`.
Use that annotation alone when authorization is involved; inject
`HttpMockServer` for any additional HTTP dependency stubs.
