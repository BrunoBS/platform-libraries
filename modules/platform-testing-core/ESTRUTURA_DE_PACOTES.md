# Estrutura de pacotes — platform-testing-core

## Organização aplicada

Os pacotes agrupam recursos por responsabilidade, mantendo separadas as APIs públicas e as integrações específicas:

```text
testing
├── architecture
│   ├── annotation
│   └── PlatformArchitectureExtension
├── cloud
│   ├── aws
│   │   ├── annotation
│   │   └── suporte LocalStack, SQS e S3
│   └── azure
│       ├── annotation
│       └── suporte a emuladores e clients
├── context
├── database
│   ├── annotation
│   └── scripts, limpeza e fixture MySQL
├── fixture
│   ├── builder
│   ├── factory
│   ├── scenario
│   └── utilitários (TestClock e TestIds)
├── http
│   └── response
├── kafka
│   ├── annotation
│   └── configuração do broker
└── lifecycle
    ├── annotation
    └── extensões Spring/JUnit
```

A estrutura de testes acompanha os pacotes de produção para que cada teste fique perto do recurso que valida. Os testes de integração permanecem agrupados em `integration`.

## Agrupamentos realizados

| Origem | Pacote atual | Motivo |
|---|---|---|
| `builder`, `factory`, `scenario` | `fixture.builder`, `fixture.factory`, `fixture.scenario` | Separa os contratos de construção, variação de dados e preparação de cenários; utilitários genéricos permanecem em `fixture` |
| `client` e `client.response` | `http` e `http.response` | Reúne clients, configuração e respostas sob a mesma responsabilidade |
| `mysql` e `database` | `database` | Evita dividir o suporte de banco entre pacotes |
| `extension` e `unit` | `lifecycle` | Reúne extensões que controlam o ciclo de vida dos testes |
| `annotation` | Subpacote de cada contexto | Mantém as anotações separadas dos componentes de suporte e próximas do recurso que ativam |

As anotações ficam no subpacote `annotation` de cada contexto: `cloud.aws.annotation`, `cloud.azure.annotation`, `database.annotation`, `kafka.annotation`, `architecture.annotation` e `lifecycle.annotation`. O contexto HTTP não tem anotações próprias.

## Tipos reutilizáveis do core

Os contratos e classes-base para os microsserviços ficam nestes arquivos:

| Uso no microsserviço | Tipo no `platform-testing-core` |
|---|---|
| Builder de dados | `fixture.builder.TestDataBuilder` e `fixture.builder.AbstractTestDataBuilder` |
| Factory de massa válida e variações semânticas | `fixture.factory.TestDataFactory` e `fixture.factory.AbstractTestDataFactory` |
| Preparação de pré-condições | `fixture.scenario.TestScenario` |
| Client HTTP | `http.BaseClient` |
| Requisição HTTP | `http.PlatformRequestSpecificationFactory` |
| Assertions e extração HTTP | `http.response.BaseResponse` |

Builders, factories e cenários concretos continuam no projeto consumidor, pois usam DTOs, endpoints e pré-condições próprios do domínio. O módulo fornece os contratos, as classes-base e a infraestrutura comum para implementá-los com a mesma forma.

## Compatibilidade

Os tipos públicos movidos exigem atualizar imports dos consumidores. Os módulos deste repositório foram verificados e não possuem referências aos pacotes antigos. Consumidores externos devem trocar os imports para os novos pacotes antes de adotar esta versão.

Como nomes de pacotes fazem parte da API Java, publique essa reorganização como uma mudança incompatível segundo a política de versionamento do projeto.
