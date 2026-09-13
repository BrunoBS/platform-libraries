# platform-crud

Infraestrutura CRUD genérica, fortemente tipada e sem conhecimento de atributos de domínio.

A biblioteca abstrai o ciclo de vida básico de CRUD sem impor campos como `name`, `description`, `active`, `accountId` ou qualquer outro atributo de negócio.

## Objetivo

Padronizar a estrutura comum de recursos CRUD nos serviços da plataforma:

- DTO
- Repository
- Mapper
- Validator
- Service
- Controller

O serviço consumidor continua responsável por suas entidades, atributos, constraints, regras de negócio, autorização, semântica de erros e efeitos colaterais.

## Dependência

```xml
<dependency>
    <groupId>com.empresa.platform</groupId>
    <artifactId>platform-crud</artifactId>
</dependency>
```

A versão deve ser gerenciada pelo `platform-parent`/`dependencyManagement` da plataforma.

## Estrutura recomendada no serviço

```text
core/customer/
├── Customer.java
├── CustomerDTO.java
├── CustomerRepository.java
├── CustomerMapper.java
├── CustomerValidator.java
└── CustomerService.java

entrypoint/web/customer/
└── CustomerController.java
```

## 1. DTO

O contrato base exige somente um identificador e a capacidade de devolver uma cópia com outro ID.

```java
public record CustomerDTO(
        Long id,
        String name,
        String email
) implements BaseCrudDTO<Long, CustomerDTO> {

    @Override
    public CustomerDTO withId(Long id) {
        return new CustomerDTO(id, name, email);
    }
}
```

O framework usa `withId(null)` no create e `withId(pathId)` no update. Assim, o ID do body nunca prevalece sobre o contrato da operação.

## 2. Repository

```java
public interface CustomerRepository
        extends BaseCrudRepository<Customer, Long> {
}
```

Consultas específicas continuam no repository concreto.

## 3. Mapper

```java
@Component
public class CustomerMapper
        implements BaseCrudMapper<Customer, CustomerDTO> {

    @Override
    public Customer toEntity(CustomerDTO dto) {
        Customer entity = new Customer();
        entity.setName(dto.name());
        entity.setEmail(dto.email());
        return entity;
    }

    @Override
    public CustomerDTO toDTO(Customer entity) {
        return new CustomerDTO(
                entity.getId(),
                entity.getName(),
                entity.getEmail());
    }

    @Override
    public void updateEntity(Customer entity, CustomerDTO dto) {
        entity.setName(dto.name());
        entity.setEmail(dto.email());
    }
}
```

A base não usa reflection e não conhece atributos da entidade.

## 4. Validator

```java
@Component
public class CustomerValidator
        extends BaseCrudValidator<CustomerDTO, Long> {

    @Override
    protected void validateAttributes(
            CustomerDTO dto,
            ValidationResult result) {
        if (dto.name() == null || dto.name().isBlank()) {
            result.addError("name", "customer.name.required");
        }
    }

    @Override
    public String entityName() {
        return "customer";
    }
}
```

Hooks disponíveis:

```text
validateAttributes
validateCreateIntegrity
validateUpdateIntegrity
validateDelete
validateAdditionalCreate
validateAdditionalUpdate
```

## 5. Service

```java
@Service
public class CustomerService
        extends BaseCrudService<Customer, CustomerDTO, Long> {

    public CustomerService(
            CustomerRepository repository,
            CustomerMapper mapper,
            CustomerValidator validator) {
        super(repository, mapper, validator);
    }

    @Override
    protected RuntimeException notFoundException(Long id) {
        return new CustomerNotFoundException(id);
    }
}
```

A `platform-crud` detecta que o recurso não foi encontrado, mas não define qual exception deve representar essa condição. O consumidor fornece a exception adequada por meio de `notFoundException(ID id)`. Assim, mensagens, códigos, internacionalização e semântica da aplicação não vazam para a infraestrutura CRUD.

O service base fornece:

```text
findAll
findById
create
update
delete
```

Hooks de ciclo de vida:

```text
beforeCreate
applyCreate
afterCreate
beforeUpdate
applyUpdate
afterUpdate
beforeDelete
afterDelete
```

O delete padrão é físico. Para soft delete, sobrescreva `deleteEntity` no service concreto.

Exemplo:

```java
@Override
protected void deleteEntity(Customer entity) {
    entity.setActive(false);
    repository().save(entity);
}
```

## 6. Controller

```java
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController
        extends BaseCrudController<Customer, CustomerDTO, Long> {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @Override
    protected CustomerService service() {
        return service;
    }
}
```

Endpoints fornecidos:

```text
GET    /
GET    /{id}
POST   /
PUT    /{id}
DELETE /{id}
```

Autorização continua sendo responsabilidade do controller concreto.

## Responsabilidades

### platform-crud

- fluxo CRUD comum
- normalização de ID
- integração genérica repository/mapper/validator
- transações do fluxo básico
- detecção de recurso ausente
- hooks de ciclo de vida
- controller HTTP básico

### serviço consumidor

- entidade e seus atributos
- migrations e constraints
- regras de negócio
- consultas adicionais
- autorização
- soft delete, quando necessário
- exception e semântica de recurso não encontrado
- mensagens específicas
- integrações e efeitos colaterais

## Relação com platform-catalog

A `platform-catalog` pode especializar a `platform-crud` preservando seu contrato público. O CRUD fornece o fluxo comum; catálogo acrescenta semânticas como `active`, restore, ordenação e filtros.

## Princípio

A biblioteca abstrai o comportamento do CRUD, não o domínio.

Não usar `Map<String, Object>`, `JsonNode` ou reflection como mecanismo para representar entidades arbitrárias. O objetivo é preservar tipagem forte e deixar cada serviço dono do seu modelo.
