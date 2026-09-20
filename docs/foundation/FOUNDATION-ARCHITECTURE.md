# Foundation Architecture

## Escopo

Este documento registra a arquitetura estabilizada da Foundation após as fases F1–F5. A Foundation é composta por `platform-build` e `platform-libraries`; Golden Reference e Scaffold permanecem fora deste checkpoint.

## Camadas

```text
platform-build
    ↓
platform-libraries
    ↓
Golden Reference (após FOUNDATION-GOLDEN-V1)
    ↓
Scaffold (posterior)
```

`platform-build` governa baseline Java/Spring Boot, dependency management, plugins e gates Maven. Não deve forçar capabilities funcionais transitivamente.

`platform-libraries` fornece capacidades técnicas transversais realmente reutilizáveis. Padrões arquiteturais de aplicação que não são capabilities transversais devem ser demonstrados posteriormente pela Golden Reference.

## Baseline

- Java 25.
- Spring Boot 4.1.1.
- Maven >= 3.9.9.
- Maven Enforcer e dependency convergence ativos.
- GitHub Packages é o repositório Maven remoto oficial entre build e libraries.

## Libraries

Baseline obrigatório do `platform-starter`:

```text
platform-starter
├── platform-logging
├── platform-messaging
└── platform-authorization
```

Capabilities explícitas:

- `platform-audit`
- `platform-catalog`
- `platform-tagging`
- `platform-test-support` para infraestrutura de testes

`platform-crud` não faz parte da Foundation.

## Catalog

`platform-catalog` permanece como capability específica para catálogos persistidos e gerenciados. É independente de CRUD genérico e mantém apenas comportamento próprio de catálogo: active/inactive, restore, ordenação, nome(s), filtros, validação, dynamic catalogs e enum catalogs.

## Dependências internas relevantes

```text
starter -> logging
starter -> messaging
starter -> authorization
authorization -> messaging
audit -> authorization
audit -> messaging
catalog -> messaging
```

Tagging e logging não dependem de outras libraries da plataforma. Test-support pode integrar authorization opcionalmente.

## Auto-configuração e comportamento explícito

Capabilities opcionais não entram no starter apenas por existirem na Foundation.

Authorization preserva o comportamento aprovado: habilitada por padrão; o consumidor mínimo usado para validar o starter define `platform.authorization.enabled=false`.

Messaging possui core independente de JDBC. JDBC e Redis são integrações condicionais; sem JDBC, o repositório de mensagens utiliza o fallback NoOp.

Audit possui fallback Redis opcional e condicionado à configuração explícita.

## Distribuição

A integração oficial é:

```text
platform-build
  -> publish
GitHub Packages
  -> resolve
platform-libraries
  -> mvn clean verify
```

Instalação local em `.m2`, cópia manual de POMs ou dependência de workspace não constituem evidência oficial de integração.

## Estado para F6

As fases F1–F5 deixaram build e libraries tecnicamente verdes em seus respectivos workflows. A F6 deve revalidar o build e depois provar a resolução remota da versão efetivamente estabilizada de parent/BOM pelas libraries antes do checkpoint FOUNDATION-GOLDEN-V1.
