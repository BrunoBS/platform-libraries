# platform-audit

Módulo Maven de Platform Libraries. Contratos, configuração e exemplos estão no [guia oficial](https://github.com/BrunoBS/golden-platform/blob/docs/golden-platform-foundation/docs/libraries/modules/platform-audit.md).

- [POM e dependências](pom.xml)
- [Desenvolvimento e build do reactor](../../README.md)

Execute os comandos de build na raiz do repositório para incluir os módulos dependentes. Fixtures de integração exigem Docker; agregadores POM não são dependências JAR.

## Contrato do objeto de auditoria — formato 2

O arquivo JSON contém **somente o snapshot** (`AuditOutboxEntity.payload`), sem
wrapper `identifier/payload/metadata`. O identificador do recurso presente no
próprio snapshot é preservado. A chave continua `<key-prefix>/<event-id>.json` e
o upload continua `CREATE_ONLY` com `Content-Type: application/json`.

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
`resourceVersion` é copiado do campo `version` do snapshot retornado pela operação
auditável, tanto na criação quanto na atualização. Zero é preservado. Se `version`
estiver ausente ou nulo, `resourceVersion` será nulo; valores não inteiros ou fora
do intervalo de `Long` são rejeitados antes de escrever a outbox. Essa versão é
independente do `versionId` S3 e do `audit-format-version`. Caracteres não ASCII são escapados como
JSON (`\uXXXX`); um parser JSON recupera o valor original. A outbox no banco
continua guardando payload e metadata separadamente, sem alteração de schema.

O produtor valida o limite de 2048 bytes para a soma UTF-8 das chaves e valores
([limite de metadados S3](https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingMetadata.html)).
Se excedido, não trunca nem envia o arquivo: mantém o fluxo de falha/retry da outbox.
A recuperação de upload com resposta perdida exige tamanho e todos os metadados
esperados iguais antes de remover a outbox.

Arquivos já existentes no formato antigo não são regravados. O novo formato se
aplica aos novos uploads; um objeto antigo com a mesma chave não é sobrescrito.
Consumers que liam o wrapper antigo devem passar a obter os metadados por HEAD.

Para validar o contrato contra um LocalStack já em execução (bucket exclusivo,
removido pelo teste):

```sh
mvn --offline -pl modules/platform-audit \
  -Dtest=AuditOutboxHeaderLocalStackIT -Daudit.localstack.enabled=true test
```
