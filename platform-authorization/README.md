# 🛡️ Módulo Platform Authorization (`platform-authorization`)

O **`platform-authorization`** é o módulo corporativo de governança de segurança, controle de acesso e gerenciamento de contexto de usuário, desenvolvido para **Spring Boot 4.0.0 e Java 21+**. Ele gerencia o pipeline de interceptação HTTP, validação de tokens e isolamento de escopo de execução em Threads seguras.

---

## 🚀 Como Ativar no Microsserviço

Para habilitar a camada de segurança e contexto unificado, adicione a dependência diretamente no seu arquivo `pom.xml`:

```xml
<dependency>
    <groupId>com.empresa.platform</groupId>
    <artifactId>platform-authorization</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## 🛠️ Todos os Parâmetros Disponíveis (`application.yml`)

Abaixo estão listadas as chaves de controle sob o prefixo `platform.authorization` que comandam o comportamento do motor de segurança:

```yaml
platform:
  authorization:
    enabled: true                 # Liga/desliga o motor real de segurança. Padrão: true
    service-url: "https://empresa.com" # URL do servidor central de Identidade (obrigatório se enabled=true)
```

---

## 💎 Funcionalidades Core

### 1. 🔑 Anotação Declarativa de Escopo (`@AuthorizationRequired`)
Permite aos desenvolvedores proteger métodos de controladores (`@RestController`) injetando regras de nível de acesso diretamente sobre o método:
```java
@GetMapping("/faturamento")
@AuthorizationRequired(level = AuthorizationLevel.RESTRICTED)
public ResponseEntity<Dados> buscarDados() { ... }
```

### 🧵 2. Isolamento de Threads com `UserContext`
Assim que o token do usuário é validado no pipeline do Tomcat, a biblioteca popula o `UserContext`, que utiliza um **`ThreadLocal`** encapsulado de forma segura em uma API de `Optional<UserSession>`.
* **Prevenção de Memory Leaks:** A limpeza do contexto ocorre de forma automática e obrigatória através do método `afterCompletion` do interceptor, garantindo que os dados da requisição anterior sejam expurgados assim que a rota HTTP finaliza.

### 🎭 3. Modo Híbrido Local (Mock Guest)
Criado com foco na **Experiência do Desenvolvedor (DevEx)**. Quando a flag `platform.authorization.enabled` é definida como `false` em ambiente de desenvolvimento local, a autoconfiguração inteligente desativa o cliente HTTP real de segurança e injeta um interceptor alternativo que simula uma sessão real do tipo **`GUEST`**.
* **Benefício:** O desenvolvedor consegue testar a API na máquina local sem a necessidade de obter tokens reais ou de que o servidor de identidade centralizado da empresa esteja online.

### 🔄 4. Resiliência Nativa com `RestClient` e Spring Retry
A comunicação de validação de tokens contra o servidor centralizado foi desenhada utilizando o moderno **`RestClient`** do Spring Boot 4, acoplado a políticas de reativação automatizada com *exponential backoff* do **Spring Retry** para mitigar oscilações ou quedas parciais de rede.

---

## 📋 Comportamento das Flags e Componentes

| Propriedade `enabled` | Interceptor Carregado | Comportamento do `UserContext` | Chamada de Rede Externa |
| :---: | :---: | :--- | :---: |
| **`true`** *(Padrão)* | `AuthorizationInterceptor` | Populado com dados reais do Token Bearer | **Sim** (Ativa o `RestClient`) |
| **`false`** | *Anônimo interno do Spring* | Populado automaticamente com o usuário `"guest"` | **Não** (Totalmente offline) |

---