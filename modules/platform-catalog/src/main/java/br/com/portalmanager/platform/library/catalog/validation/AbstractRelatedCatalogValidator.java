package br.com.portalmanager.platform.library.catalog.validation;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTOContract;
import br.com.portalmanager.platform.library.catalog.message.CatalogMessageKeys;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;

import java.util.Map;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;

/**
 * Base validator for catalogs that also depend on an active related catalog.
 * Catalog identity is always the immutable semantic code inherited from the
 * standard catalog contract; the relation is an additional domain rule.
 *
 * @param <D> catalog DTO
 * @param <R> related catalog key type
 */
public abstract class AbstractRelatedCatalogValidator<D extends CatalogDTOContract<D>, R>
        extends AbstractCatalogValidator<D> {

    protected AbstractRelatedCatalogValidator(CatalogRepository<?> repository) {
        super(repository);
    }

    @Override
    protected final void validateAdditionalCatalogFields(D dto, ValidationResult result) {
        R relation = relatedValue(dto);
        if (isMissing(relation)) {
            result.addError(
                    relatedField(),
                    CatalogMessageKeys.REQUIRED,
                    Map.of("0", relatedEntityName())
            );
            return;
        }

        if (!relatedExistsAndIsActive(relation)) {
            result.addError(
                    relatedField(),
                    CatalogMessageKeys.NOT_FOUND,
                    Map.of("0", relatedEntityName())
            );
            return;
        }

        validateRelatedFields(dto, relation, result);
    }

    protected boolean isMissing(R relation) {
        return relation == null || (relation instanceof String value && value.isBlank());
    }

    protected void validateRelatedFields(D dto, R relation, ValidationResult result) {
    }

    protected abstract R relatedValue(D dto);
    protected abstract String relatedField();
    protected abstract String relatedEntityName();
    protected abstract boolean relatedExistsAndIsActive(R relation);
}
