# Foundation Publishing

## Registry oficial alvo

O único registry oficial após a consolidação será:

`https://maven.pkg.github.com/brunobs/platform-libraries`

Os POMs estruturais, capabilities, settings e workflow da branch de consolidação já apontam para esse destino.

## Build limpo

A Foundation consolidada passou no GitHub Actions Verify #54, run `35537087703`.

O comando executado foi:

`mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify`

O reactor resolveu `platform-dependencies`, `platform-parent`, `platform-libraries-bom` e todas as capabilities no mesmo checkout, sem necessidade do package repository de `platform-build`.

## Probe de publicação

Foi executado um probe controlado de `clean deploy` no GitHub Actions Publish Maven packages #4, run `35537303423`.

O primeiro artifact a publicar foi `com.empresa.platform:platform-dependencies:1.0.3`.

Resultado:

```text
HTTP 422 Unprocessable Entity
BUILD FAILURE
```

A última publicação conhecida de `platform-build` contém `platform-dependencies:1.0.2` e `platform-parent:1.0.2`; portanto o erro não corresponde a sobrescrita da versão `1.0.3`.

O artifact Maven `platform-dependencies` já existe associado ao contexto de repository do `platform-build`. A migração dessa identidade de package precisa ser tratada antes do deploy pelo novo repository.

## Segurança operacional

O workflow foi restaurado para `workflow_dispatch` após o probe. Nenhum package existente foi apagado, sobrescrito ou desassociado.

Não usar `mvn install` como substituto da prova remota.

## Condição de saída

A publicação só será considerada concluída quando o reactor completo for publicado no registry de `platform-libraries`, seguido de consumo remoto verde pelo `account-service`.
