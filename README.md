# Platform Libraries (Platform Engineering Core)

O **`platform-libraries`** reúne bibliotecas reutilizáveis da plataforma corporativa. Cada módulo deve representar uma capability coesa e, por padrão, ser consumível de forma independente.

## Regra de dependência entre módulos

A regra arquitetural é:

```text
capability de plataforma -> não depende de outra capability
```

A exceção atual é uma relação explícita de especialização:

```text
platform-catalog -> platform-crud
```

`platform-test-support` possui integração opcional com `platform-authorization` porque fornece utilitários específicos para testes dessa capability. Essa dependência é de suporte de teste e não deve contaminar as bibliotecas de runtime.

A composição das capabilities pertence ao microserviço consumidor:

```text
account-api
├── platform-authorization
├── platform-messaging
├── platform-logging
├── platform-crud
└── platform-catalog
```

Assim uma biblioteca não precisa conhecer como outra biblioteca representa erros, mensagens, logs ou autorização.

## Arquitetura multimódulos

```text
platform-libraries/
├── platform-messaging/      -> mensagens, i18n e tratamento padronizado de erros
├── platform-authorization/  -> autorização e contexto do usuário
├── platform-logging/        -> logging estruturado
├── platform-test-support/   -> suporte reutilizável de testes
├── platform-crud/           -> infraestrutura genérica de ciclo CRUD
└── platform-catalog/        -> especialização de catálogos gerenciados sobre CRUD
```

## Módulos

### Platform Authorization

Motor de segurança e governança de contexto para microsserviços. Mantém exceptions próprias de autorização e não depende de `platform-messaging`; o consumidor decide como traduzi-las para HTTP, i18n ou outro formato de erro.

### Platform Logging

Padronização de logs estruturados e contexto MDC sem dependência de outras capabilities da plataforma.

### Platform Messaging

Resolução de mensagens, internacionalização e tratamento de erros da aplicação. Pode ser usado pelo consumidor para traduzir exceptions de outras capabilities, sem que essas capabilities dependam dele.

### Platform CRUD

Infraestrutura fortemente tipada para DTO, repository, mapper, validator, service e controller CRUD. Não conhece domínio nem `platform-messaging`.

Os pontos principais de inversão são:

```text
notFoundException(id)
validationException(result)
```

O CRUD detecta a condição; o consumidor define a semântica da exception.

### Platform Catalog

Especialização de `platform-crud` para catálogos persistidos, com `active`, restore, ordenação, filtros e validações de catálogo. A dependência `catalog -> crud` é intencional; não há dependência de `platform-messaging`.

### Platform Test Support

Utilitários para testes unitários e de integração, incluindo integrações especializadas como autorização, banco e Kafka.

## Como utilizar

O serviço deve declarar somente as capabilities necessárias:

```xml
<dependencies>
    <dependency>
        <groupId>com.empresa.platform</groupId>
        <artifactId>platform-logging</artifactId>
    </dependency>

    <dependency>
        <groupId>com.empresa.platform</groupId>
        <artifactId>platform-authorization</artifactId>
    </dependency>

    <dependency>
        <groupId>com.empresa.platform</groupId>
        <artifactId>platform-messaging</artifactId>
    </dependency>
</dependencies>
```

As versões devem ser gerenciadas pelo `platform-parent`/`dependencyManagement` da plataforma.

## Qualidade

Para validar todo o reactor:

```bash
mvn clean verify
```

## Tecnologias

- Java 25
- Spring Boot 4.1.1
- Spring Data JPA
- Spring Web
- Spring Retry
- JUnit 5 / Mockito
