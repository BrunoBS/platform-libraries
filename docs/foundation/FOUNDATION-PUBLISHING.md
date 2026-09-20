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

O primeiro artifact a publicar foi `br.com.portalmanager.core:platform-dependencies:1.0.3`.

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

`br.com.portalmanager.core:platform-dependencies:pom:1.0.3`

no endpoint:

`https://maven.pkg.github.com/brunobs/platform-libraries`

falhou. Portanto as resoluções anteriores executadas dentro do checkout do reactor não são aceitas como evidência remota para os artifacts estruturais.

A tentativa de deploy do mesmo artifact para o novo repository já havia retornado HTTP 422 no run `35537303423`.

Nenhum package foi apagado, sobrescrito ou transferido.

### Consumidor

O `BrunoBS/account-service` foi preparado em branch separada para usar somente o registry consolidado, com `platform-parent:1.1.0` e `platform-libraries-bom:1.1.0`.

O Verify #13, run `35538639721`, e o probe com repositório Maven vazio, run `35538758316`, falharam ao resolver `platform-parent:1.1.0` exclusivamente do registry de `platform-libraries`.

Essa falha é a evidência downstream que impede o fechamento do checkpoint.


## Release train centralizada

A release train das capabilities possui uma única fonte de versão:

```text
.mvn/maven.config
-Drevision=1.1.0
```

O root reactor, `platform-libraries-bom` e as capabilities usam essa `revision`. Dependências internas usam `${project.version}`.

Como o baseline é Maven 3.9.9, os artifacts da release train usam `flatten-maven-plugin` em `resolveCiFriendliesOnly`. O CI verifica que os POMs achatados existem, contêm a versão concreta da release e não deixam `${revision}` sem resolução antes de qualquer publicação.

`platform-parent` e `platform-dependencies` permanecem fora da release train e mantêm suas próprias versões.


## Reset de baseline para 1.0.0

Por decisão do projeto, após a limpeza manual das versões/packages experimentais no GitHub Packages, a próxima publicação da topologia consolidada parte de um baseline limpo:

```text
platform-dependencies   1.0.0
platform-parent         1.0.0
platform-libraries-bom  1.0.0
capabilities            1.0.0
```

A release train continua usando a propriedade Maven 3 CI-friendly `revision`, definida em `.mvn/maven.config` como `-Drevision=1.0.0`. O nome `revision` é mantido por compatibilidade com Maven 3.9.9; propriedades arbitrárias como `${platform-libraries.version}` no campo `project.version` não fazem parte do suporte CI-friendly do Maven 3.

Os runs e versões 1.0.3/1.1.0 documentados acima permanecem somente como evidência histórica da investigação de cutover e não representam o novo baseline de publicação.
