# Migration Notes — Foundation Golden V1

## Objetivo

Registrar impactos de adoção da Foundation estabilizada para consumidores, sem iniciar a Golden Reference ou migrar aplicações nesta fase.

## Starter

O baseline do `platform-starter` contém somente logging, messaging e authorization.

Consumidores que utilizem audit, catalog ou tagging devem declarar essas capabilities explicitamente.

Authorization continua habilitada por padrão. Um consumidor que deliberadamente não utilize autorização deve configurar `platform.authorization.enabled=false`.

## Messaging

JDBC deixou de ser dependência obrigatória do caminho mínimo. Consumidores que necessitem do repositório JDBC devem ter suporte JDBC no classpath; sem ele, messaging utiliza o repositório NoOp.

Redis permanece integração opcional.

## Catalog

`platform-catalog` não depende mais de `platform-crud`.

Consumidores de Catalog devem usar as abstrações específicas de catálogo. Não existe mais contrato Foundation baseado em `BaseCrud*`, `CrudControllerSupport` ou tipos de validação CRUD.

Os comportamentos próprios de catálogo permanecem: active/inactive, restore, ordenação, busca por nome(s), filtros e validações de catálogo.

## CRUD genérico

`platform-crud` foi removido da Foundation. Novos consumidores não devem declarar `br.com.portalmanager.platform:platform-crud`.

Aplicações que ainda dependam de APIs genéricas de CRUD precisam tratar essa migração no contexto da própria aplicação ou, posteriormente, seguir os padrões explícitos demonstrados pela Golden Reference. Esta fase não migra domínio de aplicações.

## Build

O baseline é Java 25, Spring Boot 4.1.1 e Maven >= 3.9.9.

O parent não gerencia mais `platform-crud`. O `platform-parent` e o `platform-dependencies` foram migrados para `platform-libraries`. As versões das capabilities passam a ser governadas pelo `platform-libraries-bom`, não pelo parent.

## Distribuição Maven

O fluxo oficial alvo usa somente o package repository de `platform-libraries`. Consumidores devem resolver `platform-parent` e `platform-libraries-bom` remotamente; `mvn install` local não é evidência de integração oficial. Enquanto o novo checkpoint não fechar, o fluxo publicado anterior permanece disponível apenas para rollback.

## Fora de escopo

Estas notas não autorizam iniciar Golden Reference, Scaffold, migrar Account API, copiar código legado ou alterar regras de negócio de aplicações.
