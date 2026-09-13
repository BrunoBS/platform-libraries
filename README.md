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
├── platform-messaging/      -> mensagens, i18n e tratamento padronizado de erros
├── platform-authorization/  -> autorização e contexto do usuário
├── platform-logging/        -> logging estruturado
├── platform-test-support/   -> suporte reutilizável de testes
├── platform-crud/           -> infraestrutura genérica de ciclo CRUD
└── platform-catalog/        -> especialização de catálogos gerenciados sobre CRUD
```

## Baseline obrigatório dos serviços

Os microsserviços padrão da plataforma recebem automaticamente pelo `platform-service-parent`:

```text
platform-logging
platform-messaging
platform-authorization
```

Essas dependências não precisam ser declaradas individualmente pelo serviço.

Capabilities específicas, como `platform-crud` e `platform-catalog`, continuam sendo adicionadas somente quando necessárias.

## Módulos

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
