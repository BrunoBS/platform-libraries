# Platform Libraries (Platform Engineering Core)

O **`platform-libraries`** reúne bibliotecas reutilizáveis da plataforma corporativa.

## Regra de dependência entre módulos

As dependências entre capabilities devem ser poucas, explícitas e direcionadas. Ciclos entre módulos não são permitidos.

Dependências intencionais atuais:

```text
platform-authorization -> platform-messaging
platform-catalog       -> platform-crud
```

`platform-authorization -> platform-messaging` existe porque autorização faz parte do baseline obrigatório dos microsserviços e seus erros precisam entrar diretamente no tratamento padronizado de mensagens/i18n da plataforma.

`platform-catalog -> platform-crud` é uma relação de especialização: catálogo reutiliza o ciclo CRUD comum.

`platform-test-support` possui integração opcional com `platform-authorization` para utilitários de teste.

## Arquitetura multimódulos

```text
platform-libraries/
├── platform-starter/        -> agregador do baseline obrigatório dos serviços
├── platform-messaging/      -> mensagens, i18n e tratamento padronizado de erros
├── platform-authorization/  -> autorização e contexto do usuário
├── platform-logging/        -> logging estruturado
├── platform-test-support/   -> suporte reutilizável de testes
├── platform-crud/           -> infraestrutura genérica de ciclo CRUD
└── platform-catalog/        -> especialização de catálogos gerenciados sobre CRUD
```

## Baseline obrigatório dos serviços

O baseline padrão dos microsserviços é exposto pelo `platform-starter`:

```text
platform-starter
├── platform-logging
├── platform-messaging
└── platform-authorization
```

O serviço consumidor declara somente:

```xml
<dependency>
    <groupId>com.empresa.platform</groupId>
    <artifactId>platform-starter</artifactId>
</dependency>
```

As versões são gerenciadas pelo `platform-parent` e não precisam ser informadas pelo serviço consumidor.

Quando `platform-starter` estiver declarado, o consumidor **não deve declarar novamente** `platform-logging`, `platform-messaging` ou `platform-authorization`. Essa é uma convenção de composição da plataforma, documentada para evitar redundância no POM; não é tratada como erro técnico pelo Maven Enforcer.

Capabilities específicas, como `platform-crud` e `platform-catalog`, continuam sendo declaradas somente quando necessárias.

Exemplo:

```xml
<dependencies>
    <dependency>
        <groupId>com.empresa.platform</groupId>
        <artifactId>platform-starter</artifactId>
    </dependency>

    <dependency>
        <groupId>com.empresa.platform</groupId>
        <artifactId>platform-catalog</artifactId>
    </dependency>
</dependencies>
```

## Módulos

### Platform Starter

Agregador sem código de negócio responsável por carregar o baseline comum dos serviços: logging, messaging e authorization.

### Platform Authorization

Motor de segurança e governança de contexto. Depende de `platform-messaging` para que suas exceptions sejam tratadas automaticamente pelo pipeline corporativo de mensagens/i18n.

As exceptions próprias de autorização continuam expressando a semântica do módulo:

```text
AuthorizationException
├── UnauthorizedAccessException
└── ForbiddenAccessException
```

`AuthorizationException` é compatível com `ApiException`, portanto o `ApiExceptionHandler` do messaging resolve a chave da mensagem, o idioma solicitado e o HTTP status cadastrado no catálogo de mensagens.

### Platform Messaging

Resolução de mensagens, internacionalização e tratamento padronizado de erros HTTP.

### Platform Logging

Padronização de logs estruturados e contexto MDC.

### Platform CRUD

Infraestrutura fortemente tipada para DTO, repository, mapper, validator, service e controller CRUD. Continua independente de `platform-messaging`.

Os principais pontos de inversão permanecem:

```text
notFoundException(id)
validationException(result)
```

### Platform Catalog

Especialização de `platform-crud` para catálogos persistidos, com `active`, restore, ordenação, filtros e validações de catálogo.

### Platform Test Support

Utilitários para testes unitários e de integração, incluindo integrações especializadas como autorização, banco e Kafka.

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
