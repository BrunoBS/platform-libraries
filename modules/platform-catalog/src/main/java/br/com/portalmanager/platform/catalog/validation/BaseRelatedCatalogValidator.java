package br.com.portalmanager.platform.catalog.validation;

import br.com.portalmanager.platform.catalog.dto.BaseCatalogDTO;
import br.com.portalmanager.platform.catalog.message.CatalogMessageKeys;
import br.com.portalmanager.platform.catalog.repository.BaseCatalogRepository;

import java.util.Map;

/**
 * Base validator for MANAGED catalogs whose logical identity depends on a
 * related catalog, such as (scope, name).
 *
 * @param <D> catalog DTO
 * @param <R> related catalog key type (for example Long id or String name)
 */
public abstract class BaseRelatedCatalogValidator<D extends BaseCatalogDTO<D>, R>
        extends BaseCatalogValidator<D> {

    protected BaseRelatedCatalogValidator(BaseCatalogRepository<?> repository) {
        super(repository);
    }

    @Override
    protected final void validateAdditionalCatalogFields(D dto, CatalogValidationResult result) {
        R relation = relatedValue(dto);
        if (isMissing(relation)) {
            result.addError(relatedField(), CatalogMessageKeys.REQUIRED,
                    Map.of("0", relatedEntityName()));
            return;
        }
        if (!relatedExistsAndIsActive(relation)) {
            result.addError(relatedField(), CatalogMessageKeys.NOT_FOUND,
                    Map.of("0", relatedEntityName()));
            return;
        }
        validateRelatedFields(dto, relation, result);
    }

    @Override
    protected final void validateUniqueness(D dto, CatalogValidationResult result) {
        R relation = relatedValue(dto);
        if (isMissing(relation) || dto.name() == null || dto.name().isBlank()) {
            return;
        }
        long id = dto.id() == null ? 0L : dto.id();
        if (existsByNameAndRelatedValue(dto.name(), relation, id)) {
            result.addError("name", CatalogMessageKeys.NAME_DUPLICATE,
                    Map.of("0", entityName(), "1", dto.name() + "/" + relation));
        }
    }

    protected boolean isMissing(R relation) {
        return relation == null || (relation instanceof String value && value.isBlank());
    }

    protected void validateRelatedFields(D dto, R relation, CatalogValidationResult result) {
    }

    protected abstract R relatedValue(D dto);

    protected abstract String relatedField();

    protected abstract String relatedEntityName();

    protected abstract boolean relatedExistsAndIsActive(R relation);

    protected abstract boolean existsByNameAndRelatedValue(String name, R relation, long id);
}
