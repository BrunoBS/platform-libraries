# Foundation Versioning

## Princípio

A Foundation possui três eixos conceitualmente independentes.

```text
build baseline
platform-parent:X

technology baseline
platform-dependencies:Y

capabilities baseline
platform-libraries-bom:Z
```

A igualdade eventual entre números de versão não cria dependência semântica entre os eixos.

## platform-parent

Versiona regras e convenções de build.

Recebe nova versão quando muda, por exemplo:

- regra Maven Enforcer;
- compiler/Surefire/Failsafe/JaCoCo;
- Maven mínimo;
- contrato de build consumido pelos serviços;
- forma como o parent importa o baseline tecnológico.

Mudança apenas em uma capability não exige nova versão do parent.

## platform-dependencies

Versiona o baseline tecnológico externo.

Recebe nova versão quando muda uma dependência externa governada, como Spring Boot, Testcontainers, WireMock, RestAssured, Datafaker, ArchUnit ou Logstash encoder.

Uma mudança de baseline tecnológico deve ter compatibilidade avaliada com o parent, mas não implica automaticamente nova versão do libraries BOM.

## platform-libraries-bom

Versiona o conjunto compatível de capabilities.

As capabilities seguem inicialmente release train coerente para evitar matriz de compatibilidade por módulo.

Exemplo:

```text
platform-libraries-bom 1.1.0
platform-starter       1.1.0
platform-logging       1.1.0
platform-messaging     1.1.0
platform-authorization 1.1.0
platform-audit         1.1.0
platform-catalog       1.1.0
platform-tagging       1.1.0
platform-test-support  1.1.0
```

Mesmo uma alteração localizada em uma capability pode gerar nova release train, sem alterar `platform-parent`.

### Fonte única da versão da release train

A versão da release train é definida uma única vez em:

```text
.mvn/maven.config
-Drevision=1.0.0
```

Usam essa mesma `revision`:

- root reactor `platform-libraries`;
- `platform-libraries-bom`;
- as oito capabilities em `modules/`.

Os POMs das capabilities não repetem mais o número da release train. Dependências internas entre capabilities usam `${project.version}`.

`platform-parent` e `platform-dependencies` não usam `${revision}`; seus eixos permanecem independentes.

Como o baseline oficial continua em Maven 3.9.9, o build usa `flatten-maven-plugin` para transformar as CI-friendly versions em versões concretas nos POMs instalados/publicados. O plugin é ativado explicitamente pelos artifacts da release train e não é imposto aos serviços consumidores pelo parent.

Para iniciar uma nova release train, altera-se somente `-Drevision=<nova-versão>` em `.mvn/maven.config`, seguido de `mvn clean verify` e da publicação remota.

## Namespace oficial

A partir da ADR-002, todos os artifacts próprios da Foundation usam o groupId:

```text
br.com.portalmanager.core
```

O namespace Java raiz também é `br.com.portalmanager.core`. Os `artifactId` permanecem `platform-*`.

## Baseline preparado para publicação limpa

Após a decisão de limpar as versões/packages experimentais no GitHub Packages, o baseline de publicação da Foundation consolidada reinicia em `1.0.0`:

- `platform-dependencies:1.0.0`;
- `platform-parent:1.0.0`;
- `platform-libraries-bom:1.0.0`;
- capabilities: `1.0.0`;
- root reactor: `platform-libraries:1.0.0`.

Estado de publicação anterior permanece apenas como evidência histórica dos probes de consolidação:

- release train das capabilities `1.1.0`: publicada no registry de `platform-libraries` pelo run `35538290455`;
- `platform-dependencies:1.0.3`: foi o artifact estrutural usado no probe que expôs o bloqueio de cutover;
- `platform-parent:1.1.0`: foi o parent usado no probe do `account-service`;
- `platform-libraries-bom:1.1.0`: pertenceu ao baseline experimental anterior.

Essas versões não representam mais o baseline alvo. O novo baseline `1.0.0` ainda precisa ser publicado e validado remotamente após a limpeza dos packages experimentais.

Nenhuma versão será marcada como baseline consolidado final antes do consumo remoto verde pelo consumidor de prova.
