# Estrutura de pacotes — platform-testing

## Organização aplicada

Os pacotes agrupam recursos por responsabilidade, mantendo separadas as APIs públicas e as integrações específicas:

```text
testing
├── architecture     # anotação, extensão e regras arquiteturais
├── authorization    # anotação, mocks e sessões de autorização
├── cloud
│   ├── aws          # anotação, LocalStack, SQS e S3
│   └── azure        # anotação, emuladores e clients Azure
├── context          # contexto compartilhado dos testes
├── database         # anotações, scripts, limpeza e fixtures MySQL
├── fixture          # builders, factories, cenários, clock e IDs
├── http             # clients, configuração e responses HTTP
├── kafka            # anotação e configuração do broker de teste
└── lifecycle        # anotações e extensões Spring/JUnit
```

A estrutura de testes acompanha os pacotes de produção para que cada teste fique perto do recurso que valida. Os testes de integração permanecem agrupados em `integration`.

## Agrupamentos realizados

| Origem | Pacote atual | Motivo |
|---|---|---|
| `builder`, `factory`, `scenario`, `fixture` | `fixture` | Mantém junto o suporte à criação de dados e cenários de teste |
| `client` e `client.response` | `http` e `http.response` | Reúne clients, configuração e respostas sob a mesma responsabilidade |
| `mysql` e `database` | `database` | Evita dividir o suporte de banco entre pacotes |
| `extension` e `unit` | `lifecycle` | Reúne extensões que controlam o ciclo de vida dos testes |
| `annotation` | Pacotes de contexto | Mantém cada anotação de entrada próxima da configuração ou extensão que ativa |

As anotações de entrada ficam junto ao contexto que ativam: cloud AWS/Azure, banco, Kafka, autorização, arquitetura ou ciclo de vida. Não há um pacote genérico `annotation`.

## Compatibilidade

Os tipos públicos movidos exigem atualizar imports dos consumidores. Os módulos deste repositório foram verificados e não possuem referências aos pacotes antigos. Consumidores externos devem trocar os imports para os novos pacotes antes de adotar esta versão.

Como nomes de pacotes fazem parte da API Java, publique essa reorganização como uma mudança incompatível segundo a política de versionamento do projeto.
