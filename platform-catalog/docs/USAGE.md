# Como usar o `platform-catalog`

Este guia mostra como um microserviço pode reutilizar o módulo `platform-catalog` para implementar catálogos persistidos e administráveis sem duplicar infraestrutura de CRUD, ordenação, ativação/desativação, restore e validações comuns.

## 1. Quando usar

O módulo foi desenhado para dois tipos de catálogo persistido:

- **MANAGED**: o banco é a fonte de verdade e novos valores podem ser cadastrados em runtime.
- **MANAGED_CONSTRAINED**: o catálogo continua persistido e administrável, mas o campo `name` é limitado a valores conhecidos no código por meio de um `enum`.

Catálogos **STATIC**, representados apenas por `enum` e sem tabela/CRUD, não precisam desta infraestrutura.

## 2. Adicionar a dependência

Quando o serviço usa o parent/dependency management da plataforma, basta adicionar:

```xml
<dependency>
    <groupId>com.empresa.platform</groupId>
    <artifactId>platform-catalog</artifactId>
</dependency>
```

A versão deve ser controlada pela infraestrutura Maven da plataforma. O serviço consumidor não deve fixar uma versão diferente daquela gerenciada pelo `platform-parent`/dependency management.

O módulo já depende das abstrações necessárias de Spring Data JPA, Spring Web e `platform-messaging`.

## 3. Estrutura recomendada no serviço

O microserviço continua sendo dono do catálogo concreto. Para um catálogo `OperationType`, por exemplo:

```text
core/
└── catalog/
    └── operation/
        ├── OperationType.java
        ├── OperationTypeDTO.java
        ├── OperationTypeRepository.java
        ├── OperationTypeMapper.java
        ├── OperationTypeValidator.java
        └── OperationTypeService.java

entrypoint/
└── web/
    └── catalog/
        └── OperationTypeController.java
```

A lib fornece apenas a infraestrutura base. A entidade, tabela, regras específicas, autorização e ownership continuam no serviço.

## 4. Criar a entidade

A entidade concreta deve estender `BaseCatalogEntity`.

```java
package com.empresa.event.core.catalog.operation;

import com.empresa.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "operation_type",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_operation_type_name",
                columnNames = "name"
        )
)
public class OperationType extends BaseCatalogEntity {
}
```

`BaseCatalogEntity` fornece:

```text
id          Long
name        String
label       String
description String
sortOrder   Integer
active      boolean
settings    String
```

### Importante: unicidade

A lib **não impõe mais `unique = true` no campo `name`**. A restrição física deve refletir a regra real do catálogo e ser definida pelo serviço, preferencialmente também na migration.

Para um catálogo global por tabela:

```sql
CREATE UNIQUE INDEX uk_operation_type_name
    ON operation_type (name);
```

Para um catálogo cuja unicidade seja por conta:

```sql
CREATE UNIQUE INDEX uk_operation_type_account_name
    ON operation_type (account_id, name);
```

Assim a infraestrutura não força uma regra de domínio que pode variar entre serviços.

## 5. Criar o DTO

O identificador padrão da biblioteca é `Long`.

```java
package com.empresa.event.core.catalog.operation;

import com.empresa.platform.catalog.dto.BaseCatalogDTO;
import tools.jackson.databind.JsonNode;

public record OperationTypeDTO(
        Long id,
        String name,
        String label,
        String description,
        Integer sortOrder,
        JsonNode settings
) implements BaseCatalogDTO<OperationTypeDTO> {

    @Override
    public OperationTypeDTO withId(Long id) {
        return new OperationTypeDTO(
                id,
                name,
                label,
                description,
                sortOrder,
                settings
        );
    }
}
```

Toda a API pública do `platform-catalog` usa `Long` como identificador. Isso é intencional e acompanha o identificador técnico de `BaseCatalogEntity`.

## 6. Criar o repository

```java
package com.empresa.event.core.catalog.operation;

import com.empresa.platform.catalog.repository.BaseCatalogRepository;

public interface OperationTypeRepository
        extends BaseCatalogRepository<OperationType> {
}
```

O repository base já fornece operações como:

```text
findByNameAndActiveTrue
findByIdAndActiveTrue
findByIdAndActiveFalse
findByActive
existsByNameAndIdNot
findFirstByOrderBySortOrderDesc
findFirstByIdNotOrderBySortOrderDesc
findByNameInAndActiveTrue
```

O serviço pode adicionar queries específicas normalmente.

## 7. Criar o mapper

```java
package com.empresa.event.core.catalog.operation;

import com.empresa.platform.catalog.mapper.BaseCatalogMapper;
import tools.jackson.databind.ObjectMapper;

import static java.util.Objects.isNull;

public class OperationTypeMapper
        extends BaseCatalogMapper<OperationTypeDTO, OperationType> {

    private final ObjectMapper objectMapper;

    public OperationTypeMapper(ObjectMapper objectMapper) {
        super(OperationType.class);
        this.objectMapper = objectMapper;
    }

    @Override
    public OperationTypeDTO toDTO(OperationType entity) {
        if (entity == null) {
            return null;
        }

        return new OperationTypeDTO(
                entity.getId(),
                entity.getName(),
                entity.getLabel(),
                entity.getDescription(),
                entity.getSortOrder(),
                isNull(entity.getSettings())
                        ? null
                        : objectMapper.readTree(entity.getSettings())
        );
    }
}
```

A base implementa `toEntity` e `updateEntity` para os campos comuns. O mapper concreto continua responsável pela conversão de saída e por campos adicionais do catálogo.

## 8. Criar o validator para um catálogo MANAGED

Para um catálogo dinâmico, estenda `BaseCatalogValidator`.

```java
package com.empresa.event.core.catalog.operation;

import com.empresa.platform.catalog.validation.BaseCatalogValidator;

public class OperationTypeValidator
        extends BaseCatalogValidator<OperationTypeDTO> {

    public OperationTypeValidator(OperationTypeRepository repository) {
        super(repository);
    }

    @Override
    public String entityName() {
        return "OperationType";
    }
}
```

A validação base já cobre os atributos comuns e, por padrão, considera `name` único na tabela.

## 9. Customizar a unicidade

Quando a identidade lógica depende de um escopo, o serviço deve sobrescrever `validateUniqueness`.

Exemplo: `(account_id, name)`.

Repository:

```java
public interface OperationTypeRepository
        extends BaseCatalogRepository<OperationType> {

    boolean existsByAccountIdAndNameAndIdNot(
            Long accountId,
            String name,
            Long id
    );
}
```

DTO concreto pode possuir o campo adicional:

```java
Long accountId
```

Validator:

```java
public class OperationTypeValidator
        extends BaseCatalogValidator<OperationTypeDTO> {

    private final OperationTypeRepository operationTypeRepository;

    public OperationTypeValidator(OperationTypeRepository repository) {
        super(repository);
        this.operationTypeRepository = repository;
    }

    @Override
    protected void validateUniqueness(
            OperationTypeDTO dto,
            ValidationResult result
    ) {
        long id = dto.id() == null ? 0L : dto.id();

        if (operationTypeRepository.existsByAccountIdAndNameAndIdNot(
                dto.accountId(),
                dto.name(),
                id
        )) {
            result.addError(
                    "name",
                    CatalogMessageKeys.NAME_DUPLICATE,
                    Map.of("0", entityName(), "1", dto.name())
            );
        }
    }

    @Override
    public String entityName() {
        return "OperationType";
    }
}
```

A constraint no banco deve usar a mesma regra. A validação Java melhora a resposta ao consumidor, mas não substitui a constraint de integridade no banco.

## 10. Criar o service

```java
package com.empresa.event.core.catalog.operation;

import com.empresa.platform.catalog.service.BaseCatalogService;

public class OperationTypeService
        extends BaseCatalogService<OperationType, OperationTypeDTO> {

    public OperationTypeService(
            OperationTypeRepository repository,
            OperationTypeMapper mapper,
            OperationTypeValidator validator
    ) {
        super(repository, mapper, validator);
    }
}
```

A implementação recebe automaticamente:

```text
findAll
findById
findByName
create
update
delete
restore
findByNames
```

Campos adicionais podem ser tratados sobrescrevendo `applyAdditionalFields`.

Filtros adicionais da listagem podem ser tratados sobrescrevendo `matchesAdditionalFilters`.

## 11. Criar o controller

```java
package com.empresa.event.entrypoint.web.catalog;

import com.empresa.event.core.catalog.operation.OperationType;
import com.empresa.event.core.catalog.operation.OperationTypeDTO;
import com.empresa.event.core.catalog.operation.OperationTypeService;
import com.empresa.platform.catalog.service.BaseCatalogService;
import com.empresa.platform.catalog.web.BaseCatalogController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalogs/operation-types")
public class OperationTypeController
        extends BaseCatalogController<OperationTypeDTO, OperationType> {

    private final OperationTypeService service;

    public OperationTypeController(OperationTypeService service) {
        this.service = service;
    }

    @Override
    protected BaseCatalogService<OperationType, OperationTypeDTO> getService() {
        return service;
    }
}
```

O controller base expõe:

```text
GET    /api/v1/catalogs/operation-types
GET    /api/v1/catalogs/operation-types/{id}
POST   /api/v1/catalogs/operation-types
PUT    /api/v1/catalogs/operation-types/{id}
DELETE /api/v1/catalogs/operation-types/{id}
POST   /api/v1/catalogs/operation-types/{id}/restore
```

A autorização continua sendo responsabilidade do serviço consumidor. Por exemplo, a aplicação pode colocar sua anotação de autorização no controller concreto sem que a biblioteca imponha `OWNER` ou qualquer papel específico.

## 12. Usar MANAGED_CONSTRAINED

Quando o catálogo precisa continuar limitado a valores conhecidos no código, declare um enum:

```java
public enum OperationTypeEnum implements CatalogEnum<OperationTypeEnum> {
    CREATE,
    UPDATE,
    DELETE,
    PUBLISH
}
```

E substitua o validator base por `EnumCatalogValidator`:

```java
public class OperationTypeValidator
        extends EnumCatalogValidator<OperationTypeEnum, OperationTypeDTO> {

    public OperationTypeValidator(OperationTypeRepository repository) {
        super(repository, OperationTypeEnum.class);
    }

    @Override
    public String entityName() {
        return "OperationType";
    }
}
```

Nesse modelo, a tabela continua existindo e o catálogo continua administrável, mas um `name` fora do enum é rejeitado.

## 13. Campos adicionais

Um catálogo pode possuir campos próprios além dos campos da base.

Exemplo:

```text
FeatureType
- id
- name
- label
- description
- sortOrder
- active
- settings
- scope
```

A entidade e o DTO concretos adicionam `scope`, e o service pode preencher o campo:

```java
@Override
protected void applyAdditionalFields(FeatureType entity, FeatureTypeDTO dto) {
    entity.setScope(dto.scope());
}
```

Validações podem ser adicionadas usando os hooks:

```text
validateSettings
validateAdditionalCatalogFields
validateAdditionalIntegrity
validateUniqueness
```

## 14. Filtros adicionais

O controller base encaminha parâmetros extras para o service. Um catálogo que suporte `scope`, por exemplo, pode sobrescrever:

```java
@Override
protected boolean matchesAdditionalFilters(
        FeatureTypeDTO dto,
        Map<String, String> filters
) {
    String scope = filters.get("scope");
    return scope == null || scope.equals(dto.scope());
}
```

Assim uma chamada como:

```text
GET /api/v1/catalogs/feature-types?active=true&scope=ACCOUNT
```

continua usando a infraestrutura comum sem adicionar conhecimento de `scope` na biblioteca.

## 15. Migration da tabela

A biblioteca não cria tabelas de catálogo automaticamente. O serviço consumidor continua responsável pela migration.

Exemplo mínimo:

```sql
CREATE TABLE operation_type (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    label VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order INT NOT NULL,
    is_active BIT NOT NULL,
    settings TEXT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_operation_type_name UNIQUE (name)
);
```

A migration deve ser ajustada quando houver colunas ou escopos adicionais.

## 16. Checklist de implementação

Para um novo catálogo persistido:

1. classifique o catálogo como `MANAGED` ou `MANAGED_CONSTRAINED`;
2. adicione `platform-catalog` ao `pom.xml`;
3. crie a migration e defina explicitamente a regra de unicidade;
4. crie a entidade estendendo `BaseCatalogEntity`;
5. crie o DTO implementando `BaseCatalogDTO<DTO>`;
6. crie o repository estendendo `BaseCatalogRepository<Entity>`;
7. crie o mapper estendendo `BaseCatalogMapper<DTO, Entity>`;
8. crie o validator usando `BaseCatalogValidator` ou `EnumCatalogValidator`;
9. sobrescreva `validateUniqueness` se a unicidade depender de escopo;
10. crie o service estendendo `BaseCatalogService<Entity, DTO>`;
11. crie o controller estendendo `BaseCatalogController<DTO, Entity>`;
12. aplique no controller concreto a autorização exigida pelo serviço;
13. adicione testes do catálogo concreto, principalmente regras específicas e constraints;
14. execute `mvn clean verify` no serviço consumidor.

## 17. O que não deve ir para a lib

O `platform-catalog` não deve conhecer:

```text
AccountType
EnvironmentType
FeatureType
OperationType
scope
accountId
applicationId
roles/grupos de autorização específicos
valores de negócio de enums concretos
```

Regra de ownership:

```text
platform-catalog = COMO um catálogo administrável funciona
microserviço     = QUAL catálogo existe e quais são suas regras de domínio
```

Esse limite é o que permite que a mesma infraestrutura seja reutilizada por `account-api`, `event-api`, `route-api` e outros serviços sem transformar a biblioteca em um repositório central de regras de negócio.
