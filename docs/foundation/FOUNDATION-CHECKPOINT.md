# Foundation Checkpoint — FOUNDATION-GOLDEN-V1

## Estado

O checkpoint funcional da Foundation foi previamente validado por um consumidor Golden.

A ADR-004 introduz uma mudança posterior de **identidade pública** que precisa ser revalidada antes de continuar a evolução funcional da Golden Reference.

## Baseline funcional preservado

Continuam válidos:

- Java 25;
- Spring Boot 4.1.1;
- reactor consolidado em `platform-libraries`;
- `platform-crud` removido;
- `platform-catalog` independente de CRUD genérico;
- capabilities opcionais explícitas;
- infraestrutura pesada de testing opt-in;
- registry único `platform-libraries`.

## Migração de identidade em validação

```text
br.com.portalmanager.core
→ br.com.portalmanager.platform

platform-logging
→ platform-observability

platform-test-support
→ platform-testing
```

Versão:

```text
1.0.0
```

A mudança é de namespace/naming; não adiciona capability funcional.

## Gate da Foundation

Para fechar a revalidação:

- [ ] ADR-004 registrada;
- [ ] paths Java migrados;
- [ ] POMs migrados;
- [ ] starter usando platform-observability;
- [ ] BOM gerenciando platform-observability e platform-testing;
- [ ] configuração de logging migrada para `platform.observability.logging`;
- [ ] `mvn clean verify` verde;
- [ ] artifacts `br.com.portalmanager.platform:*:1.0.0` publicados.

## Gate da Golden Reference

Depois da publicação:

- [ ] parent migrado para `br.com.portalmanager.platform`;
- [ ] BOM migrado para `br.com.portalmanager.platform`;
- [ ] imports Java migrados;
- [ ] `platform-testing` usado no escopo test;
- [ ] G4 revalidada integralmente;
- [ ] Maven repository local isolado;
- [ ] zero dependência do namespace antigo;
- [ ] `BUILD SUCCESS`.

## Regra de sequência

A G5 não deve iniciar enquanto essa revalidação de identidade estiver aberta.

A Foundation não deve receber alterações funcionais para resolver problemas que sejam apenas de migração do consumidor.
