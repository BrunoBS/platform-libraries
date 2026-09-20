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


## Evidência atualizada da consolidação

A implementação estrutural atual foi validada no GitHub Actions Verify #60, run `35537837948`, commit `e055c5a1088ec96c94ae96493d13d58cd7f73135`.

O reactor contém `platform-dependencies:1.0.3`, `platform-parent:1.1.0`, `platform-libraries-bom:1.1.0` e as oito capabilities `1.1.0`; Enforcer, dependency convergence e todas as suítes concluíram com `BUILD SUCCESS`.

As oito capabilities da release train `1.1.0` foram publicadas no registry de `platform-libraries` pelo run `35538290455` com `BUILD SUCCESS`.

Essa publicação parcial não fecha o checkpoint: os POMs das capabilities dependem do `platform-parent:1.1.0`.

### Probe remoto isolado

O run `35538856486` utilizou um POM Maven standalone fora do reactor, repositório local isolado e `-U`. A resolução de:

`com.empresa.platform:platform-dependencies:pom:1.0.3`

no endpoint:

`https://maven.pkg.github.com/brunobs/platform-libraries`

falhou. Portanto as resoluções anteriores executadas dentro do checkout do reactor não são aceitas como evidência remota para os artifacts estruturais.

A tentativa de deploy do mesmo artifact para o novo repository já havia retornado HTTP 422 no run `35537303423`.

Nenhum package foi apagado, sobrescrito ou transferido.

### Consumidor

O `BrunoBS/account-service` foi preparado em branch separada para usar somente o registry consolidado, com `platform-parent:1.1.0` e `platform-libraries-bom:1.1.0`.

O Verify #13, run `35538639721`, e o probe com repositório Maven vazio, run `35538758316`, falharam ao resolver `platform-parent:1.1.0` exclusivamente do registry de `platform-libraries`.

Essa falha é a evidência downstream que impede o fechamento do checkpoint.
