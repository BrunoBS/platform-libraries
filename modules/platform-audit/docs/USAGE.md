# Guia de uso do platform-audit

Este guia mostra como integrar a auditoria em um serviço Spring.

## Como funciona

Ao executar um Use Case anotado com `@Auditable`, a library captura o estado do recurso, monta os metadados e grava um registro em `AUDIT_OUTBOX`. Essa gravação participa da mesma transação da alteração de negócio: se a operação ou a persistência da auditoria falhar, ambas são revertidas.

O snapshot não tem limite de tamanho configurado pela library. Quando o Use Case retorna uma coleção, é gerado um registro para cada elemento.

## 1. Adicione a library ao serviço

Use o artefato gerenciado pelo BOM da plataforma, sem declarar uma versão própria:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform.library</groupId>
    <artifactId>platform-audit</artifactId>
</dependency>
```

## 2. Configure o nome do serviço

Informe o nome lógico que será registrado nos metadados:

```yaml
platform:
  audit:
    enabled: true
    service-name: key-service
```

A auditoria está habilitada por padrão quando a library está presente. `service-name` é obrigatório enquanto ela estiver habilitada. Se o serviço não usar auditoria, defina `enabled: false`.

## 3. Crie a tabela na migration do serviço

Crie uma migration Flyway usando como referência o DDL MySQL disponível em:

`src/main/resources/platform-audit/db/mysql/create-audit-outbox.sql`

A migration deve criar a tabela usada pela entidade abaixo, com as colunas e tipos compatíveis com esse DDL. O nome da tabela pode ser definido pelo serviço. As colunas `payload` e `metadata` usam o tipo JSON.

## 4. Declare a entidade e o repository

A library fornece o mapeamento JPA comum. O serviço associa esse mapeamento à própria tabela por meio de uma entidade concreta e declara um repository tipado:

```java
import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_outbox")
public class KeyAuditOutboxEntity extends AuditOutboxEntity {

    protected KeyAuditOutboxEntity() {
    }
}
```

```java
import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxRepository;

public interface KeyAuditOutboxRepository
        extends AuditOutboxRepository<KeyAuditOutboxEntity> {
}
```

Coloque as classes em pacotes escaneados pelo JPA e pelo Spring Data. Registre uma única entidade concreta de outbox por unidade de persistência.

## 5. Anote o Use Case

Coloque `@Auditable` no método público do Service/Use Case, junto da transação. Não coloque a anotação no Controller.

```java
import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Auditable(
        action = AuditAction.UPDATE,
        event = "KEY_UPDATED",
        resourceType = "KEY"
)
public KeyResponse execute(UpdateKeyInput input) {
    Key key = keyRepository.getRequired(input.identifier());
    key.update(input);
    return KeyResponse.from(key);
}
```

O método precisa ser chamado por um bean Spring. Chamadas internas entre métodos da mesma classe não passam pelo proxy e não acionam o Aspect. O retorno deve ser um objeto JSON com o identificador do recurso no campo `identifier` ou `id`. Sem esse identificador, a auditoria falha.

`event` é o nome do fato de negócio e será gravado como `eventType`. `resourceType` identifica o tipo do recurso. Use nomes estáveis definidos pelo serviço.

## 6. Escolha a ação e o snapshot

| Ação | Estado registrado |
| --- | --- |
| `CREATE`, `UPDATE`, `ACTIVATE`, `DEACTIVATE`, `RESTORE`, `DELETE` | Retorno do Use Case, depois da execução |
| `PURGE` | Estado anterior à remoção física; requer um `AuditBeforeSnapshotProvider` |

Para `PURGE`, registre um bean que implemente `AuditBeforeSnapshotProvider`. Ele recebe o método chamado, os argumentos e a anotação; deve buscar e devolver o estado salvo antes da remoção, como `JsonNode`. Esse snapshot também precisa conter `identifier` ou `id`.

No exemplo abaixo, o Use Case recebe um único `PurgeKeyInput`; o provider usa esse argumento para buscar o recurso antes da remoção e transforma um DTO de auditoria em `JsonNode`:

```java
import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.outbox.AuditBeforeSnapshotProvider;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;

@Component
public class KeyBeforeSnapshotProvider implements AuditBeforeSnapshotProvider {

    private final KeyRepository keyRepository;
    private final ObjectMapper objectMapper;

    public KeyBeforeSnapshotProvider(KeyRepository keyRepository, ObjectMapper objectMapper) {
        this.keyRepository = keyRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode capture(Method method, Object[] arguments, Auditable auditable) {
        if (arguments.length == 0 || !(arguments[0] instanceof PurgeKeyInput input)) {
            throw new IllegalArgumentException("PURGE requires a PurgeKeyInput argument");
        }

        Key key = keyRepository.findByIdentifier(input.identifier()).orElseThrow();
        return objectMapper.valueToTree(KeyAuditSnapshot.from(key));
    }
}
```

`KeyRepository`, `PurgeKeyInput` e `KeyAuditSnapshot` são tipos do serviço consumidor. Ajuste a leitura dos argumentos e a consulta ao repositório ao contrato do seu Use Case. O provider é chamado antes da execução do método anotado.

Se o retorno do Use Case for uma coleção, cada elemento gera um registro de auditoria.

## 7. Disponibilize o contexto de autorização

Durante a execução, a library lê o `UserContext` da plataforma para preencher account, application, environment, username e trace/correlation id. O contexto de autorização precisa estar disponível durante a chamada auditada.

## 8. Valide a integração

Confirme que:

- A aplicação inicia com `service-name` preenchido.
- A migration cria a tabela com as colunas e tipos necessários.
- A entidade e o repository são encontrados pelo JPA/Spring Data.
- O Use Case é chamado por um bean Spring dentro de uma transação ativa.
- O objeto retornado contém `identifier` ou `id`.
- A operação de negócio e o registro da auditoria são confirmados juntos.

## Erros comuns

| Sintoma | Causa provável |
| --- | --- |
| `service-name` obrigatório | Falta `platform.audit.service-name` |
| Entidade de outbox não encontrada | A entidade concreta não está nos pacotes escaneados pelo JPA |
| Mais de uma entidade de outbox encontrada | Há mais de uma entidade que estende `AuditOutboxEntity` na mesma unidade de persistência |
| Repository de outbox incompatível | O repository injetado não gerencia a entidade concreta de outbox registrada no JPA; declare o repository tipado para essa entidade |
| `transaction.required` | A chamada não está dentro de uma transação ativa ou não passou pelo proxy Spring |
| `context.user.missing` | `UserContext` não está disponível durante a execução |
| `resource.identifier.missing` | O snapshot não tem `identifier` nem `id` preenchido |
| `before-snapshot.provider-required` | A ação é `PURGE` e não há um `AuditBeforeSnapshotProvider` registrado |
