# Proposta de simplificação dos pacotes — platform-testing

## Estado atual

O módulo organiza a API por tipo de classe e por tecnologia. Isso é legível para quem conhece o nome da abstração, mas espalha uma feature por vários pacotes: por exemplo, MySQL aparece em `annotation`, `mysql`, `database` e `extension`. As pastas `builder`, `factory` e `scenario` também criam mais pontos de entrada para um conjunto pequeno de contratos.

## Árvore proposta

```text
testing
├── annotation            # anotações públicas de entrada
├── architecture          # regra arquitetural e contrato @CoversClasses
├── authorization         # WireMock e sessão autorizada
├── cloud
│   ├── aws               # LocalStack, SQS e S3
│   └── azure             # emuladores e clients Azure
├── database              # scripts, limpeza e fixture MySQL
├── http                  # RestAssured, clients e responses
├── kafka                 # configuração do broker de teste
├── fixture               # builder, factory, scenario, clock e IDs
└── lifecycle             # extensões Spring/JUnit compartilhadas
```

## O que seria agrupado

| Pacotes atuais | Destino proposto | Motivo |
|---|---|---|
| `builder`, `factory`, `scenario`, `fixture` | `fixture` | São utilitários para preparar dados e cenários de teste |
| `client` e `client.response` | `http` | São partes da mesma abstração HTTP |
| `mysql`, `database` | `database` | A fixture de banco e os scripts compartilham o mesmo contexto |
| `extension`, `unit` e extensões específicas | `lifecycle` ou junto da feature | Reduz a pasta genérica e aproxima implementação da responsabilidade |
| `cloud.aws`, `cloud.azure` | Mantidos | Separação por provedor já é clara e útil |
| `annotation`, `authorization`, `kafka`, `architecture` | Mantidos | São superfícies conceituais distintas e fáceis de localizar |

## Recomendação

A proposta reduz a quantidade de pacotes raiz sem criar um pacote genérico para todo o código. A principal oportunidade é agrupar contratos pequenos em `fixture`, reunir os utilitários HTTP e aproximar as extensões de ciclo de vida das features correspondentes.

Não recomendo mover classes públicas neste ajuste. Os nomes de pacote fazem parte dos imports dos consumidores, então a mudança pode quebrar compilação mesmo sem alterar comportamento. Antes de executar a reorganização:

1. confirmar se o módulo já foi publicado ou adotado por serviços;
2. se houver consumidores, manter classes de compatibilidade por uma versão ou planejar uma versão principal;
3. aplicar os movimentos em alteração isolada, com atualização dos exemplos e validação de compatibilidade;
4. manter classes internas de implementação package-private sempre que possível, expondo apenas as anotações e contratos necessários.

A árvore acima é uma proposta de destino, não uma mudança já aplicada.
