# platform-messaging

Biblioteca Spring Boot para centralizar mensagens e erros de APIs.

Uso:
```java
throw new ApiException("user.not.found");
```

Com parâmetros:
```java
throw new ApiException("user.not.found", Map.of("userId", userId));
```

A aplicação usa `messageKey`; o código técnico (`ERR-0001`) fica no catálogo.

A biblioteca lê somente uma VIEW existente no banco do consumidor. Contrato:
`code`, `message_key`, `locale`, `message`, `solution`, `http_status`.

Redis é opcional e somente cache. Sem Redis, consulta a VIEW diretamente.

Fallback de locale: `en-US -> en -> pt-BR`.

A biblioteca não possui migrations e não inclui driver MySQL; usa o DataSource/JdbcTemplate do consumidor.

Java 21.


# ✉️ Módulo Platform Messaging (`platform-messaging`)

O **`platform-messaging`** é o módulo fundacional de governança de mensagens, tratamento de exceções e segurança de dados do nosso ecossistema corporativo, desenvolvido para **Spring Boot 4.0.0 e Java 21+**. Ele centraliza o pipeline de tratamento de erros HTTP, tradução de payloads internacionais e proteção proativa contra ataques de injeção de dados.

---

## 🚀 Como Ativar no Microsserviço

Para habilitar o catálogo de mensagens corporativas, adicione a dependência diretamente no seu arquivo `pom.xml`:

```xml
<dependency>
    <groupId>com.empresa.platform</groupId>
    <artifactId>platform-messaging</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## 🛠️ Todos os Parâmetros Disponíveis (`application.yml`)

As propriedades abaixo controlam o comportamento global de tradução de chaves e os parâmetros de auditoria unificada entre os módulos:

```yaml
platform:
  messaging:
    enabled: true                 # Liga/desliga o Advice de exceções e filtros globais. Padrão: true
    default-locale: "pt-BR"       # Localidade padrão de fallback para internacionalização (i18n). Padrão: "pt-BR"
    mdc-correlation-key: "traceId" # Chave unificada que conecta este módulo ao platform-logging. Padrão: "traceId"
```

---

## 💎 Funcionalidades Core

### 1. 🎯 Handler de Exceções Global (`ControllerAdvice`)
Captura todas as falhas de infraestrutura e de negócio lançadas na Thread da requisição, padronizando a saída para o cliente final. Evita que *stack traces* internas vazem para o front-end, transformando-as no payload unificado `ApiError`.

### 🌍 2. Catálogo e Internacionalização (i18n)
O módulo possui um barramento de tradução dinâmico que lê o cabeçalho HTTP `Accept-Language`. Ele converte chaves abstratas de mensagens em textos amigáveis de forma automatizada.
* **Fallbacks integrados:** Caso o microsserviço ou a biblioteca não possuam tradução para um idioma solicitado (ex: `fr-FR`), o sistema aplica uma cascata inteligente recorrendo ao `default-locale` configurado no YAML.

### 🛡️ 3. Filtro Preventivo de Dados (Anti-SQL Injection)
Um filtro de servlet intercepta os parâmetros de URL, cabeçalhos e payloads de entrada da requisição antes que eles cheguem às camadas de banco de dados (`JPA/Hibernate`), aplicando regras de limpeza estruturada contra caracteres de escape e tentativas de injeção maliciosa.

---

## 📝 Anatomia do Payload de Erro Padronizado (`ApiError`)

Quando uma exceção é interceptada pela biblioteca (ex: um erro `ForbiddenException` ou `UnauthorizedException`), o payload devolvido no corpo da resposta HTTP segue estritamente este contrato:

```json
{
  "timestamp": "2026-09-07T22:21:40.123Z",
  "status": 403,
  "error": "Forbidden",
  "messageKey": "PLATFORM_ACCESS_DENIED",
  "message": "Acesso negado. Seu usuário não possui nível de autorização suficiente para este recurso.",
  "path": "/api/v1/pedidos",
  "traceId": "trace-uuid-123456"
}
```

### 🔍 Destaques do Contrato de Falha:
1. **`messageKey`:** Uma chave de erro abstrata e imutável de TI. Ela permite que sistemas integrados ou aplicativos de front-end tomem decisões de fluxo automatizadas com base no código do erro, independentemente do idioma traduzido no campo `message`.
2. **`traceId` (Integração Nativa com `platform-logging`):** O ID de correlação capturado é exatamente a chave definida em `mdc-correlation-key` [MDC]. Se o cliente reportar o erro ao suporte informando este número, o engenheiro conseguirá localizar o histórico completo da falha indexado no Kibana ou Datadog em um clique [MDC].

---
