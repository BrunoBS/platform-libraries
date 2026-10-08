# Foundation Publishing

## Estado atual

A Foundation está em migração de identidade pública conforme `ADR-004-PLATFORM-NAMESPACE-AND-MODULE-NAMING.md`.

Novo namespace:

```text
br.com.portalmanager.platform
```

Versão mantida por decisão do projeto:

```text
1.0.0
```

## Registry oficial

```text
https://maven.pkg.github.com/brunobs/platform-libraries
```

Nenhum outro registry faz parte do fluxo oficial.

## Coordenadas alvo

```text
br.com.portalmanager.platform:platform-dependencies:1.0.0
br.com.portalmanager.platform:platform-parent:1.0.0
br.com.portalmanager.platform:platform-libraries-bom:1.0.0
br.com.portalmanager.platform:platform-starter:1.0.0
br.com.portalmanager.platform:platform-observability:1.0.0
br.com.portalmanager.platform:platform-messaging:1.0.0
br.com.portalmanager.platform:platform-authorization:1.0.0
br.com.portalmanager.platform:platform-audit:1.0.0
br.com.portalmanager.platform:platform-catalog:1.0.0
br.com.portalmanager.platform:platform-tagging:1.0.0
br.com.portalmanager.platform:platform-testing-core:1.0.0
```

O root reactor `platform-libraries` é operacional e não é eixo de consumo.

## Por que 1.0.0 pode ser mantido

As coordenadas anteriores usavam outro `groupId`:

```text
br.com.portalmanager.core
```

A ADR-004 cria coordenadas Maven distintas ao mudar para:

```text
br.com.portalmanager.platform
```

Além disso:

```text
platform-logging      → platform-observability
platform-test-support → platform-testing-core
```

Portanto esta migração não depende de sobrescrever artifacts do namespace anterior.

## Build

Validação:

```bash
mvn --settings .github/maven-settings.xml \
    --batch-mode \
    --no-transfer-progress \
    clean verify
```

Publicação:

```bash
mvn --settings .github/maven-publish-settings.xml \
    --batch-mode \
    --no-transfer-progress \
    clean deploy
```

O workflow oficial `Publish Maven packages` usa Java 25 e `workflow_dispatch`.

## Gate de publicação

Antes do deploy:

1. reactor completo verde;
2. nenhum source path em `br/com/portalmanager/core`;
3. nenhum POM/código ativo com `br.com.portalmanager.core`;
4. nenhum módulo `platform-logging`;
5. nenhum módulo `platform-test-support`;
6. revision permanece `1.0.0`.

## Gate downstream

Após o deploy, o `account-service` deve:

- usar `br.com.portalmanager.platform:platform-parent:1.0.0`;
- importar `br.com.portalmanager.platform:platform-libraries-bom:1.0.0`;
- consumir `platform-starter`, `platform-tagging`, `platform-audit` e `platform-testing-core` sem versão;
- migrar imports Java para `br.com.portalmanager.platform.*`;
- executar `clean verify` com Maven repository local isolado;
- não depender do namespace `br.com.portalmanager.core`.

## Condição de saída

A migração só é considerada concluída quando Foundation e Golden consumer estiverem verdes no novo namespace.
