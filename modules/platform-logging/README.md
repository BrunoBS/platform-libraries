# 📝 Módulo Platform Logging (`platform-logging`)

O **`platform-logging`** é uma biblioteca de infraestrutura corporativa desenvolvida para **Spring Boot 4.0.0 e Java 21+**. Ela fornece **padronização absoluta de observabilidade em formato JSON estruturado** com política de **configuração zero**. Os microsserviços não necessitam de arquivos físicos como `logback-spring.xml` ou anotações acopladas para adotar o padrão organizacional.

## 🚀 Como Ativar no Microsserviço

Basta adicionar a dependência diretamente no seu arquivo `pom.xml`:

```xml
<dependency>
    <groupId>br.com.portalmanager.core</groupId>
    <artifactId>platform-logging</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## 🛠️ Todos os Parâmetros Disponíveis (`application.yml`)

Abaixo estão listadas todas as propriedades de configuração expostas pela biblioteca sob os prefixos `platform.logging` e chaves nativas do Spring, divididas por categoria de responsabilidade:

```yaml
# ====================================================================
# CONFIGURAÇÕES DE METADADOS DA APLICAÇÃO (Enriquecimento de Painéis)
# ====================================================================
spring:
  application:
    name: "api-vendas-checkout"   # [Opcional] Nome do serviço exposto na raiz do JSON. Fallback: "unknown-service"

info:
  build:
    version: "@project.version@"  # [Opcional] Versão do Deploy (via filtro Maven). Fallback: "unknown"

# ====================================================================
# GOVERNANÇA E CONTROLE DE MASCARAMENTO DE DADOS (LGPD)
# ====================================================================
platform:
  logging:
    masking:
      enabled: true               # Liga/desliga o conversor automatizado de CPF, Cartão e E-mail. Padrão: true

# ====================================================================
# EXTENSIBILIDADE: REGISTRO DE NOVOS FILTROS CUSTOMIZADOS (CASCATA)
# ====================================================================
    custom-converters:            # Permite plugar infinitos conversores do Logback que rodam em cascata protegida
      pixMask: "com.empresa.projeto.converter.PixMaskConverter"
      rgMask: "com.empresa.projeto.converter.RgMaskConverter"

# ====================================================================
# CUSTOMIZAÇÃO DINÂMICA DE NÍVEIS DE LOG DO MICROSSERVIÇO
# ====================================================================
    levels:                       # Adiciona ou altera níveis para qualquer pacote sem limite de tamanho
      com.empresa.projeto.vendas: DEBUG
      com.empresa.projeto.utils: WARN
```

---

## 📊 Matriz de Prioridades de Nível de Log (`configureLogLevels`)

A biblioteca possui um mecanismo de **Silenciamento Agressivo de Infraestrutura** embutido de fábrica (via arquivo interno oculto `.properties`) [MDC]. No entanto, a prioridade de resolução do Spring é híbrida, garantindo total autonomia aos times:

| Pacote Alvo | Nível Padrão da Lib | Como Sobrescrever para `DEBUG` no `application.yml` |
| :--- | :---: | :--- |
| **`org.springframework`** | `ERROR` | `logging.level.org.springframework: DEBUG` *(Padrão Spring)* |
| **`org.hibernate`** | `ERROR` | `logging.level.org.hibernate: DEBUG` *(Padrão Spring)* |
| **`com.zaxxer.hikari`** | `ERROR` | `://level.com.zaxxer.hikari: DEBUG` *(Padrão Spring)* |
| **`org.apache.kafka`** | `ERROR` | `logging.level.org.apache.kafka: DEBUG` *(Padrão Spring)* |
| **Pacote do Time** *(Novo)* | `INFO` | `://levels.com.seu-pacote: DEBUG` *(Via Plataforma)* |

---

## 📝 Anatomia do Payload JSON de Saída (Console)

O log é impresso no console em uma **única linha limpa de texto JSON nativo**, perfeitamente compatível com ferramentas de coleta e agregação em nuvem (**Kibana/Elasticsearch, Grafana Loki ou Datadog**) [MDC]:

```json
{
  "timestamp": "2026-09-07T22:15:30.456Z",
  "level": "INFO",
  "thread": "http-nio-8080-exec-1",
  "logger": "com.empresa.projeto.vendas.controller.PedidoController",
  "message": "Processando criacao de pedido com sucesso!",
  "service": "api-vendas-checkout",
  "version": "1.0.4-RELEASE",
  "host": "pod-checkout-k8s-7f89b",
  "context": {
    "traceId": "trace-uuid-123456",
    "username": "bruno.barbosa",
    "accountId": "account-corporate-99",
    "environmentId": "production",
    "applicationId": "checkout-service",
    "clientIp": "192.168.1.100",
    "userAgent": "Mozilla/5.0...",
    "uri": "/api/v1/pedidos"
  }
}
```

### 🧠 Detalhes Importantes do Esquema:
1. **`message` (Filtro LGPD Automatizado):** Se `masking.enabled` for `true`, qualquer string de log enviada pelo desenvolvedor contendo padrões de dados sensíveis será ofuscada antes da renderização no console (ex: `***.***.***-**` para CPFs e `b****@empresa.com` para e-mails).
2. **`host` (Performance e Velocidade de Boot):** O campo tenta resolver o identificador dinâmico do container Linux/Docker direto da variável de ambiente `%property{HOSTNAME}` da máquina local em menos de 1ms, eliminando travamentos de rede provocados por consultas lentas de DNS (`DNS reverse lookup`).
3. **`context` (Automático via MDC):** Mapeia de forma polimórfica e automática **todas** as variáveis contextuais de auditoria que o módulo `platform-authorization` injetou no início do pipeline da requisição HTTP [MDC].
