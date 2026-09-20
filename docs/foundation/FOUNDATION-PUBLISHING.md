# Foundation Publishing

## Estado atual

A Foundation consolidada está pronta para a publicação definitiva do baseline `1.0.0` sob o namespace oficial:

```text
br.com.portalmanager.core
```

O código em `main` passou no Verify #85, run `35544899150`.

A publicação definitiva dessas novas coordenadas ainda deve ser executada e, em seguida, validada por um consumidor remoto antes do fechamento do checkpoint.

## Registry oficial

O único registry oficial alvo é:

`https://maven.pkg.github.com/brunobs/platform-libraries`

Todos os POMs estruturais, capabilities, settings e o workflow de publicação apontam para esse destino.

O registry de `platform-build` não pertence ao fluxo oficial alvo.

## Baseline a publicar

```text
br.com.portalmanager.core:platform-dependencies:1.0.0
br.com.portalmanager.core:platform-parent:1.0.0
br.com.portalmanager.core:platform-libraries-bom:1.0.0
br.com.portalmanager.core:platform-starter:1.0.0
br.com.portalmanager.core:platform-logging:1.0.0
br.com.portalmanager.core:platform-messaging:1.0.0
br.com.portalmanager.core:platform-authorization:1.0.0
br.com.portalmanager.core:platform-audit:1.0.0
br.com.portalmanager.core:platform-catalog:1.0.0
br.com.portalmanager.core:platform-tagging:1.0.0
br.com.portalmanager.core:platform-test-support:1.0.0
```

O root reactor `br.com.portalmanager.core:platform-libraries:1.0.0` é operacional e não é um eixo de consumo da Foundation.

## Release train

A release train das capabilities possui uma única fonte:

```text
.mvn/maven.config
-Drevision=1.0.0
```

Usam essa `revision`:

- root reactor;
- `platform-libraries-bom`;
- as oito capabilities.

Dependências internas usam `${project.version}`.

Como o baseline Maven é 3.9.9, os artifacts da release train usam `flatten-maven-plugin` em `resolveCiFriendliesOnly`. Os `.flattened-pom.xml` são gerados durante o build, verificados pelo CI e não são versionados no Git.

`platform-parent` e `platform-dependencies` permanecem fora da release train e mantêm eixos de versão independentes.

## Build e publicação

Validação:

```bash
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify
```

Publicação oficial:

```bash
mvn --settings .github/maven-publish-settings.xml --batch-mode --no-transfer-progress clean deploy
```

O workflow `Publish Maven packages` utiliza `workflow_dispatch`, Java 25 e o `GITHUB_TOKEN` efêmero com `packages: write`.

Não usar `mvn install` local como substituto de publicação ou prova de resolução remota.

## Evidência local consolidada

O Verify #85, run `35544899150`, validou o `main` após:

- consolidação de parent e technology BOM;
- release train centralizada;
- namespace `br.com.portalmanager.core`;
- remoção de paths legados;
- higiene dos artifacts gerados pelo Flatten Plugin;
- metadata da release train.

Resultado: `BUILD SUCCESS`.

## Histórico do cutover Maven

### Probe inicial

Durante a consolidação foi executado o Publish Maven packages #4, run `35537303423`.

O primeiro artifact era:

`com.empresa.platform:platform-dependencies:1.0.3`

O GitHub Packages respondeu:

```text
HTTP 422 Unprocessable Entity
BUILD FAILURE
```

Naquele momento os artifacts estruturais ainda estavam associados ao contexto antigo de `platform-build`.

### Release train experimental

Durante a investigação também foram usados:

- `platform-dependencies:1.0.3`;
- `platform-parent:1.1.0`;
- `platform-libraries-bom:1.1.0`;
- capabilities `1.1.0`.

Essas versões são somente evidência histórica e não representam o baseline oficial final.

### Reset para 1.0.0

Após a limpeza manual dos packages experimentais, o baseline foi reiniciado em `1.0.0`.

O run `35542606756`, attempt 3, publicou com sucesso o reactor completo ainda no namespace provisório `com.empresa.platform`.

Essa publicação provou o funcionamento operacional do fluxo consolidado de deploy, mas deixou de representar o baseline oficial quando a ADR-002 adotou `br.com.portalmanager.core`.

## Publicação definitiva pendente

A próxima publicação deve usar exclusivamente:

```text
br.com.portalmanager.core:*:1.0.0
```

Depois do deploy, a evidência deve registrar:

- run/attempt da publicação;
- reactor completo com `BUILD SUCCESS`;
- packages estruturais e capabilities publicados;
- resolução remota sem cache local previamente instalado.

## Condição de saída

A publicação da Foundation só será considerada concluída quando:

1. o reactor completo `br.com.portalmanager.core:*:1.0.0` for publicado no registry de `platform-libraries`;
2. um consumidor resolver `platform-parent:1.0.0` e `platform-libraries-bom:1.0.0` remotamente;
3. o consumidor declarar capabilities sem versão;
4. o consumidor executar `mvn clean verify` com sucesso;
5. o consumidor não depender do registry `platform-build`;
6. as evidências finais forem registradas em `FOUNDATION-CHECKPOINT.md`.

Até lá, `FOUNDATION-GOLDEN-V1` permanece aberto.
