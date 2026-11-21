# platform-audit

Módulo Maven de Platform Libraries. Contratos, configuração e exemplos estão no [guia oficial](https://github.com/BrunoBS/golden-platform/blob/docs/golden-platform-foundation/docs/libraries/modules/platform-audit.md).

- [POM e dependências](pom.xml)
- [Desenvolvimento e build do reactor](../../README.md)

Execute os comandos de build na raiz do repositório para incluir os módulos dependentes. Fixtures de integração exigem Docker; agregadores POM não são dependências JAR.

## Contrato do objeto de auditoria — formato 2

O arquivo JSON contém **somente o snapshot** (`AuditOutboxEntity.payload`), sem
wrapper `identifier/payload/metadata`. O identificador do recurso presente no
próprio snapshot é preservado. A chave usa `<key-prefix>/{service}/{resourceIdentifier}.json`, com uma pasta por serviço. Com os padrões do Workspace, um exemplo é
`platform-audit/workspace-service/6dbe06ab-cf43-40ab-9fb3-b48c080a87cc.json`.
O upload usa `OVERWRITE` com `Content-Type: application/json`: o bucket deve ter
versionamento habilitado para que cada evento preserve uma versão do snapshot.
O identificador do evento permanece nos metadados `audit-event-id`, independente
da chave do recurso e do versionId atribuído pelo S3.

Os metadados ficam no objeto, consultáveis por HEAD sem baixar o snapshot:

| Metadado do objeto | Conteúdo |
|---|---|
| `audit-format-version` | `2` |
| `audit-event-id` | Identificador da outbox/evento |
| `audit-sha256` | SHA-256 dos bytes efetivamente enviados como snapshot |
| `audit-outbox-created-at` | `createdAt` original da outbox, em ISO-8601 |
| `audit-metadata` | JSON compacto com os metadados de negócio originais |

No S3, esses campos aparecem como headers `x-amz-meta-*`. `audit-metadata` preserva
`service`, `resourceType`, `eventType`, `resourceIdentifier`, `accountIdentifier`,
`applicationIdentifier`, `environmentIdentifier`, `correlationId`, `username` e
`occurredAt` e `resourceVersion`, incluindo valores nulos.
Por padrão, `resourceVersion` é copiado do campo `version` do snapshot retornado pela
operação auditável. O mapeamento pode ser personalizado em `@Auditable.attributes`.
Zero é preservado. Atributos de origem inexistentes são omitidos; nulos explícitos
são preservados. `resourceVersion`, quando presente e não nulo, deve ser um inteiro
no intervalo de `Long`; valores inválidos são rejeitados antes de escrever a outbox. Essa versão é
independente do `versionId` S3 e do `audit-format-version`. Caracteres não ASCII são escapados como
JSON (`\uXXXX`); um parser JSON recupera o valor original. A outbox no banco
continua guardando payload e metadata separadamente, sem alteração de schema.

O produtor valida o limite de 2048 bytes para a soma UTF-8 das chaves e valores
([limite de metadados S3](https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingMetadata.html)).
Se excedido, não trunca nem envia o arquivo: mantém o fluxo de falha/retry da outbox.
A recuperação de upload com resposta perdida exige tamanho e todos os metadados
esperados iguais antes de remover a outbox.

Arquivos com as chaves antigas por evento não são renomeados. Novos uploads usam
a chave por serviço/recurso; snapshots sucessivos dessa chave são preservados
pelo versionamento do bucket. O consumidor deve consultar a versão indicada pela
notificação S3 para não confundir eventos de uma mesma chave.
Consumers que liam o wrapper antigo devem passar a obter os metadados por HEAD.

Para validar o contrato contra um LocalStack já em execução (bucket exclusivo,
removido pelo teste):

```sh
mvn --offline -pl modules/platform-audit \
  -Dtest=AuditOutboxHeaderLocalStackIT -Daudit.localstack.enabled=true test
```

## Mapeamento de atributos do snapshot

`@AuditAttribute.key` é o nome do atributo nos metadados da auditoria e `value` é o
nome exato de um campo no primeiro nível do snapshot. `@Auditable` usa estes defaults:

```java
AuditAttribute[] attributes() default {
    @AuditAttribute(key = "resourceIdentifier", value = "identifier"),
    @AuditAttribute(key = "resourceVersion", value = "version")
};
```

Um array explícito **substitui**, sem adicionar aos defaults:

```java
@Auditable(
    action = AuditAction.UPDATE,
    event = "WORKSPACE_UPDATED",
    resourceType = "WORKSPACE",
    attributes = {
        @AuditAttribute(key = "resourceIdentifier", value = "code"),
        @AuditAttribute(key = "resourceVersion", value = "revision"),
        @AuditAttribute(key = "resourceStatus", value = "lifecycle")
    }
)
```

Campos de origem inexistentes são ignorados. Campos presentes com valor nulo,
`false`, zero, arrays ou objetos preservam o valor e tipo JSON nos atributos
adicionais. `resourceIdentifier` mantém seu tipo textual e `resourceVersion` seu
tipo inteiro de 64 bits. O snapshot completo não é alterado. Os valores mapeados
ficam diretamente no JSON `audit-metadata`, ainda sujeito ao limite total de 2 KB.

A chave S3 depende de `resourceIdentifier`: esse atributo deve ser resolvido e não
vazio para capturar um evento enviável. Não há fallback implícito para `id`; para
esse caso, declare `@AuditAttribute(key = "resourceIdentifier", value = "id")`.
Chaves duplicadas, nomes vazios ou tentativas de sobrescrever campos de contexto
(`service`, `resourceType`, `eventType`, conta/aplicação/ambiente, correlationId,
username e occurredAt) são rejeitadas antes da gravação da outbox.
