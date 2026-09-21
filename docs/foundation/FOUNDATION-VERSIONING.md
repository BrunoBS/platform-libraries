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

Recebe nova versão quando muda o baseline tecnológico governado, como Spring Boot ou overrides externos explícitos de WireMock, RestAssured, Datafaker, ArchUnit e Logstash encoder. Famílias já gerenciadas pelo Spring Boot não recebem um segundo BOM concorrente.

Uma mudança de baseline tecnológico deve ter compatibilidade avaliada com o parent, mas não implica automaticamente nova versão do libraries BOM.

## platform-libraries-bom

Versiona o conjunto compatível de capabilities.

As capabilities seguem inicialmente uma release train coerente para evitar matriz de compatibilidade por módulo.

Na release train preparada atualmente:

```text
platform-libraries-bom 1.0.0
platform-starter       1.0.0
platform-logging       1.0.0
platform-messaging     1.0.0
platform-authorization 1.0.0
platform-audit         1.0.0
platform-catalog       1.0.0
platform-tagging       1.0.0
platform-test-support  1.0.0
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

Os POMs das capabilities não repetem o número da release train. Dependências internas entre capabilities usam `${project.version}`.

`platform-parent` e `platform-dependencies` não usam `${revision}`; seus eixos permanecem independentes.

Como o Maven mínimo suportado continua em 3.9.9, o build usa `flatten-maven-plugin` para transformar as CI-friendly versions em versões concretas nos POMs instalados/publicados. O plugin é ativado explicitamente pelos artifacts da release train e não é imposto aos serviços consumidores pelo parent.

Os `.flattened-pom.xml` são artifacts gerados pelo build: devem existir durante a validação, mas não são versionados no Git.

Para iniciar uma nova release train, altera-se somente `-Drevision=<nova-versão>` em `.mvn/maven.config`, seguido de `mvn clean verify`, publicação remota e validação do consumidor.

## Namespace oficial

A partir da ADR-002, todos os artifacts próprios da Foundation usam o `groupId`:

```text
br.com.portalmanager.core
```

O namespace Java raiz também é `br.com.portalmanager.core`. Os `artifactId` permanecem `platform-*`.

As coordenadas anteriores em `com.empresa.platform` são históricas/experimentais e não pertencem às coordenadas oficiais finais.

## Versões próprias preparadas para publicação

As coordenadas próprias preparadas para publicação são:

- `br.com.portalmanager.core:platform-dependencies:1.0.0`;
- `br.com.portalmanager.core:platform-parent:1.0.0`;
- `br.com.portalmanager.core:platform-libraries-bom:1.0.0`;
- capabilities `br.com.portalmanager.core:platform-*:1.0.0`;
- root reactor `br.com.portalmanager.core:platform-libraries:1.0.0`.

A coincidência de `1.0.0` entre os artifacts próprios é operacional, não semântica.

Em particular, `platform-dependencies:1.0.0` é a versão do BOM tecnológico, não a versão das tecnologias contidas nele. O BOM gerencia Spring Boot `4.1.1`. Para famílias já gerenciadas pelo Spring Boot, a Foundation não importa um segundo BOM concorrente; Testcontainers, por exemplo, segue o gerenciamento do Spring Boot e atualmente resolve em `2.0.5`.

## Histórico de versões experimentais

Durante o cutover foram usados:

- release train `1.1.0`;
- `platform-dependencies:1.0.3`;
- `platform-parent:1.1.0`;
- `platform-libraries-bom:1.1.0`.

Essas versões permanecem somente como evidência histórica dos probes de consolidação.

Após a limpeza manual dos packages experimentais, o run `35542606756`, attempt 3, publicou `1.0.0` no namespace provisório `com.empresa.platform`. A ADR-002 tornou esse namespace obsoleto antes do checkpoint final.

## Regra de fechamento

Nenhuma versão será marcada como baseline consolidado final antes de:

1. publicação remota de `br.com.portalmanager.core:*:1.0.0`;
2. resolução remota do parent e BOM por um consumidor;
3. `mvn clean verify` verde no consumidor;
4. registro das evidências em `FOUNDATION-CHECKPOINT.md`.
