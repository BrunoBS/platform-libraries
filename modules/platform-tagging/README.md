# Platform Tagging

Motor reutilizável de tagging, independente de JPA, tabela e tipo de recurso.

## Responsabilidade

O módulo concentra a regra transversal:

- normalização;
- reconciliação por diff;
- precedência de tag `MANUAL` sobre `SYSTEM`;
- preservação do registro quando muda apenas a origem;
- leitura de tags manuais;
- busca de owners por tag;
- remoção das tags de um owner.

A persistência pertence ao serviço consumidor. A library não cria entidade `Tag`, não registra entidade JPA e não assume uma tabela genérica `tags`.

## Contratos

O recurso implementa `TagRecord` em sua entidade de tag e expõe sua persistência através de `TagPersistence<T, O, ID, KEY>`.

Isso permite tabelas físicas e FKs reais por recurso, por exemplo:

- `workspace_tag.workspace_id -> workspaces.id`;
- `application_tag.application_id -> applications.id`.

O serviço cria um `TagManager` tipado para cada recurso:

```java
@Bean
TagManager<ApplicationTag, Application, Long, String> applicationTags(
        ApplicationTagRepository repository) {
    return new TagManager<>(repository);
}
```

As system tags continuam sendo responsabilidade do domínio do recurso. O motor recebe apenas as coleções manual/system e não conhece Application, Workspace ou qualquer outro domínio.


## Contrato Golden

A fronteira pública pode receber tags como `String`, mas o motor converte imediatamente cada valor para o value object `TagName`. A partir desse ponto, comparação, precedência e diff trabalham somente com nomes normalizados.

A normalização aplica `trim`, lowercase com `Locale.ROOT` e converte sequências de espaços em `-`. Valores nulos ou em branco são ignorados nas coleções de entrada. `TagName` não aceita valor vazio.

A persistência possui invariantes simples:

- nomes devolvidos por `TagPersistence` devem estar normalizados;
- deve existir no máximo uma tag por `(owner, name)`; o banco consumidor deve proteger essa regra com constraint única;
- `findByOwnerKeysAndOrigin` deve respeitar os owners solicitados;
- `newTag` recebe sempre o nome já normalizado;
- `MANUAL` possui precedência sobre `SYSTEM` quando a mesma tag aparece nas duas entradas.

O motor valida inconsistências de nomes e duplicidade no estado corrente em vez de corrigi-las silenciosamente.

As consultas de tags manuais retornam nomes em ordem determinística. `findManualByOwnerKeys` mantém apenas as keys solicitadas, incluindo owners sem tags com lista vazia. A busca `findOwnerKeysByTag` continua sendo igualdade exata após normalização; buscas por prefixo, contains ou LIKE não fazem parte desse contrato.

`TagManager` permanece o orquestrador pequeno do motor: normaliza a entrada, calcula o diff, aplica precedência e delega persistência. A library não cria auto-configuration Spring e não conhece JPA, JDBC, tabelas ou entidades dos serviços consumidores.
