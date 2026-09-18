# Platform Tagging

Capability reutilizável para gerenciamento de tags por recurso.

## Modelo

A tabela `tags` isola tags por `owner_type + owner_id` e mantém a origem da tag em `MANUAL` ou `SYSTEM`.

A unicidade é garantida por:

`UK(owner_type, owner_id, name)`

## Reconcile

`TagManager.reconcile` recebe as tags manuais e as tags calculadas pelo sistema, normaliza os valores e substitui atomicamente o conjunto persistido do owner.

Regras:

- tags são normalizadas para minúsculas, espaços externos são removidos e espaços internos viram `-`;
- valores nulos ou vazios são ignorados;
- duplicidades são eliminadas;
- `MANUAL` prevalece sobre `SYSTEM` quando o nome normalizado coincide;
- quando o override manual deixa de existir, a tag `SYSTEM` volta a ser persistida se continuar sendo calculada;
- owners são extensíveis através de `TagOwnerType`; cada serviço define seus próprios tipos, como `ACCOUNT` e `APPLICATION`.

## Uso

```java
enum ResourceTagOwner implements TagOwnerType {
    ACCOUNT,
    APPLICATION;

    @Override
    public String value() {
        return name();
    }
}
```

```java
tagManager.reconcile(
        ResourceTagOwner.ACCOUNT,
        account.getId(),
        dto.tags(),
        systemTags
);
```
