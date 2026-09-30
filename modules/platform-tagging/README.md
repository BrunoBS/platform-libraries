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
