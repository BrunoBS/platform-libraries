# Estrutura de pacotes — platform-testing

## Organização aplicada

Os pacotes agrupam recursos por responsabilidade, mantendo separadas as APIs públicas e as integrações específicas:

```text
testing
├── architecture
│   ├── annotation
│   └── PlatformArchitectureExtension
├── authorization
│   ├── annotation
│   └── mocks e sessões
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
| `builder`, `factory`, `scenario`, `fixture` | `fixture` | Mantém junto o suporte à criação de dados e cenários de teste |
| `client` e `client.response` | `http` e `http.response` | Reúne clients, configuração e respostas sob a mesma responsabilidade |
| `mysql` e `database` | `database` | Evita dividir o suporte de banco entre pacotes |
| `extension` e `unit` | `lifecycle` | Reúne extensões que controlam o ciclo de vida dos testes |
| `annotation` | Subpacote de cada contexto | Mantém as anotações separadas dos componentes de suporte e próximas do recurso que ativam |

As anotações ficam no subpacote `annotation` de cada contexto: `cloud.aws.annotation`, `cloud.azure.annotation`, `database.annotation`, `kafka.annotation`, `authorization.annotation`, `architecture.annotation` e `lifecycle.annotation`. O contexto HTTP não tem anotações próprias.

## Compatibilidade

Os tipos públicos movidos exigem atualizar imports dos consumidores. Os módulos deste repositório foram verificados e não possuem referências aos pacotes antigos. Consumidores externos devem trocar os imports para os novos pacotes antes de adotar esta versão.

Como nomes de pacotes fazem parte da API Java, publique essa reorganização como uma mudança incompatível segundo a política de versionamento do projeto.
