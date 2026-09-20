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

## Versões preparadas nesta migração

- `platform-dependencies:1.0.3`;
- `platform-parent:1.1.0`;
- `platform-libraries-bom:1.1.0`;
- capabilities: `1.1.0`;
- root reactor: `platform-libraries:1.1.0`.

Estado de publicação nesta migração:

- release train das capabilities `1.1.0`: publicada no registry de `platform-libraries` pelo run `35538290455`;
- `platform-dependencies:1.0.3`: preparado no código, porém não comprovado como consumível no registry novo;
- `platform-parent:1.1.0`: preparado no código, porém não consumível pelo `account-service` exclusivamente no registry novo;
- `platform-libraries-bom:1.1.0`: preparado para o novo modelo, mas o checkpoint depende do cutover completo dos artifacts estruturais.

Nenhuma versão será marcada como baseline consolidado final antes do consumo remoto verde pelo consumidor de prova.
