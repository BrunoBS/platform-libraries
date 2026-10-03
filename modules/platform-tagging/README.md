# Platform Tagging

Motor reutilizável de tags, independente de Spring, JPA, tabela e domínio consumidor.

## Modelo simples

O módulo possui quatro conceitos:

- `TagManager`: reconcilia e consulta tags;
- `Tag`: contrato mínimo implementado pela entidade do serviço;
- `TagName`: nome válido e normalizado da tag;
- `TagPersistence`: porta de persistência implementada pelo serviço.

`TagOriginType` identifica a origem como `MANUAL` ou `SYSTEM`.

## Normalização

A API do `TagManager` aceita `String` na fronteira para facilitar requests e use cases. A String é convertida imediatamente para `TagName`. A partir daí, o motor e a persistência trabalham com `TagName`.

`TagName` aplica `trim`, lowercase com `Locale.ROOT` e converte sequências de espaços em `-`. O value object não aceita valor nulo ou vazio.

Valores nulos ou em branco dentro das coleções recebidas por `reconcile` são ignorados.

## Regras

- `MANUAL` prevalece sobre `SYSTEM` quando a mesma tag aparece nas duas coleções;
- a reconciliação preserva o registro quando muda apenas a origem;
- deve existir no máximo uma tag por `(owner, name)`;
- o banco do serviço consumidor deve proteger `(owner_id, name)` com constraint única;
- consultas de tags manuais retornam nomes em ordem determinística;
- `findManualByOwnerKeys` retorna somente owners solicitados e inclui lista vazia para owner sem tag;
- `findOwnerKeysByTag` faz igualdade exata após a normalização;
- busca por prefixo, contains ou LIKE não pertence a esse contrato.

Erros como owner sem ID, tag persistida sem nome ou estado persistido duplicado são violações do contrato de programação, por isso o módulo usa exceções Java e não mensagens de negócio.

## Implementação no serviço

Exemplo mínimo:

```java
final class ApplicationTag implements Tag {

    private final Application application;
    private final TagName name;
    private TagOriginType originType;

    ApplicationTag(Application application, TagName name, TagOriginType originType) {
        this.application = application;
        this.name = name;
        this.originType = originType;
    }

    @Override
    public TagName getName() {
        return name;
    }

    @Override
    public TagOriginType getOriginType() {
        return originType;
    }

    @Override
    public void changeOrigin(TagOriginType originType) {
        this.originType = originType;
    }
}
```

A persistência do recurso implementa a porta com tipos explícitos:

```java
final class ApplicationTagPersistence
        implements TagPersistence<ApplicationTag, Application, Long, String> {

    // ownerId, ownerKey, newTag e operações de persistência
}
```

E o serviço monta o manager:

```java
@Bean
TagManager<ApplicationTag, Application, Long, String> applicationTags(
        ApplicationTagPersistence persistence) {
    return new TagManager<>(persistence);
}
```

Os parâmetros genéricos representam, nesta ordem:

```text
TAG       = entidade de tag do recurso
OWNER     = recurso proprietário da tag
OWNER_ID  = identificador interno usado na persistência
OWNER_KEY = chave usada nas consultas agrupadas
```

A library não cria entidade JPA, tabela, repository Spring ou auto-configuration. Cada serviço mantém sua tabela e suas FKs reais, por exemplo `workspace_tag.workspace_id -> workspaces.id` e `application_tag.application_id -> applications.id`.
