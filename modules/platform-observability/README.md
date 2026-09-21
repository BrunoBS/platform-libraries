# Platform Observability

O `platform-observability` é a capability de observabilidade da Golden Platform.

No baseline atual ele implementa **logging estruturado**. Métricas e tracing não fazem parte do módulo enquanto não houver caso real aprovado.

## Dependência

```xml
<dependency>
    <groupId>br.com.portalmanager.platform</groupId>
    <artifactId>platform-observability</artifactId>
</dependency>
```

O `platform-starter` já inclui essa capability.

## Package

```text
br.com.portalmanager.platform.observability.logging
```

## Capacidades atuais

- saída JSON estruturada no console;
- masking de dados sensíveis;
- metadata de serviço/versão/host;
- MDC compartilhado com o contexto preenchido pela plataforma;
- níveis default de infraestrutura;
- níveis customizados por aplicação;
- conversores customizados do Logback.

## Configuração

```yaml
spring:
  application:
    name: account-service

platform:
  observability:
    logging:
      masking:
        enabled: true
      levels:
        br.com.portalmanager.account: INFO
      custom-converters: {}
```

Overrides nativos do Spring continuam válidos:

```yaml
logging:
  level:
    org.springframework: DEBUG
```

## Defaults

Os defaults internos são carregados de:

```text
platform-observability-defaults.properties
```

O prefixo oficial é:

```text
platform.observability.logging
```

## Limite atual

O nome `observability` define a fronteira da capability, não uma promessa de funcionalidades ainda inexistentes. O baseline atual fornece logging; metrics/tracing só serão adicionados por decisão arquitetural futura.
