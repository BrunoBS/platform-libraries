# Guia de uso do platform-audit

Este guia mostra como integrar a etapa atual do `platform-audit` em um serviço.

## O que esta etapa faz

O Aspect intercepta um Use Case anotado com `@Auditable`, captura o estado auditável e grava uma linha na tabela local `AUDIT_OUTBOX`. A gravação ocorre na mesma transação da alteração de negócio: se a regra de negócio ou a gravação da auditoria falhar, a transação inteira é revertida.

Nesta etapa a library **não publica em fila**, não executa worker e não envia dados para Object Storage. Ela deixa o evento persistido na outbox para processamento posterior.

A library não impõe limite de tamanho ao snapshot nem limite de quantidade de registros gerados por invocação.

## 1. Adicione a library ao serviço

Use o artefato gerenciado pelo BOM da plataforma, sem declarar uma versão própria:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform.library</groupId>
    <artifactId>platform-audit</artifactId>
</dependency>
```

Não é necessário adicionar dependências de H2, Jackson ou outra implementação de banco para usar a library. O serviço deve seguir as dependências já aprovadas e gerenciadas pelo padrão da plataforma.

## 2. Configure o nome do serviço

`platform.audit.enabled` fica habilitado por padrão quando a library está presente. Informe o nome lógico do serviço:

```yaml
platform:
  audit:
    enabled: true
    service-name: key-service
```

`service-name` é obrigatório quando a auditoria está habilitada e será gravado nos metadados do evento. Se a aplicação tiver a dependência, mas não for usar auditoria, defina `enabled: false`.

## 3. Crie a tabela na migration do serviço

A tabela pertence ao serviço consumidor. A library não executa uma migration global nem escolhe a versão da migration do serviço.

Use como referência o DDL MySQL em:

`src/main/resources/platform-audit/db/mysql/create-audit-outbox.sql`

Copie o conteúdo para uma migration Flyway com a próxima versão livre do serviço. A tabela contém `payload` e `metadata` como JSON e os campos técnicos da outbox. As colunas e os tipos precisam ser compatíveis com o DDL de referência; o nome físico da tabela pode ser escolhido pelo serviço.

## 4. Declare a entidade concreta e o repository

A library fornece o mapeamento comum como `@MappedSuperclass`. JPA precisa de uma entidade concreta para associar esse mapeamento à tabela do serviço. O serviço também declara uma especialização do repository genérico:

```java
import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxEntity;
import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxRepository;
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

Coloque ambos nos pacotes escaneados pelo JPA e pelo Spring Data. A entidade não precisa implementar setters nem lógica de persistência: a library instancia a classe concreta e grava o evento pelo repository.

Registre exatamente uma entidade concreta que estenda `AuditOutboxEntity` por unidade de persistência. A configuração atual procura essa entidade no metamodelo JPA e falha no startup se nenhuma ou mais de uma for encontrada.

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

Regras importantes:

- O método precisa ser chamado através do proxy Spring. Chamadas internas entre métodos da mesma classe não passam pelo Aspect.
- A chamada precisa ocorrer dentro de uma transação ativa. A library valida isso antes de executar a regra de negócio.
- O retorno deve ser um objeto JSON com o identificador do recurso no campo `identifier` ou `id`. Sem esse campo, o evento não pode ser associado ao recurso e a operação falha.
- Se o retorno for uma coleção, a library gera um evento por elemento. Não há limite de itens imposto pela library.
- `event` é o fato de negócio que será persistido como `eventType`; `resourceType` identifica o tipo do recurso. Use valores estáveis definidos pelo serviço.

## 6. Escolha a estratégia de captura

`AuditAction` define quando a library captura o snapshot; ela não é gravada nos metadados.

| Ação | Snapshot usado |
| --- | --- |
| `CREATE`, `UPDATE`, `ACTIVATE`, `DEACTIVATE`, `RESTORE`, `DELETE` | Retorno do Use Case, depois da execução |
| `PURGE` | Estado anterior à remoção física; requer `AuditBeforeSnapshotProvider` |
| `CUSTOM` | Não implementada; usar causa erro |

Para `PURGE`, registre um bean que implemente `AuditBeforeSnapshotProvider`. Ele recebe o método interceptado, os argumentos e a anotação e deve devolver o snapshot anterior como `JsonNode`. Essa implementação deve buscar o estado antes que o Use Case o remova. O snapshot ainda precisa conter `identifier` ou `id`.

## 7. Garanta o contexto de autorização

A library usa `UserContext` da plataforma para obter account, application, environment, username e trace/correlation id. Esse contexto precisa estar disponível durante a execução do método auditado. Sem ele, a auditoria falha com erro de contexto ausente.

## 8. Confira a integração

Antes de considerar o serviço integrado, confirme:

- A aplicação sobe com `platform.audit.enabled=true` e `service-name` preenchido.
- A migration cria a tabela e os tipos de coluna esperados.
- A entidade e o repository são encontrados por JPA/Spring Data.
- O Use Case auditado é chamado por um bean Spring e tem transação ativa.
- O retorno possui `identifier` ou `id`.
- A operação de negócio e a linha da outbox são confirmadas juntas.
- Uma falha ao persistir a outbox reverte também a operação de negócio.

## Erros comuns

| Sintoma | Causa provável |
| --- | --- |
| Startup informa que `service-name` é obrigatório | Falta `platform.audit.service-name` |
| Startup não encontra entidade de outbox | A entidade concreta não está no scan do JPA |
| Startup encontra múltiplas entidades de outbox | Há mais de uma entidade estendendo `AuditOutboxEntity` na mesma unidade de persistência |
| `transaction.required` | O Use Case não está sendo executado em uma transação ativa ou não foi chamado pelo proxy |
| `context.user.missing` | `UserContext` não está disponível durante a execução |
| `resource.identifier.missing` | O snapshot não tem `identifier` nem `id` preenchido |
| `before-snapshot.provider-required` | A ação é `PURGE`, mas nenhum provider está registrado |
| `custom-action.not-configured` | Foi usada a ação `CUSTOM`, ainda não suportada |

## Limite do escopo atual

A responsabilidade do serviço termina em disponibilizar a tabela, a entidade concreta e o repository. A library captura o snapshot, monta os metadados e grava a outbox transacionalmente. A publicação assíncrona e o processamento posterior da outbox não fazem parte desta etapa.
