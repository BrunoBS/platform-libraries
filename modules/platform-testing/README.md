# Platform Testing

O `platform-testing` reúne convenções e fixtures reutilizáveis para testes unitários e de integração dos microsserviços da plataforma.

## Documentação do módulo

- [Arquitetura](docs/ARCHITECTURE.md): pacotes, componentes e fluxo das extensões.
- [Guia de uso](docs/USAGE.md): instalação, configuração e exemplos de consumo.

## Comece por aqui

- [Guia rápido](COMECE_AQUI_TESTES.md): organização, primeiros testes e uso do módulo.
- [Guia completo](GUIA_DE_USO.md): configuração detalhada de MySQL, Kafka, autorização, AWS LocalStack e emuladores Azure.

## Recursos

- Anotações para testes Spring, unitários e de integração;
- fixtures opt-in para MySQL, Kafka, autorização simulada e serviços cloud;
- utilitários para requests HTTP, respostas, builders, factories, cenários, relógio e IDs;
- regras arquiteturais para validar testes focados.

Os serviços cloud e o Kafka são opt-in. O guia completo lista as dependências Maven específicas de cada fixture.

## Pré-requisitos

- Java 25;
- Spring Boot 4.1.1 no baseline atual;
- Docker em execução para testes que usam Testcontainers;
- acesso ao repositório Maven que publica as bibliotecas da plataforma.

## Dependência Maven

Adicione ao microsserviço com escopo `test`:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform.library</groupId>
    <artifactId>platform-testing</artifactId>
    <version>${platform-libraries.version}</version>
    <scope>test</scope>
</dependency>
```
