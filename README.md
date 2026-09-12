# Platform Libraries (Platform Engineering Core)

O **`platform-libraries`** é o ecossistema central de bibliotecas e *starters* reutilizáveis da nossa plataforma corporativa. Desenvolvido sob os pilares de **Platform Engineering**, o projeto abstrai complexidades de infraestrutura, segurança, auditoria e mensageria, fornecendo para os times de desenvolvimento uma experiência de **Configuração Zero (Plug-and-Play)**.

---

## 🏗️ Arquitetura Multimódulos (Maven Reactor)

O projeto é estruturado como um reator multimódulos homogêneo, garantindo o alinhamento estrito de versões, releases unificadas e alta performance de classpath:

```text
platform-libraries/ (POM agregador - versões internas; build herdado do platform-parent)
├── platform-messaging/      -> Catálogo de Mensagens, Exceções Globais e Anti-SQL Injection
├── platform-authorization/  -> Motor de Segurança Híbrido, Interceptação HTTP e Mock de Ambientes
├── platform-logging/        -> Padronização Cloud-Native de Logs Estruturados em JSON (Logstash)\n└── platform-test-support/   -> Suporte reutilizável para testes unitários e de integração
```

---

## 📦 Detalhamento dos Módulos

### 1. 🛡️ Platform Authorization (`platform-authorization`)
Motor de segurança e governança de contexto para microsserviços.
* **Interceptação Inteligente:** Extração e validação automática de cabeçalhos (`Authorization` Bearer, `traceId`) e mapeamento dinâmico de permissões via `@AuthorizationRequired`.
* **Segurança de Threads:** Isolamento total do contexto do usuário logado via `UserContext` (`ThreadLocal`), blindado contra *memory leaks* no pipeline do Tomcat.
* **Ambiente Híbrido Local (Mock Guest):** Quando desativado em ambientes de testes locais (`platform.authorization.enabled=false`), injeta automaticamente um perfil simulado de `guest`, eliminando a necessidade de subir o servidor de identidade centralizado localmente.
* **Resiliência:** Cliente HTTP reescrito utilizando o moderno `RestClient` do Spring Boot 4 integrado ao **Spring Retry** com políticas de *exponential backoff*.

### 📊 2. 📝 Platform Logging (`platform-logging`)
Centralização e padronização absoluta de observabilidade corporativa **100% transparente**.
* **Zero Configuração:** Os microsserviços não precisam de arquivos `logback-spring.xml`. A ativação ocorre via código nativo no instante zero do boot (`ApplicationContextInitializer`).
* **Logs Estruturados JSON:** Saída padrão em linha única em conformidade com o **Logstash Encoder**, injetando chaves ricas na raiz do JSON (`service`, `version`, `host`) prontas para indexação no Kibana, Grafana Loki ou Datadog.
* **Correlação MDC:** Envelopamento automático de todo o contexto injetado pela segurança (`traceId`, `username`, `accountId`, `uri`) no bloco `"context"` de cada linha de log.
* **Silenciamento Agressivo Dinâmico:** Limpa o console de produção atenuando ruídos de frameworks (`Spring`, `Hibernate`, `Kafka`), mantendo total autonomia de sobrescritas locais e suporte a pacotes customizados via `application.yml`.

### ✉️ 3. Platform Messaging (`platform-messaging`)
Módulo fundacional de tratamento de mensagens e tratamento global de erros.
* **Catálogo Resiliente:** Tradução automática e centralizada de mensagens de erro.
* **Segurança Preventiva:** Validação contra ataques de injeção de SQL diretamente nas requisições.
* **Handler de Exceções Global:** Captura de falhas e exposição padronizada de payloads de erro (`ApiError`) para o cliente final.

---

## 🚀 Como Utilizar (Guia do Desenvolvedor)

Toda a suite de bibliotecas foi desenhada sob o conceito de **Convenção sobre Configuração**. Para ativar os recursos em um microsserviço, basta seguir dois passos:

### 1. Adicionar as Dependências no seu `pom.xml`
Se o seu microsserviço consome a suite inteira, basta importar os artefatos (as versões são herdadas automaticamente caso utilize a gestão corporativa):

```xml
<dependencies>
    <dependency>
        <groupId>com.empresa.platform</groupId>
        <artifactId>platform-logging</artifactId>
    </dependency>
    <dependency>
        <groupId>com.empresa.platform</groupId>
        <artifactId>platform-authorization</artifactId>
    </dependency>
</dependencies>
```

### 2. Configurar o seu `application.yml`
Configure o comportamento das plataformas de forma simplificada:

```yaml
spring:
  application:
    name: "api-vendas-checkout" # Injetado automaticamente nos metadados do JSON de log

info:
  build:
    version: "@project.version@" # Versionamento semântico automatizado via Maven

platform:
  messaging:
    enabled: true
    mdc-correlation-key: "traceId" # Chave unificada de rastreabilidade
  
  authorization:
    enabled: true # Em ambiente local, altere para false para ativar o modo "Guest Mock"
    service-url: "https://empresa.com"
  
  logging:
    levels:
      com.novaequipe.vendas: DEBUG # Adiciona ou altera o comportamento de pacotes dinamicamente
```

---

## 🧪 Qualidade de Código e Testes

O projeto possui **cobertura rigorosa de testes unitários e de integração de contexto (superior a 99%)**, validada de forma estrita no Java 25 utilizando o `ApplicationContextRunner` do Spring e isolamento térmico de memória de Threads (`ThreadLocal`).

Para rodar a suíte completa de testes locais e extrair os relatórios do JaCoCo, execute na pasta raiz:

```bash
mvn clean verify
```

---

## 🛠️ Tecnologias Utilizadas

* **Java 25** (Suporte nativo a *Virtual Threads* e APIs modernas de Record)
* **Spring Boot 4.1.1**
* **Logstash Logback Encoder 9.0**
* **Retry nativo do Spring Framework 7**
* **JUnit 5 / Mockito**
