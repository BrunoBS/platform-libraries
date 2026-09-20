# Platform Libraries — Golden Platform Foundation

O `platform-libraries` é o repositório consolidado da Foundation da Golden Platform.

## Estrutura

```text
platform-libraries/
├── platform-parent/           -> governança de build
├── platform-dependencies/     -> BOM tecnológico externo
├── platform-libraries-bom/    -> BOM das capabilities
├── platform-starter/
├── platform-logging/
├── platform-messaging/
├── platform-authorization/
├── platform-audit/
├── platform-catalog/
├── platform-tagging/
└── platform-test-support/
```

O POM raiz é somente reactor/aggregator.

`platform-crud` foi removido da Foundation e não deve ser recriado.

## Responsabilidades Maven

`platform-parent` define Java 25, Maven mínimo, plugins, Enforcer, dependency convergence e regras comuns de build. Ele importa o `platform-dependencies`, mas não conhece versões de capabilities.

`platform-dependencies` gerencia somente o baseline tecnológico externo, incluindo Spring Boot 4.1.1 e dependências de teste/infraestrutura compartilhadas.

`platform-libraries-bom` gerencia as versões das capabilities da plataforma.

## Dependências intencionais entre capabilities

```text
platform-authorization -> platform-messaging
platform-audit         -> platform-authorization
platform-audit         -> platform-messaging
platform-catalog       -> platform-messaging

platform-starter
├── platform-logging
├── platform-messaging
└── platform-authorization
```

`platform-test-support` possui integrações opcionais e não deve introduzir JDBC/MySQL/Kafka/Testcontainers transitivamente quando essas capacidades não forem declaradas.

## Versionamento

Os eixos são independentes:

```text
platform-parent          -> baseline de build
platform-dependencies    -> baseline tecnológico
platform-libraries-bom   -> baseline das capabilities
```

As capabilities seguem inicialmente um release train coerente para reduzir a matriz de compatibilidade.

Na consolidação atual estão preparados:

- `platform-dependencies:1.0.3`;
- `platform-parent:1.1.0`;
- `platform-libraries-bom:1.1.0`;
- capabilities `1.1.0`.

Essas versões só serão consideradas publicadas após o checkpoint remoto documentado em `docs/foundation/FOUNDATION-CHECKPOINT.md`.

## Consumo alvo

Após a publicação consolidada, um serviço consumidor deverá usar o parent separadamente do BOM das capabilities:

```xml
<parent>
    <groupId>br.com.portalmanager.core</groupId>
    <artifactId>platform-parent</artifactId>
    <version>1.1.0</version>
    <relativePath/>
</parent>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>br.com.portalmanager.core</groupId>
            <artifactId>platform-libraries-bom</artifactId>
            <version>1.1.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Capabilities podem então ser declaradas sem versão:

```xml
<dependency>
    <groupId>br.com.portalmanager.core</groupId>
    <artifactId>platform-starter</artifactId>
</dependency>
```

## Registry alvo

O único registry oficial após o fechamento do checkpoint será:

`https://maven.pkg.github.com/brunobs/platform-libraries`

O repositório `platform-build` permanece temporariamente necessário apenas enquanto a migração de packages Maven não for concluída.

## Qualidade

A validação do reactor é:

```bash
mvn --settings .github/maven-settings.xml clean verify
```

O checkpoint exige também publicação remota e validação de um consumidor limpo; `mvn install` local não substitui essa evidência.
