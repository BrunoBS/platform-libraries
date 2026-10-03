# Platform Tagging

Tagging reutilizável com Spring Data JPA e uma tabela própria por recurso.

## Regra de simplicidade

Para adicionar tags a um novo recurso, o consumidor implementa somente:

1. o owner implementa `TagOwner` expondo o `Long id` interno e o `String identifier` público;
2. uma entidade `ResourceTag extends Tag<Resource>` com a associação JPA chamada `owner`;
3. um repository vazio: `ResourceTagRepository extends TagRepository<ResourceTag, Resource>`;
4. um bean `TagManager` usando o construtor da entidade como factory.

Todo o restante fica no `platform-tagging`: id da tag, nome, origem, normalização, conversão JPA, reconciliação, persistência das mudanças, consultas comuns e exclusão.

## Entidade mínima

```java
@Entity
@Table(name = "workspace_tag")
public class WorkspaceTag extends Tag<Workspace> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace owner;

    protected WorkspaceTag() {
    }

    public WorkspaceTag(Workspace owner, TagName name, TagOriginType originType) {
        super(name, originType);
        this.owner = owner;
    }

    @Override
    public Workspace getOwner() {
        return owner;
    }
}
```

O nome Java da associação é, por contrato, `owner`. A coluna física continua específica do recurso (`workspace_id`, `application_id`, etc.). Essa convenção única permite que o repository consumidor não tenha nenhuma query.

## Repository mínimo

```java
@Repository
public interface WorkspaceTagRepository
        extends TagRepository<WorkspaceTag, Workspace> {
}
```

## Manager

```java
@Bean
TagManager<WorkspaceTag, Workspace> workspaceTags(WorkspaceTagRepository repository) {
    return new TagManager<>(repository, WorkspaceTag::new);
}
```

## Integridade

- `TagName` normaliza trim, lowercase e espaços para hífen;
- nome vazio/nulo é inválido;
- o limite de 150 caracteres é validado antes de chegar ao banco;
- `TagNameConverter` persiste o value object em VARCHAR;
- `TagOriginType` é obrigatório e aceita `MANUAL` ou `SYSTEM`;
- `MANUAL` prevalece sobre `SYSTEM`;
- mudança de origem é salva explicitamente, sem depender de dirty checking;
- o banco consumidor deve possuir `UNIQUE(owner_id, name)`;
- a FK para o owner deve ser obrigatória;
- consultas por tag são exatas após normalização;
- o owner precisa estar previamente persistido e possuir `id` e `identifier` válidos;
- a factory não pode devolver `null`;
- command services devem executar `reconcile` dentro da transação da operação de negócio;
- operações de Tagging não limpam o persistence context do consumidor; o ciclo de vida da transação permanece sob responsabilidade do serviço.

## Segurança e escopo

`platform-tagging` gerencia tags; ele não decide autorização nem visibilidade do recurso. Métodos como `findOwnerKeysByTag` podem retornar identifiers de qualquer owner persistido na tabela do recurso. O serviço consumidor deve sempre aplicar sua regra de autorização/`ResourceVisibility` na consulta final antes de expor recursos ao cliente.

O vínculo também deve partir da entidade de domínio já carregada e validada pelo fluxo do consumidor. Não monte uma Tag diretamente a partir de um `ownerId` recebido no request. O fluxo esperado é: request -> autorização/finder -> owner existente e permitido -> `TagManager.reconcile(owner, ...)` -> FK do banco.

A FK garante integridade referencial e `UNIQUE(owner_id, name)` impede duplicidade inclusive sob concorrência. Conflitos concorrentes de constraint devem ser tratados pelo padrão de exceções do serviço consumidor; o módulo não adiciona lock pessimista nem cria uma transação própria.

## Índices mínimos recomendados

Cada tabela de tags deve manter:

```sql
UNIQUE (owner_id, name)
INDEX (name)
```

Em MySQL, `UNIQUE (owner_id, name)` já atende consultas cujo prefixo é `owner_id`; não é necessário criar outro índice apenas para `owner_id`. A FK e os nomes físicos continuam sob responsabilidade do recurso para preservar integridade referencial e permitir otimização específica.

`findManualByOwnerKeys` deve receber um conjunto previamente limitado/paginado pelo domínio consumidor; o módulo não transforma essa consulta em busca massiva nem adiciona paginação própria.

## Testes da library

O módulo mantém testes unitários do `TagManager` e do `TagName`. A integração real do repository deve ser validada contra a tecnologia de banco homologada pela plataforma; banco embarcado alternativo não faz parte deste módulo.
