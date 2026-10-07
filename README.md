# Platform Libraries — Golden Platform Foundation

O `platform-libraries` é o repositório consolidado da Foundation da Golden Platform.

A Foundation fornece capacidades reutilizáveis e governança técnica. A Golden Reference demonstra os padrões de implementação sobre essas capacidades.

## Baseline

- Java 25;
- Spring Boot 4.1.1;
- Maven 3.9.9 ou superior dentro da faixa suportada pelo parent;
- namespace Maven e Java oficial: `br.com.portalmanager.platform`;
- versão preparada das coordenadas próprias da Foundation: `1.0.0`.

`platform-crud` foi removido da Foundation e não deve ser recriado.

## Estrutura

```text
platform-libraries/
├── pom.xml                    -> reactor/aggregator
├── platform-parent/           -> governança de build
├── platform-dependencies/     -> BOM tecnológico externo
├── platform-libraries-bom/    -> BOM das capabilities
└── modules/
    ├── platform-starter/
    ├── platform-observability/
    ├── platform-messaging/
    ├── platform-schema-validation/
    ├── platform-authorization/
    ├── platform-audit/
    ├── platform-catalog/
    ├── platform-tagging/
    └── platform-testing/
```

O POM raiz é somente reactor/aggregator e não concentra regras de build.

## Responsabilidades Maven

`platform-parent` define Java 25, Maven mínimo, plugins, Enforcer, dependency convergence e regras comuns de build. Ele importa `platform-dependencies`, mas não gerencia versões das capabilities.

`platform-dependencies` gerencia somente o baseline tecnológico externo. Spring Boot 4.1.1 é a fonte de verdade para as famílias de dependências que ele próprio gerencia, incluindo Testcontainers.

`platform-libraries-bom` gerencia as versões das capabilities da plataforma.

Os três eixos são conceitualmente independentes:

```text
platform-parent          -> baseline de build
platform-dependencies    -> baseline tecnológico
platform-libraries-bom   -> baseline das capabilities
```

A coincidência atual em `1.0.0` refere-se somente às versões dos artifacts próprios da Foundation e não cria acoplamento entre esses eixos.

O `platform-dependencies:1.0.0` representa uma versão do BOM tecnológico; as tecnologias que ele gerencia possuem suas próprias versões, por exemplo Spring Boot `4.1.1`; Testcontainers segue o gerenciamento do Spring Boot e atualmente resolve em `2.0.5`.

## Release train das capabilities

A release train é definida uma única vez em:

```text
.mvn/maven.config
-Drevision=1.0.0
```

Usam essa `revision`:

- root reactor `platform-libraries`;
- `platform-libraries-bom`;
- as nove capabilities em `modules/`.

As dependências internas entre capabilities usam `${project.version}`. Os POMs de consumo gerados pelo Flatten Maven Plugin não são versionados no Git.

## Dependências intencionais entre capabilities

```text
platform-authorization -> platform-messaging
platform-audit         -> platform-authorization
platform-audit         -> platform-messaging
platform-catalog       -> platform-messaging

platform-starter
├── platform-observability
├── platform-messaging
└── platform-authorization

platform-schema-validation -> platform-messaging
```

`platform-testing` mantém fixtures opt-in para MySQL, Kafka e cloud, com as dependências dos containers fornecidas transitivamente. O starter JDBC é opcional para não ativar auto-configuração de `DataSource` em consumidores sem banco.

## Consumo alvo

Após a publicação definitiva das coordenadas da Foundation, um serviço consumidor deve combinar explicitamente o parent de build e o BOM das capabilities:

```xml
<parent>
    <groupId>br.com.portalmanager.platform.library</groupId>
    <artifactId>platform-parent</artifactId>
    <version>1.0.0</version>
    <relativePath/>
</parent>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>br.com.portalmanager.platform.library</groupId>
            <artifactId>platform-libraries-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Capabilities podem então ser declaradas sem versão. O `platform-starter` mantém somente o baseline comum; `platform-schema-validation` é opt-in e deve ser declarado explicitamente pelo serviço que pretende validar payloads por JSON Schema:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform.library</groupId>
    <artifactId>platform-starter</artifactId>
</dependency>
```

Para habilitar Schema Validation:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform.library</groupId>
    <artifactId>platform-schema-validation</artifactId>
</dependency>
```

Ao declarar essa capability, o serviço deve possuir `JdbcTemplate` para a fonte JDBC default ou fornecer um `ResourceSchemaRepository` customizado; sem uma fonte, a aplicação falha no startup.

## Registry oficial

O registry oficial alvo da Foundation consolidada é:

`https://maven.pkg.github.com/brunobs/platform-libraries`

O antigo `platform-build` não pertence ao fluxo oficial alvo. Ele permanece apenas como legado até a publicação das novas coordenadas, a prova remota do consumidor e o fechamento documental do checkpoint.

## Qualidade

A validação do reactor é:

```bash
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify
```

O Verify #85 do `main`, run `35544899150`, validou o reactor após a migração para `br.com.portalmanager.platform`, incluindo os gates de namespace, higiene do repositório e metadata da release train.

O checkpoint exige também publicação remota em `br.com.portalmanager.platform:*:1.0.0` e validação de um consumidor com resolução remota; `mvn install` local não substitui essa evidência.

Consulte:

- `docs/foundation/FOUNDATION-ARCHITECTURE.md`;
- `docs/foundation/FOUNDATION-VERSIONING.md`;
- `docs/foundation/FOUNDATION-PUBLISHING.md`;
- `docs/foundation/FOUNDATION-CHECKPOINT.md`;
- `docs/adr/ADR-001-CONSOLIDATE-FOUNDATION-REPOSITORIES.md`;
- `docs/adr/ADR-002-PORTALMANAGER-NAMESPACE.md`.
